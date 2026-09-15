/**
 * Telegram Web K File Uploader (upload.ts)
 * Based on morethanwords/tweb src/lib/files/upload.ts
 */

import { csrfFetch } from '../../services/csrfFetch';

export interface FileUploadOptions {
  fileName?: string;
  onProgress?: (percentage: number) => void;
  signal?: AbortSignal;
}

export class FileUploader {
  public async upload(file: File | Blob, options: FileUploadOptions = {}): Promise<{ fileId: string; url: string }> {
    const formData = new FormData();
    formData.append('file', file, options.fileName || (file instanceof File ? file.name : 'upload.bin'));

    const res = await csrfFetch('/api/telegram/upload', {
      method: 'POST',
      body: formData,
      signal: options.signal,
    });

    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err.error || `Upload failed: ${res.statusText}`);
    }

    options.onProgress?.(100);
    return await res.json();
  }
}

export const fileUploader = new FileUploader();
export default fileUploader;
