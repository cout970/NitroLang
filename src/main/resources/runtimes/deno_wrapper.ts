#!/usr/bin/env -S deno run --allow-read --allow-write

// Wrapper to run the file output/compiled.wasm emitted by the old compiler

import {run} from '../extern/run.ts';
import * as internals from '../extern/internal.ts';
import { init } from './deno_fs.ts';

init(internals.fs);

// Run
try {
  let path = Deno.args.length
    ? Deno.args[0]
    : `file://${new URL('.', import.meta.url).pathname}../output/compiled.wasm`;

  await run(path, []);
} catch (e) {
  console.error(e);
  Deno.exit(0);
}