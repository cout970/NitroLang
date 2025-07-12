import '../res/css/style.scss';
import { setupEditor } from './editor';
import { setupTerminal } from './terminal';
import { setupCompiler } from './compiler';
import { setupFiles, setupFileContextMenu } from './files';

async function setup(): Promise<void> {
  await setupTerminal();
  await setupCompiler();
  await setupFiles();
  setupFileContextMenu();
  await setupEditor();
}

setup().then(() => console.debug("All systems ready!"));