import { compileAndRun } from './compiler';
import ace, {Ace} from 'ace-builds';
import 'ace-builds/esm-resolver';
import 'ace-builds/src-noconflict/theme-monokai';
import 'ace-builds/src-noconflict/theme-dracula';
import 'ace-builds/src-noconflict/mode-javascript';
import 'ace-builds/src-noconflict/mode-typescript';
import './mode-nitro.js';
import { getProject, saveFS, updateFiles } from './files';
import { openTerminal, toggleTerminal } from './terminal';

const validThemes = [
  'ace/theme/cout970',
  'ace/theme/monokai',
  'ace/theme/dracula',
] as const;

type ValidTheme = typeof validThemes[number];

export async function compile(): Promise<void> {
  await saveEditorFile();
  try {
    await compileAndRun(await getProject());
  } catch (e) {
    console.error(e);
  }
}

export async function editorSelectFile(path: string): Promise<void> {
  await saveEditorFile();

  const fs = await getProject();
  const file = await fs.readFile(path);
  const content = new TextDecoder().decode(file);
  const editor = window.editor!;

  localStorage.setItem('currentEditorFile', path);
  editor.setValue(content);
  editor.clearSelection();

  const extension = path.split('.').pop();
  if (extension === 'js') {
    editor.session.setMode("ace/mode/javascript");
  } else if (extension === 'ts') {
    editor.session.setMode("ace/mode/typescript");
  } else if (extension === 'nitro') {
    editor.session.setMode("ace/mode/nitro");
  } else if (extension === 'json') {
    editor.session.setMode("ace/mode/json");
  } else if (extension === 'html') {
    editor.session.setMode("ace/mode/html");
  } else if (extension === 'css') {
    editor.session.setMode("ace/mode/css");
  } else {
    editor.session.setMode("ace/mode/javascript");
  }

  addTab(path);
}

export function addTab(file: string): void {
  const tabs = document.querySelector('#tabs')!;
  const existing = tabs.querySelector(`[data-file="${file}"]`) as HTMLElement;

  if (existing) {
    tabs.querySelectorAll('.tab').forEach(tab => {
      tab.classList.remove('active');
      const order = parseInt(tab.getAttribute('data-order') || '0', 10);
      tab.setAttribute('data-order', String(order + 1));
    });
    existing.classList.add('active');
    existing.setAttribute('data-order', '0');
    return;
  }

  const template = document.querySelector('#tab-template') as HTMLTemplateElement;
  const node = template.content.cloneNode(true) as DocumentFragment;
  const tab = node.querySelector('.tab') as HTMLElement;

  tab.querySelector('.name')!.textContent = file.split('/').pop() || '';
  tab.setAttribute('data-file', file);
  tab.setAttribute('data-order', '0');
  tab.title = file;
  tab.classList.add('active');

  tab.addEventListener('click', () => {
    editorSelectFile(file).catch(console.error);
  });

  tab.querySelector('.close')!.addEventListener('click', (e) => {
    removeTab(file);
    selectLastTab();
    e.stopPropagation();
  });

  tabs.querySelectorAll('.tab').forEach(tab => {
    tab.classList.remove('active');
    const order = parseInt(tab.getAttribute('data-order') || '0', 10);
    tab.setAttribute('data-order', String(order + 1));
  });
  tabs.appendChild(node);

  const allTabs = tabs.querySelectorAll('.tab');
  if (allTabs.length > 8) {
    let max: number | undefined;
    let selected: HTMLElement | undefined;

    allTabs.forEach(tab => {
      const order = parseInt(tab.getAttribute('data-order') || '0', 10);
      if (max === undefined || order > max) {
        max = order;
        selected = tab as HTMLElement;
      }
    });

    if (selected) {
      removeTab(selected.getAttribute('data-file')!);
    }
  }
}

export function removeTab(path: string): void {
  const tabs = document.querySelector('#tabs')!;
  const tab = tabs.querySelector(`[data-file="${path}"]`);

  if (!tab) {
    return;
  }

  const allTabs = tabs.querySelectorAll('.tab');
  if (allTabs.length === 1) {
    return;
  }

  tabs.removeChild(tab);
}

function selectLastTab(): void {
  const tabs = document.querySelector('#tabs')!;
  const allTabs = tabs.querySelectorAll('.tab');

  let min: number | undefined;
  let selected: HTMLElement | undefined;

  allTabs.forEach(tab => {
    const order = parseInt(tab.getAttribute('data-order') || '0', 10);
    if (min === undefined || order < min) {
      min = order;
      selected = tab as HTMLElement;
    }
  });

  if (selected) {
    editorSelectFile(selected.getAttribute('data-file')!).catch(console.error);
  }
}

