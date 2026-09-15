/**
 * Telegram Web K Download Manager (appDownloadManager.ts)
 * Based on morethanwords/tweb src/lib/appManagers/appDownloadManager.ts
 */

import { EventEmitter } from './utils';

export interface DownloadProgress {
  fileId: string;
  loaded: number;
  total: number;
  percentage: number;
}

export class AppDownloadManager extends EventEmitter {
  private activeDownloads: Map<string, AbortController> = new Map();

  public async downloadFile(
    fileUrl: string,
    fileId: string,
    fileName: string,
    onProgress?: (progress: DownloadProgress) => void
  ): Promise<Blob> {
    if (this.activeDownloads.has(fileId)) {
      this.activeDownloads.get(fileId)!.abort();
    }

    const controller = new AbortController();
    this.activeDownloads.set(fileId, controller);

    try {
      const response = await fetch(fileUrl, { signal: controller.signal });
      if (!response.ok) throw new Error(`Download failed: ${response.statusText}`);

      const contentLength = response.headers.get('content-length');
      const total = contentLength ? parseInt(contentLength, 10) : 0;
      let loaded = 0;

      if (!response.body) {
        const blob = await response.blob();
        return blob;
      }

      const reader = response.body.getReader();
      const chunks: Uint8Array[] = [];

      while (true) {
        const { done, value } = await reader.read();
        if (done) break;

        chunks.push(value);
        loaded += value.length;

        if (total > 0) {
          const progress: DownloadProgress = {
            fileId,
            loaded,
            total,
            percentage: Math.round((loaded / total) * 100),
          };
          onProgress?.(progress);
          this.emit('progress', progress);
        }
      }

      const blob = new Blob(chunks);
      this.triggerBrowserDownload(blob, fileName);
      return blob;
    } finally {
      this.activeDownloads.delete(fileId);
    }
  }

  public cancelDownload(fileId: string): void {
    if (this.activeDownloads.has(fileId)) {
      this.activeDownloads.get(fileId)!.abort();
      this.activeDownloads.delete(fileId);
      this.emit('cancelled', fileId);
    }
  }

  private triggerBrowserDownload(blob: Blob, fileName: string): void {
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = fileName;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    setTimeout(() => URL.revokeObjectURL(url), 1000);
  }
}

export const appDownloadManager = new AppDownloadManager();
export default appDownloadManager;
