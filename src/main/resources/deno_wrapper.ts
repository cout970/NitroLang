#!/usr/bin/env -S deno run --allow-read --allow-write

import {run} from './run.ts';
import * as internals from './internal.ts';
import { join, dirname, basename } from "https://deno.land/std@0.221.0/path/mod.ts";
import { existsSync } from "https://deno.land/std@0.221.0/fs/exists.ts";

const __dirname = new URL('.', import.meta.url).pathname;

// File system
internals.fs.isSupported = true;

internals.fs.readTextFileSync = (path: string) => {
    return Deno.readTextFileSync(join(__dirname, path), 'utf-8');
};
internals.fs.writeTextFileSync = (path: string, data: string) => {
    Deno.writeTextFileSync(join(__dirname, path), data, 'utf-8');
};
internals.fs.fileExistsSync = (path: string) => {
    return existsSync(join(__dirname, path));
};
internals.fs.readFileSync = (path: string) => {
    return Deno.readFileSync(join(__dirname, path));
};
internals.fs.writeFileSync = (path: string, data: Uint8Array) => {
    Deno.writeFileSync(join(__dirname, path), data);
};
internals.fs.getLastModifiedTime = (path: string) => {
    const info = Deno.statSync(join(__dirname, path));
    if (info.mtime === null) {
        throw new Error(`Unable to get last modified time for file: ${path}`);
    }
    return info.mtime.getTime() / 1000;
};
internals.fs.join = (a: string, b: string) => {
    return join(a, b);
};
internals.fs.dirname = (path: string) => {
    return dirname(path);
};
internals.fs.basename = (path: string) => {
    return basename(path);
};

// Run

try {
  let path = Deno.args.length
    ? Deno.args[0]
    : `file://${__dirname}output/compiled.wasm`;

  run(path);
} catch (e) {
  console.error(e);
  Deno.exit(0);
}