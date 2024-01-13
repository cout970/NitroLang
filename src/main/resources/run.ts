
import * as internal from './internal.ts'
import * as core from './impl.ts'

export async function run(url: string) {
    const res = await WebAssembly.instantiateStreaming(
        fetch(url),
        {core}
    );
    const wasmExports = res.instance.exports;
    const memory: WebAssembly.Memory = wasmExports.memory as WebAssembly.Memory;

    internal.mem.i32 = new Int32Array(memory.buffer);
    internal.mem.u32 = new Uint32Array(memory.buffer);
    internal.mem.f32 = new Float32Array(memory.buffer);
    internal.mem.u16 = new Uint16Array(memory.buffer);
    internal.mem.u8 = new Uint8Array(memory.buffer);
    internal.mem.program = wasmExports;

    const main = wasmExports._start_main as CallableFunction
    const print_stack_trace = wasmExports.print_stack_trace as CallableFunction

    try {
        main();
    } catch (e) {
        try {
            print_stack_trace();
        } catch (_e) {
            console.error("Failed to print stack trace");
        }
        throw e;
    }
}
