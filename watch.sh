#!/usr/bin/env bash

./compile_and_run.sh

while inotifywait -e close_write src/**/* ; do
  clear
  ./compile_and_run.sh
done
