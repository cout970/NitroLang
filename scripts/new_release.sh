#!/usr/bin/env bash

# switch to the root directory
cd "$(dirname "$0")/.."

version=$(cat src/main/nitro/compiler/main.nitro | grep -oP 'let VERSION: String = "(.*?)"' | grep -oP "\d+\.\d+\.\d+")

if [ -z "$version" ]; then
    echo "Could not detect version"
    exit 1
fi

echo "Detected version '$version'"

cp "out/compiler.wasm" "releases/compiler_v$version.wasm"

wasm-opt --enable-bulk-memory --debuginfo -O4 "releases/compiler_v$version.wasm" -o "releases/compiler_v$version-opt.wasm"

echo "compiler_v$version-opt.wasm" > "releases/latest.txt"

echo "Done"
