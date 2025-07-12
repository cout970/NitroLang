import { editorSelectFile, addTab, removeTab } from './editor';
import { Directory } from '@wasmer/sdk';
import { IndexedDBFileSystem } from './fs';
import { WasmerBridge } from './wasmer-bridge';

interface FileIndex {
  default_directories: string[];
  default_files: Array<{
    source: string;
    target: string;
    files: string[];
  }>;
}

let currentProject: WasmerBridge | null = null;

export async function setupFiles(): Promise<void> {
  window.expandedDirs = new Set(['/src']);

  await getProject();
  await updateFiles();
  initGrabber();
}

export async function getProject(): Promise<WasmerBridge> {
  if (!currentProject) {
    currentProject = await initFS();
  }
  return currentProject;
}

// Debug helper functions - expose to global scope for console debugging
(window as any).debugFs = {
  printTree: async (path = '/') => {
    const fs = await getProject();
    await fs.printTree(path);
  },
  printFullTree: async () => {
    const fs = await getProject();
    await fs.printFullTree();
  },
  debugPrintAllFiles: async () => {
    const fs = await getProject();
    await fs.debugPrintAllFiles();
  },
  getStorageUsage: async () => {
    const fs = await getProject();
    return await fs.getStorageUsage();
  }
};

async function initFS(): Promise<WasmerBridge> {
  console.debug('Initializing file system');

  const idbFs = new IndexedDBFileSystem();
  await idbFs.init();

  const wasmerFs = new Directory();
  const bridge = new WasmerBridge(idbFs, wasmerFs);

  await loadDefaultFiles(bridge);
  return bridge;
}

async function loadDefaultFiles(fs: WasmerBridge): Promise<void> {
  const index: FileIndex = await (await fetch('/file_index.json')).json();
  console.debug('Loading default files');

  for (const dir of index.default_directories) {
    try {
      await fs.mkdir(dir, { recursive: true });
    } catch {
      // The directory already exists, ignore the error
    }
  }

  for (const fileGroup of index.default_files) {
    const { source, target, files } = fileGroup;

    const loadFile = async (source: string, target: string, file: string) => {
      const path = `${target}/${file}`;
      const url = new URL(`${source}/${file}`, import.meta.url).href;
      await loadSingleFile(fs, url, path);
    };

    for (const file of files) {
      await loadFile(source, target, file);
    }
  }

  // Create /src/main.nitro, if it's on cache load it to preserve use modifications
  if (!(await fs.exists('/src/main.nitro'))) {
    const contents = `\n// This is the starting point of your program, feel free to experiment and mess around.\n\nfun main() {\n  println("Hello, World!")\n}\n`;
    await fs.writeFile('/src/main.nitro', new TextEncoder().encode(contents));
  }
}

async function loadSingleFile(fs: WasmerBridge, url: string, path: string): Promise<void> {
  // Check if file already exists in our IndexedDB filesystem
  if (await fs.exists(path)) {
    // console.debug(`File ${path} already exists, skipping download`);
    return;
  }

  console.log(`Loading file ${path} from ${url}`);

  try {
    // Try to download and cache the file
    await fs.downloadAndCache(url, path);
  } catch (error) {
    console.warn(`Failed to download ${path}, trying direct fetch:`, error);

    // Fallback to direct fetch
    try {
      const buffer = await (await fetch(url)).arrayBuffer();
      await fs.writeFile(path, new Uint8Array(buffer));
    } catch (fetchError) {
      console.error(`Failed to load file ${path}:`, fetchError);
    }
  }
}

export async function saveFS(fs: WasmerBridge): Promise<void> {
  // The IndexedDB filesystem automatically persists changes
  // We just need to ensure Wasmer is in sync
  await fs.syncToWasmer('/');
}

