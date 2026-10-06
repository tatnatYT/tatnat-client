// In-game test of the Forge 1.8.9 jar on a real Forge install: boots the Legacy Fabric 1.8.9 dev
// server (port 25600), then installs/launches Forge 1.8.9 with the launcher's own code and the jar
// in its mods folder, running the scripted test. Screenshots land in <root>/instances/forge-1.8.9/screenshots.
//   node devtest.js [main|smoke]   (JAVA_HOME = JDK 21, JAVA8_HOME = JDK 8, FORGE_ROOT = launcher root to use)
const { spawn, spawnSync } = require('child_process');
const fs = require('fs');
const path = require('path');
const crypto = require('crypto');

const MC = '1.8.9';
const mode = process.argv[2] || 'main';
const ROOT = path.join(__dirname, '..', '..');
const LAUNCHER = path.join(ROOT, '..', 'minecraft-launcher', 'src', 'core');
const { launch, getManifest } = require(path.join(LAUNCHER, 'launch'));
const { resolveVersion } = require(path.join(LAUNCHER, 'versions'));
const { installJava } = require(path.join(LAUNCHER, 'install'));
const forge = require(path.join(LAUNCHER, 'forge'));

const legacy = path.join(__dirname, '..');
const run = path.join(legacy, 'run', MC);
fs.mkdirSync(run, { recursive: true });
fs.writeFileSync(path.join(run, 'eula.txt'), 'eula=true\n');
fs.writeFileSync(path.join(run, 'server.properties'), ['online-mode=false', 'server-port=25600', 'spawn-protection=0', 'level-seed=tatnat',
  'difficulty=0', 'view-distance=8', 'motd=tatnat forge dev'].join('\n') + '\n');
const h = crypto.createHash('md5').update('OfflinePlayer:tatnat').digest();
h[6] = (h[6] & 0x0f) | 0x30; h[8] = (h[8] & 0x3f) | 0x80;
const x = h.toString('hex');
const uuid = `${x.slice(0, 8)}-${x.slice(8, 12)}-${x.slice(12, 16)}-${x.slice(16, 20)}-${x.slice(20)}`;
fs.writeFileSync(path.join(run, 'ops.json'), JSON.stringify([{ uuid, name: 'tatnat', level: 4, bypassesPlayerLimit: false }]));
fs.rmSync(path.join(run, 'world', 'playerdata'), { recursive: true, force: true });

const java = path.join(process.env.JAVA_HOME, 'bin', 'java.exe');
const wrapper = ['-cp', path.join(ROOT, 'gradle', 'wrapper', 'gradle-wrapper.jar'), 'org.gradle.wrapper.GradleWrapperMain'];
const logs = path.join(ROOT, 'build');
const serverLog = path.join(logs, `server-forge-${MC}.log`);
const log = fs.openSync(serverLog, 'w');
const server = spawn(java, [...wrapper, 'runServer', '--no-daemon', `-Porg.gradle.java.installations.paths=${process.env.JAVA8_HOME}`,
  '-Porg.gradle.java.installations.auto-download=false'], { cwd: legacy, stdio: ['ignore', log, log] });

function stopAll() {
  server.kill();
  spawnSync('powershell', ['-NoProfile', '-Command',
    `Get-CimInstance Win32_Process -Filter "Name='java.exe'" | Where-Object { $_.CommandLine -like '*tatnat-client-mod-b*legacy*' -or $_.CommandLine -like '*legacy*run*${MC}*' } | ForEach-Object { Stop-Process -Id $_.ProcessId -Force -ErrorAction SilentlyContinue }`], { stdio: 'ignore' });
}

(async () => {
  const deadline = Date.now() + 600000;
  for (;;) {
    const txt = fs.existsSync(serverLog) ? fs.readFileSync(serverLog, 'utf8') : '';
    if (/Done \(/.test(txt)) break;
    if (/BUILD FAILED|FAILURE/.test(txt) || Date.now() > deadline) throw new Error('server failed:\n' + txt.slice(-3000));
    await new Promise(r => setTimeout(r, 3000));
  }
  console.log('server up');

  const root = process.env.FORGE_ROOT || path.join(__dirname, 'run');
  const vanilla = await resolveVersion(root, MC, await getManifest(root));
  const javaBin = await installJava(root, vanilla, () => {});
  const id = await forge.installLoader({ root, kind: 'forge', mcVersion: MC, javaBin });
  const gameDir = path.join(root, 'instances', `forge-${MC}`);
  const shots = path.join(gameDir, 'screenshots');
  fs.mkdirSync(path.join(gameDir, 'mods'), { recursive: true });
  fs.mkdirSync(shots, { recursive: true });
  for (const f of fs.readdirSync(shots)) if (f.startsWith('devtest-')) fs.rmSync(path.join(shots, f));
  fs.rmSync(path.join(gameDir, 'config', 'tatnat-client.json'), { force: true });
  fs.rmSync(path.join(gameDir, 'crash-reports'), { recursive: true, force: true });
  fs.copyFileSync(path.join(ROOT, 'dist', `tatnat-client-forge-mc${MC}-1.0.0.jar`), path.join(gameDir, 'mods', 'tatnat-client.jar'));
  fs.writeFileSync(path.join(gameDir, 'options.txt'), 'tutorialStep:none\n');

  process.env.JAVA_TOOL_OPTIONS = `-Dtatnat.devtest=${mode} -Dtatnat.join=127.0.0.1:25600`;
  let out = '';
  const child = await launch({ root, gameDir, versionId: id, account: { type: 'offline', name: 'tatnat' }, memoryMb: 3072,
    onProgress: () => {}, onLog: t => { out += t; } });
  const code = await new Promise(resolve => {
    const timer = setTimeout(() => { child.kill(); resolve('timeout'); }, 900000);
    child.on('exit', c => { clearTimeout(timer); resolve(c); });
  });
  fs.writeFileSync(path.join(logs, `client-forge-${MC}.log`), out);
  stopAll();
  // Forge's splash screen prints a harmless "Crash Report" with system specs; skip it.
  const problems = out.split('\n').filter(l => !/SplashProgress/.test(l) && /\[devtest\].*FAILED|Mixin apply failed|InvalidInjectionException|InvalidMixinException|---- Minecraft Crash Report ----$|Minecraft has crashed/.test(l));
  const n = fs.readdirSync(shots).filter(f => f.startsWith('devtest-')).length;
  console.log(`client exit ${code}; ${n} screenshots; ${problems.length} problems (shots in ${shots})`);
  for (const p of problems.slice(0, 10)) console.log('  ' + p.trim());
  process.exit(problems.length || code ? 1 : 0);
})().catch(e => {
  console.error(e.stack || e);
  stopAll();
  process.exit(1);
});
