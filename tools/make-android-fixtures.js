// Writes the reference answers that the Android unit tests compare with (app/src/test/resources/fixtures).
// They come from src/core.js, the same rules the page runs, so the Kotlin port must give the same answers.
// Run from the project root: node tools/make-android-fixtures.js
'use strict';
const fs = require('fs');
const path = require('path');
const CORE = require('../src/core.js');

const root = path.join(__dirname, '..');
const dir = path.join(root, 'app', 'src', 'test', 'resources', 'fixtures');
const levels = JSON.parse(fs.readFileSync(path.join(root, 'data', 'levels.json'), 'utf8'));
fs.mkdirSync(dir, { recursive: true });
const write = (name, value) => {
  fs.writeFileSync(path.join(dir, name), JSON.stringify(value));
  console.log('wrote', name);
};

// [length, x0, y0, x1, y1, ...] for a path of cells.
const flatPath = (p) => [p.length].concat(...p.map((q) => [q[0], q[1]]));

// The page's hint loop: each hint is played as a turn, the way the page plays it, until the hole, a pit, or no hint.
function hintRun(grid, n, start) {
  const seq = [];
  const s0 = CORE.slide(grid, n, start[0], start[1], 0);
  let x = s0.x, y = s0.y, g = 0, end = 'open';
  for (let step = 0; step < 40; step++) {
    const d = CORE.hint(grid, n, x, y, g);
    if (d === null) { end = 'none'; break; }
    seq.push(d);
    g = (g + (d === 'R' ? 1 : 3)) % 4;
    const r = CORE.slide(grid, n, x, y, g);
    if (!r.moved) continue;
    x = r.x; y = r.y;
    if (r.end === 'win' || r.end === 'pit') { end = r.end; break; }
  }
  return { seq, end };
}

// The designed levels: solutions, par with each mechanic taken away, looping, hints, and every slide from every cell.
const designed = levels.map((L, index) => {
  const P = CORE.parse(L.rows);
  const grid = P.grid;
  const slides = [];
  for (let y = 0; y < P.n; y++) {
    for (let x = 0; x < P.n; x++) {
      if (grid[y][x] === 'W') continue;
      for (let g = 0; g < 4; g++) {
        const r = CORE.slide(grid, P.n, x, y, g);
        slides.push([x, y, g, r.x, r.y, r.end, r.moved ? 1 : 0, r.loop ? 1 : 0].concat(flatPath(r.path)));
      }
    }
  }
  return {
    index, n: L.n, rows: L.rows, par: L.par, sol: L.sol,
    start: P.start,
    strict: CORE.solve(grid, P.n, P.start, true, 16),
    free: CORE.solve(grid, P.n, P.start, false, 16),
    parOf: CORE.parOf(L.rows),
    parWithout: {
      mud: CORE.parOf(L.rows, { mud: false }),
      pits: CORE.parOf(L.rows, { pits: false }),
      arrows: CORE.parOf(L.rows, { arrows: false }),
      springs: CORE.parOf(L.rows, { springs: false }),
    },
    evaluate: CORE.evaluate(L.rows, L.par),
    looping: CORE.looping(L.rows),
    hints: hintRun(grid, P.n, P.start),
    slides,
  };
});
write('designed.json', designed);

// Endless levels: the first 54 past the designed ones, and a few later ones that use bigger boards.
const endlessIndex = [];
for (let i = 108; i < 162; i++) endlessIndex.push(i);
endlessIndex.push(210, 300);
const endless = endlessIndex.map((index) => {
  const t0 = Date.now();
  const L = CORE.endlessLevel(index);
  console.log('endless', index, L ? 'found' : 'NOT FOUND', Date.now() - t0, 'ms');
  return Object.assign({ index }, L || { missing: true });
});
write('endless.json', endless);

// Daily puzzles for a few dates.
const dayKeys = ['2026-10-20', '2026-01-01', '2026-02-28', '2026-07-04', '2026-12-31', '2027-03-15', '2028-02-29'];
write('daily.json', dayKeys.map((key) => Object.assign({ key, seed: CORE.dailySeed(key) }, CORE.dailyLevel(key))));

// The random numbers: the first forty values for several seeds, including negative and oversized ones.
const seeds = [0, 1, 42, -1, 20261020, 20261020 + 7919, 123456789, 2147483647, 4294967295, 4294967296 + 5];
write('prng.json', seeds.map((seed) => {
  const rnd = CORE.mulberry32(seed);
  const values = [];
  for (let i = 0; i < 40; i++) values.push(rnd());
  return { seed, values };
}));
