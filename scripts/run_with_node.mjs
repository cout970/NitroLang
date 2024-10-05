import { readFile } from 'node:fs/promises';
import { WASI } from 'node:wasi';
import { argv, env, exit } from 'node:process';

const program = argv[2];
const args = argv.slice(3);

if (!program) {
  console.error('Usage: node scripts/run_with_node.mjs <program>');
  Deno.exit(1);
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
    exit(0);
} catch (e) {
    console.error(e);
    exit(1);
}
