#!/usr/bin/env bash

# Root of the project
json=$(realpath output/files.json)

cd ../../..
cd src/main/nitro

# Create a json with file paths as keys and file contents as values
echo "{" > $json
for file in $(find . -type f); do
    # Remove ./ from file path
    file=$(echo $file | sed 's/^\.\///g')
    echo "  \"src/main/nitro/$file\": \"$(cat $file | sed 's/\\/\\\\/g' | sed 's/"/\\"/g' | sed 's/$/\\n/g' | tr -d '\n')\"," >> $json
done

# Remove , from list line in json
sed -i '$ s/,\s*$//' $json
echo "}" >> $json

