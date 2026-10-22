# Google Play store listing

Everything the Play Console asks for, in one place. Paste the text files into their fields, upload the images, and use [app-content.md](app-content.md) for the forms.

## What is here

| Path | What it is | Status |
| --- | --- | --- |
| `en-US/title.txt` | App name | Ready |
| `en-US/short-description.txt` | Short description (under 80 characters) | Ready |
| `en-US/full-description.txt` | Full description | Ready |
| `en-US/release-notes-1.0.0.txt` | "What's new" for version 1.0.0 | Ready |
| `graphics/icon-512.png` | 512 × 512 icon, 32-bit PNG with alpha | Ready |
| `graphics/feature-graphic-1024x500.jpg` | Feature graphic | Ready |
| `screenshots/phone/` | Four phone screenshots, 1080 × 1920, from the app on the emulator | Ready |
| `app-content.md` | Answers for the app content, data safety, Families, and release forms | Ready |
| `publish-guide-he.md` | Step-by-step upload guide in Hebrew | Ready |
| `tools/render-graphics.js`, `tools/feature-graphic.html` | Rebuilds the graphics from the app's own icon and fonts | Tool |
| `tools/capture-screenshots.ps1` | Retakes the screenshots from the emulator after UI changes | Tool |

## Not in this folder

- **Hebrew listing.** The app has no Hebrew text, so the listing is English only. Add a Hebrew listing only after the app gets Hebrew strings.
- **Developer contact email.** Enter it in the Play Console. It is public, so it is not stored here.
- **Privacy policy link.** `https://github.com/giamat13/turnfall/blob/main/PRIVACY.md` is live now: the repository is public and the file is on `main` (checked, HTTP 200).
- **Signing key and `keystore.properties`.** These stay outside the repository. See the "Signing a release" section of [README.md](../README.md).

## Rebuilding the graphics and screenshots

- Graphics: `node store/tools/render-graphics.js` from the project root (needs the npm packages installed).
- Screenshots: start the emulator, install the debug build with `.\gradlew.bat :app:installDebug`, then run `powershell -ExecutionPolicy Bypass -File store\tools\capture-screenshots.ps1`.
