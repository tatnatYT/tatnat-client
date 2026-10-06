// Builds every tatnat client jar (Fabric, Legacy Fabric, NeoForge) and copies them into the
// launcher's assets/mods together with builds.json, which tells the launcher which jar covers
// which Minecraft versions on which loader.
//   node bundle.js [--skip-build]   (JAVA_HOME = JDK 21, JAVA25_HOME = JDK 25)
const fs = require('fs');
const path = require('path');
const { spawnSync } = require('child_process');

const LAUNCHER_MODS = path.join(__dirname, '..', 'minecraft-launcher', 'assets', 'mods');
const DIST = path.join(__dirname, 'dist');
const skipBuild = process.argv.includes('--skip-build');

function gradle(dir, args, java = process.env.JAVA_HOME) {
  const r = spawnSync(path.join(java, 'bin', 'java.exe'),
    ['-cp', path.join(__dirname, 'gradle', 'wrapper', 'gradle-wrapper.jar'), 'org.gradle.wrapper.GradleWrapperMain', ...args, '--no-daemon', '-q'],
    { cwd: dir, stdio: 'inherit' });
  if (r.status !== 0) throw new Error(`gradle ${args.join(' ')} failed in ${dir}`);
}

// "~1.21.11" | ">=1.21 <1.21.2" | "1.8.9" | "[1.21,1.21.2)" -> { from, until } (until exclusive).
function range(dep) {
  let m;
  if ((m = dep.match(/^>=(\S+) <(\S+)$/))) return { from: m[1], until: m[2] };
  if ((m = dep.match(/^\[([^,]+),([^)]+)\)$/))) return { from: m[1], until: m[2] };
  if ((m = dep.match(/^~(\S+)$/))) {
    const p = m[1].split('.').map(Number);
    p[p.length - 1]++;
    return { from: m[1], until: p.join('.') };
  }
  return { from: dep, until: dep + '.0.1' };
}

const builds = [];
fs.rmSync(DIST, { recursive: true, force: true });

// Fabric (1.14.4 - 26.x)
const fabric = JSON.parse(fs.readFileSync(path.join(__dirname, 'targets.json'), 'utf8')).targets;
if (!skipBuild) {
  const r = spawnSync(process.execPath, [path.join(__dirname, 'build.js'), 'all'], { cwd: __dirname, stdio: 'inherit' });
  if (r.status !== 0) throw new Error('Fabric builds failed');
}
for (const t of fabric) builds.push({ loader: 'fabric', ...range(t.dep), file: `tatnat-client-mc${t.mc}-1.0.0.jar`, built: t.mc });

// Legacy Fabric (1.8.9 ...)
const legacy = JSON.parse(fs.readFileSync(path.join(__dirname, 'legacy', 'targets.json'), 'utf8')).targets;
for (const t of legacy) {
  if (!skipBuild) gradle(path.join(__dirname, 'legacy'), ['collectJar', `-Pminecraft_version=${t.mc}`, `-Pminecraft_dependency=${t.dep}`, `-Pplatform_source=${t.src}`]);
  builds.push({ loader: 'fabric', ...range(t.dep), file: `tatnat-client-mc${t.mc}-1.0.0.jar`, built: t.mc });
}

// NeoForge
const neoTargets = path.join(__dirname, 'neoforge', 'targets.json');
const neo = fs.existsSync(neoTargets) ? JSON.parse(fs.readFileSync(neoTargets, 'utf8')).targets : [];
for (const t of neo) {
  if (!skipBuild) gradle(path.join(__dirname, 'neoforge'), ['collectJar', `-Pminecraft_version=${t.mc}`, `-Pminecraft_range=${t.mcRange}`,
    `-Pneoforge_version=${t.neoforge}`, `-Pneoforge_range=${t.neoRange}`, `-Pplatform_source=${t.src}`, `-Pjava_version=${t.java}`,
    `-Porg.gradle.java.installations.paths=${[process.env.JAVA25_HOME, process.env.JAVA_HOME].filter(Boolean).join(',')}`,
    '-Porg.gradle.java.installations.auto-download=false']);
  builds.push({ loader: 'neoforge', ...range(t.mcRange), file: `tatnat-client-neoforge-mc${t.mc}-1.0.0.jar`, built: t.mc });
}

// Forge (JAVA17_HOME = JDK 17 for the 1.17 - 1.20.1 toolchain)
const forgeTargets = path.join(__dirname, 'forge', 'targets.json');
const forge = fs.existsSync(forgeTargets) ? JSON.parse(fs.readFileSync(forgeTargets, 'utf8')).targets : [];
for (const t of forge) {
  if (!skipBuild) gradle(path.join(__dirname, 'forge'), ['collectJar', `-Pminecraft_version=${t.mc}`, `-Pminecraft_range=${t.mcRange}`,
    `-Pforge_version=${t.forge}`, `-Pforge_range=${t.forgeRange}`, `-Pplatform_source=${t.src}`, `-Pjava_version=${t.java}`,
    `-Porg.gradle.java.installations.paths=${process.env.JAVA17_HOME}`, '-Porg.gradle.java.installations.auto-download=false']);
  builds.push({ loader: 'forge', ...range(t.mcRange), file: `tatnat-client-forge-mc${t.mc}-1.0.0.jar`, built: t.mc });
}

// Forge 1.8.9: the Legacy Fabric build remapped to SRG (legacy/forge/build.js)
if (!skipBuild) {
  const r = spawnSync(process.execPath, [path.join(__dirname, 'legacy', 'forge', 'build.js'), '--skip-legacy'], { cwd: __dirname, stdio: 'inherit' });
  if (r.status !== 0) throw new Error('Forge 1.8.9 build failed');
}
builds.push({ loader: 'forge', ...range('1.8.9'), file: 'tatnat-client-forge-mc1.8.9-1.0.0.jar', built: '1.8.9' });

// Copy into the launcher.
fs.rmSync(LAUNCHER_MODS, { recursive: true, force: true });
fs.mkdirSync(LAUNCHER_MODS, { recursive: true });
const missing = [];
for (const b of builds) {
  const src = path.join(DIST, b.file);
  if (!fs.existsSync(src)) { missing.push(b.file); continue; }
  fs.copyFileSync(src, path.join(LAUNCHER_MODS, b.file));
}
fs.writeFileSync(path.join(LAUNCHER_MODS, 'builds.json'), JSON.stringify({ builds: builds.filter(b => !missing.includes(b.file)) }, null, 2) + '\n');
console.log(`bundled ${builds.length - missing.length} jars into ${LAUNCHER_MODS}`);
if (missing.length) {
  console.log('missing: ' + missing.join(', '));
  process.exit(1);
}
