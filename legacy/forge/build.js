// Turns the Legacy Fabric 1.8.9 build into a Forge 1.8.9 mod (same code, same mixins):
//   1. remap intermediary -> SRG (Legacy Fabric intermediary joined with MCP's joined.srg on the
//      official names), refmap included;
//   2. swap the Fabric entry point / LoaderInfo for the Forge ones in src/;
//   3. shade Mixin 0.7.11 and boot it through LaunchWrapper's MixinTweaker (Forge 1.8.9 has none).
//   node build.js [--skip-legacy]   (JAVA_HOME = JDK 21, JAVA8_HOME = JDK 8)
const fs = require('fs');
const path = require('path');
const os = require('os');
const { spawnSync } = require('child_process');
const AdmZip = require(path.join(__dirname, '..', '..', '..', 'minecraft-launcher', 'node_modules', 'adm-zip'));

const MC = '1.8.9';
const ROOT = path.join(__dirname, '..', '..');
const CACHE = path.join(__dirname, '.cache');
const WORK = path.join(__dirname, 'build');
const INTERMEDIARY = path.join(os.homedir(), '.gradle', 'caches', 'fabric-loom', MC, 'legacy-intermediary-1-v2.tiny');
const FABRIC_JAR = path.join(ROOT, 'dist', `tatnat-client-mc${MC}-1.0.0.jar`);
const OUT = path.join(ROOT, 'dist', `tatnat-client-forge-mc${MC}-1.0.0.jar`);
const java = name => path.join(process.env[name] || process.env.JAVA_HOME, 'bin', 'java.exe');
const javac = path.join(process.env.JAVA_HOME, 'bin', 'javac.exe');

const TOOLS = {
  srg: ['mcp-srg.zip', 'https://maven.minecraftforge.net/de/oceanlabs/mcp/mcp/1.8.9/mcp-1.8.9-srg.zip'],
  remapper: ['tiny-remapper.jar', 'https://maven.fabricmc.net/net/fabricmc/tiny-remapper/0.10.4/tiny-remapper-0.10.4-fat.jar'],
  mixin: ['mixin-0.7.11.jar', 'https://repo.spongepowered.org/repository/maven-public/org/spongepowered/mixin/0.7.11-SNAPSHOT/mixin-0.7.11-20180703.121122-1.jar'],
  forge: ['forge-universal.jar', 'https://maven.minecraftforge.net/net/minecraftforge/forge/1.8.9-11.15.1.2318-1.8.9/forge-1.8.9-11.15.1.2318-1.8.9-universal.jar'],
  log4j: ['log4j-api.jar', 'https://repo1.maven.org/maven2/org/apache/logging/log4j/log4j-api/2.0-beta9/log4j-api-2.0-beta9.jar'],
};

function run(cmd, args, opts = {}) {
  const r = spawnSync(cmd, args, { stdio: 'inherit', ...opts });
  if (r.status !== 0) throw new Error(`${path.basename(cmd)} ${args.slice(0, 3).join(' ')} ... failed (${r.status})`);
}

async function tool(key) {
  const [file, url] = TOOLS[key];
  const dest = path.join(CACHE, file);
  if (!fs.existsSync(dest)) {
    const res = await fetch(url);
    if (!res.ok) throw new Error(`HTTP ${res.status} for ${url}`);
    fs.mkdirSync(CACHE, { recursive: true });
    fs.writeFileSync(dest, Buffer.from(await res.arrayBuffer()));
  }
  return dest;
}

// ------------------------------------------------------------------ mappings

