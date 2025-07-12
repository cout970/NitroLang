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
