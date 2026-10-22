// Builds the 108 campaign levels (18 chapters of 6) with core.js. Writes levels.json (everything),
// solutions.json (the optimal turns of each level) and levels_block.txt (the LEVELS array for the page).
// Writes nothing when a level cannot be found.
const fs = require('fs');
const path = require('path');
const CORE = require('../src/core.js');
const dir = path.join(__dirname, '..', 'data');

// Per chapter: wall density, mud and pit density, which mechanic must matter, and [board size, fewest turns] per level.
const CHAPTERS = [
  { walls: 0.25, mud: 0, pits: 0, need: 'none', levels: [[5, 2], [5, 3], [5, 3], [5, 4], [5, 4], [5, 5]] },
  { walls: 0.2, mud: 0.14, pits: 0, need: 'mud', levels: [[5, 3], [5, 3], [5, 4], [6, 4], [6, 5], [6, 5]] },
  { walls: 0.2, mud: 0.08, pits: 0.12, need: 'pits', levels: [[6, 3], [6, 4], [6, 4], [6, 5], [6, 5], [6, 6]] },
  { walls: 0.2, mud: 0.1, pits: 0.1, need: 'any', levels: [[6, 4], [6, 4], [6, 5], [6, 5], [6, 6], [6, 6]] },
  { walls: 0.2, mud: 0.1, pits: 0.1, need: 'any', levels: [[6, 5], [6, 6], [6, 6], [6, 7], [6, 7], [7, 7]] },
  { walls: 0.18, mud: 0.1, pits: 0.1, need: 'any', levels: [[6, 7], [6, 7], [7, 8], [7, 8], [7, 8], [7, 8]] },
  { walls: 0.2, mud: 0, pits: 0, arrows: 0.12, springs: 0, need: 'arrows', levels: [[6, 3], [6, 4], [6, 4], [6, 5], [6, 5], [7, 6]] },
  { walls: 0.2, mud: 0, pits: 0.05, arrows: 0.04, springs: 0.1, need: 'springs', levels: [[6, 3], [6, 4], [6, 4], [6, 5], [7, 5], [7, 6]] },
  { walls: 0.18, mud: 0.08, pits: 0.08, arrows: 0.07, springs: 0.07, need: ['arrows', 'springs'], levels: [[6, 4], [6, 5], [6, 5], [7, 6], [7, 6], [7, 7]] },
  { walls: 0.2, mud: 0.1, pits: 0.1, arrows: 0.06, springs: 0.06, need: 'any', levels: [[6, 6], [6, 7], [7, 7], [7, 8], [7, 8], [7, 8]] },
  { walls: 0.2, mud: 0.08, pits: 0.1, arrows: 0.08, springs: 0.08, need: ['arrows', 'springs'], levels: [[7, 7], [7, 8], [7, 8], [7, 8], [7, 9], [7, 9]] },
  { walls: 0.18, mud: 0.1, pits: 0.1, arrows: 0.08, springs: 0.08, need: 'any', levels: [[7, 8], [7, 8], [7, 9], [7, 9], [7, 9], [7, 10]] },
  { walls: 0.2, mud: 0.1, pits: 0.1, arrows: 0.06, springs: 0.06, need: 'any', levels: [[7, 8], [7, 8], [7, 9], [7, 9], [7, 9], [7, 9]] },
  { walls: 0.2, mud: 0.08, pits: 0.12, arrows: 0.08, springs: 0.08, need: ['arrows', 'springs'], levels: [[7, 9], [7, 9], [7, 9], [7, 10], [7, 10], [7, 10]] },
  { walls: 0.18, mud: 0.1, pits: 0.1, arrows: 0.08, springs: 0.08, need: 'any', levels: [[7, 10], [7, 10], [8, 10], [8, 10], [8, 11], [8, 11]] },
  { walls: 0.22, mud: 0.1, pits: 0.1, arrows: 0.08, springs: 0.08, need: 'any', levels: [[8, 10], [8, 10], [8, 11], [8, 11], [8, 11], [8, 12]] },
  { walls: 0.2, mud: 0.1, pits: 0.12, arrows: 0.1, springs: 0.1, need: ['arrows', 'springs'], levels: [[8, 11], [8, 11], [8, 12], [8, 12], [8, 12], [8, 12]] },
  { walls: 0.2, mud: 0.1, pits: 0.1, arrows: 0.1, springs: 0.1, need: 'any', levels: [[8, 12], [8, 12], [8, 12], [8, 12], [8, 12], [8, 12]] }
];

const avoid = Object.create(null);
const out = [];
let missing = 0, totalMs = 0;
CHAPTERS.forEach((C, ci) => {
  C.levels.forEach(([n, par], k) => {
    const idx = ci * 6 + k;
    const t0 = Date.now();
    const L = CORE.generate({
      n, par, walls: C.walls, mud: C.mud, pits: C.pits, arrows: C.arrows || 0, springs: C.springs || 0, need: C.need,
      tries: 4000000, avoid, rnd: CORE.mulberry32(20261020 + idx * 7919)
    });
    const ms = Date.now() - t0;
    totalMs += ms;
    if (!L) { missing++; console.log(`level ${idx + 1}: NOT FOUND (n ${n}, par ${par}, need ${String(C.need)})`); out.push(null); return; }
    out.push({ n, rows: L.rows, par, sol: L.moves, chapter: ci });
    console.log(`level ${String(idx + 1).padStart(2)}: chapter ${ci + 1}, ${n}x${n}, par ${par}, need ${String(C.need).padEnd(4)}, tries ${String(L.tries).padStart(7)}, ${ms} ms, solution ${L.moves}`);
  });
});

if (missing) { console.log(missing, 'level(s) not found; nothing written'); process.exit(1); }
fs.writeFileSync(path.join(dir, 'levels.json'), JSON.stringify(out, null, 1));
fs.writeFileSync(path.join(dir, 'solutions.json'), JSON.stringify(out.map(L => L.sol)));
const block = '  var LEVELS = [\n' + out.map(L =>
  `    { n: ${L.n}, rows: [${L.rows.map(r => `'${r}'`).join(', ')}], par: ${L.par} }`).join(',\n') + '\n  ];\n';
fs.writeFileSync(path.join(dir, 'levels_block.txt'), block);
console.log(`wrote ${out.length} levels in ${totalMs} ms`);
