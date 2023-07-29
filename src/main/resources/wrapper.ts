#!/usr/bin/env -S deno run --allow-read

import * as coreLib from './core.ts'

const __dirname = new URL('.', import.meta.url).pathname;

const wasmCode = await Deno.readFile(`${__dirname}output.wasm`);
const wasmModule = new WebAssembly.Module(wasmCode);

const {mem, ...core} = coreLib;

const wasmInstance = new WebAssembly.Instance(wasmModule, {core});

const memory: WebAssembly.Memory = wasmInstance.exports.memory as WebAssembly.Memory;
mem.u32 = new Uint32Array(memory.buffer);
mem.f32 = new Float32Array(memory.buffer);
mem.u16 = new Uint16Array(memory.buffer);
mem.u8 = new Uint8Array(memory.buffer);

const main = wasmInstance.exports.main as CallableFunction

try {
  main();
} catch (e) {
  console.error(e);
  Deno.exit(0);
}
