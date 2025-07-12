import { Directory } from "@wasmer/sdk";
import {Ace} from "ace-builds";

export interface Tab {
  file: string;
  order: number;
  active: boolean;
}

export interface EditorGlobals {
  editor?: Ace.Editor;
  project?: Directory;
  debugMode?: boolean;
  expandedDirs?: Set<string>;
}

declare global {
  interface Window extends EditorGlobals {}
}

export interface CompilationResult {
  code: number;
  stdout: string;
  stderr: string;
}

export interface RunProgramOptions {
  env?: Record<string, string>;
  args?: string[];
  mount?: Record<string, Directory>;
}

export interface CompilationOptions {
  target: 'wasm32-wasi' | 'wasm32-js';
  verbose: boolean;
  dumpIr: boolean;
  coreLibrary: 'bundled' | 'source';
  mainFile: string;
  flags: Record<string, string>;
  customArgs: string[];
}

export interface CompilationSettings {
  target: string;
  verbose: boolean;
  dumpIr: boolean;
  coreLibrary: string;
  enableRcGc: boolean;
}
