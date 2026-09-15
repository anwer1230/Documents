/**
 * Telegram Web K Connection Transport (connection.ts)
 * Based on morethanwords/tweb src/lib/mtproto/connection.ts
 */

import { EventEmitter } from '../appManagers/utils';

export type ConnectionState = 'connecting' | 'connected' | 'closed' | 'broken';

export class MTProtoConnection extends EventEmitter {
  private ws: WebSocket | null = null;
  private url: string;
  private state: ConnectionState = 'closed';
  private pingInterval: any = null;

  constructor(url: string) {
    super();
    this.url = url;
  }

  public connect(): Promise<void> {
    if (this.state === 'connected' && this.ws?.readyState === WebSocket.OPEN) {
      return Promise.resolve();
    }

    this.setState('connecting');

    return new Promise((resolve, reject) => {
      try {
        this.ws = new WebSocket(this.url);
        this.ws.binaryType = 'arraybuffer';

        this.ws.onopen = () => {
          this.setState('connected');
          this.startHeartbeat();
          resolve();
        };

        this.ws.onmessage = (event) => {
          this.emit('message', event.data);
        };

        this.ws.onerror = (err) => {
          this.emit('error', err);
        };

        this.ws.onclose = () => {
          this.stopHeartbeat();
          this.setState('closed');
          this.emit('close', {});
        };
      } catch (err) {
        this.setState('broken');
        reject(err);
      }
    });
  }

  public send(data: string | ArrayBuffer | Uint8Array): void {
    if (this.ws && this.ws.readyState === WebSocket.OPEN) {
      this.ws.send(data);
    }
  }

  public close(): void {
    this.stopHeartbeat();
    if (this.ws) {
      this.ws.close();
      this.ws = null;
    }
    this.setState('closed');
  }

  public getState(): ConnectionState {
    return this.state;
  }

  private setState(newState: ConnectionState): void {
    if (this.state !== newState) {
      this.state = newState;
      this.emit('state_change', newState);
    }
  }

  private startHeartbeat(): void {
    this.stopHeartbeat();
    this.pingInterval = setInterval(() => {
      if (this.ws && this.ws.readyState === WebSocket.OPEN) {
        try {
          this.ws.send(JSON.stringify({ type: 'ping' }));
        } catch {}
      }
    }, 25000);
  }

  private stopHeartbeat(): void {
    if (this.pingInterval) {
      clearInterval(this.pingInterval);
      this.pingInterval = null;
    }
  }
}
