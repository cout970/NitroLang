#!/usr/bin/env bash

# switch to the root directory
cd "$(dirname "$0")/.."

# detect is wasmtime is installed
if ! command -v wasmtime &> /dev/null
then
    echo "Command 'wasmtime' not found, please install https://wasmtime.dev/"
    exit
fi

input="$1"
shift

if [ -z "$input" ]; then
    echo "Please provide a file to run"
    exit
fi

output="out/tmp_program.wasm"
cache="out/cache0"
compiler=$(realpath "releases/$(cat releases/latest.txt)")

mkdir -p "$cache"

function log {
    printf "\e[38;5;039m"
    echo "$@"
    printf '\e[0m';
}

root="$(realpath .)"

# Clean up temporary file
rm "$output" 2> /dev/null

log "Compiling $input"
wasmtime run \
  --dir "$root" \
  "$compiler" "$input" -o "$output" --cache-dir "$cache" --verbose

if [ ! -f "$output" ]; then
    log "Compilation did not produce output"
    exit -1
fi

log "Running $input"
wasmtime run \
  --dir "$root" \
   "$output" "$@" || exit -1

log "Success"
