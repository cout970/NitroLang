
import * as internal from './internal.ts'
import * as core from './core.ts'
import * as compiler from '../nitro/compiler/extern.ts'

export async function run(url: string) {
    const res = await WebAssembly.instantiateStreaming(
        fetch(url),
        {core, compiler}
    );
    const wasmExports = res.instance.exports;
    const memory: WebAssembly.Memory = wasmExports.memory as WebAssembly.Memory;

    internal.mem.i32 = new Int32Array(memory.buffer);
    internal.mem.u32 = new Uint32Array(memory.buffer);
    internal.mem.f32 = new Float32Array(memory.buffer);
    internal.mem.u16 = new Uint16Array(memory.buffer);
    internal.mem.u8 = new Uint8Array(memory.buffer);
    internal.mem.program = wasmExports;

    const main = wasmExports.main as CallableFunction

    main();
}
