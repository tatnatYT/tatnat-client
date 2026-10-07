#!/bin/bash
# buildall.sh [mc...]: collectJar for each Forge target (all when none given).
# Needs JAVA_HOME (JDK 21) and JAVA17_HOME (JDK 17).
cd "$(dirname "$0")"
list="$@"; [ -z "$list" ] && list=$(node -e "console.log(require('./targets.json').targets.map(t=>t.mc).join(' '))")
for v in $list; do
  args=$(node -e "const t=require('./targets.json').targets.find(x=>x.mc==='$v');console.log([\`-Pminecraft_version=\${t.mc}\`,\`-Pminecraft_range=\${t.mcRange}\`,\`-Pforge_version=\${t.forge}\`,\`-Pforge_range=\${t.forgeRange}\`,\`-Pplatform_source=\${t.src}\`,\`-Pjava_version=\${t.java}\`].join(' '))")
  "$JAVA_HOME/bin/java" -cp ../gradle/wrapper/gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain collectJar --no-daemon -q $args \
    "-Porg.gradle.java.installations.paths=$JAVA17_HOME" -Porg.gradle.java.installations.auto-download=false > build/build-$v.log 2>&1
  echo "$v: exit $? $(grep -m3 -E 'error:|FAILED|What went wrong' -A2 build/build-$v.log | tr '\n' ' ' | cut -c1-400)"
done
echo FORGEDONE
