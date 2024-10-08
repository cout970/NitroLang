#!/usr/bin/env -S deno run --allow-read --allow-write

// Wrapper to run the file output.wasm that contains the compiler

import {run} from '../run.ts';
import * as internals from '../internal.ts';
import { init } from '../deno_fs.ts';

init(internals.fs);

// Run
try {
  let path = Deno.args.length
    ? Deno.args[0]
    : `file://${new URL('.', import.meta.url).pathname}test.wasm`;

  run(path);
} catch (e) {
  console.error(e);
  Deno.exit(-1);
}