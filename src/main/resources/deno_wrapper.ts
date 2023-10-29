#!/usr/bin/env -S deno run --allow-read

import {run} from './run.ts'

const __dirname = new URL('.', import.meta.url).pathname;

try {
  run(`file://${__dirname}output/compiled.wasm`);
} catch (e) {
  console.error(e);
  Deno.exit(0);
}