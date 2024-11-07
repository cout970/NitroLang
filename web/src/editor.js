import {compileAndRun} from "./compiler.js";
import ace from 'ace-builds';
import 'ace-builds/esm-resolver';
import 'ace-builds/src-noconflict/theme-monokai';
import 'ace-builds/src-noconflict/theme-dracula';
import 'ace-builds/src-noconflict/mode-javascript';
import './mode-nitro.js';
import {getProject, saveFS, updateFiles} from "./files";
import {openTerminal, toggleTerminal} from "./terminal";

const validThemes = [
  'ace/theme/cout970',
  'ace/theme/monokai',
  'ace/theme/dracula',
];

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

  addTab(path);
}

export function addTab(file) {
  const tabs = document.querySelector('#tabs');
  const existing = tabs.querySelector(`[data-file="${file}"]`);

  if (existing) {
    tabs.querySelectorAll('.tab').forEach(tab => {
      tab.classList.remove('active');
      const order = parseInt(tab.getAttribute('data-order'), 10);
      tab.setAttribute('data-order', String(order + 1));
    });
    existing.classList.add('active');
    existing.setAttribute('data-order', '0');
    return;
  }

  const template = document.querySelector('#tab-template');
  const node = template.content.cloneNode(true);
  const tab = node.querySelector('.tab');

  tab.querySelector('.name').textContent = file.split('/').pop();
  tab.setAttribute('data-file', file);
  tab.setAttribute('data-order', 0);
  tab.title = file;
  tab.classList.add('active');

  tab.addEventListener('click', () => {
    editorSelectFile(file).catch(console.error);
  });

  tab.querySelector('.close').addEventListener('click', (e) => {
    removeTab(file);
    selectLastTab();
    e.stopPropagation();
  });

  tabs.querySelectorAll('.tab').forEach(tab => {
    tab.classList.remove('active');
    const order = parseInt(tab.getAttribute('data-order'), 10);
    tab.setAttribute('data-order', String(order + 1));
  });
  tabs.appendChild(node);

  // Remove excess tabs
  const all_tabs = tabs.querySelectorAll('.tab');
  if (all_tabs.length > 8) {
    // Get the tab with less 'data-order' value
    let max = undefined;
    let selected = undefined;

    all_tabs.forEach(tab => {
      const order = parseInt(tab.getAttribute('data-order'), 10);
      if (max === undefined || order > max) {
        max = order;
        selected = tab;
      }
    });

    if (selected) {
      removeTab(selected.getAttribute('data-file'));
    }
  }
}

export function removeTab(path) {
  const tabs = document.querySelector('#tabs');
  const tab = tabs.querySelector(`[data-file="${path}"]`);

  if (!tab) {
    return;
  }

  const all_tabs = tabs.querySelectorAll('.tab');
  if (all_tabs.length === 1) {
    return;
  }

  tabs.removeChild(tab);
}

function selectLastTab() {
  const tabs = document.querySelector('#tabs');
  const all_tabs = tabs.querySelectorAll('.tab');

  // Get the tab with less 'data-order' value
  let min = undefined;
  let selected = undefined;

  all_tabs.forEach(tab => {
    const order = parseInt(tab.getAttribute('data-order'), 10);
    if (min === undefined || order < min) {
      min = order;
      selected = tab;
    }
  });

  if (selected) {
    editorSelectFile(selected.getAttribute('data-file')).catch(console.error);
  }
}

export async function saveEditorFile() {
  const editor = window.editor;
  clearTimeout(editor.__save_timeout_id__);

  const text = editor.getValue();
  const fs = await getProject();
  const path = localStorage.getItem('currentEditorFile');

  // There is a bug, if you write a file multiple times, and the length of the new file is shorter
  // than the previous one, the file will corrupt
  // To prevent this, we remove the file first, then write the new file
  try {
    await fs.removeFile(path);
  } catch (_) {
    // ignore
  }
  await fs.writeFile(path, new TextEncoder().encode(text));
  await saveFS(fs);
  await updateFiles();
}

export async function setupEditor() {
  const editor = ace.edit("editor", {useWorker: false});
  window.editor = editor;

  editor.setTheme(localStorage.getItem('selectedTheme') || validThemes[0]);
  editor.session.setMode("ace/mode/nitro");

  editor.setOption("scrollPastEnd", 0.25)

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
  document.querySelector('#menu-btn-compile, #compiler-btn').addEventListener('click', () => {
    openTerminal();
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
    let index = validThemes.indexOf(editor.getTheme());
    index = (index + 1) % validThemes.length;

    const newTheme = validThemes[index];
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

  addTab(localStorage.getItem('currentEditorFile'));

  await saveEditorFile();
}

