import {Directory, init, runWasix} from "@wasmer/sdk";
import compilerUrl from "../res/wasm/compiler_v0.0.11-opt.wasm?url";
import wasmerUrl from "../res/wasm/wasmer_js_bg.wasm?url";
import {getProject, saveFS, updateFiles} from "./files";

export async function compileAndRun(fs) {
  const bytes = await compileProgram(fs);
  await updateFiles();
  await runProgram(bytes, fs);
  await updateFiles();
}

/**
 * @param {Directory} fs
 * @return {Uint8Array}
 */
export async function compileProgram(fs) {
  const module = await WebAssembly.compileStreaming(fetch(compilerUrl));

  console.debug("Running the compiler...");
  const instance = await runWasix(module, {
    args: ['/project/src/input.nitro', '-o', '/project/out/output.wasm', '--cache-dir', '/project/out/cache'],
    mount: {"/project": fs},
  });

  const result = await instance.wait();
  console.info(result.stdout);

  if (result.code !== 0) {
    console.debug(`(exit code: ${result.code})`);
  }

  await saveFS(fs);

  if (result.code !== 0) {
    throw new Error("The compiler failed");
  }

  return await fs.readFile('out/output.wasm');
}

/**
 * @param {Uint8Array} bytes
 * @param {Directory} fs
 * @param {Object} options
 */
export async function runProgram(bytes, fs, options = {}) {
  const module = await WebAssembly.compile(bytes);

  console.debug("Running the compiled program...");
  const instance = await runWasix(module, {
    env: {},
    args: [],
    mount: {'/project': fs},
    ...options,
  });
  const result = await instance.wait();
  console.info(result.stdout);

  if (result.code !== 0) {
    console.debug(`(exit code: ${result.code})`);
  }
  return result;
}

export async function setupCompiler() {
  const config = {
    module: wasmerUrl,
    log: window.debugMode ? 'trace' : 'error',
  };
  await init(config);
  await getProject();
  console.debug("Wasmer is ready");
}
