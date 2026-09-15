/**
 * Telegram Web K Files Controller (controller.ts)
 * Based on morethanwords/tweb src/lib/files/controller.ts
 */

import { fileDownloader, FileDownloadOptions } from './download';
import { fileUploader, FileUploadOptions } from './upload';
import { fileStorage } from './fileStorage';

export class FilesController {
  private activeTasks: Map<string, AbortController> = new Map();

  public async getFile(fileId: string, url?: string, options: FileDownloadOptions = {}): Promise<Blob | null> {
    if (url) {
      return await fileDownloader.download(fileId, url, options);
    }
    const cachedUrl = await fileStorage.getFileUrl(fileId, options.mimeType);
    if (!cachedUrl) return null;
    const res = await fetch(cachedUrl);
    return await res.blob();
  }

  public async uploadFile(file: File | Blob, options: FileUploadOptions = {}): Promise<{ fileId: string; url: string }> {
    const taskId = `task_${Date.now()}_${Math.random().toString(36).substring(2, 6)}`;
    const controller = new AbortController();
    this.activeTasks.set(taskId, controller);

    try {
      return await fileUploader.upload(file, { ...options, signal: controller.signal });
    } finally {
      this.activeTasks.delete(taskId);
    }
  }

  public cancelTask(taskId: string): void {
    const controller = this.activeTasks.get(taskId);
    if (controller) {
      controller.abort();
      this.activeTasks.delete(taskId);
    }
  }
}

export const filesController = new FilesController();
export default filesController;