function buildMappings(srgZip) {
  // official -> intermediary
  const cls = new Map(), meth = new Map(), field = new Map(); // official keys
  let owner = null;
  for (const line of fs.readFileSync(INTERMEDIARY, 'utf8').split('\n')) {
    const p = line.replace(/\r$/, '').split('\t');
    if (p[0] === 'c') {
      owner = p[1];
      cls.set(p[1], p[2]);
    } else if (p[1] === 'm') {
      meth.set(`${owner}.${p[3]}${p[2]}`, { inter: p[4], desc: p[2] });
    } else if (p[1] === 'f') {
      field.set(`${owner}.${p[3]}`, { inter: p[4], desc: p[2] });
    }
  }
  // official -> srg
  const srgCls = new Map(), srgMeth = new Map(), srgField = new Map();
  for (const line of new AdmZip(srgZip).readAsText('joined.srg').split('\n')) {
    const p = line.trim().split(' ');
    if (p[0] === 'CL:') srgCls.set(p[1], p[2]);
    else if (p[0] === 'FD:') {
      const i = p[1].lastIndexOf('/');
      srgField.set(`${p[1].slice(0, i)}.${p[1].slice(i + 1)}`, p[2].slice(p[2].lastIndexOf('/') + 1));
    } else if (p[0] === 'MD:') {
      const i = p[1].lastIndexOf('/');
      srgMeth.set(`${p[1].slice(0, i)}.${p[1].slice(i + 1)}${p[2]}`, p[3].slice(p[3].lastIndexOf('/') + 1));
    }
  }
  const interDesc = d => d.replace(/L([^;]+);/g, (m, c) => `L${cls.get(c) || c};`);
  const lines = ['v1\tintermediary\tsrg'];
  const names = new Map(); // intermediary name -> srg (members are unique per family) + classes
  for (const [off, inter] of cls) {
    const srg = srgCls.get(off) || off;
    lines.push(`CLASS\t${inter}\t${srg}`);
    names.set(inter, srg);
  }
  for (const [key, { inter, desc }] of meth) {
    const srg = srgMeth.get(key);
    if (!srg || srg === inter) continue;
    const ownerOff = key.slice(0, key.indexOf('.'));
    lines.push(`METHOD\t${cls.get(ownerOff)}\t${interDesc(desc)}\t${inter}\t${srg}`);
    names.set(inter, srg);
  }
  for (const [key, { inter, desc }] of field) {
    const srg = srgField.get(key);
    if (!srg) continue;
    const ownerOff = key.slice(0, key.indexOf('.'));
    lines.push(`FIELD\t${cls.get(ownerOff)}\t${interDesc(desc)}\t${inter}\t${srg}`);
    names.set(inter, srg);
  }
  return { tiny: lines.join('\n') + '\n', names };
}

// Rewrites one refmap entry ("Lowner;name(desc)", "Lowner;name:desc", "pkg/Class") into SRG names.
function remapRef(s, names) {
  return s.replace(/net\/minecraft\/class_\d+|method_\d+|field_\d+/g, t => names.get(t) || t);
}

// ------------------------------------------------------------------ build

