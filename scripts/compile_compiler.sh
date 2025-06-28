#!/usr/bin/env bash

# switch to the root directory
cd "$(dirname "$0")/.."

# detect is deno is installed
if ! command -v deno &> /dev/null
then
    echo "Command 'deno' not found, please install https://deno.com/"
    exit
fi

prev_compiler=$(realpath "releases/$(cat releases/latest.txt)")

src_compiler="src/main/nitro/compiler/main.nitro"
out_compiler="out/tmp_compiler.wasm"
out_test_program="out/test_program.wasm"

cache="out/cache1"
cache2="out/cache2"

src_test_program="$1"
shift

# Default value iof no input is provided:
if [ -z "$src_test_program" ]; then
    src_test_program="src/main/nitro/compiler/main.nitro"
fi

mkdir -p "$cache"
mkdir -p "$cache2"

function cleanup() {
    # Clean up temporary file
    echo "Cleaning up"
    rm -f "$out_compiler"
}

function fail() {
    echo "$1"
    cleanup
    exit -1
}

function log {
    printf "\e[38;5;039m"
    echo "$@"
    printf '\e[0m'
}

function run {
    log "Running: $1"
    wasmer run \
      --mapdir "/src:$(realpath ./src)" \
      --mapdir "/out:$(realpath ./out)" \
      --mapdir "/:$(realpath .)" \
      "$@" || fail "Command failed: $@"
}

log "Compiling compiler $prev_compiler"
run "$prev_compiler" -- "$src_compiler" -o "$out_compiler" --cache-dir "$cache" --verbose || fail "Compilation failed"

if [ ! -f "$out_compiler" ]; then
    log "Compilation did not produce output"
    exit -1
fi

log "Testing compiler $out_compiler"
wasm2wat -o "$out_compiler.wat" "$out_compiler"
run "$out_compiler" -- "$src_test_program" -o "$out_test_program" --cache-dir "$cache2" --verbose || fail "Compilation failed"

log "Generated $out_test_program"

wasm2wat -o "$out_test_program.wat" "$out_test_program"
log "Generated $out_test_program.wat"
