#!/usr/bin/env -S deno run --allow-read

import * as internal from './internal.ts'
import * as core from './core.ts'
import * as compiler from '../nitro/compiler/extern.ts'

const __dirname = new URL('.', import.meta.url).pathname;

const wasmCode = await Deno.readFile(`${__dirname}output/compiled.wasm`);
const wasmModule = new WebAssembly.Module(wasmCode);

const wasmInstance = new WebAssembly.Instance(wasmModule, {core, compiler});

const memory: WebAssembly.Memory = wasmInstance.exports.memory as WebAssembly.Memory;

internal.mem.i32 = new Int32Array(memory.buffer);
internal.mem.u32 = new Uint32Array(memory.buffer);
internal.mem.f32 = new Float32Array(memory.buffer);
internal.mem.u16 = new Uint16Array(memory.buffer);
internal.mem.u8 = new Uint8Array(memory.buffer);
internal.mem.program = wasmInstance.exports;

const main = wasmInstance.exports.main as CallableFunction

try {
  main();
} catch (e) {
  console.error(e);
  Deno.exit(0);
}
