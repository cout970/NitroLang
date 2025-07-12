import {compileAndRun} from './compiler';
import ace, {Ace} from 'ace-builds';
import 'ace-builds/esm-resolver';
import 'ace-builds/src-noconflict/theme-monokai';
import 'ace-builds/src-noconflict/theme-dracula';
import 'ace-builds/src-noconflict/mode-javascript';
import 'ace-builds/src-noconflict/mode-typescript';
import './mode-nitro.js';
import {getProject, saveFS, updateFiles} from './files';
import {openTerminal, toggleTerminal} from './terminal';

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

  hideWelcomeMessage();
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

function getFileIcon(filePath: string): { iconClass: string; iconPath: string } {
  const extension = filePath.split('.').pop()?.toLowerCase();

  switch (extension) {
    case 'nitro':
      return {
        iconClass: 'nitro-file',
        iconPath: 'M14 3v4a1 1 0 0 0 1 1h4 M17 21h-10a2 2 0 0 1 -2 -2v-14a2 2 0 0 1 2 -2h7l5 5v11a2 2 0 0 1 -2 2z M9 9l2 2l4 -4'
      };
    case 'js':
      return {
        iconClass: 'js-file',
        iconPath: 'M14 3v4a1 1 0 0 0 1 1h4 M17 21h-10a2 2 0 0 1 -2 -2v-14a2 2 0 0 1 2 -2h7l5 5v11a2 2 0 0 1 -2 2z M9 12v-1a1 1 0 0 1 1 -1h1 M15 12v1a1 1 0 0 0 1 1h1'
      };
    case 'ts':
      return {
        iconClass: 'ts-file',
        iconPath: 'M14 3v4a1 1 0 0 0 1 1h4 M17 21h-10a2 2 0 0 1 -2 -2v-14a2 2 0 0 1 2 -2h7l5 5v11a2 2 0 0 1 -2 2z M9 9v6 M12 9v6 M15 15v-6l3 6'
      };
    case 'json':
      return {
        iconClass: 'json-file',
        iconPath: 'M14 3v4a1 1 0 0 0 1 1h4 M17 21h-10a2 2 0 0 1 -2 -2v-14a2 2 0 0 1 2 -2h7l5 5v11a2 2 0 0 1 -2 2z M10 12h-1a1 1 0 0 1 -1 -1v-2a1 1 0 0 0 -1 -1 M14 12h1a1 1 0 0 0 1 -1v-2a1 1 0 0 1 1 -1'
      };
    case 'html':
      return {
        iconClass: 'html-file',
        iconPath: 'M14 3v4a1 1 0 0 0 1 1h4 M17 21h-10a2 2 0 0 1 -2 -2v-14a2 2 0 0 1 2 -2h7l5 5v11a2 2 0 0 1 -2 2z M8 12h8 M8 16h8'
      };
    case 'css':
      return {
        iconClass: 'css-file',
        iconPath: 'M14 3v4a1 1 0 0 0 1 1h4 M17 21h-10a2 2 0 0 1 -2 -2v-14a2 2 0 0 1 2 -2h7l5 5v11a2 2 0 0 1 -2 2z M8 12h8 M10 16h4'
      };
    default:
      return {
        iconClass: '',
        iconPath: 'M14 3v4a1 1 0 0 0 1 1h4 M17 21h-10a2 2 0 0 1 -2 -2v-14a2 2 0 0 1 2 -2h7l5 5v11a2 2 0 0 1 -2 2z'
      };
  }
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
    existing.scrollIntoView({ behavior: 'smooth', block: 'nearest', inline: 'nearest' });
    return;
  }

  const template = document.querySelector('#tab-template') as HTMLTemplateElement;
  const node = template.content.cloneNode(true) as DocumentFragment;
  const tab = node.querySelector('.tab') as HTMLElement;
  const fileName = file.split('/').pop() || '';
  const { iconClass, iconPath } = getFileIcon(file);

  // Setup file icon
  const fileIcon = tab.querySelector('.file-icon') as SVGElement;
  if (iconClass) {
    fileIcon.classList.add(iconClass);
  }
  fileIcon.querySelector('path:last-child')!.setAttribute('d', iconPath);

  tab.querySelector('.name')!.textContent = fileName;
  tab.setAttribute('data-file', file);
  tab.setAttribute('data-order', '0');
  tab.title = file;
  tab.classList.add('active');

  tab.addEventListener('click', (e) => {
    if ((e.target as Element).closest('.close')) return;
    editorSelectFile(file).catch(console.error);
  });

  tab.querySelector('.close')!.addEventListener('click', (e) => {
    removeTab(file);
    selectLastTab();
    e.stopPropagation();
  });

  // Add context menu support
  tab.addEventListener('contextmenu', (e) => {
    e.preventDefault();
    showTabContextMenu(e, file);
  });

  tabs.querySelectorAll('.tab').forEach(tab => {
    tab.classList.remove('active');
    const order = parseInt(tab.getAttribute('data-order') || '0', 10);
    tab.setAttribute('data-order', String(order + 1));
  });
  tabs.appendChild(node);

  // Scroll new tab into view
  tab.scrollIntoView({ behavior: 'smooth', block: 'nearest', inline: 'nearest' });

  const allTabs = tabs.querySelectorAll('.tab');
  if (allTabs.length > 12) {
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

  tabs.removeChild(tab);

  // Check if this was the last tab
  const remainingTabs = tabs.querySelectorAll('.tab');
  if (remainingTabs.length === 0) {
    showWelcomeMessage();
  }
}