(async () => {
  if (!process.argv.includes('--skip-legacy')) {
    const props = fs.readFileSync(path.join(ROOT, 'legacy', 'gradle.properties'), 'utf8');
    run(java('JAVA_HOME'), ['-cp', path.join(ROOT, 'gradle', 'wrapper', 'gradle-wrapper.jar'), 'org.gradle.wrapper.GradleWrapperMain',
      'collectJar', '--no-daemon', '-q'], { cwd: path.join(ROOT, 'legacy') });
    if (!props) throw new Error('legacy/gradle.properties missing');
  }
  const [srgZip, remapper, mixin, forge, log4j] = await Promise.all(['srg', 'remapper', 'mixin', 'forge', 'log4j'].map(tool));
  fs.rmSync(WORK, { recursive: true, force: true });
  fs.mkdirSync(path.join(WORK, 'classes'), { recursive: true });

  const { tiny, names } = buildMappings(srgZip);
  const mappings = path.join(WORK, 'intermediary-srg.tiny');
  fs.writeFileSync(mappings, tiny);

  // 1. remap the classes
  const mcJar = fs.readdirSync(path.join(os.homedir(), '.gradle', 'caches', 'fabric-loom', 'minecraftMaven', 'net', 'minecraft'))
    .filter(d => d.startsWith('minecraft-merged-legacy-intermediary') && d.endsWith('-intermediary'))
    .map(d => path.join(os.homedir(), '.gradle', 'caches', 'fabric-loom', 'minecraftMaven', 'net', 'minecraft', d))
    .flatMap(d => fs.readdirSync(d).map(v => path.join(d, v)))
    .flatMap(d => fs.readdirSync(d).filter(f => f.endsWith('.jar')).map(f => path.join(d, f)))
    .find(f => f.includes(MC));
  if (!mcJar) throw new Error('intermediary Minecraft jar not found in the Loom cache; build legacy first');
  const remapped = path.join(WORK, 'remapped.jar');
  run(java('JAVA_HOME'), ['-cp', remapper, path.join(__dirname, 'Remap.java'), FABRIC_JAR, remapped, mappings, mcJar]);

  // 2. Forge entry point + LoaderInfo
  run(javac, ['--release', '8', '-nowarn', '-encoding', 'UTF-8', '-cp', [forge, log4j, remapped].join(path.delimiter), '-d', path.join(WORK, 'classes'),
    path.join(__dirname, 'src', 'com', 'tatnat', 'client', 'mc', 'ForgeEntry.java'),
    path.join(__dirname, 'src', 'com', 'tatnat', 'client', 'mc', 'LoaderInfo.java')]);

  // 3. assemble
  const src = new AdmZip(remapped);
  const out = new AdmZip();
  let refmapName = null;
  for (const e of src.getEntries()) {
    const n = e.entryName;
    if (e.isDirectory || n === 'fabric.mod.json' || n.startsWith('META-INF/')) continue;
    if (/^com\/tatnat\/client\/mc\/(Entry|LoaderInfo)(\$.*)?\.class$/.test(n)) continue;
    if (/refmap\.json$/.test(n)) {
      refmapName = n;
      const refmap = JSON.parse(e.getData().toString('utf8'));
      const mapped = {};
      for (const [mixinClass, entries] of Object.entries(refmap.mappings || {})) {
        mapped[mixinClass] = {};
        for (const [k, v] of Object.entries(entries)) mapped[mixinClass][k] = remapRef(v, names);
      }
      out.addFile(n, Buffer.from(JSON.stringify({ mappings: mapped, data: { searge: mapped } }, null, 2)));
      continue;
    }
    out.addFile(n, e.getData());
  }
  const classesDir = path.join(WORK, 'classes', 'com', 'tatnat', 'client', 'mc');
  for (const f of fs.readdirSync(classesDir)) out.addFile(`com/tatnat/client/mc/${f}`, fs.readFileSync(path.join(classesDir, f)));
  out.addFile('mcmod.info', fs.readFileSync(path.join(__dirname, 'mcmod.info')));
  // Mixin's JSON config must name the refmap explicitly (Loom adds it to the Fabric jar's copy at build time).
  const cfg = JSON.parse(out.readAsText('tatnatclient.mixins.json'));
  if (refmapName) cfg.refmap = refmapName;
  out.updateFile('tatnatclient.mixins.json', Buffer.from(JSON.stringify(cfg, null, '\t')));
  for (const e of new AdmZip(mixin).getEntries()) {
    // Keep META-INF/services: Mixin finds its LaunchWrapper service through ServiceLoader.
    if (e.isDirectory || (e.entryName.startsWith('META-INF/') && !e.entryName.startsWith('META-INF/services/'))) continue;
    if (!out.getEntry(e.entryName)) out.addFile(e.entryName, e.getData());
  }
  out.addFile('META-INF/MANIFEST.MF', Buffer.from([
    'Manifest-Version: 1.0',
    'TweakClass: org.spongepowered.asm.launch.MixinTweaker',
    'TweakOrder: 0',
    'MixinConfigs: tatnatclient.mixins.json,tatnatclient.forge.mixins.json',
    'FMLCorePluginContainsFMLMod: true',
    'ForceLoadAsMod: true',
    '', ''].join('\r\n')));
  out.writeZip(OUT);
  console.log(`built ${path.relative(ROOT, OUT)} (refmap ${refmapName || 'none'})`);
})().catch(e => {
  console.error(e.stack || e);
  process.exit(1);
});
