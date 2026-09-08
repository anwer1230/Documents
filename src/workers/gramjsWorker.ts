/**
 * gramjsWorker.ts - Dedicated GramJS MTProto Web Worker
 * 
 * Offloads all MTProto 2.0 cryptographic operations, TL serialization,
 * WebSocket transport networking, and incoming update handling from the
 * main browser UI thread to a dedicated background worker thread.
 * 
 * Capabilities:
 * - MTProto 2.0 handshake & Diffie-Hellman key exchange in background
 * - AES-IGE 256-bit encryption & decryption off the UI thread
 * - High-throughput binary WebSocket stream management
 * - Real-time MTProto updates fanout and streaming
 * - Two-Factor Authentication (2FA) SRP & PBKDF2 hash computation
 * - Zero UI thread locking during high-frequency message bursts
 */

import { TelegramClient, Api } from 'telegram';
import { StringSession } from 'telegram/sessions';

export interface WorkerGramJsRequest {
  id: string;
  type:
    | 'INIT'
    | 'CONNECT'
    | 'DISCONNECT'
    | 'CHECK_AUTH'
    | 'GET_ME'
    | 'SEND_CODE'
    | 'SIGN_IN_USER'
    | 'SIGN_IN_PASSWORD'
    | 'SEND_MESSAGE'
    | 'GET_MESSAGES'
    | 'GET_DIALOGS'
    | 'INVOKE'
    | 'INVOKE_RAW'
    | 'GET_SESSION'
    | 'PING'
    | 'SET_CONFIG';
  payload?: any;
}

export interface WorkerGramJsResponse {
  id: string;
  success: boolean;
  result?: any;
  error?: string;
}

export interface WorkerGramJsEvent {
  type: 'UPDATE' | 'CONNECTION_STATUS' | 'SESSION_SAVED' | 'LOG' | 'ERROR';
  data?: any;
  timestamp: number;
}

function resolveApiConstructor(methodName: string): any {
  if (!methodName) return null;
  // 1. Direct match on Api
  if ((Api as any)[methodName]) return (Api as any)[methodName];
  // 2. Namespaced match: e.g. messages.GetHistory
  if (methodName.includes('.')) {
    const parts = methodName.split('.');
    let cur: any = Api;
    for (const p of parts) {
      if (!cur) break;
      cur = cur[p];
    }
    if (typeof cur === 'function') return cur;
  }
  // 3. Capitalization fallback: e.g. 'messages.getHistory' -> 'messages.GetHistory'
  const parts = methodName.split('.');
  if (parts.length === 2) {
    const ns = parts[0];
    const method = parts[1].charAt(0).toUpperCase() + parts[1].slice(1);
    if ((Api as any)[ns]?.[method]) return (Api as any)[ns][method];
  }
  return null;
}

function reconstructApiObject(data: any): any {
  if (!data || typeof data !== 'object') return data;
  if (Array.isArray(data)) {
    return data.map(reconstructApiObject);
  }
  const typeName = data.className || data._;
  const constructor = typeName ? resolveApiConstructor(typeName) : null;
  const reconstructedProps: Record<string, any> = {};
  for (const [key, val] of Object.entries(data)) {
    if (key === 'className' || key === '_' || key === 'CONSTRUCTOR_ID' || key === 'SUBCLASS_OF_ID') continue;
    reconstructedProps[key] = reconstructApiObject(val);
  }
  if (constructor) {
    try {
      return new constructor(reconstructedProps);
    } catch {
      return reconstructedProps;
    }
  }
  return reconstructedProps;
}

class GramJsWorkerServer {
  private client: TelegramClient | null = null;
  private session: StringSession | null = null;
  private apiId: number = 22043994;
  private apiHash: string = '56f64582b363d367280db96586b97801';
  private isConnecting = false;

  public constructor() {
    this.setupMessageListener();
  }

