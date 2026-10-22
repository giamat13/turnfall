# Turnfall

A one-screen puzzle. The world turns in steps of 90 degrees, and the ball falls with gravity. Turn the world so the ball rolls into the hole, in as few turns as possible.

The main product is the **Android app**, in this repository's root. The browser version (the HTML page) is the base it was made from: it holds the rules and the levels, and the Android app is checked against it.

## Rules

- Push the joystick left or right (or press the left and right arrow keys) to turn the world. Push past halfway and let go to turn. A small push turns nothing.
- Walls stop the ball. Mud stops it on the spot. A pit ends the attempt, and you can undo or restart.
- Arrows send the ball in their direction. Springs send it back.
- Stars: 3 at or under par, 2 within par + 2, 1 otherwise.
- Each attempt has 2 undos and 3 hints. A hint adds no turn.
- There are 108 designed levels in 18 chapters. After them, the levels never end: each one is generated from its number.
- The daily puzzle is the same for everyone on the same UTC date.

Progress and settings are saved on the device only. Nothing is sent anywhere.

## Android app

- Kotlin and Jetpack Compose. Android 10 (API 29) and newer; built for Android 16 (API 36).
- No network access, no ads, no account. The only permission is `VIBRATE`, for the buzz that you can turn off in Settings.
- Package (the app's id in Google Play): `com.turnfall.game`. It is only an identifier, not a web address, so it needs no domain. Privacy policy: [PRIVACY.md](PRIVACY.md).

### Screens and mobile behavior

- **Main menu**, the first screen: Play (continues at the first unfinished level), Daily puzzle, Levels (chapters and the chapter map), Settings, and a line with your figures. The browser page has no menu; it starts on the game.
- **Back button:** closes an open sheet first. On the play screen it goes back to the menu, and from the menu it leaves the app.
- **Home button** in the play screen's top bar goes back to the menu.
- **Share** on the win card opens the phone's share sheet with the result text. The browser version copies it instead.
- The status bar and navigation bar icons follow the chosen theme. The game works in portrait and in landscape.

### Status

The debug app and the release bundle build. The unit tests pass: the Kotlin rules against the browser version's reference answers, and a Compose smoke test that opens the menu and the screens on the JVM with Robolectric. The app has not been run on a phone or an emulator yet; the emulator needs the Windows Hypervisor Platform, see above.

### Run it on a phone emulator (F5 in VS Code)

Open the folder in VS Code and press **F5**. The launch configuration runs [tools/run-android.js](tools/run-android.js), which starts the emulator if none is running, builds and installs the app, and opens it. The first run creates an emulator named `Turnfall_API_36`.

You need:

1. The Android SDK with these packages: `platform-tools`, `emulator`, `platforms;android-36`, `build-tools;36.0.0`, `system-images;android-36;google_apis;x86_64`. Install them from Android Studio (SDK Manager), or from the command line with the new `android` tool that comes with the command-line tools: `android sdk install "platforms;android-36"`, and so on.
2. Hardware virtualization. On Windows, the emulator needs the Windows Hypervisor Platform. Open PowerShell as administrator, run `Enable-WindowsOptionalFeature -Online -FeatureName HypervisorPlatform -All -NoRestart` (the `-NoRestart` stops Windows from restarting by itself), then save your work and restart Windows yourself.
3. Java 17 or newer (`JAVA_HOME`).

The SDK is found through `ANDROID_HOME`, through `local.properties` (Android Studio writes it), or in the default folder `%LOCALAPPDATA%\Android\Sdk`. If the emulator fails to start, its log is in `build/emulator.log`.

### Build and test from a terminal

```sh
./gradlew :app:assembleDebug            # debug APK: app/build/outputs/apk/debug/app-debug.apk
./gradlew :app:testDebugUnitTest        # the unit tests
./gradlew :app:bundleRelease            # release bundle: app/build/outputs/bundle/release/app-release.aab
```

On Windows, use `gradlew.bat` instead of `./gradlew`.

The unit tests check the Kotlin rules against the browser version. `tools/make-android-fixtures.js` writes the reference answers (the optimal turns, the slides, the hints, the endless levels, the daily puzzles and the random numbers) from `src/core.js` into `app/src/test/resources/fixtures/`. The designed levels are copied from `data/levels.json` when the app is built, so both versions read the same boards.

### Signing a release

A release bundle is signed when `keystore.properties` exists in the project root. The file is not in git. Create the upload key once, outside the repository:

```sh
keytool -genkeypair -v -keystore upload-key.jks -keyalg RSA -keysize 2048 -validity 10000 -alias upload
```

Then write `keystore.properties`:

```properties
storeFile=C:/path/to/upload-key.jks
storePassword=...
keyAlias=upload
keyPassword=...
```

Without this file, `bundleRelease` still builds, but the bundle is unsigned and Google Play will not accept it.

### Publishing to Google Play

Checklist, from Google's own pages:

- **Target API.** New apps and updates must target Android 16 (API 36) or higher from August 31, 2026 ([Google's target API page](https://developer.android.com/google/play/requirements/target-sdk)). The app targets 36.
- **Testing for personal accounts.** A new personal developer account must first run a closed test with at least 12 testers, opted in for at least 14 days, before it can apply for production access ([Android Developers Blog](https://android-developers.googleblog.com/2023/11/ensuring-high-quality-apps-on-google-play.html)). Organization accounts are not mentioned there.
- **Account.** A Google Play Console developer account (one-time registration fee, identity verification). Check the current fee and documents in the [Play Console Help](https://support.google.com/googleplay/android-developer/answer/6112435).
- **Store listing.** App icon 512 × 512 px, feature graphic 1024 × 500 px, at least two phone screenshots, a short and a full description, and the category (Games > Puzzle).
- **Policy forms.** Privacy policy URL (use the link to [PRIVACY.md](PRIVACY.md) once the repository is public), Data safety (the app collects no data), content rating questionnaire, and target audience. No ads.

## Browser version (the base)

The HTML page is built from parts in `src/` and the levels in `data/`. The rules are in `src/core.js`, which the Node build, the tests and the Android fixtures use.

```sh
npm install
npx playwright install chromium   # only if Chromium is not already installed
npm run build                     # writes index.html and build/test.html
npm test                          # builds, then runs all the browser tests
```

`build/test.html` is a complete page. Serve it from any static host to play.

## Repository layout

| Path | What it is |
| --- | --- |
| `app/` | The Android app: Kotlin sources, resources, and unit tests. |
| `app/src/main/java/io/github/giamat13/turnfall/game/` | The rules, ported from `src/core.js`: board, sliding, search, hints, level generation. |
| `…/data/` | Progress and settings, saved on the device. |
| `…/feedback/` | Sounds (made in code), vibration, and saving the chapter map to the gallery. |
| `…/ui/` | The screens: play, levels, settings, chapter map, win card, joystick. |
| `gradle/`, `gradlew`, `*.gradle.kts` | The Gradle build (the wrapper is included). |
| `fonts/` | License texts of the fonts (Assistant and Secular One, SIL Open Font License). |
| `PRIVACY.md` | The privacy policy for the Play listing. |
| `.vscode/launch.json` | F5 in VS Code: runs the app on the emulator. |
| `tools/run-android.js` | The launcher behind F5: emulator, build, install, start. |
| `tools/make-android-fixtures.js` | Writes the reference answers from `src/core.js` for the unit tests. |
| `src/`, `data/`, `tools/assemble.js`, `tools/build.js`, `tests/`, `index.html` | The browser version (the base). |

## License

MIT. See [LICENSE](LICENSE). The fonts are under the SIL Open Font License; their texts are in `fonts/`.
