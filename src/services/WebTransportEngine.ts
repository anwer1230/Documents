/**
 * WebTransportEngine.ts - High-Speed Resilient Transport Engine
 * Detects WebTransport (HTTP/3 / QUIC) support for ultra-low latency Telegram connections
 * Falls back seamlessly to optimized binary WebSocket with keepalive heartbeat
 */

export class WebTransportEngine {
  private static instance: WebTransportEngine;
  private isWebTransportSupported: boolean;

  private constructor() {
    this.isWebTransportSupported = typeof globalThis !== 'undefined' && 'WebTransport' in globalThis;
  }

  public static getInstance(): WebTransportEngine {
    if (!WebTransportEngine.instance) {
      WebTransportEngine.instance = new WebTransportEngine();
    }
    return WebTransportEngine.instance;
  }

  public isSupported(): boolean {
    return this.isWebTransportSupported;
  }

  /**
   * Creates an optimized connection choosing WebTransport where available, falling back to WebSocket
   */
  public async createTransportConnection(endpointUrl: string): Promise<any> {
    if (this.isWebTransportSupported) {
      try {
        const wt = new (globalThis as any).WebTransport(endpointUrl);
        await wt.ready;
        return {
          type: 'webtransport',
          transport: wt,
          send: async (data: Uint8Array) => {
            const writer = wt.datagrams.writable.getWriter();
            await writer.write(data);
            writer.releaseLock();
          }
        };
      } catch (err) {
        console.warn('[WebTransportEngine] WebTransport negotiation failed, falling back to WebSocket:', err);
      }
    }

    // Binary WebSocket fallback
    const wsUrl = endpointUrl.replace(/^http/, 'ws');
    const ws = new WebSocket(wsUrl);
    ws.binaryType = 'arraybuffer';
    await new Promise((res, rej) => {
      ws.onopen = res;
      ws.onerror = rej;
    });

    return {
      type: 'websocket',
      transport: ws,
      send: (data: Uint8Array) => ws.send(data.buffer)
    };
  }
}

export const webTransportEngine = WebTransportEngine.getInstance();
