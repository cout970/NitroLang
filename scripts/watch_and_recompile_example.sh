#!/usr/bin/env bash

# switch to the scripts directory
cd "$(dirname "$0")"

echo "Compiling and running for the first time..."
./run_with_wasmer.sh "$@"

echo "Watching for changes in source code..."
while inotifywait --quiet --event close_write --recursive ../src/main/nitro ../src/examples ./run_with_wasmer.sh ; do
  clear
  echo "Detected change in source code. Recompiling and running..."
  ./run_with_wasmer.sh "$@"
done
