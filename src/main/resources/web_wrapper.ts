// Update with:
// esbuild web_wrapper.ts --bundle --outfile=output/build.js
//
import {run} from './run.ts'

// Export for reuse in the browser console
window.runWasm = run;

if (!window.disableWasmRun) {
  try {
    run("./output/compiled.wasm");
  } catch (e) {
    console.error(e);
    window.alert('Crashed');
  }
}