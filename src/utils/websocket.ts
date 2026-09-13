/**
 * Telegram Web K Real-time WebSocket Client
 * Connects to server WebSocket and dispatches live updates:
 * - new_message
 * - typing_status
 * - user_status
 * - message_read
 */

export interface WebSocketMessage {
  type:
    | 'connected'
    | 'new_message'
    | 'typing_status'
    | 'user_status'
    | 'message_read'
    | 'message_edited'
    | 'messages_deleted'
    | 'notification'
    | 'sync_batch';
  payload?: any;
  peerId?: string;
  action?: string;
  userName?: string;
  message?: any;
  messages?: any[];
  lastTimestamp?: number;
  count?: number;
  messageId?: string;
  messageIds?: string[];
  text?: string;
  editDate?: number;
  isOnline?: boolean;
  userId?: string;
  time?: number;
}

type MessageListener = (data: WebSocketMessage) => void;

class TelegramWebSocketClient {
  private socket: WebSocket | null = null;
  private listeners: Set<MessageListener> = new Set();
  private reconnectTimeout: any = null;
  private sessionToken: string = '';
  private isConnecting: boolean = false;
  private hasConnectedBefore: boolean = false;
  private wasDisconnected: boolean = false;
  private lastTimestamp: number = 0;

  /**
   * Sets and persists the latest synchronized message timestamp
   */
  public setLastTimestamp(timestamp: number) {
    if (!timestamp || isNaN(timestamp)) return;
    const sec = timestamp > 10000000000 ? Math.floor(timestamp / 1000) : Math.floor(timestamp);
    if (sec > this.lastTimestamp) {
      this.lastTimestamp = sec;
      try {
        localStorage.setItem('tg_last_sync_timestamp', String(sec));
      } catch {}
    }
  }

  /**
   * Retrieves the last synchronized timestamp in seconds
   */
  public getLastTimestamp(): number {
    if (!this.lastTimestamp) {
      try {
        const stored = localStorage.getItem('tg_last_sync_timestamp');
        if (stored) {
          this.lastTimestamp = Number(stored);
        }
      } catch {}
    }
    if (!this.lastTimestamp || isNaN(this.lastTimestamp)) {
      // Default to 1 hour ago if no timestamp stored
      this.lastTimestamp = Math.floor(Date.now() / 1000) - 3600;
    }
    return this.lastTimestamp;
  }

  public connect(sessionToken: string) {
    if (this.sessionToken === sessionToken && this.socket && this.socket.readyState === WebSocket.OPEN) {
      return;
    }
    this.sessionToken = sessionToken;
    this.disconnect();
    this.initSocket();
  }

  private initSocket() {
    if (this.isConnecting) return;
    this.isConnecting = true;

    try {
      const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
      const wsUrl = `${protocol}//${window.location.host}/ws?token=${encodeURIComponent(this.sessionToken)}`;
      
      this.socket = new WebSocket(wsUrl);

      this.socket.onopen = () => {
        this.isConnecting = false;
        console.log('[WebSocket] Connected to Telegram Web real-time server');

        // Stage 5: Reconnect catchup sync protocol
        if (this.wasDisconnected || this.hasConnectedBefore) {
          const lastTs = this.getLastTimestamp();
          console.log('[WebSocket Reconnect] Sending sync_request catchup with lastTimestamp:', lastTs);
          this.send({
            action: 'sync_request',
            lastTimestamp: lastTs,
          });
        }
        this.hasConnectedBefore = true;
        this.wasDisconnected = false;
        this.notifyListeners({ type: 'connected' });
      };

      this.socket.onmessage = (event) => {
        try {
          const data: WebSocketMessage = JSON.parse(event.data);
          // Catchup sync_batch normalization
          if (data.action === 'sync_batch' || data.type === 'sync_batch') {
            data.type = 'sync_batch';
            if (Array.isArray(data.messages)) {
              data.messages.forEach((m: any) => {
                if (m.timestamp) this.setLastTimestamp(m.timestamp);
              });
            }
          }
          if (data.message?.timestamp) {
            this.setLastTimestamp(data.message.timestamp);
          }
          this.notifyListeners(data);
        } catch (e) {
          console.warn('[WebSocket] Error parsing message:', e);
        }
      };

      this.socket.onclose = () => {
        this.isConnecting = false;
        this.wasDisconnected = true;
        this.scheduleReconnect();
      };

      this.socket.onerror = (err) => {
        this.isConnecting = false;
        this.wasDisconnected = true;
        console.warn('[WebSocket] Connection error, will reconnect:', err);
      };
    } catch (e) {
      this.isConnecting = false;
      this.wasDisconnected = true;
      this.scheduleReconnect();
    }
  }

  private scheduleReconnect() {
    if (this.reconnectTimeout) clearTimeout(this.reconnectTimeout);
    this.reconnectTimeout = setTimeout(() => {
      if (this.sessionToken) {
        this.initSocket();
      }
    }, 3000);
  }

  public subscribe(listener: MessageListener) {
    this.listeners.add(listener);
    return () => {
      this.listeners.delete(listener);
    };
  }

  private notifyListeners(data: WebSocketMessage) {
    this.listeners.forEach((listener) => {
      try {
        listener(data);
      } catch (err) {
        console.error('[WebSocket] Listener error:', err);
      }
    });
  }

  public send(data: any) {
    if (this.socket && this.socket.readyState === WebSocket.OPEN) {
      this.socket.send(JSON.stringify(data));
    }
  }

  public disconnect() {
    if (this.reconnectTimeout) {
      clearTimeout(this.reconnectTimeout);
      this.reconnectTimeout = null;
    }
    if (this.socket) {
      this.socket.onclose = null;
      this.socket.onerror = null;
      this.socket.close();
      this.socket = null;
    }
    this.isConnecting = false;
  }
}

export const wsClient = new TelegramWebSocketClient();
