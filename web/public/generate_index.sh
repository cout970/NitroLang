#!/usr/bin/env bash

# Setups a file_index.json file referencing the ./files/ directory.
# Make sure to run ./copy_files.sh first to populate the ./files/ directory

# Usage: ./generate_index.sh

# Ensure the script is run from the correct directory
cd $(dirname "$(realpath "$0")")

SOURCE_DIR="./files/"

# Check if jq is installed
if ! command -v jq &> /dev/null; then
  echo "jq is required but not installed. Please install jq to run this script."
  exit 1
fi

# Create file_index.json
JSON='{
  "default_directories": [
    "src",
    "src/core",
    "src/examples",
    "src/compiler",
    "out",
    "out/cache"
  ],
  "default_files": []
}'

for path in $(ls "$SOURCE_DIR"); do
  # Skip files
  if [[ -f "$SOURCE_DIR$path" ]]; then
    continue
  fi
  # Skip directory 'debug'
  if [[ "$path" == "debug" ]]; then
    continue
  fi

  echo "Processing directory: '$path'"
  source=$(echo "/files/$path" | jq -R .)
  target=$(echo "src/$path" | jq -R .)
  files=$(find "$SOURCE_DIR/$path" -type f -printf '%P\n' | jq -R . | jq -s .)
  file_info=$(jq -n --argjson source "$source" --argjson target "$target" --argjson files "$files" \
    '{
        source: $source,
        target: $target,
        files: $files
     }')
  JSON=$(echo "$JSON" | jq --argjson file_info "$file_info" '.default_files += [$file_info]')
done

echo "Generated file_index.json"
echo $JSON | jq . > file_index.json

