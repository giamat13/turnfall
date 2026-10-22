// Assembles the page from its parts. index.html is the content only (the platform adds the document skeleton);
// build/test.html is the same content in a full document for the local tests. The parts, in order, from src/ and data/:
// head_a, head_b (styles), body, script_a, core.js (the shared rules), levels_block.txt (the 108 levels), script_b.
const fs = require('fs');
const path = require('path');
const root = path.join(__dirname, '..');
const build = path.join(root, 'build');
const read = f => fs.readFileSync(path.join(root, f), 'utf8');
const levels = read('data/levels_block.txt');
const page = read('src/head_a.html') + read('src/head_b.html') + read('src/body.html') + read('src/script_a.txt')
  + read('src/core.js') + '\n' + levels + read('src/script_b.txt');
fs.writeFileSync(path.join(root, 'index.html'), page);
const test = '<!doctype html>\n<html lang="en" dir="ltr">\n<head>\n<meta charset="utf-8">\n'
  + '<meta name="viewport" content="width=device-width, initial-scale=1, viewport-fit=cover">\n</head>\n<body>\n'
  + page + '\n</body>\n</html>\n';
fs.mkdirSync(build, { recursive: true });
fs.writeFileSync(path.join(build, 'test.html'), test);
// The script alone, for a syntax check with node --check.
const script = page.slice(page.indexOf('<script>') + 8, page.lastIndexOf('</script>'));
fs.writeFileSync(path.join(build, 'page_script.js'), script);
console.log('levels in page:', (levels.match(/\{ n:/g) || []).length, '| page chars:', page.length);
