#!/usr/bin/env bash

# switch to the root directory
cd "$(dirname "$0")/.."

version="0.0.8"

cp "out/compiler.wasm" "releases/compiler_v$version.wasm"

wasm-opt --debuginfo -O4 "releases/compiler_v$version.wasm" -o "releases/compiler_v$version-opt.wasm"

echo "compiler_v$version-opt.wasm" > "releases/latest.txt"