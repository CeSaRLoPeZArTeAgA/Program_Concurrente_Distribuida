#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
rm -rf build
mkdir -p build
find src -name "*.java" > sources.txt
javac -encoding UTF-8 -d build @sources.txt
printf '\nCompilado OK. Ejecuta con: java -cp build cc4p1.raft.RaftNode ...\n'
