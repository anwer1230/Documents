/**
 * Telegram Web K Networker (networker.ts)
 * Based on morethanwords/tweb src/lib/mtproto/networker.ts
 * Manages transport switching, reconnect with exponential backoff, and packet dispatching.
 */

import { MTProtoConnection, ConnectionState } from './connection';
import { EventEmitter } from '../appManagers/utils';

export interface NetworkerOptions {
  dcId: number;
  url?: string;
  maxRetries?: number;
}

export class Networker extends EventEmitter {
  private dcId: number;
  private connection: MTProtoConnection | null = null;
  private retryCount = 0;
  private maxRetries: number;
  private reconnectTimeout: any = null;
  private isDestroyed = false;

  constructor(options: NetworkerOptions) {
    super();
    this.dcId = options.dcId;
    this.maxRetries = options.maxRetries || 10;
    this.setupConnection(options.url);
  }

  private setupConnection(customUrl?: string): void {
    const protocol = typeof window !== 'undefined' && window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    const host = typeof window !== 'undefined' ? window.location.host : 'localhost:3000';
    const url = customUrl || `${protocol}//${host}/ws/telegram?dc=${this.dcId}`;

    this.connection = new MTProtoConnection(url);

    this.connection.on('state_change', (state: ConnectionState) => {
      this.emit('status', state);
      if (state === 'connected') {
        this.retryCount = 0;
      }
    });

    this.connection.on('message', (raw: any) => {
      try {
        const payload = typeof raw === 'string' ? JSON.parse(raw) : raw;
        this.emit('update', payload);
      } catch {
        this.emit('update', raw);
      }
    });

    this.connection.on('close', () => {
      if (!this.isDestroyed) {
        this.scheduleReconnect();
      }
    });

    this.connection.on('error', () => {
      if (!this.isDestroyed) {
        this.scheduleReconnect();
      }
    });
  }

  public async connect(): Promise<void> {
    if (this.connection) {
      try {
        await this.connection.connect();
      } catch {
        this.scheduleReconnect();
      }
    }
  }

  public send(data: any): void {
    if (this.connection) {
      const payload = typeof data === 'string' || data instanceof ArrayBuffer || data instanceof Uint8Array
        ? data
        : JSON.stringify(data);
      this.connection.send(payload);
    }
  }

  public disconnect(): void {
    if (this.reconnectTimeout) {
      clearTimeout(this.reconnectTimeout);
      this.reconnectTimeout = null;
    }
    if (this.connection) {
      this.connection.close();
    }
  }

  public destroy(): void {
    this.isDestroyed = true;
    this.disconnect();
  }

  private scheduleReconnect(): void {
    if (this.reconnectTimeout || this.isDestroyed) return;
    if (this.retryCount >= this.maxRetries) {
      this.emit('status', 'broken');
      return;
    }

    const delay = Math.min(1000 * Math.pow(1.5, this.retryCount), 30000);
    this.retryCount++;
    this.emit('status', 'connecting');

    this.reconnectTimeout = setTimeout(() => {
      this.reconnectTimeout = null;
      this.connect();
    }, delay);
  }
}

export default Networker;
