#!/usr/bin/env bash
set -e

# cd current directory
cd "$(dirname "$0")"

# Wait for code initialization
#sleep 1

# Decode hex to wasm
#cat output.wasm.hex | xxd -r -p > output.wasm

# Create wat version for debugging
wasm2wat --no-check -o output.wat output.wasm
# Run a second time to get check errors and use the first output to debug
wasm2wat -o output.wat output.wasm

# Print the wat version
#cat output.wat

# Run the wasm file
echo "--- Running wasm file ---"
./deno_wrapper2.ts
echo "--- Finished ---"