import {getProject} from "./compiler";
import {editorSelectFile} from "./editor";


export async function setupFiles() {
  const files = document.querySelector('#files');

  document.expandedDirs = new Set(['/src']);

  await updateFiles();

  const initGrabber = () => {
    const grab = document.querySelector('#files .files-grab');
    let holding = false;
    let initialHeight = 0;
    let initialWidth = 0;
    let startPos = {x: 0, y: 0};
    const horizontal = true;

    grab.addEventListener('mousedown', e => {
      startPos = {x: e.clientX, y: e.clientY};
      initialHeight = parseInt(files.offsetHeight, 10);
      initialHeight = isNaN(initialHeight) ? 200 : initialHeight;
      initialWidth = parseInt(files.offsetWidth, 10);
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
      const pos = {x: e.clientX, y: e.clientY};
      const diff = {x: (pos.x - startPos.x), y: -(pos.y - startPos.y)};

      if (horizontal) {
        let newWidth = initialWidth + diff.x;
        if (newWidth < 40) newWidth = 40;
        if (newWidth > window.innerWidth - 40) newWidth = window.innerWidth - 40;

        files.style.width = `${newWidth}px`;
        files.style.minWidth = `${newWidth}px`;
        files.style.height = '100%';
        files.style.minHeight = '100%';
      } else {
        let newHeight = initialHeight + diff.y;
        if (newHeight < 40) newHeight = 40;
        if (newHeight > window.innerHeight - 40) newHeight = window.innerHeight - 40;

        files.style.width = '100%';
        files.style.minWidth = '100%';
        files.style.height = `${newHeight}px`;
        files.style.minHeight = `${newHeight}px`;

        if (files.classList.contains('files-collapsed')) {
          files.classList.remove('files-collapsed');
        }
      }
    });
  }

  initGrabber();
}

export async function updateFiles() {
  const fileTree = document.querySelector('#file-tree');
  const fileTemplate = document.querySelector('#files-file-template');
  const project = await getProject();

  const root = await project.readDir('/');
  fileTree.innerHTML = '';

  /** @type {Set<string>} */
  const expandedDirs = document.expandedDirs;

  const iter = async (entry, path, level) => {
    const currentPath = path + '/' + entry.name;

    const elem = fileTemplate.content.cloneNode(true);
    const rootElem = elem.querySelector('.file');
    entry.type === 'dir' && rootElem.classList.add('file-directory');
    entry.type === 'file' && rootElem.classList.add('file-regular');
    entry.type === 'unknown' && rootElem.classList.add('file-unknown');
    rootElem.style.paddingLeft = `${level * 29}px`;

    const nameElem = elem.querySelector('.name');
    nameElem.textContent = entry.name;
    fileTree.appendChild(elem);

    if (entry.type === 'file') {
      rootElem.addEventListener('click', () => {
        editorSelectFile(currentPath)
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
    }

    if (entry.type === 'dir') {
      const expanded = expandedDirs.has(currentPath);

      if (expanded) {
        rootElem.classList.add('expanded');

        const children = await project.readDir(currentPath);

        for (const child of children) {
          await iter(child, currentPath, level + 1);
        }
      }
    }
  }

  for (const entry of root) {
    await iter(entry, '', 0);
  }
}
