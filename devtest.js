// In-game test for one target: boots that version's dev server, joins it with the dev client
// running the scripted test (DevTestSteps), then stops the server. Screenshots land in
// run/<version>/screenshots.
//   node devtest.js 1.21.10 [main|p2|smoke]
const { spawn, spawnSync } = require('child_process');
const fs = require('fs');
const path = require('path');
const crypto = require('crypto');

const [version, mode = 'smoke'] = process.argv.slice(2);
const { targets } = JSON.parse(fs.readFileSync(path.join(__dirname, 'targets.json'), 'utf8'));
const t = targets.find(x => x.mc === version);
if (!t) { console.error('unknown target ' + version); process.exit(1); }

const run = path.join(__dirname, 'run', version);
fs.mkdirSync(path.join(run, 'screenshots'), { recursive: true });
fs.writeFileSync(path.join(run, 'eula.txt'), 'eula=true\n');
const props = path.join(run, 'server.properties');
if (!fs.existsSync(props)) {
  fs.writeFileSync(props, ['online-mode=false', 'server-port=25599', 'spawn-protection=0', 'level-seed=tatnat',
    'difficulty=peaceful', 'view-distance=8', 'simulation-distance=6', 'motd=tatnat dev'].join('\n') + '\n');
}
// Skip first-launch screens (accessibility onboarding, multiplayer warning) that block auto-join.
const opts = path.join(run, "options.txt");
if (!fs.existsSync(opts)) fs.writeFileSync(opts, ["onboardAccessibility:false", "skipMultiplayerWarning:true", "joinedFirstServer:true", "tutorialStep:none", "narrator:0"].join("\n") + "\n");
// Op the dev player ("tatnat", offline UUID) so the test can use commands.
const h = crypto.createHash('md5').update('OfflinePlayer:tatnat').digest();
h[6] = (h[6] & 0x0f) | 0x30; h[8] = (h[8] & 0x3f) | 0x80;
const x = h.toString('hex');
const uuid = `${x.slice(0, 8)}-${x.slice(8, 12)}-${x.slice(12, 16)}-${x.slice(16, 20)}-${x.slice(20)}`;
fs.writeFileSync(path.join(run, 'ops.json'), JSON.stringify([{ uuid, name: 'tatnat', level: 4, bypassesPlayerLimit: false }]));
for (const f of fs.readdirSync(path.join(run, 'screenshots'))) if (f.startsWith('devtest-')) fs.rmSync(path.join(run, 'screenshots', f));
fs.rmSync(path.join(run, 'config', 'tatnat-client.json'), { force: true });
// Every run starts at world spawn (the test moves the player 400 blocks at the end).
fs.rmSync(path.join(run, 'world', 'playerdata'), { recursive: true, force: true });
fs.rmSync(path.join(run, 'world', 'players'), { recursive: true, force: true }); // 26.x

const java = path.join((t.java >= 25 && process.env.JAVA25_HOME) || process.env.JAVA_HOME, 'bin', 'java.exe');
const wrapper = ['-cp', path.join(__dirname, 'gradle', 'wrapper', 'gradle-wrapper.jar'), 'org.gradle.wrapper.GradleWrapperMain'];
const props2 = [`-Pminecraft_version=${t.mc}`, `-Pminecraft_dependency=${t.dep}`, `-Pplatform_source=${t.src}`,
  `-Pjava_version=${t.java}`, `-Pfabric_api_version=${t.api}`,
  ...(t.java === 8 && process.env.JAVA8_HOME ? [`-Porg.gradle.java.installations.paths=${process.env.JAVA8_HOME}`, '-Porg.gradle.java.installations.auto-download=false'] : [])];

const audit = mode === 'audit';
let server = { kill() {} };
if (!audit) {
const log = fs.openSync(path.join(__dirname, 'build', `server-${version}.log`), 'w');
server = spawn(java, [...wrapper, 'runServer', '--no-daemon', ...props2], { cwd: __dirname, stdio: ['ignore', log, log] });
const serverLog = path.join(__dirname, 'build', `server-${version}.log`);
const deadline = Date.now() + 600000;
while (Date.now() < deadline) {
  const txt = fs.existsSync(serverLog) ? fs.readFileSync(serverLog, 'utf8') : '';
  if (/Done \(/.test(txt)) break;
  if (/BUILD FAILED|Exception in server tick loop|FAILURE/.test(txt)) { console.log(txt.slice(-3000)); process.exit(1); }
  spawnSync(process.platform === 'win32' ? 'timeout' : 'sleep', process.platform === 'win32' ? ['/t', '3', '/nobreak'] : ['3'], { stdio: 'ignore', shell: true });
}
console.log('server up for ' + version);
}

// Never pause when the window loses focus (screenshots would show the pause menu).
{
  const o = fs.existsSync(opts) ? fs.readFileSync(opts, 'utf8') : '';
  const lines = o.split(/\r?\n/).filter(l => l && !l.startsWith('pauseOnLostFocus:'));
  lines.push('pauseOnLostFocus:false');
  fs.writeFileSync(opts, lines.join('\n') + '\n');
}
const client = spawnSync(java, [...wrapper, 'runClient', '--no-daemon', ...props2, `-Pdevtest=${mode}`, ...(audit ? [] : ['-Pquickjoin=127.0.0.1:25599'])],
  { cwd: __dirname, encoding: 'utf8', maxBuffer: 1 << 28, timeout: 900000 });
fs.writeFileSync(path.join(__dirname, 'build', `client-${version}.log`), (client.stdout || '') + (client.stderr || ''));
server.kill();
// Gradle leaves the server JVM running; stop anything launched from this project's run dir.
spawnSync('powershell', ['-NoProfile', '-Command',
  `Get-CimInstance Win32_Process -Filter "Name='java.exe'" | Where-Object { $_.CommandLine -like '*tatnat-client-mod*' -and $_.CommandLine -notlike '*GradleWrapperMain*runClient*' -and $_.CommandLine -notlike '*neoforge*' -and $_.CommandLine -notlike '*legacy*' -and $_.CommandLine -notlike '*forge*' } | ForEach-Object { Stop-Process -Id $_.ProcessId -Force -ErrorAction SilentlyContinue }`], { stdio: 'ignore' });

const out = (client.stdout || '') + (client.stderr || '');
const problems = out.split('\n').filter(l => /\[(devtest|allcheck)\].*FAILED|Event handler in .* failed|Mixin apply failed|InvalidInjectionException|---- Minecraft Crash Report/.test(l));
const shots = fs.readdirSync(path.join(run, 'screenshots')).filter(f => f.startsWith('devtest-'));
console.log(`client exit ${client.status}; ${shots.length} screenshots; ${problems.length} problems`);
for (const p of problems.slice(0, 10)) console.log('  ' + p.trim());
process.exit(problems.length || client.status ? 1 : 0);
