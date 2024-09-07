#!/usr/bin/env bash
set -e

# cd current directory
cd "$(dirname "$0")"

# Create wat version for debugging
wasm2wat --no-check -o test.wat test.wasm
# Run a second time to get check errors and use the first output to debug
wasm2wat -o test.wat test.wasm

# Run the wasm file
echo "--- Running wasm file ---"
./deno_test_wrapper.ts > output.txt || exit -1

echo "--- Comparing outputs ---"
diff --color=always expected_output.txt output.txt