#!/usr/bin/env bash

# switch to the root directory
cd "$(dirname "$0")/../.."

echo "Compiling and running for the first time..."
./scripts/run_in_browser/compile_for_browser.sh "$@"

echo "Watching for changes in source code..."
while inotifywait --quiet --event close_write --recursive src/main/nitro --recursive src/examples ; do
  clear
  echo "Detected change in source code. Recompiling and running..."
  ./scripts/run_in_browser/compile_for_browser.sh "$@"
done
