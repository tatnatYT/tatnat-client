// In-game test for a legacy target (1.8.9 - 1.13.2): boots that version's dev server on port 25600,
// joins it with the dev client running the scripted test (DevTestSteps) on Java 8, then stops.
// Screenshots land in legacy/run/<version>/screenshots.
//   node legacy/devtest.js 1.8.9 [main|audit]
const { spawn, spawnSync } = require('child_process');
const fs = require('fs');
const path = require('path');
const crypto = require('crypto');

const [version, mode = 'main'] = process.argv.slice(2);
const { targets } = JSON.parse(fs.readFileSync(path.join(__dirname, 'targets.json'), 'utf8'));
const t = targets.find(x => x.mc === version);
if (!t) { console.error('unknown legacy target ' + version); process.exit(1); }

const run = path.join(__dirname, 'run', version);
fs.mkdirSync(path.join(run, 'screenshots'), { recursive: true });
fs.writeFileSync(path.join(run, 'eula.txt'), 'eula=true\n');
fs.writeFileSync(path.join(run, 'server.properties'), ['online-mode=false', 'server-port=25600', 'spawn-protection=0', 'level-seed=tatnat',
  'difficulty=0', 'view-distance=8', 'motd=tatnat legacy dev'].join('\n') + '\n');
// Op the dev player ("tatnat", offline UUID) so the test can use commands.
const h = crypto.createHash('md5').update('OfflinePlayer:tatnat').digest();
h[6] = (h[6] & 0x0f) | 0x30; h[8] = (h[8] & 0x3f) | 0x80;
const x = h.toString('hex');
const uuid = `${x.slice(0, 8)}-${x.slice(8, 12)}-${x.slice(12, 16)}-${x.slice(16, 20)}-${x.slice(20)}`;
fs.writeFileSync(path.join(run, 'ops.json'), JSON.stringify([{ uuid, name: 'tatnat', level: 4, bypassesPlayerLimit: false }]));
for (const f of fs.readdirSync(path.join(run, 'screenshots'))) if (f.startsWith('devtest-')) fs.rmSync(path.join(run, 'screenshots', f));
fs.rmSync(path.join(run, 'config', 'tatnat-client.json'), { force: true });
fs.rmSync(path.join(run, 'world', 'playerdata'), { recursive: true, force: true });

const java = path.join(process.env.JAVA_HOME, 'bin', 'java.exe');
const wrapper = ['-cp', path.join(__dirname, '..', 'gradle', 'wrapper', 'gradle-wrapper.jar'), 'org.gradle.wrapper.GradleWrapperMain'];
const props = [`-Pminecraft_version=${t.mc}`, `-Pminecraft_dependency=${t.dep}`, `-Pplatform_source=${t.src}`,
  `-Porg.gradle.java.installations.paths=${process.env.JAVA8_HOME}`, '-Porg.gradle.java.installations.auto-download=false'];
const logs = path.join(__dirname, '..', 'build');

const serverLog = path.join(logs, `server-legacy-${version}.log`);
let server = null;
if (mode !== 'audit') {
  const log = fs.openSync(serverLog, 'w');
  server = spawn(java, [...wrapper, 'runServer', '--no-daemon', ...props], { cwd: __dirname, stdio: ['ignore', log, log] });
  const deadline = Date.now() + 600000;
  while (Date.now() < deadline) {
    const txt = fs.existsSync(serverLog) ? fs.readFileSync(serverLog, 'utf8') : '';
    if (/Done \(/.test(txt)) break;
    if (/BUILD FAILED|Exception in server tick loop|FAILURE/.test(txt)) { console.log(txt.slice(-3000)); process.exit(1); }
    spawnSync('timeout', ['/t', '3', '/nobreak'], { stdio: 'ignore', shell: true });
  }
  console.log('server up for ' + version);
}

const client = spawnSync(java, [...wrapper, 'runClient', '--no-daemon', ...props, `-Pdevtest=${mode}`, '-Pquickjoin=127.0.0.1:25600'],
  { cwd: __dirname, encoding: 'utf8', maxBuffer: 1 << 28, timeout: 900000 });
fs.writeFileSync(path.join(logs, `client-legacy-${version}.log`), (client.stdout || '') + (client.stderr || ''));
if (server) server.kill();
spawnSync('powershell', ['-NoProfile', '-Command',
  `Get-CimInstance Win32_Process -Filter "Name='java.exe'" | Where-Object { $_.CommandLine -like '*tatnat-client-mod-b*legacy*' -or $_.CommandLine -like '*legacy*run*${version}*' } | ForEach-Object { Stop-Process -Id $_.ProcessId -Force -ErrorAction SilentlyContinue }`], { stdio: 'ignore' });

const out = (client.stdout || '') + (client.stderr || '');
const problems = out.split('\n').filter(l => /\[devtest\].*FAILED|Mixin apply failed|InvalidInjectionException|---- Minecraft Crash Report/.test(l));
const shots = fs.readdirSync(path.join(run, 'screenshots')).filter(f => f.startsWith('devtest-'));
console.log(`client exit ${client.status}; ${shots.length} screenshots; ${problems.length} problems`);
for (const p of problems.slice(0, 10)) console.log('  ' + p.trim());
process.exit(problems.length || client.status ? 1 : 0);
