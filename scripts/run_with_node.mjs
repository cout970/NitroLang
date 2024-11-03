// This script is used to run a WebAssembly program with Node.js using the WASI API.
// This API is unstable and usually crashes in practice, this is only a demo to show how it works.

import {readFile} from 'node:fs/promises';
import {WASI} from 'node:wasi';
import {argv, env, exit} from 'node:process';

const program = argv[2];
const args = argv.slice(3);

if (!program) {
  console.error('Usage: node scripts/run_with_node.mjs <program>');
  process.exit(1);
}

const wasi = new WASI({
  version: 'preview1',
  args,
  env,
  preopens: {
    '/': './',
  },
});

const wasm_path = new URL(`../${program}`, import.meta.url);

try {
  const wasm = await WebAssembly.compile(await readFile(wasm_path));
  const instance = await WebAssembly.instantiate(wasm, wasi.getImportObject());

  await wasi.start(instance);
  console.log('Program exited successfully');
  process.exit(0);
} catch (e) {
  console.error(e);
  process.exit(1);
}
