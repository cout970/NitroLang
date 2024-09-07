#!/usr/bin/env bash
# This script is called by the old compiler during the bootstrapping process

set -e

# switch to the root directory
cd "$(dirname "$0")"

mkdir -p out
out=$(realpath './out')
main="src/main/nitro/compiler/main.nitro"

mkdir -p $out/cache0
mkdir -p $out/cache1
mkdir -p $out/cache2
mkdir -p $out/cache3

echo "--- Step 1 ---"

# Create wat version for debugging
wasm2wat --no-check -o $out/compiler_v0.wat $out/compiler_v0.wasm
# Run a second time to get check errors and use the first output to debug
wasm2wat -o $out/compiler_v0.wat $out/compiler_v0.wasm

echo "--- Running compiler_v0.wasm ---"
src/main/resources/runtimes/deno_compiler.ts "file://$(realpath "$out/compiler_v0.wasm")" "$main" "$out/compiler_v1.wasm" "$out/cache1" || exit -1

echo "--- Step 2 ---"

# Create wat version for debugging
wasm2wat --no-check -o $out/compiler_v1.wat $out/compiler_v1.wasm
# Run a second time to get check errors and use the first output to debug
wasm2wat -o $out/compiler_v1.wat $out/compiler_v1.wasm

echo "--- Running compiler_v1 ---"
src/main/resources/runtimes/deno_compiler.ts "file://$(realpath "$out/compiler_v1.wasm")" "$main" "$out/compiler_v2.wasm" "$out/cache2" || exit -1

echo "--- Step 3 ---"

# Create wat version for debugging
wasm2wat --no-check -o $out/compiler_v2.wat $out/compiler_v2.wasm
# Run a second time to get check errors and use the first output to debug
wasm2wat -o $out/compiler_v2.wat $out/compiler_v2.wasm

echo "--- Running compiler_v2 ---"
src/main/resources/runtimes/deno_compiler.ts "file://$(realpath "$out/compiler_v2.wasm")" "$main" "$out/compiler_v3.wasm" "$out/cache3" || exit -1

# Create wat version for debugging
wasm2wat --no-check -o $out/compiler_v3.wat $out/compiler_v3.wasm
# Run a second time to get check errors and use the first output to debug
wasm2wat -o $out/compiler_v3.wat $out/compiler_v3.wasm

echo "--- Success ---"