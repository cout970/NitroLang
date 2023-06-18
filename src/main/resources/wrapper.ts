#!/usr/bin/env -S deno run --allow-read

import * as core from './core.ts'

const __dirname = new URL('.', import.meta.url).pathname;

const wasmCode = await Deno.readFile(`${__dirname}output.wasm`);
const wasmModule = new WebAssembly.Module(wasmCode);
const wasmInstance = new WebAssembly.Instance(wasmModule, {core});
const main = wasmInstance.exports.main as CallableFunction

if(main() !== 0) {
    console.error("Non 0 return code");
}