export async function updateFiles(): Promise<void> {
  const fileTree = document.querySelector('#file-tree')!;
  const fileTemplate = document.querySelector('#files-file-template') as HTMLTemplateElement;
  const project = await getProject();
  const root = await project.readDir('/');
  fileTree.innerHTML = '';

  // Add context menu support for empty file tree area
  fileTree.addEventListener('contextmenu', (e) => {
    // Only show context menu if clicking on empty area (not on a file)
    if (e.target === fileTree) {
      e.preventDefault();
      showFileTreeContextMenu(e as MouseEvent);
    }
  });

  const expandedDirs = window.expandedDirs!;

  const iter = async (entry: any, path: string, level: number) => {
    const currentPath = path + '/' + entry.name;

    const elem = fileTemplate.content.cloneNode(true) as DocumentFragment;
    const rootElem = elem.querySelector('.file') as HTMLElement;

    if (entry.type === 'dir') rootElem.classList.add('file-directory');
    if (entry.type === 'file') rootElem.classList.add('file-regular');
    if (entry.type === 'unknown') rootElem.classList.add('file-unknown');

    rootElem.style.paddingLeft = `${level * 29}px`;

    const nameElem = elem.querySelector('.name') as HTMLElement;
    nameElem.textContent = entry.name;
    fileTree.appendChild(elem);

    // Add context menu support
    rootElem.addEventListener('contextmenu', (e) => {
      e.preventDefault();
      showFileContextMenu(e, currentPath, entry.type);
    });

    if (entry.type === 'file') {
      rootElem.addEventListener('click', () => {
        editorSelectFile(currentPath);
      });
    }

    if (entry.type === 'dir') {
      rootElem.addEventListener('click', () => {
        if (expandedDirs.has(currentPath)) {
          expandedDirs.delete(currentPath);
        } else {
          expandedDirs.add(currentPath);
        }
        updateFiles().catch(console.error);
      });

      const expanded = expandedDirs.has(currentPath);

      if (expanded) {
        rootElem.classList.add('expanded');

        const children = await project.readDir(currentPath);

        children.sort((a, b) => {
          if (a.type === 'dir' && b.type !== 'dir') return -1;
          if (a.type !== 'dir' && b.type === 'dir') return 1;
          return a.name.localeCompare(b.name);
        });

        for (const child of children) {
          await iter(child, currentPath, level + 1);
        }
      }
    }
  };

  for (const entry of root) {
    await iter(entry, '', 0);
  }
}

function initGrabber(): void {
  const files = document.querySelector('#files')! as HTMLElement;
  const grab = document.querySelector('#files .files-grab') as HTMLElement;
  let holding = false;
  let initialHeight = 0;
  let initialWidth = 0;
  let startPos = { x: 0, y: 0 };
  const horizontal = true;

  grab.addEventListener('mousedown', e => {
    startPos = { x: e.clientX, y: e.clientY };
    initialHeight = parseInt(files.offsetHeight.toString(), 10);
    initialHeight = isNaN(initialHeight) ? 200 : initialHeight;
    initialWidth = parseInt(files.offsetWidth.toString(), 10);
    initialWidth = isNaN(initialWidth) ? 200 : initialWidth;

    holding = true;
    e.stopPropagation();
    e.preventDefault();
  });

  document.addEventListener('mouseup', () => {
    holding = false;
  });

  document.addEventListener('mousemove', e => {
    if (!holding) return;
    const pos = { x: e.clientX, y: e.clientY };
    const diff = { x: (pos.x - startPos.x), y: -(pos.y - startPos.y) };

    if (horizontal) {
      let newWidth = initialWidth + diff.x;
      if (newWidth < 40) newWidth = 40;
      if (newWidth > window.innerWidth - 40) newWidth = window.innerWidth - 40;

      (files as HTMLElement).style.width = `${newWidth}px`;
      (files as HTMLElement).style.minWidth = `${newWidth}px`;
      (files as HTMLElement).style.height = '100%';
      (files as HTMLElement).style.minHeight = '100%';
    } else {
      let newHeight = initialHeight + diff.y;
      if (newHeight < 40) newHeight = 40;
      if (newHeight > window.innerHeight - 40) newHeight = window.innerHeight - 40;

      (files as HTMLElement).style.width = '100%';
      (files as HTMLElement).style.minWidth = '100%';
      (files as HTMLElement).style.height = `${newHeight}px`;
      (files as HTMLElement).style.minHeight = `${newHeight}px`;

      if (files.classList.contains('files-collapsed')) {
        files.classList.remove('files-collapsed');
      }
    }
  });
}

// File context menu functionality
let currentFileContextPath: string | null = null;
let currentFileContextType: string | null = null;
let fileClipboard: { path: string; operation: 'copy' | 'cut' } | null = null;

