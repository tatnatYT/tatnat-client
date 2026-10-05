// Builds the mod for one or all targets in targets.json.
//   node build.js            -> every target, jars collected in dist/
//   node build.js 1.20.1     -> just that target
//   node build.js 1.20.1 compileJava   -> run another Gradle task instead of collectJar
// Needs JAVA_HOME pointing at a JDK 21+ (Gradle itself), whatever the target's Java level.
const { spawnSync } = require('child_process');
const fs = require('fs');
const path = require('path');

const { targets } = JSON.parse(fs.readFileSync(path.join(__dirname, 'targets.json'), 'utf8'));
const [only, task = 'collectJar', ...extra] = process.argv.slice(2);
const list = only && only !== 'all' ? targets.filter(t => t.mc === only) : targets;
if (!list.length) {
  console.error(`No target "${only}". Known: ${targets.map(t => t.mc).join(', ')}`);
  process.exit(1);
}

// Run the Gradle wrapper through Java directly: no shell, so version ranges like ">=1.21.9 <1.21.11"
// are passed as-is instead of being read as redirects by cmd.exe.
const java = path.join(process.env.JAVA_HOME || '', 'bin', process.platform === 'win32' ? 'java.exe' : 'java');
const wrapper = ['-cp', path.join(__dirname, 'gradle', 'wrapper', 'gradle-wrapper.jar'), 'org.gradle.wrapper.GradleWrapperMain'];
const failed = [];
for (const t of list) {
  console.log(`\n=== ${t.mc} (${t.src}, supports ${t.dep}) ===`);
  const args = [task, '--no-daemon', '-q',
    `-Pminecraft_version=${t.mc}`, `-Pminecraft_dependency=${t.dep}`, `-Pplatform_source=${t.src}`,
    `-Pjava_version=${t.java}`, `-Pfabric_api_version=${t.api}`, ...extra];
  const r = spawnSync(java, [...wrapper, ...args], { cwd: __dirname, stdio: 'inherit' });
  if (r.status !== 0) failed.push(t.mc);
}
if (failed.length) {
  console.error(`\nFAILED: ${failed.join(', ')}`);
  process.exit(1);
}
console.log('\nAll targets built.');
