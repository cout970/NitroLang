import {init, runWasix, WasmerInitInput} from '@wasmer/sdk';
import compilerUrl from '../res/wasm/compiler-opt.wasm?url';
import wasmerUrl from '../res/wasm/wasmer_js_bg.wasm?url';

import { saveFS, updateFiles } from './files';
import { WasmerBridge } from './wasmer-bridge';
import type { CompilationResult, RunProgramOptions, CompilationOptions } from './types';

export async function compileAndRun(fs: WasmerBridge, options?: CompilationOptions): Promise<void> {
  const compilationOptions = options || getCompilationSettings();

  if (compilationOptions.dumpIr) {
    // When dumping IR, just compile and don't run
    await compileIR(fs, compilationOptions);
    await updateFiles();
  } else {
    // Normal compilation and execution
    const bytes = await compileProgram(fs, compilationOptions);
    await updateFiles();
    await runProgram(bytes, fs);
    await updateFiles();
  }
}

function parseCustomArgs(customArgsString: string): string[] {
  if (!customArgsString.trim()) {
    return [];
  }

  // Simple argument parsing - splits on whitespace but respects quotes
  const args: string[] = [];
  let currentArg = '';
  let inQuotes = false;

  for (let i = 0; i < customArgsString.length; i++) {
    const char = customArgsString[i];

    if (char === '"' || char === "'") {
      inQuotes = !inQuotes;
    } else if (char === ' ' && !inQuotes) {
      if (currentArg.trim()) {
        args.push(currentArg.trim());
        currentArg = '';
      }
    } else {
      currentArg += char;
    }
  }

  if (currentArg.trim()) {
    args.push(currentArg.trim());
  }

  return args;
}

function getCompilationSettings(): CompilationOptions {
  const customArgsString = localStorage.getItem('nitro-custom-args') || '';

  return {
    target: (localStorage.getItem('nitro-target') as 'wasm32-wasi' | 'wasm32-js') || 'wasm32-wasi',
    verbose: localStorage.getItem('nitro-verbose') === 'true',
    dumpIr: localStorage.getItem('nitro-dump-ir') === 'true',
    coreLibrary: (localStorage.getItem('nitro-core-library') as 'bundled' | 'source') || 'bundled',
    mainFile: localStorage.getItem('nitro-main-file') || 'src/main.nitro',
    flags: {
      'enable-rc-gc': localStorage.getItem('nitro-enable-rc-gc') || 'true'
    },
    customArgs: parseCustomArgs(customArgsString)
  };
}

function buildCompilerArgs(options: CompilationOptions): string[] {
  let args: string[];
  const mainFilePath = `/project/${options.mainFile}`;

  if (options.dumpIr) {
    // When dumping IR, output to ir.txt instead of wasm
    args = [mainFilePath, '-o', '/project/out/ir.txt', '--cache-dir', '/project/out/cache'];
  } else {
    args = [mainFilePath, '-o', '/project/out/output.wasm', '--cache-dir', '/project/out/cache'];
  }

  // Add target
  if (options.target !== 'wasm32-wasi') {
    args.push('-t', options.target);
  }

  // Add debug options
  if (options.verbose) {
    args.push('--verbose');
  }

  if (options.dumpIr) {
    args.push('--dump-ir');
  }

  // Add core library option
  if (options.coreLibrary === 'source') {
    args.push('--core-path', '/project/src/core/core.nitro');
  }

  // Add compilation flags
  for (const [flag, value] of Object.entries(options.flags)) {
    args.push('--flag', `${flag}=${value}`);
  }

  // Add custom arguments
  args.push(...options.customArgs);

  return args;
}

export async function compileIR(fs: WasmerBridge, options: CompilationOptions): Promise<void> {
  const module = await WebAssembly.compileStreaming(fetch(compilerUrl));
  const args = buildCompilerArgs(options);

  console.debug("Running the compiler for IR dump with args:", args);

  // Sync our IndexedDB filesystem to Wasmer before compilation
  await fs.syncToWasmer('/');

  const instance = await runWasix(module, {
    args,
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

  console.info("IR dumped to out/ir.txt");
}

export async function compileProgram(fs: WasmerBridge, options?: CompilationOptions): Promise<Uint8Array> {
  const module = await WebAssembly.compileStreaming(fetch(compilerUrl));
  const compilationOptions = options || getCompilationSettings();
  const args = buildCompilerArgs(compilationOptions);

  console.debug("Running the compiler with args:", args);

  // Sync our IndexedDB filesystem to Wasmer before compilation
  await fs.syncToWasmer('/');

  const instance = await runWasix(module, {
    args,
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
