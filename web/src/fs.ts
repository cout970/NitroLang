interface FileStats {
  size: number;
  mtime: number;
  ctime: number;
  isFile: boolean;
  isDirectory: boolean;
}

interface DirectoryEntry {
  name: string;
  type: 'file' | 'dir';
  stats: FileStats;
}

interface FileSystemNode {
  id?: number;
  path: string;
  name: string;
  type: 'file' | 'dir';
  content?: Uint8Array;
  mtime: number;
  ctime: number;
  size: number;
  parent: string|null;
}

interface CacheEntry {
  url: string;
  etag?: string;
  lastModified?: string;
  mtime: number;
  content: Uint8Array;
}

class IndexedDBFileSystem {
  private db: IDBDatabase | null = null;
  private readonly dbName = 'NitroFileSystem';
  private readonly dbVersion = 1;
  private readonly fileStore = 'files';
  private readonly cacheStore = 'cache';

  async init(): Promise<void> {
    return new Promise((resolve, reject) => {
      const request = indexedDB.open(this.dbName, this.dbVersion);

      request.onerror = () => reject(request.error);
      request.onsuccess = async () => {
        this.db = request.result;
        // Ensure root directory exists
        await this.ensureRootDirectory();
        resolve();
      };

      request.onupgradeneeded = (event) => {
        const db = (event.target as IDBOpenDBRequest).result;

        if (!db.objectStoreNames.contains(this.fileStore)) {
          const fileStore = db.createObjectStore(this.fileStore, {
            keyPath: 'id',
            autoIncrement: true
          });
          fileStore.createIndex('path', 'path', { unique: true });
          fileStore.createIndex('parent', 'parent', { unique: false });
        }

        if (!db.objectStoreNames.contains(this.cacheStore)) {
          db.createObjectStore(this.cacheStore, {
            keyPath: 'url'
          });
        }
      };
    });
  }

  private ensureInitialized(): void {
    if (!this.db) {
      throw new Error('FileSystem not initialized. Call init() first.');
    }
  }

  private async ensureRootDirectory(): Promise<void> {
    try {
      await this.stat('/');
    } catch (error) {
      // Root directory doesn't exist, create it
      console.debug('Creating root directory');
      const now = Date.now();

      return new Promise((resolve, reject) => {
        const transaction = this.db!.transaction([this.fileStore], 'readwrite');
        const store = transaction.objectStore(this.fileStore);

        const rootNode: FileSystemNode = {
          path: '/',
          name: '/',
          type: 'dir',
          mtime: now,
          ctime: now,
          size: 0,
          parent: null
        };

        const request = store.add(rootNode);
        request.onsuccess = () => resolve();
        request.onerror = () => reject(request.error);
      });
    }
  }

  private normalizePath(path: string): string {
    if (!path.startsWith('/')) path = '/' + path;
    return path.replace(/\/+/g, '/').replace(/\/$/, '') || '/';
  }

  private getParentPath(path: string): string | null {
    const normalized = this.normalizePath(path);
    if (normalized === '/') return null;
    const parts = normalized.split('/');
    parts.pop();
    return parts.join('/') || '/';
  }

  private getFileName(path: string): string {
    const normalized = this.normalizePath(path);
    if (normalized === '/') return '/';
    return normalized.split('/').pop() || '';
  }

  async stat(path: string): Promise<FileStats> {
    this.ensureInitialized();
    const normalized = this.normalizePath(path);

    return new Promise((resolve, reject) => {
      const transaction = this.db!.transaction([this.fileStore], 'readonly');
      const store = transaction.objectStore(this.fileStore);
      const index = store.index('path');
      const request = index.get(normalized);

      request.onsuccess = () => {
        const node = request.result as FileSystemNode;
        if (!node) {
          reject(new Error(`ENOENT: no such file or directory, stat '${path}'`));
          return;
        }

        resolve({
          size: node.size,
          mtime: node.mtime,
          ctime: node.ctime,
          isFile: node.type === 'file',
          isDirectory: node.type === 'dir'
        });
      };

      request.onerror = () => reject(request.error);
    });
  }

  async exists(path: string): Promise<boolean> {
    try {
      await this.stat(path);
      return true;
    } catch {
      return false;
    }
  }

