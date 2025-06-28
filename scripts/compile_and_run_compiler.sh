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
src_compiler="src/main/nitro/compiler/main.nitro"
new_compiler="out/tmp_compiler_wasm32-wasi.wasm"
new_compiler_debug="out/tmp_compiler_wasm32-js.wasm"

out_program="out/tmp_program_wasm32-wasi.wasm"
out_program_debug="out/tmp_program_wasm32-js.wasm"

cache="out/cache0"
cache2="out/cache4"

dump_ir=n

src_program="$1"
shift

# Default value if no input is provided:
if [ -z "$src_program" ]; then
    src_program="src/main/nitro/debug/current_program.nitro"
fi

function fail() {
    echo "$1"
    exit -1
}

mkdir -p "$cache"

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

log "Compiling compiler $compiler"

rm -f "$new_compiler"
run "$compiler" -- "$src_compiler" -o "$new_compiler" --cache-dir "$cache" --core-path src/main/nitro/core/core.nitro --verbose
run "$compiler" -- "$src_compiler" -o "$new_compiler_debug" --cache-dir "$cache" --core-path src/main/nitro/core/core.nitro --target 'wasm32-js'

if [ ! -f "$new_compiler" ]; then
    log "Compilation did not produce output"
    exit -1
fi

mkdir -p "$cache"

if [ "$dump_ir" = "y" ]; then
    log "Dumping IR"
    run "$new_compiler" -- "$src_program" -o "$out_program.ir" --cache-dir "$cache2" --core-path src/main/nitro/core/core.nitro --dump-ir
    exit
fi

log "Running compiler $new_compiler"
cp "$new_compiler" "out/compiler.wasm"
wasm2wat --no-check -o "$new_compiler.wat" "$new_compiler"

rm -f "$out_program"

run "$new_compiler" -- "$src_program" --dump-ir -o "$out_program.ir" --core-path src/main/nitro/core/core.nitro --verbose
run "$new_compiler" -- "$src_program" -o "$out_program" --core-path src/main/nitro/core/core.nitro --verbose

#run "$new_compiler" -- "$src_program" -o "$out_program_debug" --core-path src/main/nitro/core/core.nitro --target 'wasm32-js' --verbose

wasm2wat --no-check -o "$out_program.wat" "$out_program"
#wasm2wat --no-check -o "$out_program_debug.wat" "$out_program_debug"

log "Running program $out_program"
run "$out_program" -- "$@" | tee out/output.txt

log "Success"