function showFileTreeContextMenu(e: MouseEvent): void {
  const contextMenu = document.querySelector('#file-tree-context-menu') as HTMLElement;

  // Update paste button state
  const pasteButton = contextMenu.querySelector('#tree-context-paste') as HTMLElement;
  if (fileClipboard) {
    pasteButton.classList.remove('disabled');
  } else {
    pasteButton.classList.add('disabled');
  }

  // Show menu temporarily to measure its size
  contextMenu.classList.remove('hidden');
  contextMenu.style.visibility = 'hidden';
  contextMenu.style.left = '0px';
  contextMenu.style.top = '0px';

  // Get menu dimensions
  const menuRect = contextMenu.getBoundingClientRect();
  const menuWidth = menuRect.width;
  const menuHeight = menuRect.height;

  // Calculate position, adjusting for screen boundaries
  let left = e.clientX;
  let top = e.clientY;

  // Check right boundary
  if (left + menuWidth > window.innerWidth) {
    left = window.innerWidth - menuWidth - 10; // 10px margin
  }

  // Check bottom boundary
  if (top + menuHeight > window.innerHeight) {
    top = window.innerHeight - menuHeight - 10; // 10px margin
  }

  // Check left boundary
  if (left < 10) {
    left = 10;
  }

  // Check top boundary
  if (top < 10) {
    top = 10;
  }

  // Apply final position and show menu
  contextMenu.style.left = `${left}px`;
  contextMenu.style.top = `${top}px`;
  contextMenu.style.visibility = 'visible';
}

function hideFileTreeContextMenu(): void {
  const contextMenu = document.querySelector('#file-tree-context-menu') as HTMLElement;
  contextMenu.classList.add('hidden');
}

function showFileContextMenu(e: MouseEvent, filePath: string, fileType: string): void {
  const contextMenu = document.querySelector('#file-context-menu') as HTMLElement;
  currentFileContextPath = filePath;
  currentFileContextType = fileType;

  // Show/hide file-only options
  const fileOnlyButtons = contextMenu.querySelectorAll('.file-only');
  fileOnlyButtons.forEach(button => {
    (button as HTMLElement).style.display = fileType === 'file' ? 'flex' : 'none';
  });

  // Update paste button state
  const pasteButton = contextMenu.querySelector('#file-context-paste') as HTMLElement;
  if (fileClipboard) {
    pasteButton.classList.remove('disabled');
  } else {
    pasteButton.classList.add('disabled');
  }

  // Show menu temporarily to measure its size
  contextMenu.classList.remove('hidden');
  contextMenu.style.visibility = 'hidden';
  contextMenu.style.left = '0px';
  contextMenu.style.top = '0px';

  // Get menu dimensions
  const menuRect = contextMenu.getBoundingClientRect();
  const menuWidth = menuRect.width;
  const menuHeight = menuRect.height;

  // Calculate position, adjusting for screen boundaries
  let left = e.clientX;
  let top = e.clientY;

  // Check right boundary
  if (left + menuWidth > window.innerWidth) {
    left = window.innerWidth - menuWidth - 10; // 10px margin
  }

  // Check bottom boundary
  if (top + menuHeight > window.innerHeight) {
    top = window.innerHeight - menuHeight - 10; // 10px margin
  }

  // Check left boundary
  if (left < 10) {
    left = 10;
  }

  // Check top boundary
  if (top < 10) {
    top = 10;
  }

  // Apply final position and show menu
  contextMenu.style.left = `${left}px`;
  contextMenu.style.top = `${top}px`;
  contextMenu.style.visibility = 'visible';
}

function hideFileContextMenu(): void {
  const contextMenu = document.querySelector('#file-context-menu') as HTMLElement;
  contextMenu.classList.add('hidden');
  currentFileContextPath = null;
  currentFileContextType = null;
}

function hideAllContextMenus(): void {
  hideFileContextMenu();
  hideFileTreeContextMenu();
}

async function downloadFile(filePath: string): Promise<void> {
  try {
    const fs = await getProject();
    const fileContent = await fs.readFile(filePath);
    const fileName = filePath.split('/').pop() || 'download';

    const blob = new Blob([fileContent], { type: 'text/plain' });
    const url = URL.createObjectURL(blob);

    const a = document.createElement('a');
    a.href = url;
    a.download = fileName;
    a.click();

    URL.revokeObjectURL(url);
    console.info(`Downloaded: ${filePath}`);
  } catch (error) {
    console.error('Failed to download file:', error);
  }
}

async function createNewFileInPath(basePath: string, isFolder: boolean = false): Promise<void> {
  const itemType = isFolder ? 'folder' : 'file';
  const extension = isFolder ? '' : '.nitro';
  const defaultName = isFolder ? 'new_folder' : 'new_file';

  const name = prompt(`Enter ${itemType} name:`, defaultName + extension);
  if (!name || !name.trim()) return;

  const newPath = basePath === '/' ? `/${name.trim()}` : `${basePath}/${name.trim()}`;

  try {
    const fs = await getProject();

    if (isFolder) {
      await fs.mkdir(newPath, { recursive: true });
      console.info(`Created folder: ${newPath}`);
    } else {
      // Check if file already exists
      try {
        await fs.readFile(newPath);
        alert('File already exists!');
        return;
      } catch {
        // File doesn't exist, create it
      }

      const defaultContent = getDefaultFileContent(newPath);
      await fs.writeFile(newPath, new TextEncoder().encode(defaultContent));
      console.info(`Created file: ${newPath}`);

      // Open the new file
      editorSelectFile(newPath).catch(console.error);
    }

    await saveFS(fs);
    await updateFiles();
  } catch (error) {
    console.error(`Failed to create ${itemType}:`, error);
    alert(`Failed to create ${itemType}`);
  }
}

