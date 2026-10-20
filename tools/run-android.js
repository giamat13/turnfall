#!/usr/bin/env node
// Runs Turnfall on an Android phone emulator. It starts the emulator when none is running, builds and installs the
// debug app, and opens it. The F5 launch in .vscode/launch.json runs this file; from a terminal, run:
//   node tools/run-android.js
// Set TURNFALL_AVD to use another emulator (an AVD name from `emulator -list-avds`).
'use strict';
const fs = require('fs');
const os = require('os');
const path = require('path');
const { spawn, spawnSync } = require('child_process');

const repo = path.join(__dirname, '..');
// The Android project is at the root of the repository: app/ and gradle/ sit beside src/ and data/.
const android = repo;
const logFile = path.join(repo, 'build', 'emulator.log');
const avdName = process.env.TURNFALL_AVD || 'Turnfall_API_36';
const systemImage = 'system-images;android-36;google_apis;x86_64';
const appId = 'com.turnfall.game';
// The activity keeps its class name from the code's package, which is not the same as the app's id.
const activity = `${appId}/io.github.giamat13.turnfall.MainActivity`;
const win = process.platform === 'win32';

function say(text) {
  console.log(`[turnfall] ${text}`);
}

function fail(text) {
  console.error(`[turnfall] ${text}`);
  process.exit(1);
}

// The Android SDK: ANDROID_HOME, then local.properties, then the folder Android Studio uses by default.
function findSdk() {
  const fromProps = readLocalProperty('sdk.dir');
  const defaults = win
    ? [path.join(process.env.LOCALAPPDATA || '', 'Android', 'Sdk')]
    : [path.join(os.homedir(), 'Library', 'Android', 'sdk'), path.join(os.homedir(), 'Android', 'Sdk')];
  const candidates = [process.env.ANDROID_HOME, process.env.ANDROID_SDK_ROOT, fromProps, ...defaults];
  return candidates.find((p) => p && fs.existsSync(p)) || null;
}

function readLocalProperty(key) {
  const file = path.join(android, 'local.properties');
  if (!fs.existsSync(file)) return null;
  const line = fs.readFileSync(file, 'utf8').split(/\r?\n/).find((l) => l.startsWith(`${key}=`));
  // local.properties escapes ':' and '\' with a backslash.
  return line ? line.slice(key.length + 1).replace(/\\(.)/g, '$1') : null;
}

function tool(sdk, folder, name) {
  const exe = win ? `${name}.exe` : name;
  return path.join(sdk, folder, exe);
}

// Runs a command and returns its text output.
function run(file, args, options = {}) {
  const result = spawnSync(file, args, { encoding: 'utf8', ...options });
  return { code: result.status, out: `${result.stdout || ''}${result.stderr || ''}` };
}

// Windows batch files (gradlew.bat, avdmanager.bat) run through cmd.exe, the way a console would run them.
function batch(file, args, options = {}) {
  return spawnSync(process.env.ComSpec || 'cmd.exe', ['/d', '/s', '/c', `"${file}" ${args.join(' ')}`], {
    encoding: 'utf8',
    windowsVerbatimArguments: true,
    ...options,
  });
}

function connectedDevices(adb) {
  const { out } = run(adb, ['devices']);
  return out
    .split(/\r?\n/)
    .slice(1)
    .map((l) => l.trim().split(/\s+/))
    .filter((parts) => parts.length >= 2 && parts[1] === 'device')
    .map((parts) => parts[0]);
}

function bootCompleted(adb, serial) {
  const { out } = run(adb, ['-s', serial, 'shell', 'getprop', 'sys.boot_completed']);
  return out.trim() === '1';
}