  private setupMessageListener(): void {
    if (typeof self === 'undefined' || !('onmessage' in self)) return;

    self.onmessage = async (e: MessageEvent<WorkerGramJsRequest>) => {
      const { id, type, payload } = e.data || {};
      if (!id || !type) return;

      try {
        const result = await this.handleRequest(type, payload);
        this.sendSuccess(id, result);
      } catch (err: any) {
        console.error(`[GramJsWorker] Error handling ${type}:`, err);
        this.sendError(id, err?.message || String(err));
      }
    };
  }

  private async handleRequest(type: WorkerGramJsRequest['type'], payload: any): Promise<any> {
    switch (type) {
      case 'INIT': {
        const { sessionString, apiId, apiHash, options } = payload || {};
        if (apiId) this.apiId = Number(apiId);
        if (apiHash) this.apiHash = String(apiHash);

        this.session = new StringSession(sessionString || '');
        this.client = new TelegramClient(this.session, this.apiId, this.apiHash, {
          connectionRetries: options?.connectionRetries ?? 5,
          useWSS: true,
          autoReconnect: true,
          floodSleepThreshold: options?.floodSleepThreshold ?? 60,
          deviceModel: options?.deviceModel ?? 'Telegram Web (GramJS Worker)',
          systemVersion: '1.0.0',
          appVersion: '10.14.0',
        });

        // Attach event handler for live MTProto updates
        this.client.addEventHandler((update: any) => {
          this.emitEvent('UPDATE', this.sanitizeData(update));
        });

        return { initialized: true };
      }

      case 'CONNECT': {
        if (!this.client) {
          throw new Error('TelegramClient not initialized in worker. Call INIT first.');
        }

        if (!this.client.connected && !this.isConnecting) {
          this.isConnecting = true;
          try {
            await this.client.connect();
            this.emitEvent('CONNECTION_STATUS', { connected: true });
            
            // Save and emit the session string
            if (this.session) {
              const savedStr = this.session.save();
              this.emitEvent('SESSION_SAVED', { sessionString: savedStr });
            }
          } finally {
            this.isConnecting = false;
          }
        }

        return {
          connected: Boolean(this.client.connected),
          sessionString: this.session?.save() || '',
        };
      }

      case 'DISCONNECT': {
        if (this.client && this.client.connected) {
          await this.client.disconnect();
          this.emitEvent('CONNECTION_STATUS', { connected: false });
        }
        return { disconnected: true };
      }

      case 'CHECK_AUTH': {
        if (!this.client) return { authorized: false };
        const isAuth = await this.client.checkAuthorization().catch(() => false);
        return { authorized: isAuth };
      }

      case 'GET_ME': {
        if (!this.client) throw new Error('Client not initialized');
        const me = await this.client.getMe();
        return this.sanitizeData(me);
      }

      case 'SEND_CODE': {
        if (!this.client) throw new Error('Client not initialized');
        const { phoneNumber } = payload;
        const res = await this.client.sendCode(
          {
            apiId: this.apiId,
            apiHash: this.apiHash,
          },
          phoneNumber
        );
        return this.sanitizeData(res);
      }

      case 'SIGN_IN_USER': {
        if (!this.client) throw new Error('Client not initialized');
        const { phoneNumber, phoneCodeHash, phoneCode } = payload;
        const res = await this.client.invoke(
          new Api.auth.SignIn({
            phoneNumber,
            phoneCodeHash,
            phoneCode,
          })
        );
        if (this.session) {
          this.emitEvent('SESSION_SAVED', { sessionString: this.session.save() });
        }
        return this.sanitizeData(res);
      }

      case 'SIGN_IN_PASSWORD': {
        if (!this.client) throw new Error('Client not initialized');
        const { password } = payload;
        const res = await (this.client as any).signInWithPassword({
          password,
        });
        if (this.session) {
          this.emitEvent('SESSION_SAVED', { sessionString: this.session.save() });
        }
        return this.sanitizeData(res);
      }

      case 'SEND_MESSAGE': {
        if (!this.client) throw new Error('Client not initialized');
        const { peer, message, options } = payload;
        const sent = await this.client.sendMessage(peer, {
          message,
          ...options,
        });
        return this.sanitizeData(sent);
      }

      case 'GET_MESSAGES': {
        if (!this.client) throw new Error('Client not initialized');
        const { peer, options } = payload;
        const messages = await this.client.getMessages(peer, options || {});
        return this.sanitizeData(messages);
      }

      case 'GET_DIALOGS': {
        if (!this.client) throw new Error('Client not initialized');
        const { options } = payload || {};
        const dialogs = await this.client.getDialogs(options || {});
        return this.sanitizeData(dialogs);
      }

      case 'INVOKE': {
        if (!this.client) throw new Error('Client not initialized');
        const { rpcMethod, params } = payload;
        
        let rpcQuery: any;
        if (rpcMethod) {
          const constructor = resolveApiConstructor(rpcMethod);
          if (constructor) {
            const reconstructedParams = reconstructApiObject(params);
            rpcQuery = new constructor(reconstructedParams || {});
          } else {
            rpcQuery = reconstructApiObject(params);
          }
        } else {
          rpcQuery = reconstructApiObject(params);
        }

        const result = await this.client.invoke(rpcQuery);
        return this.sanitizeData(result);
      }

      case 'INVOKE_RAW': {
        if (!this.client) throw new Error('Client not initialized');
        const { query } = payload;
        const reconstructed = reconstructApiObject(query);
        const result = await this.client.invoke(reconstructed);
        return this.sanitizeData(result);
      }

      case 'GET_SESSION': {
        return {
          sessionString: this.session?.save() || '',
        };
      }

      case 'PING': {
        const start = performance.now();
        const connected = Boolean(this.client?.connected);
        return {
          pong: true,
          connected,
          latencyMs: Math.round(performance.now() - start),
          timestamp: Date.now(),
        };
      }

      default:
        throw new Error(`Unsupported GramJS worker request type: ${type}`);
    }
  }

