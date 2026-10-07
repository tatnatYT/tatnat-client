#!/bin/bash
# compile.sh <mc> <src> [n]: compile one legacy target and print the first n errors (JAVA_HOME = JDK 21).
cd "$(dirname "$0")"
"$JAVA_HOME/bin/java" -cp ../gradle/wrapper/gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain compileJava --no-daemon -q \
  -Pminecraft_version=$1 -Pminecraft_dependency=$1 -Pplatform_source=$2 > ../build/legacy-compile-$1.log 2>&1
echo "exit $?"
grep -E "error:" -A2 ../build/legacy-compile-$1.log | grep -v "^--$" | sed 's#.*/client/##' | head -${3:-60}
grep -c "error:" ../build/legacy-compile-$1.log