export async function saveEditorFile(): Promise<void> {
  const editor = window.editor!;
  const saveTimeoutId = (editor as any).__save_timeout_id__;
  if (saveTimeoutId) {
    clearTimeout(saveTimeoutId);
  }

  const text = editor.getValue();
  const fs = await getProject();
  const path = localStorage.getItem('currentEditorFile');

  if (!path) return;

  try {
    await fs.removeFile(path);
  } catch (_) {
    // ignore
  }

  try {
    await fs.writeFile(path, new TextEncoder().encode(text));
    await saveFS(fs);
    await updateFiles();
  } catch (e) {
    console.error("Failed to save file", e);
    throw e;
  }
}

export async function setupEditor(): Promise<Ace.Editor> {
  const editor = ace.edit("editor", { useWorker: false });
  window.editor = editor;

  const storedTheme = localStorage.getItem('selectedTheme') as ValidTheme;
  editor.setTheme(storedTheme || validThemes[0]);
  editor.session.setMode("ace/mode/nitro");
  editor.setOption("scrollPastEnd", 0.25);

  await restore();

  editor.on('change', () => {
    const text = editor.getValue();
    localStorage.setItem('currentEditorValue', text);

    const saveTimeoutId = (editor as any).__save_timeout_id__;
    if (saveTimeoutId) {
      clearTimeout(saveTimeoutId);
    }

    (editor as any).__save_timeout_id__ = setTimeout(() => {
      saveEditorFile().catch(console.error);
      (editor as any).__save_timeout_id__ = 0;
    }, 200);
  });

  setupEventListeners();
  setupKeyboardShortcuts(editor);

  console.info("Use Ctrl-S to compile and run");
  compile().then(() => console.debug('Done!'));
  console.debug("Editor is ready");

  return editor;
}

function setupEventListeners(): void {
  document.addEventListener('click', (e) => {
    if (!(e.target as Element).closest('#open-menu')) {
      document.querySelector('#menu')!.classList.remove('open');
    }
  });

  document.addEventListener('keydown', (e) => {
    if (e.key === 'Escape') {
      document.querySelector('#menu')!.classList.remove('open');
    }
  });

  document.querySelector('#open-menu')!.addEventListener('click', () => {
    document.querySelector('#menu')!.classList.toggle('open');
  });

  document.querySelector('#menu-btn-compile, #compiler-btn')!.addEventListener('click', () => {
    openTerminal();
    compile().then(() => console.debug('Done!'));
  });

  document.querySelector('#menu-btn-toggle-files')!.addEventListener('click', () => {
    document.querySelector('#files')!.classList.toggle('open');
  });

  document.querySelector('#menu-btn-toggle-console')!.addEventListener('click', () => {
    toggleTerminal();
  });

  document.querySelector('#menu-btn-download')!.addEventListener('click', () => {
    const editor = window.editor!;
    const a = document.createElement('a');
    const file = new Blob([editor.getValue()], { type: 'text/plain' });
    a.href = URL.createObjectURL(file);
    a.download = 'input.nitro';
    a.click();
  });

  document.querySelector('#menu-btn-toggle-theme')!.addEventListener('click', () => {
    const editor = window.editor!;
    let index = validThemes.indexOf(editor.getTheme() as ValidTheme);
    index = (index + 1) % validThemes.length;

    const newTheme = validThemes[index];
    localStorage.setItem('selectedTheme', newTheme);
    editor.setTheme(newTheme);
  });
}

function setupKeyboardShortcuts(editor: Ace.Editor): void {
  editor.commands.addCommand({
    name: 'comment line',
    bindKey: { win: 'Ctrl-/', mac: 'Command-/' },
    exec: function () {
      editor.toggleCommentLines();
    },
    readOnly: true,
  });

  editor.commands.addCommand({
    name: 'compile',
    bindKey: { win: 'Ctrl-S', mac: 'Command-S' },
    exec: function () {
      compile().then(() => console.debug('Done!'));
    },
    readOnly: true,
  });
}

async function restore(): Promise<void> {
  const editor = window.editor!;
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
  addTab(localStorage.getItem('currentEditorFile') || 'src/input.nitro');
  await saveEditorFile();
}
