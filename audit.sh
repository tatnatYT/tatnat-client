#!/bin/bash
# audit.sh <version>: boot the dev client in audit mode and print hook failures (dev only).
# Needs JAVA_HOME (JDK 21), plus JAVA25_HOME / JAVA8_HOME for 26.x / pre-1.17 targets.
cd "$(dirname "$0")"
timeout 900 node devtest.js "$1" audit 2>&1 | grep -E "client exit|error:" | cut -c1-200
grep -E "\[audit\]|Mixin apply.*failed|Critical injection|Invalid descriptor|could not find any targets|has an invalid signature|error: " build/client-$1.log 2>/dev/null | sed -E 's/.*(Mixin apply for mod tatnatclient failed |InvalidInjectionException:? |InjectionError: )//' | cut -c1-330 | sort -u | head -8
C=$(ls -t run/$1/crash-reports 2>/dev/null | head -1); [ -n "$C" ] && [ $(( $(date +%s) - $(stat -c %Y run/$1/crash-reports/$C) )) -lt 120 ] && grep -m3 "Caused by" run/$1/crash-reports/$C | cut -c1-330
