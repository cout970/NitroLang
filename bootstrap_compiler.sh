#!/usr/bin/env bash
# This script bootstraps the compiler

# Check for wat2wasm
if ! command -v wat2wasm &> /dev/null
then
    echo "wat2wasm command could not be found, this is required by the old compiler"
    exit
fi

# Enable optimizations if wasm-opt is available
if command -v wasm-opt &> /dev/null
then
    optimize='--optimize'
else
    optimize=''
fi

out=$(realpath './out')
mkdir -p out
mkdir -p out/cache

if [ "$1" == "--watch" ]
then
    watch='--watch src/main/nitro/core,src/main/resources,src/main/nitro/compiler --exec-watch .*compiler/example.nitro$,.*\.ts$,.*/memory_allocator\.nitro$,.*/list\.nitro$,.*/byte_buffer\.nitro$,.*/bytes\.nitro$,.*/compilation_unit\.nitro$'
else
    watch=''
fi

# Flag --execute will convert the `out/compiler_bootstrap.wat` file into `out/compiled.wasm` and run it
# Generating `out/compiler_v0.wasm` the first step in the bootstrap process
./gradlew run --args="--output $out/compiler_bootstrap.wat --profile $optimize $watch --execute src/main/nitro/compiler/bootstrap.nitro --post-exec ./helper_bootstrap_compiler.sh" || exit -1
