// Joystick checks in a real browser (Playwright, Chromium). The joystick is the only control that turns the world.
// A tap turns nothing, and so does a small push. Past half of the travel, the half pushed toward shows where the ball
// would go, and letting go turns that way. Bringing the knob back to the middle, or a cancel from the system, turns
// nothing. The knob springs back to the middle, the joystick is off while the world turns, and the board and the dial
// take no turns. Also saves two pictures (at rest, and pulled) to look at.
const { chromium } = require('playwright');
const fs = require('fs');
const path = require('path');
const CORE = require('../src/core.js');
const BUILD = path.join(__dirname, '..', 'build');
const FILE = 'file://' + path.join(BUILD, 'test.html');
const LEVELS = JSON.parse(fs.readFileSync(path.join(__dirname, '..', 'data', 'levels.json'), 'utf8'));

const results = [];
function check(label, ok, detail) {
  results.push(ok);
  console.log((ok ? 'PASS ' : 'FAIL ') + label + (detail !== undefined ? '  [' + detail + ']' : ''));
}
const PREVIEW = { win: 'Into the hole!', pit: 'Into a pit!', mud: 'Mud', wall: 'Wall' };
// Where one turn from the start of a level takes the ball: dir 1 is right, -1 is left.
function oneTurn(rows, dir) {
  const P = CORE.parse(rows);
  const s0 = CORE.slide(P.grid, P.n, P.start[0], P.start[1], 0);
  return CORE.slide(P.grid, P.n, s0.x, s0.y, (dir + 4) % 4);
}

