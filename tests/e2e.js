// End-to-end checks in a real browser (Playwright, Chromium). The joystick turns the world, so every turn here is a pull
// past half of its travel and a release (joy.js checks the joystick itself). Plays the designed levels with their
// solutions, then the levels past them (made from their index). Checks the undo, the keyboard, pits and mud, the chapter
// map, the level sheet, the daily board, a deep level compared with Node, the settings and persistence.
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
// The settled start of a level, and where one turn would take the ball.
function startOf(rows) {
  const P = CORE.parse(rows);
  const s0 = CORE.slide(P.grid, P.n, P.start[0], P.start[1], 0);
  return { P, s0 };
}
// The first turn sequence (up to maxDepth) whose last turn ends in the wanted way; earlier turns must not win or fall.
function probe(rows, want, maxDepth) {
  const { P, s0 } = startOf(rows);
  function dfs(x, y, g, moves) {
    if (moves.length >= maxDepth) return null;
    for (const [ch, d] of [['R', 1], ['L', 3]]) {
      const ng = (g + d) % 4;
      const r = CORE.slide(P.grid, P.n, x, y, ng);
      if (!r.moved) continue;
      if (r.end === want) return moves + ch;
      if (r.end === 'wall' || r.end === 'mud') {
        const deeper = dfs(r.x, r.y, ng, moves + ch);
        if (deeper) return deeper;
      }
    }
    return null;
  }
  return dfs(s0.x, s0.y, 0, '');
}
const kickerOf = i => 'Level ' + (i + 1);

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
  const ballCls = () => page.getAttribute('#ball-body', 'class');
  // The joystick is on again once the turn has finished.
  const ready = () => page.waitForFunction(() => !document.getElementById('joy').classList.contains('off'), null, { timeout: 8000 });
  const center = async () => { const b = await page.locator('#joy').boundingBox(); return { x: b.x + b.width / 2, y: b.y + b.height / 2 }; };
  // Pulls the joystick past half of its travel to one side (dir 1 right, -1 left), then lets go: the turn that way.
  async function pull(dir) {
    const c = await center();
    await page.mouse.move(c.x, c.y);
    await page.mouse.down();
    await page.mouse.move(c.x + dir * 70, c.y, { steps: 4 });
    await page.mouse.up();
  }
  const press = async ch => { await ready(); await pull(ch === 'R' ? 1 : -1); };
  // The board as it is drawn: '#' wall, '.' floor, 'm' mud, 'p' pit, 'O' hole. The ball's start is a floor cell.
  const boardRows = () => page.evaluate(() => {
    const cells = {};
    const set = (x, y, c) => { cells[x + ',' + y] = c; };
    document.querySelectorAll('#world rect').forEach(r => {
      const cls = r.getAttribute('class') || '';
      const x = Math.round(parseFloat(r.getAttribute('x')) - (cls.indexOf('lip') >= 0 || cls.indexOf('wall') >= 0 ? 0.04 : 0.05));
      const y = Math.round(parseFloat(r.getAttribute('y')) - (cls.indexOf('lip') >= 0 ? 0.1 : cls.indexOf('wall') >= 0 ? 0.03 : 0.05));
      if (cls.indexOf('wall') >= 0) set(x, y, '#');
      else if (cls.indexOf('mud') >= 0) set(x, y, 'm');
      else if (cls.indexOf('pit') >= 0) set(x, y, 'p');
      else if (cls.indexOf('tile') >= 0) set(x, y, '.');
    });
    document.querySelectorAll('#world circle.hole').forEach(c => {
      set(Math.round(parseFloat(c.getAttribute('cx')) - 0.5), Math.round(parseFloat(c.getAttribute('cy')) - 0.5), 'O');
    });
    // arrows and springs: an arrow's tip is the point farthest from its cell's centre; a spring is a zigzag inside its cell
    document.querySelectorAll('#world polygon.arrow').forEach(p => {
      const pts = p.getAttribute('points').trim().split(/\s+/).map(q => q.split(',').map(Number));
      const ax = pts.reduce((t, q) => t + q[0], 0) / pts.length, ay = pts.reduce((t, q) => t + q[1], 0) / pts.length;
      const x = Math.floor(ax), y = Math.floor(ay), cx = x + 0.5, cy = y + 0.5;
      const tip = pts.reduce((best, q) => Math.hypot(q[0] - cx, q[1] - cy) > Math.hypot(best[0] - cx, best[1] - cy) ? q : best);
      const dx = tip[0] - cx, dy = tip[1] - cy;
      set(x, y, Math.abs(dx) > Math.abs(dy) ? (dx > 0 ? '>' : '<') : (dy > 0 ? 'v' : '^'));
    });
    document.querySelectorAll('#world polyline.spring').forEach(p => {
      const pts = p.getAttribute('points').trim().split(/\s+/).map(q => q.split(',').map(Number));
      const ax = pts.reduce((t, q) => t + q[0], 0) / pts.length, ay = pts.reduce((t, q) => t + q[1], 0) / pts.length;
      set(Math.floor(ax), Math.floor(ay), 'z');
    });
    const n = Math.sqrt(Object.keys(cells).length);
    const rows = [];
    for (let y = 0; y < n; y++) { let s = ''; for (let x = 0; x < n; x++) s += cells[x + ',' + y] || '?'; rows.push(s); }
    return rows;
  });

  // A. a fresh start: the first level, the rule and the first-time line, two undos, the joystick
  check('A1 starts on level 1, with the knob inviting a push', (await txt('kicker')) === 'Level 1' && (await page.getAttribute('#joy', 'class')).includes('teach'), await txt('kicker'));
  check('A2 there is no progress line', (await page.$('#progress')) === null);
  check('A3 undo label starts at two', (await txt('undo-label')) === '2');
  check('A4 the joystick is on screen, and there are no turn buttons', (await page.isVisible('#joy')) && (await page.$('#turn-row')) === null);

  // B. pull right and let go: one turn; pull left and let go: the next; the undos take them back
  await pull(1);
  await ready();
  check('B1 pulling right turns once', (await txt('turns')) === '1', await txt('turns'));
  await pull(-1);
  await ready();
  check('B2 pulling left turns again, and the level is not won yet', (await txt('turns')) === '2' && (await page.isHidden('#win')), await txt('turns'));
  await page.click('#undo');
  await ready();
  check('B3 the first undo', (await txt('turns')) === '1' && (await txt('undo-label')) === '1', await txt('undo-label'));
  await page.click('#undo');
  await ready();
  check('B4 the second undo', (await txt('turns')) === '0' && (await txt('undo-label')) === '0', await txt('undo-label'));
  check('B5 undo is off with nothing to undo', await page.isDisabled('#undo'));

  // C. keyboard: the arrows turn; z undoes while there are undos left
  await page.keyboard.press('ArrowRight');
  await ready();
  check('C1 arrow right turns', (await txt('turns')) === '1', await txt('turns'));
  await page.keyboard.press('z');
  await ready();
  check('C2 z does nothing once the undos are used up', (await txt('turns')) === '1', await txt('undo-label'));
  await page.click('#reset');
  await ready();
  check('C3 restart gives back the two undos', (await txt('undo-label')) === '2' && (await txt('turns')) === '0');
  await page.keyboard.press('ArrowLeft');
  await ready();
  await page.keyboard.press('z');
  await ready();
  check('C4 arrow left turns, and z takes it back', (await txt('turns')) === '0' && (await txt('undo-label')) === '1', await txt('undo-label'));

  // D. the campaign: every designed level with its solution; a mud probe in chapter two, a pit probe in chapter three
  await page.evaluate(() => localStorage.setItem('world-settings-v2', JSON.stringify({ speed: 'off' })));
  await page.reload();
  await page.waitForTimeout(300);
  let stars3 = 0;
  const mudAt = LEVELS.findIndex((L, i) => Math.floor(i / 6) === 1 && probe(L.rows, 'mud', 3));
  const pitAt = LEVELS.findIndex((L, i) => Math.floor(i / 6) === 2 && probe(L.rows, 'pit', 6));
  check('D-probes: a mud level and a pit level exist', mudAt >= 0 && pitAt >= 0, 'mud at ' + (mudAt + 1) + ', pit at ' + (pitAt + 1));
  for (let i = 0; i < LEVELS.length; i++) {
    const L = LEVELS[i];
    if (i > 0) {
      await page.click('#next');
      await page.waitForFunction(() => document.getElementById('win').hidden, null, { timeout: 4000 });
    }
    const kicker = await txt('kicker');
    if (i === mudAt) {
      // mud: the ball stops in it; then restart
      const seq = probe(L.rows, 'mud', 3) || '';
      for (const ch of seq) await press(ch);
      await page.waitForFunction(() => document.getElementById('ball-body').getAttribute('class') === 'mud', null, { timeout: 4000 });
      check('D-mud the ball stops on mud', seq.length > 0 && (await ballCls()) === 'mud' && (await txt('msg')) === 'Stuck in mud', await txt('msg'));
      await page.click('#reset');
      await ready();
    }
    if (i === pitAt) {
      // pit: the attempt fails and the joystick stops; undo brings the ball back
      const seq = probe(L.rows, 'pit', 6) || '';
      check('D-pit a pit can be reached', seq.length > 0, seq);
      for (const ch of seq) await press(ch);
      await page.waitForFunction(() => document.getElementById('ball-body').getAttribute('class') === 'drop', null, { timeout: 4000 });
      check('D-pit the ball falls in and the attempt stops', (await ballCls()) === 'drop' && (await txt('msg')).includes('Fell in a pit') && !(await page.isDisabled('#undo')), await txt('msg'));
      await page.click('#undo');
      await ready();
      check('D-pit undo brings the ball back', (await ballCls()) !== 'drop' && (await txt('undo-label')) === '1', await txt('undo-label'));
      await page.click('#reset');
      await ready();
    }
    for (const ch of L.sol) await press(ch);
    await page.waitForFunction(() => !document.getElementById('win').hidden, null, { timeout: 8000 });
    const stars = await txt('win-stars');
    if (stars === '★★★') stars3 += 1;
    else check('D-stars level ' + (i + 1) + ' is three stars', false, stars + ' ' + kicker);
    if (i === 5) {
      check('D-map chapter one: the map button appears when the chapter is done', (await page.getAttribute('#win-map', 'hidden')) === null);
      await page.click('#win-map');
      await page.waitForFunction(() => document.getElementById('map-img').naturalWidth > 0, null, { timeout: 8000 });
      const w = await page.evaluate(() => document.getElementById('map-img').naturalWidth);
      check('D-map picture drawn', w === 720, 'width ' + w);
      await page.screenshot({ path: path.join(BUILD, 'shot_map.png') });
      await page.keyboard.press('Escape');
      check('D-map closes with Escape', await page.isHidden('#map'));
    }
  }
  check('D-all ' + LEVELS.length + ' designed levels solved in three stars', stars3 === LEVELS.length, stars3);

  // G. past the designed levels the campaign goes on: each level is made from its index, and it is solvable
  for (const i of [108, 109, 110, 111, 112, 113]) {
    await page.click('#next');
    await page.waitForFunction(() => document.getElementById('win').hidden, null, { timeout: 4000 });
    const E = CORE.endlessLevel(i);
    check('G level ' + (i + 1) + ' is ' + kickerOf(i) + ' with the target from its index', (await txt('kicker')) === kickerOf(i) && (await txt('par')) === String(E.par), (await txt('kicker')) + ' target ' + (await txt('par')));
    for (const ch of E.moves) await press(ch);
    await page.waitForFunction(() => !document.getElementById('win').hidden, null, { timeout: 8000 });
    check('G level ' + (i + 1) + ' is solved in three stars', (await txt('win-stars')) === '★★★', await txt('win-stars'));
  }
  await page.screenshot({ path: path.join(BUILD, 'shot_endless.png') });

  // H. persistence: a reload opens the first level not yet finished; the level sheet steps through the chapters
  await page.reload();
  await page.waitForTimeout(300);
  check('H1 reload opens the first level not yet finished', (await txt('kicker')) === kickerOf(114), await txt('kicker'));
  await page.click('#open-levels');
  check('H2 the level sheet opens on the chapter of the current level', (await txt('chapter-name')) === 'Chapter 20', await txt('chapter-name'));
  check('H3 the next arrow is off past the furthest chapter', await page.isDisabled('#ch-next'));
  await page.click('#ch-prev');
  check('H4 the previous arrow shows chapter nineteen', (await txt('chapter-name')) === 'Chapter 19', await txt('chapter-name'));
  await page.click('#ch-next');
  check('H5 and the next arrow comes back to chapter twenty', (await txt('chapter-name')) === 'Chapter 20', await txt('chapter-name'));
  await page.click('#levels-grid .lvl:nth-child(3)');
  check('H6 picking level two starts it and closes the sheet', (await txt('kicker')) === kickerOf(115) && (await page.isHidden('#levels')), await txt('kicker'));

  // I. the daily board: the same board in the page and in Node, solvable, saved per day
  const today = (() => { const d = new Date(); const p = n => (n < 10 ? '0' : '') + n; return d.getUTCFullYear() + '-' + p(d.getUTCMonth() + 1) + '-' + p(d.getUTCDate()); })();
  const D = CORE.dailyLevel(today);
  await page.click('#open-daily');
  await ready();
  check('I1 the daily kicker shows the date', (await txt('kicker')).startsWith('Daily '), await txt('kicker'));
  check('I2 the daily target matches', (await txt('par')) === String(D.par), (await txt('par')) + ' vs ' + D.par);
  const dom = await boardRows();
  const expectRows = D.rows.map(r => r.replace('S', '.'));
  check('I3 the board on screen is the board from the rules', JSON.stringify(dom) === JSON.stringify(expectRows), JSON.stringify(dom) === JSON.stringify(expectRows) ? '' : dom.join(' / '));
  for (const ch of D.moves) await press(ch);
  await page.waitForFunction(() => !document.getElementById('win').hidden, null, { timeout: 8000 });
  check('I4 the daily can be solved with its optimal turns', (await txt('win-text')).includes('goal'), await txt('win-text'));
  await page.click('#next');
  await page.waitForFunction(() => !document.getElementById('levels').hidden, null, { timeout: 4000 });
  check('I5 the daily card shows the stars', (await txt('daily-stars')) === '★★★', await txt('daily-stars'));
  await page.keyboard.press('Escape');
  await page.reload();
  await page.waitForTimeout(300);
  await page.click('#open-levels');
  check('I6 the daily stars survive a reload', (await txt('daily-stars')) === '★★★');
  await page.keyboard.press('Escape');

  // K. a deep level (301, chapter fifty-one): the same board here as in Node, and solvable
  await page.evaluate(() => {
    const best = {};
    for (let i = 0; i < 300; i++) best[i] = { s: 3, t: 3 };
    localStorage.setItem('world-best-v2', JSON.stringify({ best: best, daily: {} }));
  });
  await page.reload();
  await page.waitForTimeout(300);
  const DEEP = CORE.endlessLevel(300);
  check('K1 level 301 opens as ' + kickerOf(300), (await txt('kicker')) === kickerOf(300), await txt('kicker'));
  const deepRows = await boardRows();
  const deepExpect = DEEP.rows.map(r => r.replace('S', '.'));
  check('K2 the board matches the one made in Node', JSON.stringify(deepRows) === JSON.stringify(deepExpect), JSON.stringify(deepRows) === JSON.stringify(deepExpect) ? '' : deepRows.join(' / '));
  check('K3 the target matches', (await txt('par')) === String(DEEP.par), (await txt('par')) + ' vs ' + DEEP.par);
  for (const ch of DEEP.moves) await press(ch);
  await page.waitForFunction(() => !document.getElementById('win').hidden, null, { timeout: 8000 });
  check('K4 it is solved in three stars', (await txt('win-stars')) === '★★★', await txt('win-stars'));

  // J. settings: the speed of the animation, and resetting the progress in two taps. Settings from the earlier version are ignored.
  await page.evaluate(() => localStorage.setItem('world-best-v2', JSON.stringify({ best: {}, daily: {} })));
  await page.reload();
  await page.waitForTimeout(300);
  await page.click('#open-settings');
  const gone = await page.evaluate(() => ['ctl-board', 'ctl-dial', 'ctl-buttons', 'hand', 'hand-row', 'control-note'].filter(id => document.getElementById(id)));
  check('J1 the settings have no other way to turn the world', gone.length === 0, gone.join(', ') || 'none');
  await page.click('#speed-fast');
  check('J2 fast speed sets the turn time', (await page.evaluate(() => getComputedStyle(document.documentElement).getPropertyValue('--turn').trim())) === '210ms');
  await page.click('#speed-off');
  check('J3 no animation sets the turn time to zero', (await page.evaluate(() => getComputedStyle(document.documentElement).getPropertyValue('--turn').trim())) === '0ms');
  await page.click('#speed-fast');
  await page.reload();
  await page.waitForTimeout(300);
  await page.click('#open-settings');
  check('J4 the speed survives a reload', (await page.getAttribute('#speed-fast', 'aria-pressed')) === 'true');
  await page.click('#speed-normal');
  await page.evaluate(() => localStorage.setItem('world-settings-v2', JSON.stringify({ speed: 'normal', lefty: true, control: 'buttons' })));
  await page.reload();
  await page.waitForTimeout(300);
  const oldClasses = await page.evaluate(() => ['buttons-mode', 'lefty', 'dial-mode'].some(c => document.body.classList.contains(c)));
  check('J5 settings from the earlier version are ignored', !oldClasses && (await page.isVisible('#joy')));
  await page.click('#open-settings');
  await page.click('#reset-progress');
  check('J6 first tap asks for confirmation', (await txt('reset-progress')) === 'Sure?', await txt('reset-progress'));
  await page.evaluate(() => localStorage.setItem('world-best-v2', JSON.stringify({ best: { 0: { s: 3, t: 2 } }, daily: {} })));
  check('J7 progress survives until the second tap', await page.evaluate(() => JSON.parse(localStorage.getItem('world-best-v2')).best[0] !== undefined));
  await page.click('#reset-progress');
  await page.waitForTimeout(100);
  check('J8 second tap resets the progress', await page.evaluate(() => Object.keys(JSON.parse(localStorage.getItem('world-best-v2')).best).length === 0));
  await page.keyboard.press('Escape');

  check('Z1 no page errors', errs.length === 0, JSON.stringify(errs));
  console.log('\n' + results.filter(Boolean).length + ' of ' + results.length + ' checks passed');
  await browser.close();
})().catch(e => { console.error('FAILED', e); process.exit(1); });
