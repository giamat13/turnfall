// Rules of "Turnfall", shared by the page (embedded as text) and by the build and test scripts (Node).
// The ball always falls toward the bottom of the screen. A turn rotates the world, so the gravity index g
// moves: 0 down, 1 right, 2 up, 3 left. 'R' (turn right, clockwise) adds 1, 'L' (turn left) adds 3.
// Rows: '#' wall, '.' floor, 'S' ball start, 'O' hole, 'm' mud, 'p' pit,
// 'v' '>' '^' '<' arrows (a ball that enters one falls in that arrow's direction), 'z' spring (it sends the ball back).
var CORE = (function () {
  'use strict';
  var DX = [0, 1, 0, -1], DY = [1, 0, -1, 0];
  var ARROW_ROW = { 'v': 0, '>': 1, '^': 2, '<': 3 };   // row character -> direction
  var ARROW_CHARS = ['v', '>', '^', '<'];              // direction -> row character
  var ENDLESS_FROM = 108;   // the levels from this index on are made from their index, not designed
  var MAX_BOUNCES = 4;     // a ball sent back by springs this many times stops on the spring

  // Cell codes after parse: 'W' wall, 'F' floor, 'H' hole, 'M' mud, 'P' pit, 'Z' spring, 'A0' to 'A3' arrows (the direction).
  // opts.mud, opts.pits, opts.arrows, opts.springs === false turn that mechanic into plain floor (used to test whether it matters).
  function parse(rows, opts) {
    var keepMud = !opts || opts.mud !== false;
    var keepPits = !opts || opts.pits !== false;
    var keepArrows = !opts || opts.arrows !== false;
    var keepSprings = !opts || opts.springs !== false;
    var n = rows.length, grid = [], start = null;
    for (var y = 0; y < n; y++) {
      var line = [];
      for (var x = 0; x < n; x++) {
        var c = rows[y].charAt(x), k = 'F';
        if (c === '#') k = 'W';
        else if (c === 'O') k = 'H';
        else if (c === 'm') k = keepMud ? 'M' : 'F';
        else if (c === 'p') k = keepPits ? 'P' : 'F';
        else if (c === 'z') k = keepSprings ? 'Z' : 'F';
        else if (ARROW_ROW.hasOwnProperty(c)) k = keepArrows ? 'A' + ARROW_ROW[c] : 'F';
        else if (c === 'S') start = [x, y];
        line.push(k);
      }
      grid.push(line);
    }
    return { n: n, grid: grid, start: start };
  }

  // The ball slides in the direction of gravity until something stops it. end is 'win' (it reaches the hole),
  // 'pit' (it falls into a pit), 'mud' (it stops on mud) or 'wall' (a wall, the edge, or the springs' limit stops it).
  // An arrow turns the ball to its own direction; a spring sends it back the way it came.
  // moved says whether it moved at all; path lists the cells it passed, in order; loop is set when the path runs too long.
  function slide(grid, n, x, y, g) {
    var path = [], bounces = 0, limit = 4 * n * n + 16;
    for (;;) {
      var nx = x + DX[g], ny = y + DY[g];
      if (nx < 0 || ny < 0 || nx >= n || ny >= n || grid[ny][nx] === 'W') {
        return { x: x, y: y, end: 'wall', moved: path.length > 0, path: path };
      }
      x = nx; y = ny;
      path.push([x, y]);
      if (path.length > limit) return { x: x, y: y, end: 'wall', moved: true, path: path, loop: true };
      var k = grid[y][x];
      if (k === 'H') return { x: x, y: y, end: 'win', moved: true, path: path };
      if (k === 'P') return { x: x, y: y, end: 'pit', moved: true, path: path };
      if (k === 'M') return { x: x, y: y, end: 'mud', moved: true, path: path };
      if (k === 'Z') {
        if (++bounces > MAX_BOUNCES) return { x: x, y: y, end: 'wall', moved: true, path: path };
        g = (g + 2) % 4;
      } else if (k.charAt(0) === 'A') {
        g = +k.charAt(1);
      }
    }
  }

  // Breadth-first search over (cell, gravity) from one state. strict: a turn that does not move the ball is not allowed.
  // Returns { par, moves } for the fewest turns that reach the hole within maxDepth, or null.
  function search(grid, n, x0, y0, g0, strict, maxDepth) {
    var key = function (x, y, g) { return (y * n + x) * 4 + g; };
    var from = {};
    from[key(x0, y0, g0)] = null;
    var frontier = [[x0, y0, g0]];
    for (var depth = 1; depth <= maxDepth && frontier.length; depth++) {
      var next = [];
      for (var i = 0; i < frontier.length; i++) {
        var st = frontier[i], x = st[0], y = st[1], g = st[2];
        for (var t = 0; t < 2; t++) {
          var ch = t === 0 ? 'R' : 'L', ng = (g + (t === 0 ? 1 : 3)) % 4;
          var r = slide(grid, n, x, y, ng);
          if (strict && !r.moved) continue;
          if (r.end === 'pit') continue;
          if (r.end === 'win') {
            var moves = ch, cur = key(x, y, g);
            while (from[cur]) { moves = from[cur].ch + moves; cur = from[cur].p; }
            return { par: depth, moves: moves };
          }
          var nk = key(r.x, r.y, ng);
          if (!(nk in from)) { from[nk] = { p: key(x, y, g), ch: ch }; next.push([r.x, r.y, ng]); }
        }
      }
      frontier = next;
    }
    return null;
  }

  // Breadth-first search from the start of a level (the first fall, gravity down). Returns { par, moves } or null.
  function solve(grid, n, start, strict, maxDepth) {
    var s0 = slide(grid, n, start[0], start[1], 0);
    if (s0.end === 'win' || s0.end === 'pit') return null;
    return search(grid, n, s0.x, s0.y, 0, strict, maxDepth);
  }

  // The next turn on a fewest-turn solution from where the ball is now: 'R', 'L', or null when no way on is left.
  // A turn that moves the ball is preferred; one that does not move it is used only when nothing else works.
  function hint(grid, n, x, y, g) {
    var r = search(grid, n, x, y, g, true, 16) || search(grid, n, x, y, g, false, 16);
    return r ? r.moves.charAt(0) : null;
  }

  // A board is kept when its fewest turns is exactly T, both allowing and forbidding wasted turns.
  // Returns the optimal turns as a string of 'R' and 'L' (no wasted turns), or null.
  function evaluate(rows, T) {
    var P = parse(rows);
    if (!P.start) return null;
    var free = solve(P.grid, P.n, P.start, false, T);
    if (!free || free.par !== T) return null;
    var strict = solve(P.grid, P.n, P.start, true, T);
    if (!strict || strict.par !== T) return null;
    return strict.moves;
  }

  // Fewest turns with the given mechanics; 99 when the level cannot be solved within 16 turns.
  function parOf(rows, opts) {
    var P = parse(rows, opts);
    if (!P.start) return 99;
    var r = solve(P.grid, P.n, P.start, false, 16);
    return r ? r.par : 99;
  }

  // need: 'none'; one mechanic name ('mud', 'pits', 'arrows', 'springs'); 'any' (at least one of them); or a list of
  // names (each one). A mechanic is needed when removing it changes the fewest turns.
  function meets(rows, T, need) {
    if (need === 'none') return true;
    var list = need === 'any' ? ['mud', 'pits', 'arrows', 'springs'] : typeof need === 'string' ? [need] : need;
    var found = list.map(function (name) {
      var off = {}; off[name] = false;
      return parOf(rows, off) !== T;
    });
    return need === 'any' ? found.some(Boolean) : found.every(Boolean);
  }

  // Can the ball run on for ever? Only arrows can make such a loop; a board that can is not used.
  function hasLoop(grid, n, start) {
    var s0 = slide(grid, n, start[0], start[1], 0);
    if (s0.loop) return true;
    if (s0.end === 'win' || s0.end === 'pit') return false;
    var key = function (x, y, g) { return (y * n + x) * 4 + g; };
    var seen = {}, stack = [[s0.x, s0.y, 0]];
    seen[key(s0.x, s0.y, 0)] = true;
    while (stack.length) {
      var st = stack.pop(), x = st[0], y = st[1], g = st[2];
      for (var t = 0; t < 2; t++) {
        var ng = (g + (t === 0 ? 1 : 3)) % 4;
        var r = slide(grid, n, x, y, ng);
        if (r.loop) return true;
        if (r.end === 'win' || r.end === 'pit') continue;
        var nk = key(r.x, r.y, ng);
        if (!seen[nk]) { seen[nk] = true; stack.push([r.x, r.y, ng]); }
      }
    }
    return false;
  }
  function looping(rows) {
    if (!/[v<>^]/.test(rows.join(''))) return false;
    var P = parse(rows);
    return P.start ? hasLoop(P.grid, P.n, P.start) : false;
  }

  // Seeded random numbers (mulberry32), so the same seed always gives the same levels.
  function mulberry32(a) {
    return function () {
      a |= 0; a = a + 0x6D2B79F5 | 0;
      var t = Math.imul(a ^ a >>> 15, a | 1);
      t = t + Math.imul(t ^ t >>> 7, t | 61) ^ t;
      return ((t ^ t >>> 14) >>> 0) / 4294967296;
    };
  }

  // A random board: walls, then the start and the hole, then mud, pits, arrows and springs on the remaining floor.
  // A spec without arrows or springs draws exactly the same numbers as before, so the old boards do not change.
  function randomRows(rnd, spec) {
    var n = spec.n, cells = [], floors = [];
    var arrows = spec.arrows || 0, springs = spec.springs || 0;
    for (var y = 0; y < n; y++) {
      var row = [];
      for (var x = 0; x < n; x++) {
        if (rnd() < spec.walls) row.push('#');
        else { row.push('.'); floors.push([x, y]); }
      }
      cells.push(row);
    }
    if (floors.length < 8) return null;
    var pick = function () { return floors[Math.floor(rnd() * floors.length)]; };
    var s = pick(), o = pick(), guard = 0;
    while (o[0] === s[0] && o[1] === s[1] && guard++ < 50) o = pick();
    if (o[0] === s[0] && o[1] === s[1]) return null;
    cells[s[1]][s[0]] = 'S';
    cells[o[1]][o[0]] = 'O';
    for (var j = 0; j < floors.length; j++) {
      var c = floors[j];
      if (cells[c[1]][c[0]] !== '.') continue;
      var r = rnd();
      if (r < spec.pits) cells[c[1]][c[0]] = 'p';
      else if (r < spec.pits + spec.mud) cells[c[1]][c[0]] = 'm';
      else if (r < spec.pits + spec.mud + arrows) cells[c[1]][c[0]] = ARROW_CHARS[Math.floor(rnd() * 4)];
      else if (r < spec.pits + spec.mud + arrows + springs) cells[c[1]][c[0]] = 'z';
    }
    return cells.map(function (row) { return row.join(''); });
  }

  // Tries random boards until one fits: exactly spec.par turns, the mechanic needed, and not already in spec.avoid.
  // Returns { n, rows, par, moves, tries } or null when the tries run out.
  function generate(spec) {
    var avoid = spec.avoid || null;
    for (var tries = 0; tries < spec.tries; tries++) {
      var rows = randomRows(spec.rnd, spec);
      if (!rows) continue;
      var k = rows.join('/');
      if (avoid && avoid[k]) continue;
      var moves = evaluate(rows, spec.par);
      if (!moves || !meets(rows, spec.par, spec.need) || looping(rows)) continue;
      if (avoid) avoid[k] = true;
      return { n: spec.n, rows: rows, par: spec.par, moves: moves, tries: tries + 1 };
    }
    return null;
  }

  // A board whose fewest turns lie between spec.lo and spec.hi, the same with and without wasted turns.
  // Returns { n, rows, par, moves, tries } or null when the tries run out.
  function fit(spec) {
    for (var tries = 0; tries < spec.tries; tries++) {
      var rows = randomRows(spec.rnd, spec);
      if (!rows) continue;
      var P = parse(rows);
      if (!P.start) continue;
      var free = solve(P.grid, P.n, P.start, false, spec.hi);
      if (!free || free.par < spec.lo) continue;
      var strict = solve(P.grid, P.n, P.start, true, spec.hi);
      if (!strict || strict.par !== free.par) continue;
      if (!meets(rows, free.par, spec.need) || looping(rows)) continue;
      return { n: spec.n, rows: rows, par: free.par, moves: strict.moves, tries: tries + 1 };
    }
    return null;
  }

  // Level i (0-based) from ENDLESS_FROM on, so the campaign never ends. Every level is found from its own seed, so a
  // given level is the same board for everyone. Chapters of six: the board grows (7, then 8, then 9 squares),
  // the fewest turns rise from eight to twelve, and arrows and springs become more common chapter by chapter.
  function endlessLevel(i) {
    var j = i - ENDLESS_FROM, t = Math.floor(j / 6), k = j % 6;
    var n = t < 4 ? 7 : t < 12 ? 8 : 9;
    var base = Math.min(12, 8 + Math.floor(t / 3));
    var lo = Math.min(12, base + (k >= 3 ? 1 : 0));
    var seed = 20261020 + j * 7919;
    var arrows = Math.min(0.1, 0.03 + t * 0.01), springs = Math.min(0.09, 0.02 + t * 0.008);
    for (var attempt = 0; attempt < 3; attempt++) {
      var L = fit({ n: n, lo: lo, hi: lo + 1, walls: 0.2, mud: 0.1, pits: 0.08, arrows: arrows, springs: springs, need: 'any', tries: 20000, rnd: mulberry32(seed + attempt * 1000003) });
      if (L) return L;
    }
    // Not found in three tries (very rare): a wider window, then a small board that always fits.
    return fit({ n: n, lo: Math.max(3, lo - 1), hi: lo + 3, walls: 0.2, mud: 0.1, pits: 0.08, arrows: arrows, springs: springs, need: 'any', tries: 200000, rnd: mulberry32(seed + 77) })
      || fit({ n: 6, lo: 3, hi: 8, walls: 0.2, mud: 0.1, pits: 0.08, need: 'any', tries: 200000, rnd: mulberry32(seed + 99) });
  }

  // The daily board is the same for everyone on a given day. dateKey is 'YYYY-MM-DD'.
  function dailySeed(dateKey) {
    var p = dateKey.split('-');
    return (+p[0]) * 10000 + (+p[1]) * 100 + (+p[2]);
  }
  function dailyLevel(dateKey) {
    var seed = dailySeed(dateKey);
    var spec = { n: 6, walls: 0.2, mud: 0.1, pits: 0.08, par: 4 + (seed % 3), need: 'any', tries: 200000, rnd: mulberry32(seed) };
    var L = generate(spec);
    if (!L) { spec.par = 4; spec.rnd = mulberry32(seed + 1); L = generate(spec); }
    return L;
  }

  // Stars: 3 at or under par, 2 within two turns of par, 1 otherwise.
  function stars(turns, par) { return turns <= par ? 3 : (turns <= par + 2 ? 2 : 1); }

  return {
    DX: DX, DY: DY, ENDLESS_FROM: ENDLESS_FROM, parse: parse, slide: slide, solve: solve, hint: hint,
    evaluate: evaluate, parOf: parOf, meets: meets, hasLoop: hasLoop, looping: looping,
    mulberry32: mulberry32, randomRows: randomRows, generate: generate, fit: fit, endlessLevel: endlessLevel,
    dailySeed: dailySeed, dailyLevel: dailyLevel, stars: stars
  };
})();
if (typeof module !== 'undefined' && module.exports) module.exports = CORE;