function selectLastTab(): void {
  const tabs = document.querySelector('#tabs')!;
  const allTabs = tabs.querySelectorAll('.tab');

  if (allTabs.length === 0) {
    showWelcomeMessage();
    return;
  }

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

let currentContextFile: string | null = null;

function showTabContextMenu(e: MouseEvent, file: string): void {
  const contextMenu = document.querySelector('#tab-context-menu') as HTMLElement;
  currentContextFile = file;

  contextMenu.classList.remove('hidden');
  contextMenu.style.left = `${e.clientX}px`;
  contextMenu.style.top = `${e.clientY}px`;
}

function hideTabContextMenu(): void {
  const contextMenu = document.querySelector('#tab-context-menu') as HTMLElement;
  contextMenu.classList.add('hidden');
  currentContextFile = null;
}

function setupTabContextMenu(): void {
  // Close context menu when clicking elsewhere
  document.addEventListener('click', (e) => {
    if (!(e.target as Element).closest('#tab-context-menu')) {
      hideTabContextMenu();
    }
  });

  // Context menu actions
  document.querySelector('#context-close-tab')!.addEventListener('click', () => {
    if (currentContextFile) {
      removeTab(currentContextFile);
      selectLastTab();
    }
    hideTabContextMenu();
  });

  document.querySelector('#context-close-others')!.addEventListener('click', () => {
    if (currentContextFile) {
      const tabs = document.querySelector('#tabs')!;
      const allTabs = Array.from(tabs.querySelectorAll('.tab')) as HTMLElement[];

      allTabs.forEach(tab => {
        const file = tab.getAttribute('data-file');
        if (file && file !== currentContextFile) {
          removeTab(file);
        }
      });
    }
    hideTabContextMenu();
  });

  document.querySelector('#context-close-all')!.addEventListener('click', () => {
    const tabs = document.querySelector('#tabs')!;
    const allTabs = Array.from(tabs.querySelectorAll('.tab')) as HTMLElement[];

    allTabs.forEach(tab => {
      const file = tab.getAttribute('data-file');
      if (file) {
        removeTab(file);
      }
    });

    hideTabContextMenu();
  });

  document.querySelector('#context-copy-path')!.addEventListener('click', () => {
    const path = currentContextFile;
    if (path) {
      navigator.clipboard.writeText(path).then(() => {
        console.info(`Copied path: ${path}`);
      }).catch(() => {
        console.warn('Failed to copy path to clipboard');
      });
    }
    hideTabContextMenu();
  });
}

function showWelcomeMessage(): void {
  const welcomeMessage = document.querySelector('#welcome-message') as HTMLElement;
  const editor = window.editor;

  if (editor) {
    editor.container.style.display = 'none';
  }

  welcomeMessage.classList.remove('hidden');
  localStorage.removeItem('currentEditorFile');
  localStorage.removeItem('currentEditorValue');
}

function hideWelcomeMessage(): void {
  const welcomeMessage = document.querySelector('#welcome-message') as HTMLElement;
  const editor = window.editor;

  welcomeMessage.classList.add('hidden');

  if (editor) {
    editor.container.style.display = 'block';
  }
}

function showNewFileDialog(): void {
  const fileName = prompt('Enter filename (e.g., my_file.nitro):', 'new_file.nitro');

  if (fileName && fileName.trim()) {
    let filePath = fileName.trim();

    // Add src/ prefix if not already present
    if (!filePath.startsWith('src/')) {
      filePath = 'src/' + filePath;
    }

    // Ensure .nitro extension if no extension provided
    if (!filePath.includes('.')) {
      filePath += '.nitro';
    }

    createNewFile(filePath);
  }
}

async function createNewFile(filePath: string): Promise<void> {
  try {
    const fs = await getProject();

    // Check if file already exists
    try {
      await fs.readFile(filePath);
      // File exists, just open it
      editorSelectFile(filePath).catch(console.error);
      return;
    } catch {
      // File doesn't exist, create it
    }

    const defaultContent = getDefaultFileContent(filePath);
    await fs.writeFile(filePath, new TextEncoder().encode(defaultContent));
    await saveFS(fs);
    await updateFiles();

    editorSelectFile(filePath).catch(console.error);
    console.info(`Created new file: ${filePath}`);
  } catch (error) {
    console.error('Failed to create file:', error);
  }
}

function getDefaultFileContent(filePath: string): string {
  const extension = filePath.split('.').pop()?.toLowerCase();

  switch (extension) {
    case 'nitro':
      return ``;
    case 'js':
      return ``;
    case 'ts':
      return ``;
    case 'json':
      return '{\n  \n}\n';
    case 'html':
      return '<!DOCTYPE html>\n<html>\n<head>\n    <title></title>\n</head>\n<body>\n    \n</body>\n</html>\n';
    case 'css':
      return `/* ${filePath.split('/').pop()} */\n\n`;
    default:
      return `// ${filePath.split('/').pop()}\n\n`;
  }
}

function setupWelcomeMessage(): void {
  document.querySelector('#welcome-open-index')!.addEventListener('click', () => {
    editorSelectFile('/src/main.nitro').catch(console.error);
  });
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

function restoreCompilationSettings(): void {
  // Restore target selection
  const targetSelect = document.querySelector('#target-select') as HTMLSelectElement;
  const storedTarget = localStorage.getItem('nitro-target');
  if (storedTarget) {
    targetSelect.value = storedTarget;
  }

  // Restore core library selection
  const coreLibrarySelect = document.querySelector('#core-library-select') as HTMLSelectElement;
  const storedCoreLibrary = localStorage.getItem('nitro-core-library');
  if (storedCoreLibrary) {
    coreLibrarySelect.value = storedCoreLibrary;
  }

  // Restore checkboxes
  const verboseCheckbox = document.querySelector('#verbose-checkbox') as HTMLInputElement;
  verboseCheckbox.checked = localStorage.getItem('nitro-verbose') === 'true';

  const dumpIrCheckbox = document.querySelector('#dump-ir-checkbox') as HTMLInputElement;
  dumpIrCheckbox.checked = localStorage.getItem('nitro-dump-ir') === 'true';

  const enableRcGcCheckbox = document.querySelector('#enable-rc-gc-checkbox') as HTMLInputElement;
  enableRcGcCheckbox.checked = localStorage.getItem('nitro-enable-rc-gc') !== 'false'; // Default to true

  // Restore custom arguments
  const customArgsInput = document.querySelector('#custom-args-input') as HTMLInputElement;
  const storedCustomArgs = localStorage.getItem('nitro-custom-args');
  if (storedCustomArgs) {
    customArgsInput.value = storedCustomArgs;
  }

  // Restore main file
  const mainFileInput = document.querySelector('#main-file-input') as HTMLInputElement;
  const storedMainFile = localStorage.getItem('nitro-main-file');
  if (storedMainFile) {
    mainFileInput.value = storedMainFile;
  }
}

export async function setupEditor(): Promise<Ace.Editor> {
  const editor = ace.edit("editor", {useWorker: false});
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
  setupTabContextMenu();
  setupWelcomeMessage();
  restoreCompilationSettings();

  console.info("Use Ctrl-S to compile and run");
  compile().then(() => console.debug('Done!'));
  console.debug("Editor is ready");

  return editor;
}

function setupEventListeners(): void {
  document.addEventListener('click', (e) => {
    const target = e.target as Element;
    // Don't close menu if clicking on menu button or inside the dropdown
    if (!target.closest('#open-menu') && !target.closest('.menu-dropdown')) {
      document.querySelector('#menu')!.classList.remove('open');
    }
  });

  document.addEventListener('keydown', (e) => {
    if (e.key === 'Escape') {
      document.querySelector('#menu')!.classList.remove('open');
    }

    // Global keyboard shortcuts
    if ((e.ctrlKey || e.metaKey) && !e.shiftKey && !e.altKey) {
      switch (e.key) {
        case 's':
          e.preventDefault();
          compile().then(() => console.debug('Done!'));
          break;
        case 'b':
          e.preventDefault();
          document.querySelector('#files')!.classList.toggle('open');
          break;
        case 'j':
          e.preventDefault();
          toggleTerminal();
          break;
        case 'n':
          e.preventDefault();
          showNewFileDialog();
          break;
      }
    }
  });

  document.querySelector('#open-menu')!.addEventListener('click', () => {
    document.querySelector('#menu')!.classList.toggle('open');
  });

  document.querySelectorAll('#menu-btn-compile, #compiler-btn').forEach(btn =>
    btn.addEventListener('click', () => {
      openTerminal();
      compile().then(() => console.debug('Done!'));
    })
  );

  document.querySelector('#menu-btn-toggle-files')!.addEventListener('click', () => {
    document.querySelector('#files')!.classList.toggle('open');
  });

  document.querySelector('#menu-btn-toggle-console')!.addEventListener('click', () => {
    toggleTerminal();
  });

  document.querySelector('#menu-btn-download')!.addEventListener('click', () => {
    const basename = (path: string) => path.split('/').pop() || path;
    const editor = window.editor!;
    const a = document.createElement('a');
    const file = new Blob([editor.getValue()], {type: 'text/plain'});
    a.href = URL.createObjectURL(file);
    a.download = basename(localStorage.getItem('currentEditorFile') || 'download.nitro');
    a.click();
  });

  document.querySelector('#menu-btn-new-file')!.addEventListener('click', () => {
    showNewFileDialog();
  });

  document.querySelector('#menu-btn-toggle-theme')!.addEventListener('click', () => {
    const editor = window.editor!;
    let index = validThemes.indexOf(editor.getTheme() as ValidTheme);
    index = (index + 1) % validThemes.length;

    const newTheme = validThemes[index];
    localStorage.setItem('selectedTheme', newTheme);
    editor.setTheme(newTheme);
  });

  // Compilation settings event handlers
  document.querySelector('#target-select')!.addEventListener('change', (e) => {
    const target = (e.target as HTMLSelectElement).value;
    localStorage.setItem('nitro-target', target);
  });

  document.querySelector('#verbose-checkbox')!.addEventListener('change', (e) => {
    const checked = (e.target as HTMLInputElement).checked;
    localStorage.setItem('nitro-verbose', checked.toString());
  });

  document.querySelector('#dump-ir-checkbox')!.addEventListener('change', (e) => {
    const checked = (e.target as HTMLInputElement).checked;
    localStorage.setItem('nitro-dump-ir', checked.toString());
  });

  document.querySelector('#core-library-select')!.addEventListener('change', (e) => {
    const value = (e.target as HTMLSelectElement).value;
    localStorage.setItem('nitro-core-library', value);
  });

  document.querySelector('#enable-rc-gc-checkbox')!.addEventListener('change', (e) => {
    const checked = (e.target as HTMLInputElement).checked;
    localStorage.setItem('nitro-enable-rc-gc', checked.toString());
  });

  document.querySelector('#custom-args-input')!.addEventListener('input', (e) => {
    const value = (e.target as HTMLInputElement).value;
    localStorage.setItem('nitro-custom-args', value);
  });

  document.querySelector('#main-file-input')!.addEventListener('input', (e) => {
    const value = (e.target as HTMLInputElement).value;
    localStorage.setItem('nitro-main-file', value);
  });
}

function setupKeyboardShortcuts(editor: Ace.Editor): void {
  editor.commands.addCommand({
    name: 'comment line',
    bindKey: {win: 'Ctrl-/', mac: 'Command-/'},
    exec: function () {
      editor.toggleCommentLines();
    },
    readOnly: true,
  });

  editor.commands.addCommand({
    name: 'compile',
    bindKey: {win: 'Ctrl-S', mac: 'Command-S'},
    exec: function () {
      compile().then(() => console.debug('Done!'));
    },
    readOnly: true,
  });

  editor.commands.addCommand({
    name: 'toggle file explorer',
    bindKey: {win: 'Ctrl-B', mac: 'Command-B'},
    exec: function () {
      document.querySelector('#files')!.classList.toggle('open');
    },
    readOnly: true,
  });

  editor.commands.addCommand({
    name: 'toggle console',
    bindKey: {win: 'Ctrl-J', mac: 'Command-J'},
    exec: function () {
      toggleTerminal();
    },
    readOnly: true,
  });
}

async function restore(): Promise<void> {
  const editor = window.editor!;
  const saved = localStorage.getItem('currentEditorValue');
  const currentFile = localStorage.getItem('currentEditorFile');

  if (saved && currentFile) {
    hideWelcomeMessage();
    editor.setValue(saved);
    editor.clearSelection();
    addTab(currentFile);
    await saveEditorFile();
  } else {
    // No previous session - show welcome message
    showWelcomeMessage();
  }
}
