#!/usr/bin/env -S deno run --allow-read --allow-write

// Wrapper to run the file output.wasm that contains the compiler

import {run} from '../extern/run.ts';
import * as internals from '../extern/internal.ts';
import { init } from './deno_fs.ts';

init(internals.fs);

// Run
try {
  run(Deno.args[0], Deno.args.slice(1));
} catch (e) {
  console.error(e);
  Deno.exit(-1);
}