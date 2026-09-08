/**
 * TelegramWorkerService.ts - Worker Service & Thread Offload Manager
 * Coordinates background worker execution with fallback to main thread
 */

import { CryptoAccelerationEngine } from '../utils/cryptoAcceleration';

export class TelegramWorkerService {
  private static instance: TelegramWorkerService;
  private worker: Worker | null = null;
  private pendingRequests: Map<string, { resolve: (res: any) => void; reject: (err: any) => void }> = new Map();
  private isAvailable = false;

  private constructor() {
    this.initWorker();
  }

  public static getInstance(): TelegramWorkerService {
    if (!TelegramWorkerService.instance) {
      TelegramWorkerService.instance = new TelegramWorkerService();
    }
    return TelegramWorkerService.instance;
  }

  private initWorker() {
    if (typeof window === 'undefined' || typeof Worker === 'undefined') {
      return;
    }
    try {
      this.worker = new Worker(
        new URL('../workers/mtprotoWorker.ts', import.meta.url),
        { type: 'module' }
      );
      this.worker.onmessage = (e: MessageEvent) => {
        const { id, success, result, error } = e.data;
        const pending = this.pendingRequests.get(id);
        if (pending) {
          this.pendingRequests.delete(id);
          if (success) {
            pending.resolve(result);
          } else {
            pending.reject(new Error(error));
          }
        }
      };
      this.worker.onerror = (err) => {
        console.warn('[TelegramWorkerService] Worker error, falling back to main thread:', err);
        this.isAvailable = false;
      };
      this.isAvailable = true;
    } catch (err) {
      console.warn('[TelegramWorkerService] Worker initialization skipped, using main thread:', err);
      this.isAvailable = false;
    }
  }

  public async encryptIge(plaintext: Uint8Array, key: Uint8Array, iv: Uint8Array): Promise<Uint8Array> {
    if (this.isAvailable && this.worker) {
      return this.sendRpc('ENCRYPT_IGE', { plaintext, key, iv });
    }
    return CryptoAccelerationEngine.aesIgeEncrypt(plaintext, key, iv);
  }

  public async decryptIge(ciphertext: Uint8Array, key: Uint8Array, iv: Uint8Array): Promise<Uint8Array> {
    if (this.isAvailable && this.worker) {
      return this.sendRpc('DECRYPT_IGE', { ciphertext, key, iv });
    }
    return CryptoAccelerationEngine.aesIgeDecrypt(ciphertext, key, iv);
  }

  public async calculateSha256(data: Uint8Array): Promise<Uint8Array> {
    if (this.isAvailable && this.worker) {
      return this.sendRpc('SHA256', { data });
    }
    return CryptoAccelerationEngine.sha256(data);
  }

  public async processDiff(incoming: any[], existingIds: string[]): Promise<any[]> {
    if (this.isAvailable && this.worker) {
      return this.sendRpc('PROCESS_DIFF', { incoming, existingIds });
    }
    const existingSet = new Set(existingIds || []);
    return (incoming || []).filter(msg => !existingSet.has(msg.id));
  }

  public async parseMessages(rawList: any[]): Promise<any[]> {
    if (this.isAvailable && this.worker) {
      return this.sendRpc('PARSE_MESSAGES', { rawList });
    }
    return (rawList || []).map((m: any) => ({
      id: m.id,
      chatId: m.chat_id || m.chatId,
      text: m.message || m.text || '',
      senderId: m.sender_id || m.senderId,
      date: m.date || Math.floor(Date.now() / 1000),
      out: Boolean(m.out),
      media: m.media ? true : false,
    }));
  }

  public async batchCoalesce(updates: any[]): Promise<any[]> {
    if (this.isAvailable && this.worker) {
      return this.sendRpc('BATCH_COALESCE', { updates });
    }
    const seenMessages = new Map<string, any>();
    const otherUpdates: any[] = [];
    for (const update of (updates || [])) {
      if (update.type === 'message' && update.data?.id) {
        seenMessages.set(`${update.chatId}_${update.data.id}`, update);
      } else {
        otherUpdates.push(update);
      }
    }
    return [...Array.from(seenMessages.values()), ...otherUpdates];
  }

  public async parseEntities(text: string): Promise<any[]> {
    if (this.isAvailable && this.worker) {
      return this.sendRpc('PARSE_ENTITIES', { text });
    }
    const entities: any[] = [];
    const urlRegex = /(https?:\/\/[^\s]+)/g;
    let match;
    while ((match = urlRegex.exec(text || '')) !== null) {
      entities.push({
        type: 'url',
        offset: match.index,
        length: match[0].length,
        url: match[0],
      });
    }
    return entities;
  }

  private sendRpc(type: any, payload: any): Promise<any> {
    const id = `rpc_${Date.now()}_${Math.random().toString(36).substring(2, 9)}`;
    return new Promise((resolve, reject) => {
      this.pendingRequests.set(id, { resolve, reject });
      try {
        this.worker!.postMessage({ id, type, payload });
      } catch (err) {
        this.pendingRequests.delete(id);
        reject(err);
      }
    });
  }

  public isWorkerActive(): boolean {
    return this.isAvailable;
  }
}

export const telegramWorker = TelegramWorkerService.getInstance();
