import {core, state} from '../../src/main/nitro/core/js_runtime/runtime';

/**
 * Result type of the load() function.
 */
export interface LoadResult {
  run: () => void,
  memory: WebAssembly.Memory,
  exports: WebAssembly.Exports,
  runtime: typeof core,
  utils: typeof utils
}

/**
 * Given a wasm file URL and a set of imports, loads the module and initializes it.
 *
 * The result object contains the following properties:
 * - `run`: a function that runs the program
 * - `memory`: the WebAssembly memory object
 * - `exports`: the WebAssembly exports object
 * - `runtime`: the nitro runtime passed to the WebAssembly module
 * - `utils`: utility functions to send and receive data from the WebAssembly module
 *
 * Example usage:
 *
 * ```ts
 * const program = await load(programUrl, {});
 * program.run();
 * ```
 *
 * @param wasmUrl
 * @param imports
 */
export async function load(wasmUrl: string, imports: WebAssembly.Imports): Promise<LoadResult> {
  const result = await WebAssembly.instantiateStreaming(fetch(wasmUrl), {...imports, core});
  const exports = result.instance.exports;

  const memory = exports['memory'] as WebAssembly.Memory;
  state.memory_8 = new Uint8Array(memory.buffer);
  state.memory_16 = new Uint16Array(memory.buffer);
  state.memory_32 = new Uint32Array(memory.buffer);
  state.exports = exports;

  const run = exports['_start'] as () => void;

  return {
    run,
    memory,
    exports,
    runtime: core,
    utils,
  } as LoadResult;
}

/**
 * Utility functions to send and receive data from the WebAssembly module.
 * They handle complex types that need to be converted to and from the WebAssembly memory.
 */
const utils = {
  // Strings
  sendString: state.sendString satisfies (string: string) => number,
  getString: state.getString satisfies (ptr: number) => string,
  // Js objects
  sendObject: state.sendObject satisfies (value: any) => number,
  getObject: state.getObject satisfies (ptr: number) => any,
  // Optional values
  sendOptional: state.sendOptional satisfies (value: number | null) => number,
  getOptional: state.getOptional satisfies (ptr: number) => number | null,
};
