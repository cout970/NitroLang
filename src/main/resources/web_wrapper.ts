// Update with:
// esbuild web_wrapper.ts --bundle --outfile=output/build.js
//
import {run} from './run.ts'

try {
  run("./output/compiled.wasm");
} catch (e) {
  console.error(e);
  window.alert('Crashed');
}