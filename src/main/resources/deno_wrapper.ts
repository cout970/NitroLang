#!/usr/bin/env -S deno run --allow-read --allow-write

import {run} from './run.ts';
import * as internals from './internal.ts';
import { join } from "https://deno.land/std@0.221.0/path/mod.ts";

const __dirname = new URL('.', import.meta.url).pathname;

internals.fs.isSupported = true;
internals.fs.readFileSync = (path: string) => {
    return Deno.readFileSync(join(__dirname, path));
};
internals.fs.writeFileSync = (path: string, data: string) => {
    Deno.writeTextFileSync(join(__dirname, path), data);
};

try {
  let path = Deno.args.length
    ? Deno.args[0]
    : `file://${__dirname}output/compiled.wasm`;

  run(path);
} catch (e) {
  console.error(e);
  Deno.exit(0);
}