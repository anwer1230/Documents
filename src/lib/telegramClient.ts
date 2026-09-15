/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

/**
 * telegramClient.ts - Worker-Offloaded GramJS MTProto Client
 * 
 * Re-architected to run GramJS inside a dedicated Web Worker (src/workers/gramjsWorker.ts).
 * Offloads all MTProto 2.0 cryptographic calculations (AES-IGE, SHA-256, DH exchange),
 * binary WebSocket framing, and TL packet serialization from the main UI thread.
 * 
 * In environments without Web Worker support (e.g. server-side rendering or sandboxed iframes),
 * seamlessly falls back to an in-process TelegramClient instance.
 */

import { TelegramClient, Api } from 'telegram';
import { StringSession } from 'telegram/sessions';

// Default Telegram API credentials from official application configuration
export const TELEGRAM_CONFIG = {
  apiId: Number(
    (typeof import.meta !== 'undefined' && ((import.meta as any).env?.VITE_TELEGRAM_API_ID || (import.meta as any).env?.VITE_API_ID)) ||
    (typeof process !== 'undefined' && (process.env?.TELEGRAM_API_ID || process.env?.API_ID)) ||
    22043994
  ),
  apiHash:
    (typeof import.meta !== 'undefined' && ((import.meta as any).env?.VITE_TELEGRAM_API_HASH || (import.meta as any).env?.VITE_API_HASH)) ||
    (typeof process !== 'undefined' && (process.env?.TELEGRAM_API_HASH || process.env?.API_HASH)) ||
    '56f64582b363d367280db96586b97801',
  sessionStorageKey: 'tg_mtproto_session_string',
};

/**
 * Retrieves the saved session string from localStorage if running in browser
 */
export function getSavedSessionString(): string {
  if (typeof window !== 'undefined' && window.localStorage) {
    try {
      return localStorage.getItem(TELEGRAM_CONFIG.sessionStorageKey) || '';
    } catch {
      return '';
    }
  }
  return '';
}

/**
 * Saves session string to localStorage
 */
export function saveSessionString(sessionString: string): void {
  if (typeof window !== 'undefined' && window.localStorage) {
    try {
      if (sessionString) {
        localStorage.setItem(TELEGRAM_CONFIG.sessionStorageKey, sessionString);
      } else {
        localStorage.removeItem(TELEGRAM_CONFIG.sessionStorageKey);
      }
    } catch (err) {
      console.warn('[telegramClient] Failed to persist session to localStorage:', err);
    }
  }
}

/**
 * GramJsWorkerClient - Transparent Proxy to the Dedicated GramJS Web Worker
 */
export class GramJsWorkerClient {
  private worker: Worker | null = null;
  private pendingRequests = new Map<string, {
    resolve: (res: any) => void;
    reject: (err: any) => void;
    timer: any;
  }>();
  private eventHandlers = new Set<(update: any) => void>();
  private _connected = false;
  private _sessionString = '';
  private _isWorkerSupported = false;
  private fallbackClient: TelegramClient | null = null;
  public apiId: number;
  public apiHash: string;

  public constructor(sessionString?: string, apiId?: number, apiHash?: string) {
    this.apiId = apiId ?? TELEGRAM_CONFIG.apiId;
    this.apiHash = apiHash ?? TELEGRAM_CONFIG.apiHash;
    this._sessionString = sessionString !== undefined ? sessionString : getSavedSessionString();

    this.initWorker();
  }

  private initWorker(): void {
    if (typeof window === 'undefined' || typeof Worker === 'undefined') {
      this.initFallbackClient();
      return;
    }

    try {
      this.worker = new Worker(
        new URL('../workers/gramjsWorker.ts', import.meta.url),
        { type: 'module' }
      );

      this.worker.onmessage = (e: MessageEvent) => {
        const msg = e.data;
        if (!msg) return;

        // Handle RPC response
        if (msg.id && this.pendingRequests.has(msg.id)) {
          const req = this.pendingRequests.get(msg.id)!;
          this.pendingRequests.delete(msg.id);
          clearTimeout(req.timer);

          if (msg.success) {
            req.resolve(msg.result);
          } else {
            req.reject(new Error(msg.error || 'GramJS Worker RPC failed'));
          }
          return;
        }

        // Handle Worker Push Events
        if (msg.type === 'UPDATE') {
          for (const handler of this.eventHandlers) {
            try {
              handler(msg.data);
            } catch (err) {
              console.error('[GramJsWorkerClient] Update handler error:', err);
            }
          }
          if (typeof window !== 'undefined') {
            window.dispatchEvent(new CustomEvent('telegram:worker_update', { detail: msg.data }));
          }
        } else if (msg.type === 'CONNECTION_STATUS') {
          this._connected = Boolean(msg.data?.connected);
        } else if (msg.type === 'SESSION_SAVED') {
          if (msg.data?.sessionString) {
            this._sessionString = msg.data.sessionString;
            saveSessionString(this._sessionString);
          }
        }
      };

      this.worker.onerror = (err) => {
        console.warn('[GramJsWorkerClient] Web Worker error, switching to in-process fallback:', err);
        this._isWorkerSupported = false;
        this.initFallbackClient();
      };

      this._isWorkerSupported = true;

      // Initialize the worker client instance
      this.sendRpc('INIT', {
        sessionString: this._sessionString,
        apiId: this.apiId,
        apiHash: this.apiHash,
      }).catch((e) => {
        console.warn('[GramJsWorkerClient] Worker INIT error:', e);
      });

      console.log('[telegramClient] GramJS MTProto client successfully offloaded to dedicated Web Worker.');
    } catch (err) {
      console.warn('[GramJsWorkerClient] Failed to instantiate worker, falling back to main-thread client:', err);
      this._isWorkerSupported = false;
      this.initFallbackClient();
    }
  }

