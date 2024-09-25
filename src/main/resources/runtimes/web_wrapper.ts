// Update with:
// esbuild runtimes/web_wrapper.ts --bundle --outfile=output/build.js
//


// Export for reuse in the browser console
window.runWasm = run;

async function main() {
    if (window.disableWasmRun) {
        return;
    }

    const r = await fetch("../output/files.json");
    const data = await r.json();

    console.log('Fetched files.json');
    for (const key in data) {
        window.localStorage.setItem(asUrl(key), data[key]);
    }
    window.localStorage.setItem(asUrl("example.nitro"), 'fun main(){ println("Hello, World!"); }');
    window.localStorage.setItem(asUrl('output.wasm'), '');

    try {
        // Run compilation result
        await run("../../../../out/compiler_v0.wasm", ['example.nitro', 'output.wasm']);
        const wasm = window.localStorage.getItem(asUrl('output.wasm'));
        const uint8 = base64ToUint8Array(wasm);

        await run(uint8.buffer, []);
    } catch (e) {
        console.error(e);
        window.alert('Crashed');
    }
}

main();
