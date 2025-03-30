# Running in the browser

This example shows how to run a Nitro program in the browser using WebAssembly and JavaScript/Typescript.
It uses the `wasm32-js` target to compile the Nitro code to WebAssembly, and then uses JavaScript to load and run the
WebAssembly module in the browser.

### Steps to run

1. You need to compile the desired example with `./compile_for_browser.sh <program.nitro>`, for example:

```bash
./compile_for_browser.sh src/examples/html_canvas.nitro
```

That will generate the file `out/program_wasm32-js.wasm` and watch for any changes in the source file to recompile as
needed.

The default entry point is `./canvas_example.ts` that is meant to run with `src/examples/html_canvas.nitro`, to change the example, edit 

2. Run `npm run dev` to start a dev server that will hot-reload every time a file changes.

3. Go to you browser using the URL printed by the previous command.
4. Profit?

