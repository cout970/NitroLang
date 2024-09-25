#!/usr/bin/env bash

# switch to the root directory
cd "$(dirname "$0")/.."

# detect is wasmer is installed
if ! command -v wasmer &> /dev/null
then
    echo "Command 'wasmer' not found, please install https://wasmer.io/"
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

log "Compiling $input"
wasmer run \
  --mapdir "/src:$(realpath ./src)" --mapdir "/out:$(realpath ./out)"  --mapdir "/:$(realpath .)" \
  "$compiler" -- "$input" -o "$output" --cache-dir "$cache"

if [ ! -f "$output" ]; then
    log "Compilation did not produce output"
    exit -1
fi

log "Running $input"
wasmer run \
  --mapdir "/src:$(realpath ./src)" --mapdir "/out:$(realpath ./out)"  --mapdir "/:$(realpath .)" \
   "$output" -- "$@" || exit -1

# Clean up temporary file
rm "$output"

log "Success"
