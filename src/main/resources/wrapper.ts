#!/usr/bin/env -S deno run --allow-read=./build/output.wasm

const core = {
    println: (val) => console.log(val),
};

const wasmCode = await Deno.readFile('./build/output.wasm');
const wasmModule = new WebAssembly.Module(wasmCode);
const wasmInstance = new WebAssembly.Instance(wasmModule, {core});
const main = wasmInstance.exports.main as CallableFunction

if(main() !== 0) {
    console.error("Non 0 return code");
}