function getDefaultFileContent(filePath: string): string {
  const extension = filePath.split('.').pop()?.toLowerCase();

  switch (extension) {
    case 'nitro':
      return `// ${filePath.split('/').pop()}\n\nfun main() {\n    println("Hello, World!")\n}\n`;
    case 'js':
      return `// ${filePath.split('/').pop()}\n\nconsole.log("Hello, World!");\n`;
    case 'ts':
      return `// ${filePath.split('/').pop()}\n\nconsole.log("Hello, World!");\n`;
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

async function deleteFileOrFolder(filePath: string): Promise<void> {
  const fileName = filePath.split('/').pop() || filePath;
  const confirmed = confirm(`Are you sure you want to delete "${fileName}"?\n\nThis action cannot be undone.`);

  if (!confirmed) return;

  try {
    const fs = await getProject();

    // Check if it's a directory
    try {
      await fs.readDir(filePath);
      // It's a directory, remove recursively
      await removeDirectoryRecursive(fs, filePath);
    } catch {
      // It's a file
      await fs.removeFile(filePath);

      // Close tab if the file is open
      removeTab(filePath);
    }

    await saveFS(fs);
    await updateFiles();
    console.info(`Deleted: ${filePath}`);
  } catch (error) {
    console.error('Failed to delete:', error);
    alert('Failed to delete item');
  }
}

async function removeDirectoryRecursive(fs: WasmerBridge, dirPath: string): Promise<void> {
  try {
    const entries = await fs.readDir(dirPath);

    for (const entry of entries) {
      const entryPath = `${dirPath}/${entry.name}`;

      if (entry.type === 'dir') {
        await removeDirectoryRecursive(fs, entryPath);
      } else {
        await fs.removeFile(entryPath);
        // Close tab if file is open
        removeTab(entryPath);
      }
    }

    await fs.rmdir(dirPath);
  } catch (error) {
    console.error(`Failed to remove directory ${dirPath}:`, error);
    throw error;
  }
}

async function renameFileOrFolder(oldPath: string): Promise<void> {
  const oldName = oldPath.split('/').pop() || '';
  const newName = prompt('Enter new name:', oldName);

  if (!newName || !newName.trim() || newName === oldName) return;

  const pathParts = oldPath.split('/');
  pathParts[pathParts.length - 1] = newName.trim();
  const newPath = pathParts.join('/');

  try {
    const fs = await getProject();

    // Check if target already exists
    try {
      await fs.readFile(newPath);
      alert('A file or folder with that name already exists!');
      return;
    } catch {
      // Target doesn't exist, proceed
    }

    // For files, copy content and delete original
    try {
      const content = await fs.readFile(oldPath);
      await fs.writeFile(newPath, content);
      await fs.removeFile(oldPath);

      // Update open tab
      removeTab(oldPath);
      addTab(newPath);
    } catch {
      // It's a directory - this is more complex, for now just show error
      alert('Renaming directories is not yet supported');
      return;
    }

    await saveFS(fs);
    await updateFiles();
    console.info(`Renamed: ${oldPath} → ${newPath}`);
  } catch (error) {
    console.error('Failed to rename:', error);
    alert('Failed to rename item');
  }
}

function copyToClipboard(filePath: string, operation: 'copy' | 'cut'): void {
  fileClipboard = { path: filePath, operation };
  console.info(`${operation === 'copy' ? 'Copied' : 'Cut'}: ${filePath}`);
}

async function pasteFromClipboard(targetDir: string): Promise<void> {
  if (!fileClipboard) return;

  const { path: sourcePath, operation } = fileClipboard;
  const fileName = sourcePath.split('/').pop() || 'pasted_file';
  let targetPath = targetDir === '/' ? `/${fileName}` : `${targetDir}/${fileName}`;

  try {
    const fs = await getProject();

    // Check if target already exists
    try {
      await fs.readFile(targetPath);
      // File exists, ask user for new name
      const newName = prompt(`A file named "${fileName}" already exists. Enter a new name:`, fileName);
      if (!newName || !newName.trim()) return;

      targetPath = targetDir === '/' ? `/${newName.trim()}` : `${targetDir}/${newName.trim()}`;

      // Check if the new name also conflicts
      try {
        await fs.readFile(targetPath);
        alert('A file with that name already exists!');
        return;
      } catch {
        // New name is available, proceed
      }
    } catch {
      // Target doesn't exist, proceed with original name
    }

    // Copy file content
    const content = await fs.readFile(sourcePath);
    await fs.writeFile(targetPath, content);

    // If it was a cut operation, delete the original
    if (operation === 'cut') {
      await fs.removeFile(sourcePath);
      removeTab(sourcePath);
      fileClipboard = null; // Clear clipboard after cut
    }

    await saveFS(fs);
    await updateFiles();
    console.info(`Pasted: ${sourcePath} → ${targetPath}`);
  } catch (error) {
    console.error('Failed to paste:', error);
    alert('Failed to paste item');
  }
}

// Setup file context menu event listeners
export function setupFileContextMenu(): void {
  // Close context menus when clicking elsewhere
  document.addEventListener('click', (e) => {
    const target = e.target as Element;
    if (!target.closest('#file-context-menu') && !target.closest('#file-tree-context-menu')) {
      hideAllContextMenus();
    }
  });

  // File context menu actions
  document.querySelector('#file-context-open')!.addEventListener('click', () => {
    if (currentFileContextPath) {
      editorSelectFile(currentFileContextPath).catch(console.error);
    }
    hideFileContextMenu();
  });

  document.querySelector('#file-context-download')!.addEventListener('click', () => {
    if (currentFileContextPath) {
      downloadFile(currentFileContextPath);
    }
    hideFileContextMenu();
  });

  document.querySelector('#file-context-new-file')!.addEventListener('click', () => {
    if (currentFileContextPath) {
      const basePath = currentFileContextType === 'dir' ? currentFileContextPath :
                      currentFileContextPath.substring(0, currentFileContextPath.lastIndexOf('/')) || '/';
      createNewFileInPath(basePath, false);
    }
    hideFileContextMenu();
  });

  document.querySelector('#file-context-new-folder')!.addEventListener('click', () => {
    if (currentFileContextPath) {
      const basePath = currentFileContextType === 'dir' ? currentFileContextPath :
                      currentFileContextPath.substring(0, currentFileContextPath.lastIndexOf('/')) || '/';
      createNewFileInPath(basePath, true);
    }
    hideFileContextMenu();
  });

  document.querySelector('#file-context-cut')!.addEventListener('click', () => {
    if (currentFileContextPath) {
      copyToClipboard(currentFileContextPath, 'cut');
    }
    hideFileContextMenu();
  });

  document.querySelector('#file-context-copy')!.addEventListener('click', () => {
    if (currentFileContextPath) {
      copyToClipboard(currentFileContextPath, 'copy');
    }
    hideFileContextMenu();
  });

  document.querySelector('#file-context-paste')!.addEventListener('click', () => {
    if (currentFileContextPath && fileClipboard) {
      const targetDir = currentFileContextType === 'dir' ? currentFileContextPath :
                       currentFileContextPath.substring(0, currentFileContextPath.lastIndexOf('/')) || '/';
      pasteFromClipboard(targetDir);
    }
    hideFileContextMenu();
  });

  document.querySelector('#file-context-rename')!.addEventListener('click', () => {
    if (currentFileContextPath) {
      renameFileOrFolder(currentFileContextPath);
    }
    hideFileContextMenu();
  });

  document.querySelector('#file-context-delete')!.addEventListener('click', () => {
    if (currentFileContextPath) {
      deleteFileOrFolder(currentFileContextPath);
    }
    hideFileContextMenu();
  });

  document.querySelector('#file-context-copy-path')!.addEventListener('click', () => {
    if (currentFileContextPath) {
      navigator.clipboard.writeText(currentFileContextPath).then(() => {
        console.info(`Copied path: ${currentFileContextPath}`);
      }).catch(() => {
        console.warn('Failed to copy path to clipboard');
      });
    }
    hideFileContextMenu();
  });

  // File tree empty area context menu actions
  document.querySelector('#tree-context-new-file')!.addEventListener('click', () => {
    createNewFileInPath('/', false);
    hideFileTreeContextMenu();
  });

  document.querySelector('#tree-context-new-folder')!.addEventListener('click', () => {
    createNewFileInPath('/', true);
    hideFileTreeContextMenu();
  });

  document.querySelector('#tree-context-paste')!.addEventListener('click', () => {
    if (fileClipboard) {
      pasteFromClipboard('/');
    }
    hideFileTreeContextMenu();
  });
}
