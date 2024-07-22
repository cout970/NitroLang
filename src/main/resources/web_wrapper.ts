// Update with:
// esbuild web_wrapper.ts --bundle --outfile=output/build.js
//
import {run} from './run.ts'
import * as internals from './internal.ts';

// File system emulation
internals.fs.isSupported = true;

internals.fs.readTextFileSync = (path: string) => {
    const data = window.localStorage.getItem('file://' + path);

    if (data === null) {
        fetch(path).then(r => r.text()).then(data => {
            window.localStorage.setItem('file://' + path, data);
        });
    }

    return data;
};
internals.fs.writeTextFileSync = (path: string, data: string) => {
    window.localStorage.setItem('file://' + path, data);
};
internals.fs.fileExistsSync = (path: string) => {
    return window.localStorage.getItem('file://' + path) !== null;
};
internals.fs.readFileSync = (path: string) => {
    function dataURItoUint8Array(dataURI) {
        // convert base64 to raw binary data held in a string
        var byteString = atob(dataURI.split(',')[1]);

        // separate out the mime component
        var mimeString = dataURI.split(',')[0].split(':')[1].split(';')[0];

        // write the bytes of the string to an ArrayBuffer
        var array = new Uint8Array(byteString.length);
        for (var i = 0; i < byteString.length; i++) {
            array[i] = byteString.charCodeAt(i);
        }

        return array;
    }

    return dataURItoUint8Array(localStorage.getItem('file://' + path));
};
internals.fs.writeFileSync = (path: string, data: Uint8Array) => {
    const reader = new FileReader();

    reader.onload = (event) => {
      localStorage.setItem('file://' + path, event.target.result);
    }

    reader.readAsDataURL(data);
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

    try {
        // Run compiler
        //run("./output/compiled.wasm");
        // Run compilation result
        run("./output.wasm");
    } catch (e) {
        console.error(e);
        window.alert('Crashed');
    }
}

main();
