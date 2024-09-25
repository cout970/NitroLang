#!/usr/bin/env -S deno run --allow-read --allow-write --allow-env

import * as path from "jsr:@std/path";
import Context from "https://deno.land/std@0.206.0/wasi/snapshot_preview1.ts";

console.log('Deno version:', Deno.version.deno);

// Folder containing this file at 'src/main/resources/runtimes'
const thisFolder = import.meta.dirname;
const root = path.dirname(thisFolder);
console.log('Root:', root);

async function run(source: string | ArrayBuffer, args: string[]) {
    const context = new Context({
      // args will be the run time argument passed to WASM code
      args: [typeof source === 'string' ? source : 'program.wasm', ...args],
      // Any environment variable you wanted to pass
      env: Deno.env.toObject(),
      // Directories which will be hosted in WASI Sandbox to be used by WASM.
      // The string keys of are treated as directories within the sandboxed filesystem, the values are the real paths to those directories on the host machine.
      preopens: {
        "./":`${root}`,
        "./src":`${root}/src`,
        "./out":`${root}/out`,
      },
    });

    try {
        let module: WebAssembly.Module;
        if (typeof source === 'string') {
            module = await WebAssembly.compileStreaming(fetch(source));
        } else {
            module = await WebAssembly.compile(source);
        }

        const instance = await WebAssembly.instantiate(module, {
            "wasi_snapshot_preview1": context.exports
        });

        await context.start(instance);
    } catch (e) {
        console.error('Exception while executing web assembly:\n', e);
    }
}

if (Deno.args.length === 0) {
    console.error('Usage: ./run_with_deno.ts <file.nitro>');
    console.info('Note: `file.nitro` must be at the root of the project');
    Deno.exit(1);
}

const latest = await Deno.readTextFile(`${root}/releases/latest.txt`);
const output = 'out/output.wasm';

console.log('Compiling file', Deno.args[0], 'with', latest.trim());
await run(`file://${root}/releases/${latest}`, [Deno.args[0], '-o', output, '--cache-dir', 'out/cache']);

console.log('Running', output);
console.log('---');
await run(`file://${root}/${output}`, Deno.args.slice(1));
console.log('---');
