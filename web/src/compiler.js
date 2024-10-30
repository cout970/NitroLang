import {init, Directory, runWasix} from "@wasmer/sdk";
import compilerUrl from "../wasm/compiler_v0.0.11-opt.wasm?url";
import wasmerUrl from "../wasm/wasmer_js_bg.wasm?url";
import {updateFiles} from "./files";

/**
 * @returns {Promise<Directory>}
 */
export async function getProject() {
  if (!window.project) {
    window.project = await initFS();
  }
  return window.project;
}

async function initFS() {
  const fs = new Directory();
  await fs.createDir('src');
  await fs.createDir("out");
  await fs.createDir("out/cache");

  // Load from localstorage
  const cacheEntries = Object.keys(localStorage).filter(k => k.startsWith("fs/"));
  for (const cacheEntry of cacheEntries) {
    const name = cacheEntry.substring(3);
    const hex = localStorage.getItem(cacheEntry);
    const bytes = new Uint8Array(hex.match(/.{1,2}/g).map(byte => parseInt(byte, 16)));
    await fs.writeFile(name, bytes);
  }
  return fs;
}

export async function saveFS(fs) {
  const saveDir = async (path) => {
    const entries = await fs.readDir(path);

    for (const entry of entries) {
      const entryPath = path + entry.name;

      if (entry.type === "file") {
        const fileContents = await fs.readFile(entryPath);
        const hex = [...fileContents].map(b => b.toString(16).padStart(2, '0')).join('');
        localStorage.setItem(`fs${entryPath}`, hex);
      }

      if (entry.type === "dir") {
        await saveDir(entryPath + '/');
      }
    }
  }

  await saveDir('/');
}

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
