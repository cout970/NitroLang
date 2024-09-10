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

input=$(realpath "src/main/nitro/compiler/main.nitro")
output=$(realpath "out/tmp_compiler.wasm")
cache=$(realpath "out/cache0")

input2=$(realpath "src/main/nitro/debug/current_program.nitro")
output2=$(realpath "out/tmp_program.wasm")
cache2=$(realpath "out/cache4")

function fail
{
    echo "$1"
    rm -f "$output"
    rm -f "$output2"
    exit -1
}

mkdir -p "$cache"

function log {
    printf "\e[38;5;039m"
    echo "$@"
    printf '\e[0m';
}

log "Compiling compiler $compiler"
src/main/resources/runtimes/deno_compiler.ts "file://$compiler" "$input" "$output" "$cache" || fail "Compilation failed"

if [ ! -f "$output" ]; then
    log "Compilation did not produce output"
    exit -1
fi

mkdir -p "$cache"

log "Running compiler $output"
src/main/resources/runtimes/deno_compiler.ts "file://$output" "$input2" "$output2" "$cache2" || fail "Second compilation failed"

log "Running program $output2"
src/main/resources/runtimes/deno_compiler.ts "file://$output2" "$@" || fail "Execution failed"

# Clean up temporary file
rm "$output"
rm "$output2"

log "Success"