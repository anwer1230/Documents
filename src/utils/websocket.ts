/**
 * Telegram Web K Real-time WebSocket Client
 * Connects to server WebSocket and dispatches live updates:
 * - new_message
 * - typing_status
 * - user_status
 * - message_read
 */

export interface WebSocketMessage {
  type: 'connected' | 'new_message' | 'typing_status' | 'user_status' | 'message_read' | 'notification';
  payload?: any;
  peerId?: string;
  action?: string;
  userName?: string;
  message?: any;
  time?: number;
}

type MessageListener = (data: WebSocketMessage) => void;

class TelegramWebSocketClient {
  private socket: WebSocket | null = null;
  private listeners: Set<MessageListener> = new Set();
  private reconnectTimeout: any = null;
  private sessionToken: string = '';
  private isConnecting: boolean = false;

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
      };

      this.socket.onmessage = (event) => {
        try {
          const data: WebSocketMessage = JSON.parse(event.data);
          this.notifyListeners(data);
        } catch (e) {
          console.warn('[WebSocket] Error parsing message:', e);
        }
      };

      this.socket.onclose = () => {
        this.isConnecting = false;
        this.scheduleReconnect();
      };

      this.socket.onerror = (err) => {
        this.isConnecting = false;
        console.warn('[WebSocket] Connection error, will reconnect:', err);
      };
    } catch (e) {
      this.isConnecting = false;
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
