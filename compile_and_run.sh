#!/usr/bin/env bash

# switch to the root directory
cd "$(dirname "$0")"

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

input2="src/main/nitro/debug/current_program.nitro"
output2="out/tmp_program.wasm"
cache2="out/cache4"

function cleanup() {
    # Clean up temporary file
    mv "$output" "out/compiler_v0.wasm"
    rm -f "$output2"
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
wasmer run --mapdir "/src:$(realpath ./src)" --mapdir "/out:$(realpath ./out)" "$compiler" -- "$input" "$output" "$cache" || fail "Compilation failed"

if [ ! -f "$output" ]; then
    log "Compilation did not produce output"
    exit -1
fi

mkdir -p "$cache"

log "Running compiler $output"
wasm2wat -o "$output.wat" "$output"
#src/main/resources/runtimes/deno_compiler.ts "file://$output" "$input2" "$output2" "$cache2" || fail "Second compilation failed"
wasmer run --mapdir "/src:$(realpath ./src)" --mapdir "/out:$(realpath ./out)" "$output" -- "$input2" "$output2" "$cache2"

log "Running program $output2"
wasm2wat -o "$output2.wat" "$output2"
wasmer run --mapdir "/src:$(realpath ./src)" --mapdir "/out:$(realpath ./out)" "$output2" -- "$@"

cleanup
log "Success"