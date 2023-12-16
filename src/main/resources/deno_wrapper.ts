#!/usr/bin/env -S deno run --allow-read

import {run} from './run.ts'

const __dirname = new URL('.', import.meta.url).pathname;

try {
  let path = Deno.args.length
    ? Deno.args[0]
    : `file://${__dirname}output/compiled.wasm`;

  run(path);
} catch (e) {
  console.error(e);
  Deno.exit(0);
}