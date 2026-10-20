// Light smoke check of the new features in Chromium: the new levels solve by keys, hints, theme, stats, copy.
const { chromium } = require('playwright');
const path = require('path');
const fs = require('fs');
const BUILD = path.join(__dirname, '..', 'build');
const SOL = JSON.parse(fs.readFileSync(path.join(__dirname, '..', 'data', 'solutions.json'), 'utf8'));
const FILE = 'file://' + path.join(BUILD, 'test.html');
const results = [];
function check(label, ok, detail) { results.push(!!ok); console.log((ok ? 'PASS ' : 'FAIL ') + label + (detail !== undefined ? '  [' + detail + ']' : '')); }
async function play(page, sol) {
  for (const ch of sol) {
    await page.waitForFunction(() => !document.getElementById('joy').classList.contains('off'), null, { timeout: 8000 });
    await page.keyboard.press(ch === 'R' ? 'ArrowRight' : 'ArrowLeft');
  }
  await page.waitForFunction(() => !document.getElementById('win').hidden, null, { timeout: 8000 });
}
(async () => {
  const browser = await chromium.launch();
  const page = await browser.newPage({ viewport: { width: 390, height: 844 }, deviceScaleFactor: 1 });
  const errs = [];
  page.on('pageerror', e => errs.push('pageerror: ' + e.message));
  page.on('console', m => { if (m.type() === 'error' && !/ERR_TUNNEL|fonts\.g|ERR_|Failed to load resource/.test(m.text())) errs.push('console: ' + m.text()); });
  await page.goto(FILE);
  await page.evaluate(() => { try { localStorage.clear(); } catch (e) {} });
  await page.reload();
  await page.waitForTimeout(300);

  // 1. the hint on level 1: the label counts down, the message names the move, and the ball does not move
  const h0 = await page.textContent('#hint-label');
  await page.keyboard.press('h');
  await page.waitForTimeout(150);
  const h1 = await page.textContent('#hint-label');
  const m1 = await page.textContent('#msg');
  check('H1 the hint button starts at 3 and counts down', h0 === '3' && h1 === '2', h0 + ' -> ' + h1);
  check('H2 the hint says what would happen', m1.startsWith('Hint: '), m1);
  check('H3 a hint adds no turn', (await page.textContent('#turns')) === '0');
  await page.keyboard.press('Escape'); // harmless when nothing is open

  // 2. level 1 solved by the keys; the stars come out and the win card shows
  await play(page, SOL[0]);
  check('S1 level 1 solves with its solution', !(await page.isHidden('#win')), await page.textContent('#win-text'));

  // 3. copy the result: the clipboard, or the text shown selected
  await page.click('#copy');
  await page.waitForTimeout(200);
  const copied = await page.textContent('#copy');
  const boxShown = !(await page.isHidden('#copy-text'));
  check('C1 copy gives a result: the button says so, or the text is shown', copied.includes('Copied') || boxShown, copied + ' box ' + boxShown);

  // 4. the arrow chapter and the spring chapter, reached by finishing the first 36 levels in storage
  await page.evaluate(() => {
    const best = {}; for (let i = 0; i < 36; i++) best[i] = { s: 3, t: 1 };
    localStorage.setItem('world-best-v2', JSON.stringify({ best, daily: {} }));
  });
  await page.reload();
  await page.waitForTimeout(300);
  const k37 = await page.textContent('#kicker');
  check('A1 the first unfinished level is level 37, in chapter 7', k37 === 'Level 37', k37);
  const arrowLevelSeen = await page.evaluate(() => document.querySelectorAll('#board .arrow').length);
  check('A2 arrows are drawn on the board', arrowLevelSeen > 0, arrowLevelSeen);
  await play(page, SOL[36]);
  check('A3 the arrow level solves with its solution', !(await page.isHidden('#win')));
  // the spring level is level 43 (index 42): finish the 42 before it in storage, then go there
  await page.evaluate(() => {
    const best = {}; for (let i = 0; i < 42; i++) best[i] = { s: 3, t: 1 };
    localStorage.setItem('world-best-v2', JSON.stringify({ best, daily: {} }));
  });
  await page.reload();
  await page.waitForTimeout(300);
  const k43 = await page.textContent('#kicker');
  const sp = await page.evaluate(() => document.querySelectorAll('#board .spring').length);
  check('A4 the spring level is level 43, in chapter 8, and draws its springs', k43 === 'Level 43' && sp > 0, k43 + ' springs ' + sp);
  await play(page, SOL[42]);
  check('A5 the spring level solves with its solution', !(await page.isHidden('#win')));
  await page.click('#next');
  await page.waitForTimeout(150);
  // 5. settings: theme, sound and vibration toggles, and the figures
  await page.click('#open-settings');
  await page.click('#theme-dark');
  const dark = await page.evaluate(() => document.documentElement.getAttribute('data-theme'));
  check('T1 the dark choice sets data-theme', dark === 'dark', dark);
  await page.click('#theme-light');
  check('T2 the light choice sets data-theme', (await page.evaluate(() => document.documentElement.getAttribute('data-theme'))) === 'light');
  await page.click('#theme-auto');
  check('T3 auto removes data-theme', (await page.evaluate(() => document.documentElement.hasAttribute('data-theme'))) === false);
  await page.click('#sound-toggle');
  check('T4 the sound toggle turns on', (await page.getAttribute('#sound-toggle', 'aria-pressed')) === 'true');
  await page.click('#vibe-toggle');
  check('T5 the vibration toggle turns off', (await page.getAttribute('#vibe-toggle', 'aria-pressed')) === 'false');
  const stats = await page.evaluate(() => Array.from(document.querySelectorAll('#stats .stat-cell')).map(c => c.textContent));
  check('T6 the statistics show the solved levels and the stars', stats.length === 5, JSON.stringify(stats));
  await page.keyboard.press('Escape');

  check('Z1 no page errors', errs.length === 0, JSON.stringify(errs));
  console.log('\n' + results.filter(Boolean).length + ' of ' + results.length + ' checks passed');
  await browser.close();
})().catch(e => { console.error('FAILED', e); process.exit(1); });
