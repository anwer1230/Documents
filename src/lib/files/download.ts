/**
 * Telegram Web K File Downloader (download.ts)
 * Based on morethanwords/tweb src/lib/files/download.ts
 */

import { idbFileStorage } from './idbFileStorage';
import { fileStorage } from './fileStorage';

export interface FileDownloadOptions {
  dcId?: number;
  mimeType?: string;
  onProgress?: (progress: number) => void;
}

export class FileDownloader {
  public async download(fileId: string, url: string, options: FileDownloadOptions = {}): Promise<Blob> {
    // 1. Check local IDB storage first
    const cached = await idbFileStorage.getFile(fileId);
    if (cached) {
      return cached instanceof Blob ? cached : new Blob([cached], { type: options.mimeType || 'application/octet-stream' });
    }

    // 2. Stream from network
    const response = await fetch(url);
    if (!response.ok) {
      throw new Error(`Failed to download file: ${response.statusText}`);
    }

    const blob = await response.blob();

    // 3. Cache locally
    await idbFileStorage.saveFile(fileId, options.mimeType || blob.type, blob);
    await fileStorage.getFileUrl(fileId, options.mimeType || blob.type);

    return blob;
  }
}

export const fileDownloader = new FileDownloader();
export default fileDownloader;
