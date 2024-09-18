#!/usr/bin/env bash

version="0.0.5"

cp "out/compiler_v0.wasm" "releases/compiler_v$version.wasm"

wasm-opt -O4 "releases/compiler_v$version.wasm" -o "releases/compiler_v$version-opt.wasm"

echo "compiler_v$version-opt.wasm" > "releases/latest.txt"