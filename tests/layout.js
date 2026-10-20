// Layout check: every screen must fit its viewport with nothing to scroll, at phone sizes, phones held sideways and a
// desktop window. Checks the play screen (the board, the message, the joystick, the undo and restart row), the level
// sheet, the settings, the chapter map and the win card.
const { chromium } = require('playwright');
const fs = require('fs');
const path = require('path');
const BUILD = path.join(__dirname, '..', 'build');
const FILE = 'file://' + path.join(BUILD, 'test.html');
const SOL = JSON.parse(fs.readFileSync(path.join(__dirname, '..', 'data', 'solutions.json'), 'utf8'));
const SIZES = [[390, 844], [375, 667], [360, 640], [844, 390], [1280, 800]];

// Chapter one counted as finished, so the chapter map is available.
const DONE = {};
for (let i = 0; i < 6; i++) DONE[i] = { s: 3, t: 2 };

(async () => {
  const browser = await chromium.launch();
  let bad = 0;
  for (const [w, h] of SIZES) {
    const page = await browser.newPage({ viewport: { width: w, height: h }, deviceScaleFactor: 2 });
    const errs = [];
    page.on('pageerror', e => errs.push(e.message));
    await page.goto(FILE);
    await page.evaluate(d => {
      localStorage.clear();
      localStorage.setItem('world-best-v2', JSON.stringify({ best: d, daily: {} }));
      localStorage.setItem('world-settings-v2', JSON.stringify({ speed: 'normal' }));
    }, DONE);
    await page.reload();
    await page.waitForTimeout(300);
    const tag = `${w}x${h}`;

    // the fit of one screen: no page scroll, and the lowest visible control inside the window
    const fit = sel => page.evaluate(s => {
      const root = document.querySelector(s);
      const kids = root.querySelectorAll('button, p, h2, h3, .dash, .board, .board-wrap, .levels-grid, img');
      let low = 0, right = 0;
      kids.forEach(k => {
        const r = k.getBoundingClientRect();
        if (r.width === 0 && r.height === 0) return;
        low = Math.max(low, r.bottom); right = Math.max(right, r.right);
      });
      return {
        overflow: root.scrollHeight - root.clientHeight,
        low: Math.round(low), right: Math.round(right),
        pageScroll: document.documentElement.scrollHeight - innerHeight,
        pageWide: document.documentElement.scrollWidth - innerWidth
      };
    }, sel);

    // the play screen: the board, the message, the joystick, and the undo and restart row under it
    const play = await page.evaluate(() => {
      const b = document.getElementById('board-card').getBoundingClientRect();
      const m = document.getElementById('msg').getBoundingClientRect();
      const j = document.getElementById('joy').getBoundingClientRect();
      const u = document.getElementById('undo').getBoundingClientRect();
      const r = document.getElementById('reset').getBoundingClientRect();
      return {
        board: Math.round(b.width) + 'x' + Math.round(b.height),
        msgBottom: Math.round(m.bottom), joyTop: Math.round(j.top), joyBottom: Math.round(j.bottom), joyWide: Math.round(j.width),
        toolsTop: Math.round(u.top), tools: Math.round(r.bottom)
      };
    });
    const pageFit = await page.evaluate(() => ({
      pageScroll: document.documentElement.scrollHeight - innerHeight,
      pageWide: document.documentElement.scrollWidth - innerWidth
    }));
    const playOk = pageFit.pageScroll <= 0 && pageFit.pageWide <= 0 && play.tools <= h
      && play.joyTop >= 0 && play.msgBottom <= play.joyTop + 1 && play.joyBottom <= play.toolsTop + 1
      && parseInt(play.board) >= 150;
    console.log(`${tag} play: ${playOk ? 'fits' : 'NO FIT'} | board ${play.board} | message ends ${play.msgBottom}, joystick ${play.joyTop} to ${play.joyBottom} (${play.joyWide} wide), tools end ${play.tools} of ${h} | page scroll ${pageFit.pageScroll}, wide ${pageFit.pageWide}`);
    if (!playOk) bad++;
    await page.screenshot({ path: path.join(BUILD, `lay_play_${tag}.png`) });

    for (const [name, open, close] of [['levels', '#open-levels', '#levels-close'], ['settings', '#open-settings', '#settings-close']]) {
      await page.click(open);
      await page.waitForTimeout(80);
      const f = await fit('#' + name);
      const ok = f.overflow <= 0 && f.low <= h && f.right <= w && f.pageScroll <= 0;
      console.log(`   ${name}: ${ok ? 'fits' : 'NO FIT'} | content ${f.low} of ${h} | overflow ${f.overflow}`);
      if (!ok) bad++;
      await page.screenshot({ path: path.join(BUILD, `lay_${name}_${tag}.png`) });
      await page.click(close);
      await page.waitForTimeout(40);
    }

    // the chapter map (chapter one is finished): the level sheet opens on chapter two, so step back to chapter one
    await page.click('#open-levels');
    await page.click('#ch-prev');
    await page.click('#chapter-map');
    await page.waitForFunction(() => document.getElementById('map-img').naturalWidth > 0, null, { timeout: 8000 });
    const m = await fit('#map');
    const mapOk = m.overflow <= 0 && m.low <= h && m.pageScroll <= 0;
    console.log(`   map: ${mapOk ? 'fits' : 'NO FIT'} | content ${m.low} of ${h} | overflow ${m.overflow}`);
    if (!mapOk) bad++;
    await page.screenshot({ path: path.join(BUILD, `lay_map_${tag}.png`) });
    await page.keyboard.press('Escape');
    await page.keyboard.press('Escape');

    // the win card: the page opens on level 7, the first one not finished; play its solution with the keys
    for (const ch of SOL[6]) {
      await page.waitForFunction(() => !document.getElementById('joy').classList.contains('off'), null, { timeout: 8000 });
      await page.keyboard.press(ch === 'R' ? 'ArrowRight' : 'ArrowLeft');
    }
    await page.waitForFunction(() => !document.getElementById('win').hidden, null, { timeout: 8000 });
    await page.waitForTimeout(600);
    const card = await page.evaluate(() => {
      const r = document.querySelector('.win-card').getBoundingClientRect();
      return { t: Math.round(r.top), b: Math.round(r.bottom) };
    });
    const winOk = card.t >= 0 && card.b <= h;
    console.log(`   win card: ${winOk ? 'visible' : 'CUT'} | ${card.t} to ${card.b} of ${h}`);
    if (!winOk) bad++;
    await page.screenshot({ path: path.join(BUILD, `lay_win_${tag}.png`) });
    console.log(`   errors: ${JSON.stringify(errs)}`);
    if (errs.length) bad++;
    await page.close();
  }
  console.log(bad ? `${bad} screen(s) do not fit` : 'every screen fits at every size');
  await browser.close();
})().catch(e => { console.error('FAILED', e); process.exit(1); });