  async readFile(path: string): Promise<Uint8Array> {
    this.ensureInitialized();
    const normalized = this.normalizePath(path);

    return new Promise((resolve, reject) => {
      const transaction = this.db!.transaction([this.fileStore], 'readonly');
      const store = transaction.objectStore(this.fileStore);
      const index = store.index('path');
      const request = index.get(normalized);

      request.onsuccess = () => {
        const node = request.result as FileSystemNode;
        if (!node) {
          reject(new Error(`ENOENT: no such file or directory, open '${path}'`));
          return;
        }

        if (node.type !== 'file') {
          reject(new Error(`EISDIR: illegal operation on a directory, read`));
          return;
        }

        resolve(node.content || new Uint8Array(0));
      };

      request.onerror = () => reject(request.error);
    });
  }

  async writeFile(path: string, content: Uint8Array): Promise<void> {
    this.ensureInitialized();
    const normalized = this.normalizePath(path);
    const parent = this.getParentPath(normalized);
    const name = this.getFileName(normalized);
    const now = Date.now();

    if (parent && !(await this.exists(parent))) {
      await this.mkdir(parent, { recursive: true });
    }

    return new Promise((resolve, reject) => {
      const transaction = this.db!.transaction([this.fileStore], 'readwrite');
      const store = transaction.objectStore(this.fileStore);
      const index = store.index('path');
      const getRequest = index.get(normalized);

      getRequest.onsuccess = () => {
        const existingNode = getRequest.result as FileSystemNode;

        const node: FileSystemNode = {
          path: normalized,
          name,
          type: 'file',
          content,
          mtime: now,
          ctime: existingNode?.ctime || now,
          size: content.length,
          parent: parent === null ? '/' : parent
        };

        if (existingNode?.id){
          node.id = existingNode.id; // Preserve ID if it exists
        }

        const putRequest = store.put(node);
        putRequest.onsuccess = () => resolve();
        putRequest.onerror = () => reject(putRequest.error);
      };

      getRequest.onerror = () => reject(getRequest.error);
    });
  }

  async readDir(path: string): Promise<DirectoryEntry[]> {
    this.ensureInitialized();
    const normalized = this.normalizePath(path);

    const dirExists = await this.exists(normalized);
    if (!dirExists) {
      throw new Error(`ENOENT: no such file or directory, scandir '${path}'`);
    }

    const stats = await this.stat(normalized);
    if (!stats.isDirectory) {
      throw new Error(`ENOTDIR: not a directory, scandir '${path}'`);
    }

    return new Promise((resolve, reject) => {
      const transaction = this.db!.transaction([this.fileStore], 'readonly');
      const store = transaction.objectStore(this.fileStore);
      const index = store.index('parent');
      const request = index.getAll(normalized);

      request.onsuccess = () => {
        const nodes = request.result as FileSystemNode[];
        const entries: DirectoryEntry[] = nodes.map(node => ({
          name: node.name,
          type: node.type,
          stats: {
            size: node.size,
            mtime: node.mtime,
            ctime: node.ctime,
            isFile: node.type === 'file',
            isDirectory: node.type === 'dir'
          }
        }));

        entries.sort((a, b) => {
          if (a.type === 'dir' && b.type !== 'dir') return -1;
          if (a.type !== 'dir' && b.type === 'dir') return 1;
          return a.name.localeCompare(b.name);
        });

        resolve(entries);
      };

      request.onerror = () => reject(request.error);
    });
  }

  async mkdir(path: string, options?: { recursive?: boolean }): Promise<void> {
    this.ensureInitialized();
    const normalized = this.normalizePath(path);

    // Skip if it's root directory - it should already exist
    if (normalized === '/') {
      return;
    }

    const parent = this.getParentPath(normalized);
    const name = this.getFileName(normalized);
    const now = Date.now();

    if (await this.exists(normalized)) {
      if (!options?.recursive) {
        throw new Error(`EEXIST: file already exists, mkdir '${path}'`);
      }
      return;
    }

    if (parent && parent !== '/' && !(await this.exists(parent))) {
      if (options?.recursive) {
        await this.mkdir(parent, options);
      } else {
        throw new Error(`ENOENT: no such file or directory, mkdir '${path}'`);
      }
    }

    return new Promise((resolve, reject) => {
      const transaction = this.db!.transaction([this.fileStore], 'readwrite');
      const store = transaction.objectStore(this.fileStore);

      const node: FileSystemNode = {
        path: normalized,
        name,
        type: 'dir',
        mtime: now,
        ctime: now,
        size: 0,
        parent: parent === null ? '/' : parent
      };

      const request = store.add(node);
      request.onsuccess = () => resolve();
      request.onerror = () => reject(request.error);
    });
  }