(async () => {
  const browser = await chromium.launch();
  const page = await browser.newPage({ viewport: { width: 390, height: 844 }, deviceScaleFactor: 2 });
  const errs = [];
  page.on('pageerror', e => errs.push('pageerror: ' + e.message));
  page.on('console', m => { if (m.type() === 'error' && !/ERR_TUNNEL|fonts\.g/.test(m.text())) errs.push('console: ' + m.text()); });
  await page.goto(FILE);
  await page.waitForTimeout(300);
  await page.evaluate(() => { try { localStorage.clear(); } catch (e) {} });
  await page.reload();
  await page.waitForTimeout(300);

  const txt = id => page.textContent('#' + id);
  const clsOf = id => page.evaluate(i => document.getElementById(i).getAttribute('class') || '', id);
  const knob = () => page.evaluate(() => document.getElementById('joy-knob').style.transform);
  const knobX = async () => { const m = (await knob()).match(/translate\(([-\d.]+)px/); return m ? parseFloat(m[1]) : null; };
  const ghost = () => page.evaluate(() => { const g = document.querySelector('#fx .ghost'); return g ? g.getAttribute('class') : null; });
  const stage = () => page.evaluate(() => document.getElementById('stage').style.transform);
  const isOff = () => page.evaluate(() => document.getElementById('joy').classList.contains('off'));
  const center = async () => { const b = await page.locator('#joy').boundingBox(); return { x: b.x + b.width / 2, y: b.y + b.height / 2 }; };
  const settled = () => page.waitForFunction(() => !document.getElementById('joy').classList.contains('off'), null, { timeout: 8000 });

  // A. the joystick is the only control; the board and the dial turn nothing
  const gone = await page.evaluate(() => ['turn-row', 'turn-r', 'turn-l', 'dial', 'ctl-board', 'ctl-dial', 'ctl-buttons', 'hand', 'hand-row', 'control-note'].filter(id => document.getElementById(id)));
  check('A1 the joystick is on screen, with the knob in the middle', (await page.isVisible('#joy')) && (await knob()) === '');
  check('A2 no other turn control is left', gone.length === 0, gone.join(', ') || 'none');
  check('A3 on the first visit the knob invites a push', (await clsOf('joy')).includes('teach'), await clsOf('joy'));
  {
    const b = await page.locator('#board-card').boundingBox();
    const y = b.y + b.height / 2;
    await page.mouse.click(b.x + b.width * 0.25, y);
    await page.waitForTimeout(200);
    check('A4 a tap on the left half of the board turns nothing', (await txt('turns')) === '0' && (await ghost()) === null && (await stage()) === 'rotate(0deg)');
    await page.mouse.move(b.x + b.width * 0.75, y);
    await page.mouse.down();
    await page.mouse.move(b.x + b.width * 0.75 - 60, y, { steps: 8 });
    await page.mouse.up();
    await page.waitForTimeout(200);
    check('A5 a swipe on the board turns nothing', (await txt('turns')) === '0' && (await stage()) === 'rotate(0deg)', await txt('turns'));
  }

  // B. a tap turns nothing, wherever on the joystick it lands
  {
    const c = await center();
    for (const dx of [-40, 40]) {
      await page.mouse.move(c.x + dx, c.y);
      await page.mouse.down();
      await page.mouse.up();
      await page.waitForTimeout(150);
    }
    check('B1 a tap on either side of the joystick turns nothing', (await txt('turns')) === '0' && (await stage()) === 'rotate(0deg)', await txt('turns'));
  }

  // C. a small push moves the knob with the finger, shows nothing, and turns nothing
  {
    const c = await center();
    const hint = await txt('msg');
    await page.mouse.move(c.x, c.y);
    await page.mouse.down();
    await page.mouse.move(c.x + 10, c.y, { steps: 3 });
    const x = await knobX();
    check('C1 a small push moves the knob with the finger', x !== null && Math.abs(x - 10) < 0.5, await knob());
    check('C2 a small push shows no path, no tint and no outcome', !(await clsOf('joy')).includes('pull-r') && (await ghost()) === null && (await txt('msg')) === hint, await txt('msg'));
    await page.mouse.up();
    await page.waitForTimeout(150);
    check('C3 letting go of a small push turns nothing, and the knob goes back to the middle', (await txt('turns')) === '0' && (await knob()) === '');
  }

  // D. past half of the travel the half pushed toward shows where the ball would go; letting go turns that way
  {
    const c = await center();
    const exp = oneTurn(LEVELS[0].rows, 1);
    const top0 = (await page.locator('#board-card').boundingBox()).y;
    await page.mouse.move(c.x, c.y);
    await page.mouse.down();
    await page.mouse.move(c.x + 200, c.y, { steps: 10 });
    const x = await knobX();
    check('D1 the knob stops at the edge of its travel', x !== null && x > 100 && x <= 112.5, await knob());
    check('D2 the right half is tinted', (await clsOf('joy')).includes('pull-r'));
    check('D3 the path and the outcome show while held', (await ghost()) === 'ghost ' + exp.end && (await txt('msg')) === PREVIEW[exp.end], (await ghost()) + ' | ' + (await txt('msg')));
    check('D4 holding does not turn yet', (await txt('turns')) === '0');
    const top1 = (await page.locator('#board-card').boundingBox()).y;
    check('D5 the world does not move while the joystick is pulled', Math.abs(top1 - top0) < 0.5, top0 + ' vs ' + top1);
    const onTop = await page.evaluate(() => {
      const k = document.getElementById('joy-knob').getBoundingClientRect();
      const el = document.elementFromPoint(k.left + k.width / 2, k.top + k.height / 2);
      return el ? el.id : null;
    });
    check('D6 the knob is on top of the tint, so it can be seen', onTop === 'joy-knob', onTop);
    await page.mouse.up();
    await settled();
    check('D7 letting go turns to the right', (await txt('turns')) === '1' && (await stage()) === 'rotate(90deg)', (await txt('turns')) + ' ' + (await stage()));
    check('D8 the path, the tint and the knob are cleared', (await ghost()) === null && !(await clsOf('joy')).includes('pull') && (await knob()) === '');
  }

  // E. bringing the knob back to the middle, or a cancel from the system, turns nothing
  {
    const c = await center();
    await page.mouse.move(c.x, c.y);
    await page.mouse.down();
    await page.mouse.move(c.x - 70, c.y, { steps: 4 });
    check('E1 pulled left, the left half is tinted', (await clsOf('joy')).includes('pull-l'));
    await page.mouse.move(c.x, c.y, { steps: 4 });
    check('E2 back in the middle, the tint and the path are gone', !(await clsOf('joy')).includes('pull-l') && (await ghost()) === null);
    await page.mouse.up();
    await page.waitForTimeout(150);
    check('E3 letting go in the middle turns nothing', (await txt('turns')) === '1' && (await stage()) === 'rotate(90deg)', await txt('turns'));
    await page.mouse.move(c.x, c.y);
    await page.mouse.down();
    await page.mouse.move(c.x - 70, c.y, { steps: 4 });
    await page.evaluate(() => document.getElementById('joy').dispatchEvent(new PointerEvent('pointercancel', { pointerId: 1, bubbles: true })));
    check('E4 a cancel turns nothing, and clears the tint and the knob', (await txt('turns')) === '1' && !(await clsOf('joy')).includes('pull-l') && (await knob()) === '' && (await ghost()) === null);
    await page.mouse.up();
    await page.waitForTimeout(150);
    check('E5 the joystick works again after a cancel', !(await isOff()));
  }

  // F. while the world turns the joystick is off, and a press is ignored; the turn goes the way it was pulled
  {
    const c = await center();
    await page.mouse.move(c.x, c.y);
    await page.mouse.down();
    await page.mouse.move(c.x - 70, c.y, { steps: 4 });
    await page.mouse.up();
    check('F1 while the world turns, the joystick is off', await isOff());
    await page.mouse.move(c.x, c.y);
    await page.mouse.down();
    await page.mouse.move(c.x + 70, c.y, { steps: 4 });
    const k = await knob();
    const tinted = (await clsOf('joy')).includes('pull-r');
    await page.mouse.up();
    check('F2 a press while the world turns is ignored', k === '' && !tinted, k);
    await settled();
    check('F3 the turn was to the left, and only one', (await txt('turns')) === '2' && (await stage()) === 'rotate(0deg)', (await txt('turns')) + ' ' + (await stage()));
  }

  // G. the pictures: the joystick at rest, and pulled to the right with the path showing
  await page.reload();
  await page.waitForTimeout(300);
  await page.screenshot({ path: path.join(BUILD, 'shot_joy_rest.png') });
  {
    const c = await center();
    await page.mouse.move(c.x, c.y);
    await page.mouse.down();
    await page.mouse.move(c.x + 200, c.y, { steps: 10 });
    await page.waitForTimeout(100);
    check('G1 pulled to the right, the path shows on the board', (await ghost()) === 'ghost ' + oneTurn(LEVELS[0].rows, 1).end);
    await page.screenshot({ path: path.join(BUILD, 'shot_joy_pulled.png') });
    await page.mouse.up();
  }
  await settled();

  check('Z1 no page errors', errs.length === 0, JSON.stringify(errs));
  console.log('\n' + results.filter(Boolean).length + ' of ' + results.length + ' checks passed');
  await browser.close();
})().catch(e => { console.error('FAILED', e); process.exit(1); });
