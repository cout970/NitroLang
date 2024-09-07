import { join, dirname, basename } from "https://deno.land/std@0.221.0/path/mod.ts";
import { existsSync } from "https://deno.land/std@0.221.0/fs/exists.ts";

// This file is in src/main/resources/runtimes/deno_fs.ts but we want path to be relative to the project root
const __dirname = new URL('.', import.meta.url).pathname + '/../../../../';
const absPath = (path: string) => path.startsWith('/') ? path : join(__dirname, path);

export function init(fs) {
    // File system
    fs.isSupported = true;

    fs.readTextFileSync = (path: string) => {
        return Deno.readTextFileSync(absPath(path), 'utf-8');
    };
    fs.writeTextFileSync = (path: string, data: string) => {
        Deno.writeTextFileSync(absPath(path), data, 'utf-8');
    };
    fs.fileExistsSync = (path: string) => {
        return existsSync(absPath(path));
    };
    fs.readFileSync = (path: string) => {
        return Deno.readFileSync(absPath(path));
    };
    fs.writeFileSync = (path: string, data: Uint8Array) => {
        Deno.writeFileSync(absPath(path), data);
    };
    fs.getLastModifiedTime = (path: string) => {
        const info = Deno.statSync(absPath(path));
        if (info.mtime === null) {
            throw new Error(`Unable to get last modified time for file: ${path}`);
        }
        return info.mtime.getTime() / 1000;
    };
    fs.join = (a: string, b: string) => {
        return join(a, b);
    };
    fs.dirname = (path: string) => {
        return dirname(path);
    };
    fs.basename = (path: string) => {
        return basename(path);
    };
}