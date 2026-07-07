#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"
mkdir -p build/classes
find java/src -name "*.java" | sort > sources.txt
javac -encoding UTF-8 -d build/classes @sources.txt
echo "[OK] Compilado en $ROOT/build/classes"
