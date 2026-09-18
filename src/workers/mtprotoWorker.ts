/**
 * mtprotoWorker.ts - Dedicated MTProto & Cryptography Web Worker
 * 
 * Offloads compute-heavy MTProto operations from the main UI thread:
 * - Hardware/WebCrypto AES-IGE encryption and decryption
 * - SHA-256 and PBKDF2 hash computation for 2FA SRP
 * - Direct Telegram binary frame packing and unpacking
 */

import { CryptoAccelerationEngine } from '../utils/cryptoAcceleration';

export interface WorkerRpcRequest {
  id: string;
  type: 'ENCRYPT_IGE' | 'DECRYPT_IGE' | 'SHA256' | 'PBKDF2' | 'COMPUTE_MSG_KEY';
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

        case 'PBKDF2': {
          const { passwordBytes, salt, iterations, keyLen } = payload;
          result = await CryptoAccelerationEngine.pbkdf2(
            new Uint8Array(passwordBytes),
            new Uint8Array(salt),
            iterations,
            keyLen
          );
          break;
        }

        case 'COMPUTE_MSG_KEY': {
          const { authKey, msgKey, isClientToServer } = payload;
          result = await CryptoAccelerationEngine.computeMsgKeyAndIV(
            new Uint8Array(authKey),
            new Uint8Array(msgKey),
            isClientToServer
          );
          break;
        }

        default:
          throw new Error(`Unknown worker task type: ${type}`);
      }

      const response: WorkerRpcResponse = {
        id,
        success: true,
        result: result instanceof Uint8Array ? Array.from(result) : result,
      };
      self.postMessage(response);
    } catch (err: any) {
      const response: WorkerRpcResponse = {
        id,
        success: false,
        error: err?.message || String(err),
      };
      self.postMessage(response);
    }
  };
}
