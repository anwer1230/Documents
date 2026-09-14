/**
 * Telegram Web K File Storage Layer (fileStorage.ts)
 * Based on morethanwords/tweb src/lib/files/fileStorage.ts
 */

import { idbFileStorage } from './idbFileStorage';

export class FileStorage {
  private memoryUrls: Map<string, string> = new Map();

  public async getFileUrl(fileId: string, mimeType = 'image/jpeg'): Promise<string | null> {
    if (this.memoryUrls.has(fileId)) {
      return this.memoryUrls.get(fileId)!;
    }

    const data = await idbFileStorage.getFile(fileId);
    if (!data) return null;

    const blob = data instanceof Blob ? data : new Blob([data], { type: mimeType });
    const url = URL.createObjectURL(blob);
    this.memoryUrls.set(fileId, url);
    return url;
  }

  public async saveFile(fileId: string, mimeType: string, data: Blob | ArrayBuffer): Promise<string> {
    await idbFileStorage.saveFile(fileId, mimeType, data);
    const blob = data instanceof Blob ? data : new Blob([data], { type: mimeType });
    const url = URL.createObjectURL(blob);
    this.memoryUrls.set(fileId, url);
    return url;
  }

  public revokeFileUrl(fileId: string): void {
    const url = this.memoryUrls.get(fileId);
    if (url) {
      URL.revokeObjectURL(url);
      this.memoryUrls.delete(fileId);
    }
  }

  public clear(): void {
    this.memoryUrls.forEach((url) => URL.revokeObjectURL(url));
    this.memoryUrls.clear();
  }
}

export const fileStorage = new FileStorage();
export default fileStorage;
