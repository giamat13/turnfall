# Turnfall

A one-screen puzzle for the browser. The world turns in steps of 90 degrees, and the ball falls with gravity. Turn the world so the ball rolls into the hole, in as few turns as possible.

## Rules

- Push the joystick left or right (or press the left and right arrow keys) to turn the world. Push past halfway and let go to turn. A small push turns nothing.
- Walls stop the ball. Mud stops it on the spot. A pit ends the attempt, and you can undo or restart.
- Arrows send the ball in their direction. Springs send it back.
- Stars: 3 at or under par, 2 within par + 2, 1 otherwise.
- Each attempt has 2 undos and 3 hints. A hint adds no turn.
- There are 54 designed levels in 9 chapters. After them, the levels never end: each one is generated from its number.
- The daily puzzle is the same for everyone on the same UTC date.

Progress and settings are saved in the browser (localStorage), not on a server.

## Files

- `src/core.js`: the rules (sliding, solving, hints, stars). The page and Node both use it.
- `src/head_a.html`, `src/head_b.html`, `src/body.html`, `src/script_a.txt`, `src/script_b.txt`: the parts of the page.
- `data/levels.json`, `data/solutions.json`, `data/levels_block.txt`: the 54 designed levels, with their optimal turns and the LEVELS array for the page. `tools/build.js` generates them.
- `tools/assemble.js`: joins the parts into `index.html` (the page content) and `build/test.html` (a complete HTML document).
- `tests/`: the Playwright tests (`smoke2.js`, `joy.js`, `e2e.js`, `layout.js`, `tz.js`). They write their screenshots to `build/`.
- `build/`: generated files, not in git.

## Build and test

```sh
npm install
npx playwright install chromium   # only if Chromium is not already installed
npm run build                     # writes index.html and build/test.html
npm test                          # builds, then runs all the tests
```

`build/test.html` is a complete page. Serve it from any static host to play.

## License

MIT. See [LICENSE](LICENSE).