  private initFallbackClient(): void {
    if (!this.fallbackClient) {
      const session = new StringSession(this._sessionString);
      this.fallbackClient = new TelegramClient(session, this.apiId, this.apiHash, {
        connectionRetries: 5,
        useWSS: true,
        autoReconnect: true,
        floodSleepThreshold: 60,
      });
      console.log('[telegramClient] Running GramJS on main-thread fallback.');
    }
  }

  private sendRpc<T = any>(type: string, payload?: any, timeoutMs = 60000): Promise<T> {
    if (!this.worker || !this._isWorkerSupported) {
      return Promise.reject(new Error('Web Worker not available'));
    }

    const id = `rpc_${Date.now()}_${Math.random().toString(36).substring(2, 9)}`;

    return new Promise<T>((resolve, reject) => {
      const timer = setTimeout(() => {
        if (this.pendingRequests.has(id)) {
          this.pendingRequests.delete(id);
          reject(new Error(`GramJS Worker RPC timeout [${type}]`));
        }
      }, timeoutMs);

      this.pendingRequests.set(id, { resolve, reject, timer });

      try {
        this.worker!.postMessage({ id, type, payload });
      } catch (err) {
        this.pendingRequests.delete(id);
        clearTimeout(timer);
        reject(err);
      }
    });
  }

  public get connected(): boolean {
    if (this._isWorkerSupported) {
      return this._connected;
    }
    return Boolean(this.fallbackClient && this.fallbackClient.connected);
  }

  public get session(): { save: () => string } {
    return {
      save: () => {
        if (this._isWorkerSupported) {
          return this._sessionString;
        }
        return (this.fallbackClient?.session as any)?.save?.() || this._sessionString;
      },
    };
  }

  public async connect(): Promise<void> {
    if (this._isWorkerSupported && this.worker) {
      const res = await this.sendRpc<{ connected: boolean; sessionString: string }>('CONNECT');
      this._connected = res.connected;
      if (res.sessionString) {
        this._sessionString = res.sessionString;
        saveSessionString(this._sessionString);
      }
      return;
    }

    this.initFallbackClient();
    if (this.fallbackClient && !this.fallbackClient.connected) {
      await this.fallbackClient.connect();
      const saved = (this.fallbackClient.session as any)?.save?.();
      if (saved) {
        this._sessionString = saved;
        saveSessionString(saved);
      }
    }
  }

  public async disconnect(): Promise<void> {
    if (this._isWorkerSupported && this.worker) {
      await this.sendRpc('DISCONNECT');
      this._connected = false;
      return;
    }
    if (this.fallbackClient && this.fallbackClient.connected) {
      await this.fallbackClient.disconnect();
    }
  }

  public async checkAuthorization(): Promise<boolean> {
    if (this._isWorkerSupported && this.worker) {
      const res = await this.sendRpc<{ authorized: boolean }>('CHECK_AUTH');
      return res.authorized;
    }
    this.initFallbackClient();
    return this.fallbackClient ? this.fallbackClient.checkAuthorization().catch(() => false) : false;
  }

  public async getMe(): Promise<any> {
    if (this._isWorkerSupported && this.worker) {
      return this.sendRpc('GET_ME');
    }
    this.initFallbackClient();
    return this.fallbackClient?.getMe();
  }

  public async invoke<T = any>(query: any): Promise<T> {
    if (this._isWorkerSupported && this.worker) {
      const rpcMethod = query?.className || query?._ || (typeof query === 'string' ? query : null);
      return this.sendRpc<T>('INVOKE', {
        rpcMethod,
        params: query,
      });
    }
    this.initFallbackClient();
    return (this.fallbackClient as any).invoke(query);
  }

