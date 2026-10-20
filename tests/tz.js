// The daily puzzle is the same for everyone at the same moment. Players in far-apart time zones open the daily
// at the same instant and must see the same date and the same board.
const { chromium } = require('playwright');
const path = require('path');
const FILE = 'file://' + path.join(__dirname, '..', 'build', 'test.html');
let pass = 0, fail = 0;
function check(name, ok, detail) {
  if (ok) pass++; else fail++;
  console.log((ok ? 'PASS ' : 'FAIL ') + name + (detail === undefined ? '' : '  [' + detail + ']'));
}
(async () => {
  const b = await chromium.launch();
  const zones = ['Pacific/Kiritimati', 'Pacific/Pago_Pago', 'Asia/Jerusalem'];
  const cases = [
    { at: '2026-10-20T22:30:00Z', kicker: 'Daily Oct 20' },
    { at: '2026-10-21T01:30:00Z', kicker: 'Daily Oct 21' },
  ];
  for (const c of cases) {
    const seen = [];
    for (const tz of zones) {
      const ctx = await b.newContext({ timezoneId: tz });
      const p = await ctx.newPage();
      await p.clock.setFixedTime(new Date(c.at));
      await p.goto(FILE);
      await p.waitForTimeout(200);
      const offset = await p.evaluate(() => new Date().getTimezoneOffset());
      await p.click('#open-daily');
      await p.waitForTimeout(200);
      seen.push({
        tz: tz,
        offset: offset,
        kicker: await p.textContent('#kicker'),
        par: await p.textContent('#par'),
        world: await p.innerHTML('#world'),
      });
      await ctx.close();
    }
    for (const s of seen) {
      check('the daily at ' + c.at + ' is dated ' + c.kicker.slice(6) + ' in ' + s.tz, s.kicker === c.kicker,
        s.kicker + ', offset ' + s.offset + ' min');
    }
    const same = seen.every(s => s.world === seen[0].world && s.par === seen[0].par);
    check('the board and target at ' + c.at + ' are the same in all three zones', same,
      seen.map(s => s.par).join(' / '));
  }
  await b.close();
  console.log(pass + ' of ' + (pass + fail) + ' checks passed');
  process.exit(fail ? 1 : 0);
})().catch(e => { console.error('FAILED', e); process.exit(1); });
