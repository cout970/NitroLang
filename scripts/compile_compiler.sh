#!/usr/bin/env bash

# switch to the root directory
cd "$(dirname "$0")/.."

# detect is deno is installed
if ! command -v deno &> /dev/null
then
    echo "Command 'deno' not found, please install https://deno.com/"
    exit
fi

compiler=$(realpath "releases/$(cat releases/latest.txt)")

input="src/main/nitro/compiler/main.nitro"
output="out/tmp_compiler.wasm"
cache="out/cache0"

input2="$1"
shift

# Default value iof no input is provided:
if [ -z "$input2" ]; then
#    input2="src/main/nitro/debug/current_program.nitro"
    input2="src/main/nitro/compiler/main.nitro"
fi

output2="out/compiler.wasm"
cache2="out/cache4"

function cleanup() {
    # Clean up temporary file
    echo "Cleaning up"
    rm -f "$output"
}

function fail() {
    echo "$1"
    cleanup
    exit -1
}

mkdir -p "$cache"

function log {
    printf "\e[38;5;039m"
    echo "$@"
    printf '\e[0m'
}

log "Compiling compiler $compiler"
wasmer run --mapdir "/src:$(realpath ./src)" \
           --mapdir "/out:$(realpath ./out)" \
           --mapdir "/:$(realpath .)" \
           "$compiler" -- "$input" -o "$output" --cache-dir "$cache" || fail "Compilation failed"

if [ ! -f "$output" ]; then
    log "Compilation did not produce output"
    exit -1
fi

mkdir -p "$cache"

log "Testing compiler $output"
wasm2wat -o "$output.wat" "$output"
wasmer run --mapdir "/src:$(realpath ./src)" \
           --mapdir "/out:$(realpath ./out)" \
           --mapdir "/:$(realpath .)" \
           "$output" -- "$input2" -o "$output2" --cache-dir "$cache2" || fail "Compilation failed"

log "Generated $output2"

wasm2wat -o "$output2.wat" "$output2"
log "Generated $output2.wat"