  public async sendMessage(peer: any, options: any): Promise<any> {
    if (this._isWorkerSupported && this.worker) {
      return this.sendRpc('SEND_MESSAGE', {
        peer,
        message: typeof options === 'string' ? options : options?.message,
        options: typeof options === 'object' ? options : {},
      });
    }
    this.initFallbackClient();
    return (this.fallbackClient as any).sendMessage(peer, options);
  }

  public async getMessages(peer: any, options?: any): Promise<any[]> {
    if (this._isWorkerSupported && this.worker) {
      return this.sendRpc('GET_MESSAGES', { peer, options });
    }
    this.initFallbackClient();
    return (this.fallbackClient as any).getMessages(peer, options);
  }

  public async sendCode(phoneNumber: string): Promise<any> {
    if (this._isWorkerSupported && this.worker) {
      return this.sendRpc('SEND_CODE', { phoneNumber });
    }
    this.initFallbackClient();
    return (this.fallbackClient as any).sendCode(
      { apiId: this.apiId, apiHash: this.apiHash },
      phoneNumber
    );
  }

  public async signInUser(params: { phoneNumber: string; phoneCodeHash: string; phoneCode: string }): Promise<any> {
    if (this._isWorkerSupported && this.worker) {
      return this.sendRpc('SIGN_IN_USER', params);
    }
    this.initFallbackClient();
    return (this.fallbackClient as any).invoke(
      new Api.auth.SignIn(params)
    );
  }

  public async signInWithPassword(params: { password: string }): Promise<any> {
    if (this._isWorkerSupported && this.worker) {
      return this.sendRpc('SIGN_IN_PASSWORD', params);
    }
    this.initFallbackClient();
    return (this.fallbackClient as any).signInWithPassword(params);
  }

  public addEventHandler(handler: (update: any) => void): () => void {
    this.eventHandlers.add(handler);
    if (!this._isWorkerSupported && this.fallbackClient) {
      (this.fallbackClient as any).addEventHandler(handler);
    }
    return () => {
      this.eventHandlers.delete(handler);
    };
  }

  public removeEventHandler(handler: (update: any) => void): void {
    this.eventHandlers.delete(handler);
  }

  public async ping(): Promise<{ latencyMs: number; connected: boolean }> {
    if (this._isWorkerSupported && this.worker) {
      return this.sendRpc('PING');
    }
    return { latencyMs: 1, connected: Boolean(this.fallbackClient?.connected) };
  }

  public isWorkerActive(): boolean {
    return this._isWorkerSupported && this.worker !== null;
  }

  public destroy(): void {
    if (this.worker) {
      this.worker.terminate();
      this.worker = null;
      this._isWorkerSupported = false;
    }
    this.pendingRequests.clear();
    this.eventHandlers.clear();
  }
}

// Singleton worker client instance
let workerClientInstance: GramJsWorkerClient | null = null;

/**
 * Initializes and configures the Worker-Offloaded GramJS MTProto Client
 */
export function initTelegramClient(
  customSessionString?: string,
  customApiId?: number,
  customApiHash?: string
): GramJsWorkerClient {
  if (workerClientInstance) {
    workerClientInstance.destroy();
  }
  workerClientInstance = new GramJsWorkerClient(customSessionString, customApiId, customApiHash);
  return workerClientInstance;
}

/**
 * Gets the current GramJS MTProto client instance, initializing if not already created
 */
export function getTelegramClient(): GramJsWorkerClient {
  if (!workerClientInstance) {
    workerClientInstance = new GramJsWorkerClient();
  }
  return workerClientInstance;
}

/**
 * Connects the MTProto client to Telegram's data centers via Web Worker
 */
export async function connectTelegramClient(): Promise<GramJsWorkerClient> {
  const client = getTelegramClient();
  if (!client.connected) {
    await client.connect();
  }
  return client;
}

/**
 * Disconnects the MTProto client safely
 */
export async function disconnectTelegramClient(): Promise<void> {
  if (workerClientInstance) {
    await workerClientInstance.disconnect();
  }
}

/**
 * Checks if the client is currently active and connected
 */
export function isTelegramConnected(): boolean {
  return Boolean(workerClientInstance && workerClientInstance.connected);
}

/**
 * Direct access to the GramJs Worker Proxy instance
 */
export function getGramJsWorkerProxy(): GramJsWorkerClient {
  return getTelegramClient();
}

/**
 * Checks whether GramJS is actively running inside a Web Worker
 */
export function isGramJsWorkerRunning(): boolean {
  return Boolean(workerClientInstance && workerClientInstance.isWorkerActive());
}

// Re-export core GramJS types and modules for consumer access
export { TelegramClient, Api, StringSession };
export default getTelegramClient;
