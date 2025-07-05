#!/usr/bin/env bash

# switch to the root directory
cd "$(dirname "$0")/../.."

compiler=$(realpath "releases/$(cat releases/latest.txt)")
input="${1:-src/examples/html_canvas.nitro}"
output="out/program_wasm32-js.wasm"
cache="out/cache0"

function run() {
    wasmer run --mapdir "/src:$(realpath ./src)" --mapdir "/out:$(realpath ./scripts/run_in_browser/out)"  --mapdir "/:$(realpath .)" "$@"
}

echo "Compiling $input"
run "$compiler" -- "$input" -o "$output" --cache-dir "$cache" --target wasm32-js

if [ -f "$output" ]; then
    if command -v wasm2wat &> /dev/null; then
        echo "Creating wat file for $output"
        wasm2wat "$output" -o "$output.wat"
    else
        echo "wasm2wat not found, skipping wat file generation"
    fi
else
    echo "Compilation failed, output file not found"
fi
