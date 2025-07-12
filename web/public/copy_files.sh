#!/usr/bin/env bash

# Copy files from ../../src/main/nitro/ into ./files/
# Usage: ./copy_files.sh

# Ensure the script is run from the correct directory
cd $(dirname "$(realpath "$0")")

# Define source and destination directories
SOURCE_DIR="../../src/main/nitro/"
DEST_DIR="./files/"

# Create destination directory if it doesn't exist
mkdir -p "$DEST_DIR"

# Copy all files from source to destination
rm -rf "$DEST_DIR"*
cp -r "$SOURCE_DIR"* "$DEST_DIR"

# Also copy src/examples
EXAMPLES_DIR="../../src/examples/"
EXAMPLES_DEST_DIR="./files/examples/"
mkdir -p "$EXAMPLES_DEST_DIR"

rm -rf "$EXAMPLES_DEST_DIR"*
cp -r "$EXAMPLES_DIR"* "$EXAMPLES_DEST_DIR"

