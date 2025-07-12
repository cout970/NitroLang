import '../res/css/style.scss';
import { setupEditor } from './editor';
import { setupTerminal } from './terminal';
import { setupCompiler } from './compiler';
import { setupFiles } from './files';

async function setup(): Promise<void> {
  await setupTerminal();
  await setupCompiler();
  await setupFiles();
  await setupEditor();
}

setup().then(() => console.debug("All systems ready!"));