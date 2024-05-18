#!/usr/bin/env sh
cat output.wasm.hex | xxd -r -p > output.wasm ; wasm2wat -o output.wat output.wasm
