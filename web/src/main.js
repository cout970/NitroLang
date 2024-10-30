import '../res/css/style.scss';
import {setupEditor} from "./editor.js";
import {setupTerminal} from "./terminal.js";
import {setupCompiler} from "./compiler.js";
import {setupFiles} from "./files.js";

async function setup() {
  await setupTerminal();
  await setupCompiler();
  await setupFiles();
  await setupEditor();
}

setup().then(() => console.debug("All systems ready!"));
