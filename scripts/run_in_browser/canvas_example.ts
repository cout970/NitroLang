/**
 * Example of using Nitro with a canvas in the browser.
 *
 * This example creates a canvas element, loads a WebAssembly module, and runs it.
 *
 * @see src/examples/html_canvas.nitro
 */

// Glue code to load the WebAssembly module and run it
import {load} from './glue_code';

// JS canvas bindings to pass to the WebAssembly module
import {bindings} from '../../src/main/nitro/bindings/canvas/canvas';

// This import will fail is you haven't run the ./compile_for_browser.sh yet
import programUrl from "./out/program_wasm32-js.wasm?url";

async function canvas_example(): Promise<void> {
  // Create a canvas element and append it to the body
  let canvas = document.createElement("canvas");
  canvas.id = "myCanvas";
  canvas.style.border = "1px solid #333";
  canvas.setAttribute('width', '800');
  canvas.setAttribute('height', '600');
  document.body.appendChild(canvas);

  // Run the program
  const program = await load(programUrl, {bindings});
  program.run();
}

canvas_example().catch(console.error);
