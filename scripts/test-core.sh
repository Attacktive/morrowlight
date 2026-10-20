#!/bin/sh
set -eu
cd "$(dirname "$0")/.."
command -v kotlinc >/dev/null 2>&1 || { echo 'Kotlin CLI 1.9+ is required.' >&2; exit 1; }

work=$(mktemp -d)
trap 'rm -rf "$work"' EXIT HUP INT TERM
kotlinc core/src/main/kotlin/xyz/attacktive/morrowlight/core/*.kt \
	core/src/test/kotlin/xyz/attacktive/morrowlight/core/Scenarios.kt \
	-include-runtime -d "$work/scenarios.jar"
java -jar "$work/scenarios.jar"
