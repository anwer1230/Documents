/**
 * Telegram Official FileLoader Engine
 * Replicates org.telegram.messenger.FileLoader.java
 * Handles media caching, path formatting, and file streaming requests
 */

export class FileLoader {
  private static instance: FileLoader;
  private fileCache: Map<string, string> = new Map();

  private constructor() {}

  public static getInstance(): FileLoader {
    if (!FileLoader.instance) {
      FileLoader.instance = new FileLoader();
    }
    return FileLoader.instance;
  }

  public static getPathToAttach(location: any, ext?: string): string {
    if (!location) return '';
    if (typeof location === 'string') {
      if (location.startsWith('http') || location.startsWith('blob:') || location.startsWith('data:')) {
        return location;
      }
      return location;
    }

    if (location.local_path) return location.local_path;
    if (location.url) return location.url;

    const id = location.id || location.file_id || 'file';
    const extension = ext || location.ext || 'jpg';
    return `/api/telegram/files/${id}.${extension}`;
  }

  public static checkFileName(name: string): string {
    if (!name) return 'document';
    return name.replace(/[\\/:*?"<>|]/g, '_');
  }

  public static getAttachFileName(location: any, ext?: string): string {
    if (!location) return '';
    if (typeof location === 'string') {
      const parts = location.split('/');
      return parts[parts.length - 1] || 'file';
    }
    const id = location.id || location.file_id || 'file';
    const extension = ext || location.ext || 'dat';
    return `${id}.${extension}`;
  }

  public isFileLoaded(location: any): boolean {
    const key = typeof location === 'string' ? location : (location.id || location.url);
    return this.fileCache.has(key);
  }

  public cacheFile(location: any, localUrl: string): void {
    const key = typeof location === 'string' ? location : (location.id || location.url);
    this.fileCache.set(key, localUrl);
  }
}
