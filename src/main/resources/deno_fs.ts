import { join, dirname, basename } from "https://deno.land/std@0.221.0/path/mod.ts";
import { existsSync } from "https://deno.land/std@0.221.0/fs/exists.ts";

const __dirname = new URL('.', import.meta.url).pathname;

export function init(fs) {
    // File system
    fs.isSupported = true;

    fs.readTextFileSync = (path: string) => {
        return Deno.readTextFileSync(join(__dirname, path), 'utf-8');
    };
    fs.writeTextFileSync = (path: string, data: string) => {
        Deno.writeTextFileSync(join(__dirname, path), data, 'utf-8');
    };
    fs.fileExistsSync = (path: string) => {
        return existsSync(join(__dirname, path));
    };
    fs.readFileSync = (path: string) => {
        return Deno.readFileSync(join(__dirname, path));
    };
    fs.writeFileSync = (path: string, data: Uint8Array) => {
        Deno.writeFileSync(join(__dirname, path), data);
    };
    fs.getLastModifiedTime = (path: string) => {
        const info = Deno.statSync(join(__dirname, path));
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