  async rmdir(path: string): Promise<void> {
    this.ensureInitialized();
    const normalized = this.normalizePath(path);

    const stats = await this.stat(normalized);
    if (!stats.isDirectory) {
      throw new Error(`ENOTDIR: not a directory, rmdir '${path}'`);
    }

    const entries = await this.readDir(normalized);
    if (entries.length > 0) {
      throw new Error(`ENOTEMPTY: directory not empty, rmdir '${path}'`);
    }

    return this.unlink(normalized);
  }

  async unlink(path: string): Promise<void> {
    this.ensureInitialized();
    const normalized = this.normalizePath(path);

    return new Promise((resolve, reject) => {
      const transaction = this.db!.transaction([this.fileStore], 'readwrite');
      const store = transaction.objectStore(this.fileStore);
      const index = store.index('path');
      const getRequest = index.get(normalized);

      getRequest.onsuccess = () => {
        const node = getRequest.result as FileSystemNode;
        if (!node) {
          reject(new Error(`ENOENT: no such file or directory, unlink '${path}'`));
          return;
        }

        const deleteRequest = store.delete(node.id!);
        deleteRequest.onsuccess = () => resolve();
        deleteRequest.onerror = () => reject(deleteRequest.error);
      };

      getRequest.onerror = () => reject(getRequest.error);
    });
  }

  async rename(oldPath: string, newPath: string): Promise<void> {
    this.ensureInitialized();
    const oldNormalized = this.normalizePath(oldPath);
    const newNormalized = this.normalizePath(newPath);

    if (await this.exists(newNormalized)) {
      throw new Error(`EEXIST: file already exists, rename '${oldPath}' -> '${newPath}'`);
    }

    const content = await this.readFile(oldNormalized);
    await this.writeFile(newNormalized, content);
    await this.unlink(oldNormalized);
  }

  async downloadAndCache(url: string, path: string, etag?: string, lastModified?: string): Promise<void> {
    this.ensureInitialized();

    const cacheKey = `${url}:${path}`;
    const cachedEntry = await this.getCacheEntry(cacheKey);

    const headers: HeadersInit = {};
    if (cachedEntry?.etag) {
      headers['If-None-Match'] = cachedEntry.etag;
    } else if (cachedEntry?.lastModified) {
      headers['If-Modified-Since'] = cachedEntry.lastModified;
    }

    try {
      const response = await fetch(url, { headers });

      if (response.status === 304 && cachedEntry) {
        await this.writeFile(path, cachedEntry.content);
        return;
      }

      if (!response.ok) {
        throw new Error(`HTTP ${response.status}: ${response.statusText}`);
      }

      const content = new Uint8Array(await response.arrayBuffer());
      const responseEtag = response.headers.get('etag');
      const responseLastModified = response.headers.get('last-modified');

      await this.writeFile(path, content);
      await this.setCacheEntry(cacheKey, {
        url,
        etag: responseEtag || etag,
        lastModified: responseLastModified || lastModified,
        mtime: Date.now(),
        content
      });
    } catch (error) {
      console.trace(error);
      debugger;
      if (cachedEntry) {
        await this.writeFile(path, cachedEntry.content);
      } else {
        throw error;
      }
    }
  }

  private async getCacheEntry(key: string): Promise<CacheEntry | null> {
    return new Promise((resolve, reject) => {
      const transaction = this.db!.transaction([this.cacheStore], 'readonly');
      const store = transaction.objectStore(this.cacheStore);
      const request = store.get(key);

      request.onsuccess = () => resolve(request.result || null);
      request.onerror = () => reject(request.error);
    });
  }

