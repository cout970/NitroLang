#!/usr/bin/env bash

# switch to the root directory
cd "$(dirname "$0")"

# detect is deno is installed
if ! command -v deno &> /dev/null
then
    echo "Command 'deno' not found, please install https://deno.com/"
    exit
fi

input=$(realpath "$1")
shift
output=$(realpath "out/tmp_program.wasm")
cache=$(realpath "out/cache0")
compiler=$(realpath "releases/compiler_v0.0.1-opt.wasm")

mkdir -p "$cache"

function log {
    printf "\e[38;5;039m"
    echo "$@"
    printf '\e[0m';
}

log "Compiling $input"
src/main/resources/runtimes/deno_compiler.ts "file://$compiler" "$input" "$output" "$cache" || exit -1

if [ ! -f "$output" ]; then
    log "Compilation did not produce output"
    exit -1
fi

log "Running $input"
src/main/resources/runtimes/deno_compiler.ts "file://$output" "$@" || exit -1

# Clean up temporary file
rm "$output"

log "Success"
