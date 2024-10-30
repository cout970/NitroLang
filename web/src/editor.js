import {compileAndRun, getProject, saveFS} from "./compiler.js";
import ace from 'ace-builds';
import 'ace-builds/esm-resolver';
import 'ace-builds/src-noconflict/theme-monokai';
import 'ace-builds/src-noconflict/theme-dracula';
import 'ace-builds/src-noconflict/mode-javascript';
import './mode-nitro.js';
import {updateFiles} from "./files";
import {toggleTerminal} from "./terminal";

export async function compile() {
  await saveEditorFile();
  try {
    await compileAndRun(await getProject());
  } catch (e) {
    console.error(e);
  }
}

export async function editorSelectFile(path) {
  await saveEditorFile();

  const fs = await getProject();
  const file = await fs.readFile(path);
  const content = new TextDecoder().decode(file);
  const editor = window.editor;

  localStorage.setItem('currentEditorFile', path);
  editor.setValue(content);
  editor.clearSelection();
}

export async function saveEditorFile() {
  const editor = window.editor;
  const text = editor.getValue();
  const fs = await getProject();
  const path = localStorage.getItem('currentEditorFile');
  await fs.writeFile(path, new TextEncoder().encode(text));
  await saveFS(fs);
  await updateFiles();
}

export async function setupEditor() {
  const editor = ace.edit("editor", {useWorker: false});
  window.editor = editor;

  editor.setTheme(localStorage.getItem('selectedTheme') || 'ace/theme/monokai');
  editor.session.setMode("ace/mode/nitro");

  await restore();

  // Save editor value to local storage
  editor.on('change', () => {
    const text = editor.getValue();
    localStorage.setItem('currentEditorValue', text);

    if (editor.__save_timeout_id__) {
      clearTimeout(editor.__save_timeout_id__);
    }

    editor.__save_timeout_id__ = setTimeout(() => {
      saveEditorFile().catch(console.error);
      editor.__save_timeout_id__ = 0;
    }, 200);
  });

  // Click outside menu
  document.addEventListener('click', (e) => {
    if (!e.target.closest('#open-menu')) {
      document.querySelector('#menu').classList.remove('open');
    }
  });

  document.addEventListener('keydown', (e) => {
    if (e.key === 'Escape') {
      document.querySelector('#menu').classList.remove('open');
    }
  });

  // Click on menu
  document.querySelector('#open-menu').addEventListener('click', () => {
    document.querySelector('#menu').classList.toggle('open');
  });

  // Compile
  document.querySelector('#menu-btn-compile').addEventListener('click', () => {
    compile().then(() => console.debug('Done!'));
  });

  document.querySelector('#menu-btn-toggle-files').addEventListener('click', () => {
    document.querySelector('#files').classList.toggle('open');
  });

  document.querySelector('#menu-btn-toggle-console').addEventListener('click', () => {
    toggleTerminal();
  });

  // Download current file
  document.querySelector('#menu-btn-download').addEventListener('click', () => {
    const a = document.createElement('a');
    const file = new Blob([editor.getValue()], {type: 'text/plain'});
    a.href = URL.createObjectURL(file);
    a.download = 'input.nitro';
    a.click();
  });

  // Toggle theme
  document.querySelector('#menu-btn-toggle-theme').addEventListener('click', () => {
    const newTheme = editor.getTheme() === "ace/theme/monokai" ? "ace/theme/dracula" : "ace/theme/monokai";
    localStorage.setItem('selectedTheme', newTheme);
    editor.setTheme(newTheme);
  });

  // Comment line
  editor.commands.addCommand({
    name: 'comment line',
    bindKey: {win: 'Ctrl-/', mac: 'Command-/'},
    exec: function () {
      editor.toggleCommentLines();
    },
    readOnly: true,
  });

  // Compile
  editor.commands.addCommand({
    name: 'compile',
    bindKey: {win: 'Ctrl-S', mac: 'Command-S'},
    exec: function () {
      compile().then(() => console.debug('Done!'));
    },
    readOnly: true,
  });

  console.info("Use Ctrl-S to compile and run");

  compile().then(() => console.debug('Done!'));

  console.debug("Editor is ready");
  return editor;
}

async function restore() {
  const saved = localStorage.getItem('currentEditorValue');

  if (saved) {
    editor.setValue(saved);
  } else {
    const defaultValue = 'fun main() {\n    println("Hello, World!")\n}';
    localStorage.setItem('currentEditorFile', 'src/input.nitro');
    localStorage.setItem('currentEditorValue', defaultValue);
    editor.setValue(defaultValue);
  }
  editor.clearSelection();

  await saveEditorFile();
}