  private async setCacheEntry(key: string, entry: CacheEntry): Promise<void> {
    return new Promise((resolve, reject) => {
      const transaction = this.db!.transaction([this.cacheStore], 'readwrite');
      const store = transaction.objectStore(this.cacheStore);
      const request = store.put({ ...entry, url: key });

      request.onsuccess = () => resolve();
      request.onerror = () => reject(request.error);
    });
  }

  async clearCache(): Promise<void> {
    this.ensureInitialized();

    return new Promise((resolve, reject) => {
      const transaction = this.db!.transaction([this.cacheStore], 'readwrite');
      const store = transaction.objectStore(this.cacheStore);
      const request = store.clear();

      request.onsuccess = () => resolve();
      request.onerror = () => reject(request.error);
    });
  }

  async getStorageUsage(): Promise<{ files: number; cache: number; total: number }> {
    this.ensureInitialized();

    const getStoreSize = (storeName: string): Promise<number> => {
      return new Promise((resolve, reject) => {
        const transaction = this.db!.transaction([storeName], 'readonly');
        const store = transaction.objectStore(storeName);
        const request = store.getAll();

        request.onsuccess = () => {
          const data = request.result;
          const size = JSON.stringify(data).length;
          resolve(size);
        };

        request.onerror = () => reject(request.error);
      });
    };

    const [filesSize, cacheSize] = await Promise.all([
      getStoreSize(this.fileStore),
      getStoreSize(this.cacheStore)
    ]);

    return {
      files: filesSize,
      cache: cacheSize,
      total: filesSize + cacheSize
    };
  }

  async printTree(path: string = '/', prefix: string = '', isLast: boolean = true): Promise<void> {
    this.ensureInitialized();

    try {
      const stats = await this.stat(path);
      const name = path === '/' ? '/' : this.getFileName(path);
      const connector = isLast ? '└── ' : '├── ';
      const typeIcon = stats.isDirectory ? '📁' : '📄';
      const sizeInfo = stats.isFile ? ` (${stats.size} bytes)` : '';

      console.log(`${prefix}${connector}${typeIcon} ${name}${sizeInfo}`);

      if (stats.isDirectory) {
        const entries = await this.readDir(path);
        const newPrefix = prefix + (isLast ? '    ' : '│   ');

        for (let i = 0; i < entries.length; i++) {
          const entry = entries[i];
          const isLastEntry = i === entries.length - 1;
          const entryPath = path === '/' ? `/${entry.name}` : `${path}/${entry.name}`;
          await this.printTree(entryPath, newPrefix, isLastEntry);
        }
      }
    } catch (error) {
      // Silently skip files that don't exist - this is normal during tree traversal
      // as the filesystem may contain stale references
      if (!(error instanceof Error && error.message.includes('ENOENT'))) {
        console.error(`Error reading ${path}:`, error);
      }
    }
  }

  async printFullTree(): Promise<void> {
    console.log('📊 IndexedDB Filesystem Tree:');
    await this.printTree('/');

    const usage = await this.getStorageUsage();
    console.log(`\n💾 Storage Usage: ${usage.total} bytes total (${usage.files} files + ${usage.cache} cache)`);
  }

  async listAllFiles(): Promise<FileSystemNode[]> {
    this.ensureInitialized();

    return new Promise((resolve, reject) => {
      const transaction = this.db!.transaction([this.fileStore], 'readonly');
      const store = transaction.objectStore(this.fileStore);
      const request = store.getAll();

      request.onsuccess = () => {
        const nodes = request.result as FileSystemNode[];
        resolve(nodes.sort((a, b) => a.path.localeCompare(b.path)));
      };

      request.onerror = () => reject(request.error);
    });
  }

  async debugPrintAllFiles(): Promise<void> {
    console.log('🗂️  All files in IndexedDB:');
    const files = await this.listAllFiles();

    files.forEach(file => {
      const typeIcon = file.type === 'dir' ? '📁' : '📄';
      const parentInfo = file.parent ? ` (parent: ${file.parent})` : '';
      const sizeInfo = file.type === 'file' ? ` [${file.size} bytes]` : '';
      console.log(`${typeIcon} ${file.path}${sizeInfo}${parentInfo}`);
    });

    console.log(`\nTotal: ${files.length} entries`);
  }
}

export { IndexedDBFileSystem, type FileStats, type DirectoryEntry };