function sleep(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

async function startEmulator(sdk, adb, emulator, avdmanager) {
  const { out: avdList } = run(emulator, ['-list-avds']);
  const avds = avdList.split(/\r?\n/).map((l) => l.trim()).filter(Boolean);
  if (!avds.includes(avdName)) {
    const sysImageDir = path.join(sdk, 'system-images', ...systemImage.split(';').slice(1));
    if (!fs.existsSync(sysImageDir)) {
      const androidTool = path.join(sdk, 'cmdline-tools', 'latest', 'bin', win ? 'android.exe' : 'android');
      fail(
        `The system image ${systemImage} is not installed. Install it once with:\n` +
          `  "${androidTool}" --sdk="${sdk}" sdk install "${systemImage}"\n` +
          'or from Android Studio (Tools > SDK Manager). Then run this again.',
      );
    }
    say(`creating the emulator ${avdName} (one time)`);
    const created = win
      ? batch(avdmanager, ['create', 'avd', '-n', avdName, '-k', systemImage, '-d', 'pixel_7', '--force'], { input: 'no\n' })
      : run(avdmanager, ['create', 'avd', '-n', avdName, '-k', systemImage, '-d', 'pixel_7', '--force'], { input: 'no\n' });
    if (created.status !== 0) fail(`could not create the emulator:\n${created.stdout}${created.stderr}`);
  }

  say(`starting the emulator ${avdName}; this takes a minute the first time`);
  fs.mkdirSync(path.dirname(logFile), { recursive: true });
  const log = fs.openSync(logFile, 'w');
  const child = spawn(emulator, ['-avd', avdName, '-gpu', 'auto'], {
    detached: true,
    stdio: ['ignore', log, log],
    windowsHide: false,
  });
  let exited = false;
  child.on('exit', () => {
    exited = true;
  });

  const deadline = Date.now() + 5 * 60 * 1000;
  while (Date.now() < deadline) {
    if (exited) {
      fail(
        'The emulator stopped before it started. On Windows it needs the Windows Hypervisor Platform.\n' +
          'Open PowerShell as administrator, run\n' +
          '  Enable-WindowsOptionalFeature -Online -FeatureName HypervisorPlatform -All -NoRestart\n' +
          'then save your work, restart Windows yourself, and press F5 again. The emulator log is at ' + logFile,
      );
    }
    const serial = connectedDevices(adb).find((s) => s.startsWith('emulator-'));
    if (serial && bootCompleted(adb, serial)) {
      child.unref();
      return serial;
    }
    await sleep(2000);
  }
  fail(`the emulator did not finish starting within 5 minutes. Its log is at ${logFile}`);
  return null;
}

async function main() {
  const sdk = findSdk();
  if (!sdk) {
    fail('The Android SDK was not found. Install Android Studio, or set ANDROID_HOME to the SDK folder.');
  }
  const adb = tool(sdk, 'platform-tools', 'adb');
  const emulator = tool(sdk, 'emulator', 'emulator');
  const avdmanager = path.join(sdk, 'cmdline-tools', 'latest', 'bin', win ? 'avdmanager.bat' : 'avdmanager');
  if (!fs.existsSync(adb) || !fs.existsSync(emulator)) {
    fail('platform-tools and the emulator are not installed in the SDK. Install them, then run this again.');
  }

  let serial = connectedDevices(adb).find((s) => s.startsWith('emulator-')) || connectedDevices(adb)[0];
  if (serial) {
    say(`using the device that is already connected: ${serial}`);
  } else {
    serial = await startEmulator(sdk, adb, emulator, avdmanager);
  }

  say('building and installing the app');
  const gradlew = path.join(android, win ? 'gradlew.bat' : 'gradlew');
  const buildOptions = {
    cwd: android,
    stdio: 'inherit',
    env: { ...process.env, ANDROID_SERIAL: serial, ANDROID_HOME: sdk },
  };
  const build = win
    ? batch(gradlew, [':app:installDebug'], buildOptions)
    : spawnSync(gradlew, [':app:installDebug'], buildOptions);
  if (build.status !== 0) fail('the build or the install failed; see the messages above');

  say('opening Turnfall');
  const start = run(adb, ['-s', serial, 'shell', 'am', 'start', '-n', activity]);
  if (start.code !== 0 || /Error/.test(start.out)) fail(`could not open the app:\n${start.out}`);
  say(`Turnfall is running on ${serial}`);
}

main().catch((error) => fail(error.stack || String(error)));
