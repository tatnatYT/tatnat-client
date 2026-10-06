// In-game test of the NeoForge build: boots the NeoForge dev server, joins it with the dev client
// running the shared scripted test (DevTestSteps), then stops the server.
//   node devtest.js 1.21.1 [main|smoke]   (JAVA_HOME = JDK 21)
const { spawn, spawnSync } = require('child_process');
const fs = require('fs');
const path = require('path');
const crypto = require('crypto');

const PORT = 25601;
const [version, mode = 'smoke'] = process.argv.slice(2);
const { targets } = JSON.parse(fs.readFileSync(path.join(__dirname, 'targets.json'), 'utf8'));
const t = targets.find(x => x.mc === version);
if (!t) { console.error('unknown target ' + version); process.exit(1); }

const run = path.join(__dirname, 'run', version);
fs.mkdirSync(path.join(run, 'screenshots'), { recursive: true });
fs.mkdirSync(path.join(__dirname, 'build'), { recursive: true });
fs.writeFileSync(path.join(run, 'eula.txt'), 'eula=true\n');
fs.writeFileSync(path.join(run, 'server.properties'), ['online-mode=false', `server-port=${PORT}`, 'spawn-protection=0',
  'level-seed=tatnat', 'difficulty=peaceful', 'view-distance=8', 'simulation-distance=6', 'motd=tatnat neoforge dev'].join('\n') + '\n');
const opts = path.join(run, 'options.txt');
if (!fs.existsSync(opts)) fs.writeFileSync(opts, ['onboardAccessibility:false', 'skipMultiplayerWarning:true', 'joinedFirstServer:true', 'tutorialStep:none', 'narrator:0'].join('\n') + '\n');
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
const props = [`-Pminecraft_version=${t.mc}`, `-Pminecraft_range=${t.mcRange}`, `-Pneoforge_version=${t.neoforge}`,
  `-Pneoforge_range=${t.neoRange}`, `-Pplatform_source=${t.src}`];

const serverLog = path.join(__dirname, 'build', `server-${version}.log`);
const log = fs.openSync(serverLog, 'w');
const server = spawn(java, [...wrapper, 'runServer', '--no-daemon', ...props], { cwd: __dirname, stdio: ['ignore', log, log] });
const deadline = Date.now() + 900000;
for (;;) {
  const txt = fs.existsSync(serverLog) ? fs.readFileSync(serverLog, 'utf8') : '';
  if (/Done \(/.test(txt)) break;
  if (/BUILD FAILED|Exception in server tick loop|FAILURE/.test(txt) || Date.now() > deadline) { console.log(txt.slice(-3000)); process.exit(1); }
  spawnSync('timeout', ['/t', '3', '/nobreak'], { stdio: 'ignore', shell: true });
}
console.log('server up for neoforge ' + version);

const client = spawnSync(java, [...wrapper, 'runClient', '--no-daemon', ...props, `-Pdevtest=${mode}`, `-Pquickjoin=127.0.0.1:${PORT}`],
  { cwd: __dirname, encoding: 'utf8', maxBuffer: 1 << 28, timeout: 900000 });
const out = (client.stdout || '') + (client.stderr || '');
fs.writeFileSync(path.join(__dirname, 'build', `client-${version}.log`), out);
server.kill();
// Gradle leaves the server JVM running; stop the NeoForge dev JVMs from this folder.
spawnSync('powershell', ['-NoProfile', '-Command',
  `Get-CimInstance Win32_Process -Filter "Name='java.exe'" | Where-Object { $_.CommandLine -like '*tatnat-client-mod-b*neoforge*' -and $_.CommandLine -notlike '*GradleDaemon*' -and $_.CommandLine -notlike '*GradleWrapperMain*' } | ForEach-Object { Stop-Process -Id $_.ProcessId -Force -ErrorAction SilentlyContinue }`], { stdio: 'ignore' });

const problems = out.split('\n').filter(l => /\[devtest\].*FAILED|Mixin apply failed|InvalidInjectionException|---- Minecraft Crash Report/.test(l));
const shots = fs.readdirSync(path.join(run, 'screenshots')).filter(f => f.startsWith('devtest-'));
console.log(`client exit ${client.status}; ${shots.length} screenshots; ${problems.length} problems`);
for (const p of problems.slice(0, 10)) console.log('  ' + p.trim());
process.exit(problems.length || client.status ? 1 : 0);
