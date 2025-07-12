import {init, runWasix, WasmerInitInput} from '@wasmer/sdk';
import compilerUrl from '../res/wasm/compiler-opt.wasm?url';
import wasmerUrl from '../res/wasm/wasmer_js_bg.wasm?url';

import { saveFS, updateFiles } from './files';
import { WasmerBridge } from './wasmer-bridge';
import type { CompilationResult, RunProgramOptions } from './types';

export async function compileAndRun(fs: WasmerBridge): Promise<void> {
  const bytes = await compileProgram(fs);
  await updateFiles();
  await runProgram(bytes, fs);
  await updateFiles();
}

export async function compileProgram(fs: WasmerBridge): Promise<Uint8Array> {
  const module = await WebAssembly.compileStreaming(fetch(compilerUrl));

  console.debug("Running the compiler...");

  // Sync our IndexedDB filesystem to Wasmer before compilation
  await fs.syncToWasmer('/');


  const instance = await runWasix(module, {
    args: ['/project/src/input.nitro', '-o', '/project/out/output.wasm', '--cache-dir', '/project/out/cache'],
    mount: { "/project": fs.wasmerFs }, // Access the public Wasmer Directory
  });

  const result = await instance.wait();
  console.info(result.stdout);

  if (result.code !== 0) {
    console.debug(`(exit code: ${result.code})`);
  }

  // Sync changes back from Wasmer to IndexedDB
  await fs.syncFromWasmer('/');
  await saveFS(fs);

  if (result.code !== 0) {
    throw new Error("The compiler failed");
  }

  return await fs.readFile('out/output.wasm');
}

export async function runProgram(
  bytes: Uint8Array,
  fs: WasmerBridge,
  options: RunProgramOptions = {}
): Promise<CompilationResult> {
  const module = await WebAssembly.compile(bytes);

  console.debug("Running the compiled program...");

  // Sync our filesystem to Wasmer before running
  await fs.syncToWasmer('/');

  const instance = await runWasix(module, {
    env: {},
    args: [],
    mount: { '/project': fs.wasmerFs }, // Access the public Wasmer Directory
    ...options,
  });

  const result = await instance.wait();
  console.info(result.stdout);

  if (result.code !== 0) {
    console.debug(`(exit code: ${result.code})`);
  }

  // Sync any changes back from Wasmer
  await fs.syncFromWasmer('/');

  return result;
}

export async function setupCompiler(): Promise<void> {
  const config: WasmerInitInput = {
    module: WebAssembly.compileStreaming(fetch(wasmerUrl)),
    log: window.debugMode ? 'trace' : 'error',
  };

  await init(config);
  console.debug("Wasmer is ready");
}
