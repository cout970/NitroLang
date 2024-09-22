#!/usr/bin/env bash

echo "Compiling and running for the first time..."
./compile_and_run.sh

echo "Watching for changes in source code..."
while inotifywait --quiet --event close_write --recursive src/main/nitro ; do
  clear
  echo "Detected change in source code. Recompiling and running..."
  ./compile_and_run.sh
done
