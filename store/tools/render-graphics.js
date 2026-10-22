// Writes the Play Store graphics from the app's own files:
//   store/graphics/feature-graphic-1024x500.jpg  (feature graphic, JPEG so it has no transparency)
//   store/graphics/icon-512.png                  (high-res icon, 32-bit PNG with alpha)
// Run from the project root after installing the npm packages: node store/tools/render-graphics.js

const fs = require('fs');
const path = require('path');
const { pathToFileURL } = require('url');
const { chromium } = require('playwright');

const root = path.resolve(__dirname, '..', '..');
const graphics = path.join(root, 'store', 'graphics');
const iconArt = path.join(root, 'app', 'src', 'main', 'res', 'drawable-nodpi', 'app_icon_art.png');

async function main() {
  fs.mkdirSync(graphics, { recursive: true });
  const browser = await chromium.launch();
  try {
    const page = await browser.newPage({ viewport: { width: 1024, height: 500 }, deviceScaleFactor: 1 });
    await page.goto(pathToFileURL(path.join(__dirname, 'feature-graphic.html')).href);
    await page.evaluate(() => document.fonts.ready);
    await page.locator('.canvas').screenshot({
      path: path.join(graphics, 'feature-graphic-1024x500.jpg'),
      type: 'jpeg',
      quality: 95,
    });

    // Re-encode the icon through a canvas so the file is a 32-bit PNG with an alpha channel, which Play asks for.
    const iconPage = await browser.newPage({ viewport: { width: 512, height: 512 } });
    const iconData = fs.readFileSync(iconArt).toString('base64');
    const iconPng = await iconPage.evaluate(async (base64) => {
      const img = new Image();
      img.src = 'data:image/png;base64,' + base64;
      await img.decode();
      const canvas = document.createElement('canvas');
      canvas.width = 512;
      canvas.height = 512;
      canvas.getContext('2d').drawImage(img, 0, 0, 512, 512);
      return canvas.toDataURL('image/png').split(',')[1];
    }, iconData);
    fs.writeFileSync(path.join(graphics, 'icon-512.png'), Buffer.from(iconPng, 'base64'));
  } finally {
    await browser.close();
  }
  console.log('wrote store/graphics/feature-graphic-1024x500.jpg and store/graphics/icon-512.png');
}

main().catch((error) => {
  console.error(error);
  process.exit(1);
});
