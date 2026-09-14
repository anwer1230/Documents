/**
 * Telegram Web K IndexedDB File Storage (idbFileStorage.ts)
 * Based on morethanwords/tweb src/lib/files/idbFileStorage.ts
 */

import { idb } from '../storages/idb';

export class IDBFileStorage {
  public async saveFile(fileId: string, type: string, blob: Blob | ArrayBuffer): Promise<void> {
    try {
      await idb.put('files', {
        id: fileId,
        type,
        blob,
        updatedAt: Date.now(),
      });
    } catch (err) {
      console.warn('[IDBFileStorage] Failed to cache file to IDB:', err);
    }
  }

  public async getFile(fileId: string): Promise<Blob | ArrayBuffer | null> {
    try {
      const record = await idb.get('files', fileId);
      return record ? record.blob : null;
    } catch {
      return null;
    }
  }

  public async deleteFile(fileId: string): Promise<void> {
    try {
      await idb.delete('files', fileId);
    } catch {}
  }

  public async clear(): Promise<void> {
    try {
      await idb.clear('files');
    } catch {}
  }
}

export const idbFileStorage = new IDBFileStorage();
export default idbFileStorage;
