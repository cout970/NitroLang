import { editorSelectFile } from './editor';
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
