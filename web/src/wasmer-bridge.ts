import { Directory } from "@wasmer/sdk";
import { IndexedDBFileSystem, DirectoryEntry } from './fs';

export class WasmerBridge {
  public idbFs: IndexedDBFileSystem;
  public wasmerFs: Directory; // Make this public so compiler.ts can access it

  constructor(idbFs: IndexedDBFileSystem, wasmerFs: Directory) {
    this.idbFs = idbFs;
    this.wasmerFs = wasmerFs;
  }

  async syncToWasmer(path: string = '/'): Promise<void> {
    await this.syncDirectoryToWasmer(path);
  }

  async syncFromWasmer(path: string = '/'): Promise<void> {
    await this.syncDirectoryFromWasmer(path);
  }

  private async syncDirectoryToWasmer(path: string): Promise<void> {
    try {
      const entries = await this.idbFs.readDir(path);

      for (const entry of entries) {
        const fullPath = path === '/' ? `/${entry.name}` : `${path}/${entry.name}`;

        if (entry.type === 'dir') {
          try {
            await this.wasmerFs.createDir(fullPath);
          } catch {
            // Directory might already exist
          }
          await this.syncDirectoryToWasmer(fullPath);
        } else {
          const content = await this.idbFs.readFile(fullPath);
          await this.wasmerFs.writeFile(fullPath, content);
        }
      }
    } catch (error) {
      if (path === '/') {
        // Root directory should exist, create it if needed
        try {
          await this.idbFs.mkdir('/', { recursive: true });
        } catch {
          // Ignore if already exists
        }
      } else {
        console.warn(`Failed to sync directory ${path} to Wasmer:`, error);
      }
    }
  }

  private async syncDirectoryFromWasmer(path: string): Promise<void> {
    try {
      const entries = await this.wasmerFs.readDir(path);

      for (const entry of entries) {
        const fullPath = path === '/' ? `/${entry.name}` : `${path}/${entry.name}`;

        if (entry.type === 'dir') {
          await this.idbFs.mkdir(fullPath, { recursive: true });
          await this.syncDirectoryFromWasmer(fullPath);
        } else {
          const content = await this.wasmerFs.readFile(fullPath);
          await this.idbFs.writeFile(fullPath, content);
        }
      }
    } catch (error) {
      console.warn(`Failed to sync directory ${path} from Wasmer:`, error);
    }
  }

  async syncFileToWasmer(path: string): Promise<void> {
    let content: Uint8Array;
    try {
      content = await this.idbFs.readFile(path);
    } catch {
      console.warn(`File ${path} does not exist in IndexedDB, skipping sync to Wasmer.`);
      return;
    }
    try {
      await this.ensureParentDirectoriesInWasmer(path);
      await this.wasmerFs.writeFile(path, content);
    } catch (error) {
      console.warn(`Failed to sync file ${path} to Wasmer:`, error);
    }
  }

  async syncFileFromWasmer(path: string): Promise<void> {
    try {
      const content = await this.wasmerFs.readFile(path);
      await this.idbFs.writeFile(path, content);
    } catch (error) {
      console.warn(`Failed to sync file ${path} from Wasmer:`, error);
    }
  }

  private async ensureParentDirectoriesInWasmer(filePath: string): Promise<void> {
    const parts = filePath.split('/').filter(part => part !== '');
    parts.pop(); // Remove filename

    let currentPath = '';
    for (const part of parts) {
      currentPath += '/' + part;
      try {
        await this.wasmerFs.createDir(currentPath);
      } catch {
        // Directory might already exist
      }
    }
  }

  async readFile(path: string): Promise<Uint8Array> {
    return await this.idbFs.readFile(path);
  }

  async writeFile(path: string, content: Uint8Array): Promise<void> {
    await this.idbFs.writeFile(path, content);
    await this.syncFileToWasmer(path);
  }

  async readDir(path: string): Promise<DirectoryEntry[]> {
    return await this.idbFs.readDir(path);
  }

  async mkdir(path: string, options?: { recursive?: boolean }): Promise<void> {
    await this.idbFs.mkdir(path, options);

    if (options?.recursive) {
      const parts = path.split('/').filter(part => part !== '');
      let currentPath = '';
      for (const part of parts) {
        currentPath += '/' + part;
        try {
          await this.wasmerFs.createDir(currentPath);
        } catch {
          // Directory might already exist
        }
      }
    } else {
      try {
        await this.wasmerFs.createDir(path);
      } catch {
        // Directory might already exist
      }
    }
  }

  async rmdir(path: string): Promise<void> {
    await this.idbFs.rmdir(path);
    try {
      await this.wasmerFs.removeDir(path);
    } catch {
      // Directory might not exist in Wasmer
    }
  }

  async unlink(path: string): Promise<void> {
    await this.idbFs.unlink(path);
    try {
      await this.wasmerFs.removeFile(path);
    } catch {
      // File might not exist in Wasmer
    }
  }

  async removeFile(path: string): Promise<void> {
    return this.unlink(path);
  }

  async exists(path: string): Promise<boolean> {
    return await this.idbFs.exists(path);
  }

  async stat(path: string) {
    return await this.idbFs.stat(path);
  }

  async downloadAndCache(url: string, path: string, etag?: string, lastModified?: string): Promise<void> {
    await this.idbFs.downloadAndCache(url, path, etag, lastModified);
    await this.syncFileToWasmer(path);
  }

  async clearCache(): Promise<void> {
    return await this.idbFs.clearCache();
  }

  async getStorageUsage() {
    return await this.idbFs.getStorageUsage();
  }

  async printTree(path: string = '/'): Promise<void> {
    return await this.idbFs.printTree(path);
  }

  async printFullTree(): Promise<void> {
    return await this.idbFs.printFullTree();
  }

  async debugPrintAllFiles(): Promise<void> {
    return await this.idbFs.debugPrintAllFiles();
  }
}