  private sendSuccess(id: string, result: any): void {
    const msg: WorkerGramJsResponse = { id, success: true, result };
    try {
      (self as any).postMessage(msg);
    } catch {
      (self as any).postMessage({
        id,
        success: true,
        result: this.sanitizeData(result),
      });
    }
  }

  private sendError(id: string, error: string): void {
    const msg: WorkerGramJsResponse = { id, success: false, error };
    (self as any).postMessage(msg);
  }

  private emitEvent(type: WorkerGramJsEvent['type'], data?: any): void {
    const event: WorkerGramJsEvent = {
      type,
      data,
      timestamp: Date.now(),
    };
    try {
      (self as any).postMessage(event);
    } catch {
      (self as any).postMessage({
        type,
        data: this.sanitizeData(data),
        timestamp: Date.now(),
      });
    }
  }

  /**
   * Sanitizes GramJS TL objects and BigInt values for structured-clone transfer
   */
  private sanitizeData(obj: any, depth = 0, seen = new WeakSet()): any {
    if (obj === null || obj === undefined) return obj;
    if (typeof obj === 'bigint') return obj.toString();
    if (typeof obj === 'number' || typeof obj === 'string' || typeof obj === 'boolean') return obj;
    if (depth > 8) return null;

    if (typeof obj === 'object') {
      if (seen.has(obj)) return '[Circular]';
      seen.add(obj);

      if (Array.isArray(obj)) {
        return obj.map(item => this.sanitizeData(item, depth + 1, seen));
      }

      if (obj instanceof Uint8Array) {
        return Array.from(obj);
      }

      const copy: Record<string, any> = {};
      if (obj.className) copy._ = obj.className;
      if (obj.CONSTRUCTOR_ID) copy.CONSTRUCTOR_ID = obj.CONSTRUCTOR_ID;

      for (const key of Object.keys(obj)) {
        if (key.startsWith('_') || key === 'client') continue;
        const val = obj[key];
        if (typeof val !== 'function') {
          copy[key] = this.sanitizeData(val, depth + 1, seen);
        }
      }
      return copy;
    }

    return null;
  }
}

// Instantiate worker server in web worker scope
new GramJsWorkerServer();
