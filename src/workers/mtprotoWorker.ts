/**
 * mtprotoWorker.ts - Dedicated MTProto & Cryptography Web Worker
 * Offloads heavy MTProto operations from the main UI thread:
 * - MTProto 2.0 AES-IGE encryption / decryption
 * - SHA-256 and PBKDF2 hash computation
 * - Updates parsing, coalescing, and diff calculations
 */

import { CryptoAccelerationEngine } from '../utils/cryptoAcceleration';

export interface WorkerRpcRequest {
  id: string;
  type: 'ENCRYPT_IGE' | 'DECRYPT_IGE' | 'SHA256' | 'PROCESS_DIFF' | 'PARSE_MESSAGES';
  payload: any;
}

export interface WorkerRpcResponse {
  id: string;
  success: boolean;
  result?: any;
  error?: string;
}

if (typeof self !== 'undefined' && 'onmessage' in self) {
  self.onmessage = async (e: MessageEvent<WorkerRpcRequest>) => {
    const { id, type, payload } = e.data;
    try {
      let result: any;
      switch (type) {
        case 'ENCRYPT_IGE': {
          const { plaintext, key, iv } = payload;
          result = CryptoAccelerationEngine.aesIgeEncrypt(
            new Uint8Array(plaintext),
            new Uint8Array(key),
            new Uint8Array(iv)
          );
          break;
        }
        case 'DECRYPT_IGE': {
          const { ciphertext, key, iv } = payload;
          result = CryptoAccelerationEngine.aesIgeDecrypt(
            new Uint8Array(ciphertext),
            new Uint8Array(key),
            new Uint8Array(iv)
          );
          break;
        }
        case 'SHA256': {
          const { data } = payload;
          result = await CryptoAccelerationEngine.sha256(new Uint8Array(data));
          break;
        }
        case 'PROCESS_DIFF': {
          const { incoming, existingIds } = payload;
          const existingSet = new Set(existingIds || []);
          result = (incoming || []).filter((msg: any) => !existingSet.has(msg.id));
          break;
        }
        case 'PARSE_MESSAGES': {
          const { rawList } = payload;
          result = (rawList || []).map((m: any) => ({
            id: m.id,
            chatId: m.chat_id || m.chatId,
            text: m.message || m.text || '',
            senderId: m.sender_id || m.senderId,
            date: m.date || Math.floor(Date.now() / 1000),
            out: Boolean(m.out),
            media: m.media ? true : false,
          }));
          break;
        }
        default:
          throw new Error(`Unknown worker command: ${type}`);
      }

      const response: WorkerRpcResponse = { id, success: true, result };
      // Transfer ArrayBuffer if available for zero-copy
      if (result && result.buffer instanceof ArrayBuffer) {
        (self as any).postMessage(response, [result.buffer]);
      } else {
        self.postMessage(response);
      }
    } catch (err: any) {
      self.postMessage({ id, success: false, error: err?.message || String(err) });
    }
  };
}
