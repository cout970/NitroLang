
import * as internals from './internal.ts'
import * as core from './impl.ts'
import { trace_print_stack_trace, trace_save_flame_graph } from './trace.ts'

export async function run(url: string) {
    const res = await WebAssembly.instantiateStreaming(
        fetch(url),
        {core}
    );
    const wasmExports = res.instance.exports;
    const memory: WebAssembly.Memory = wasmExports.memory as WebAssembly.Memory;

    internals.mem.i32 = new Int32Array(memory.buffer);
    internals.mem.u32 = new Uint32Array(memory.buffer);
    internals.mem.f32 = new Float32Array(memory.buffer);
    internals.mem.u16 = new Uint16Array(memory.buffer);
    internals.mem.u8 = new Uint8Array(memory.buffer);
    internals.mem.program = wasmExports;

    const main = wasmExports._start_main as CallableFunction;

    // Increase stack trace print limit
    Error.stackTraceLimit = 100;

    try {
        main();
    } catch (e) {
        trace_print_stack_trace();
        throw e;
    }

    trace_save_flame_graph('./output/flamegraph.json');
}
