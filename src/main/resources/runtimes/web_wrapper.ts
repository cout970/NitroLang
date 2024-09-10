// Update with:
// esbuild runtimes/web_wrapper.ts --bundle --outfile=output/build.js
//
import {run} from '../extern/run.ts'
import * as internals from '../extern/internal.ts';

const asUrl = (path: string) => {
    let prev = path;
    let norm = path.replace(/\/\.\//g, '/');

    while (prev !== norm) {
        prev = norm;
        norm = norm.replace(/\/\.\//g, '/');
    }

    return `file://${norm}`;
}

function uint8ArrayToBase64(uint8Array: Uint8Array): string {
    let binaryString = '';
    for (let i = 0; i < uint8Array.length; i++) {
        binaryString += String.fromCharCode(uint8Array[i]);
    }
    return btoa(binaryString); // Encode to base64
}

function base64ToUint8Array(base64: string): Uint8Array {
    const binaryString = atob(base64); // Decode from base64
    const uint8Array = new Uint8Array(binaryString.length);

    for (let i = 0; i < binaryString.length; i++) {
        uint8Array[i] = binaryString.charCodeAt(i);
    }

    return uint8Array;
}


// File system emulation
internals.fs.isSupported = true;

internals.fs.readTextFileSync = (path: string) => {
    const data = window.localStorage.getItem(asUrl(path));
    return data;
};
internals.fs.writeTextFileSync = (path: string, data: string) => {
    window.localStorage.setItem(asUrl(path), data);
};
internals.fs.fileExistsSync = (path: string) => {
    return window.localStorage.getItem(asUrl(path)) !== null;
};
internals.fs.readFileSync = (path: string) => {
    return base64ToUint8Array(localStorage.getItem(asUrl(path)));
};
internals.fs.writeFileSync = (path: string, data: Uint8Array) => {
    localStorage.setItem(asUrl(path), uint8ArrayToBase64(data));
};
internals.fs.getLastModifiedTime = (path: string) => {
    return 0;
};
internals.fs.join = (a: string, b: string) => {
    // Dumb implementation for now
    return a + '/' + b;
};
internals.fs.dirname = (path: string) => {
    const index = path.lastIndexOf('/');
    return index === -1 ? path : path.substring(0, index);
};
internals.fs.basename = (path: string) => {
    const index = path.lastIndexOf('/');
    return index === -1 ? path : path.substring(index);
};

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
