/**
 * server/ws-security.ts
 * Hardened WebSocket Server with strict authentication, origin check,
 * rate limiting, payload size limits, and ping/pong heartbeats.
 */
import http from 'http';
import { WebSocketServer, WebSocket } from 'ws';
import { SECURITY } from './security/config.js';
import { getSessionByWsToken, getSessionByToken } from './security/sessions.js';

export interface WsAuthResult {
  valid: boolean;
  token?: string;
  sessionId?: string;
  reason?: string;
}

interface ClientRateState {
  msgCountSec: number;
  lastSecReset: number;
  msgCountMin: number;
  lastMinReset: number;
}

// IP tracking
interface IpTracker {
  concurrent: number;
  recentConnections: number[];
}

const ipTrackers = new Map<string, IpTracker>();
const activeWsClients = new Map<string, Set<WebSocket>>();
let messageHandler: ((ws: WebSocket, token: string, data: any) => Promise<void> | void) | null = null;
let heartbeatInterval: NodeJS.Timeout | null = null;

function getClientIp(req: http.IncomingMessage): string {
  const forwarded = req.headers['x-forwarded-for'];
  if (typeof forwarded === 'string') {
    return forwarded.split(',')[0].trim();
  }
  return req.socket.remoteAddress || '127.0.0.1';
}

function parseCookies(cookieHeader?: string): Record<string, string> {
  const cookies: Record<string, string> = {};
  if (!cookieHeader) return cookies;
  const pairs = cookieHeader.split(';');
  for (const pair of pairs) {
    const idx = pair.indexOf('=');
    if (idx < 0) continue;
    const key = pair.substring(0, idx).trim();
    const val = pair.substring(idx + 1).trim();
    cookies[key] = decodeURIComponent(val);
  }
  return cookies;
}

export function validateOrigin(origin?: string, host?: string): boolean {
  if (!origin) return true; // non-browser or same-origin
  if (host && origin === `http://${host}` || origin === `https://${host}`) return true;

  if (SECURITY.origins.allowList.includes(origin)) return true;

  if (SECURITY.origins.allowLocalhostInDev &&
      /^https?:\/\/(localhost|127\.0\.0\.1)(:\d+)?$/.test(origin)) {
    return true;
  }

  return false;
}

export function validateSessionToken(token: string): WsAuthResult {
  if (!token || token.length < 10) {
    return { valid: false, reason: 'invalid_format' };
  }

  // Check WS token first
  const sessionByWs = getSessionByWsToken(token);
  if (sessionByWs) {
    return { valid: true, token, sessionId: sessionByWs.token };
  }

  // Fallback to session token
  const sessionByToken = getSessionByToken(token);
  if (sessionByToken) {
    return { valid: true, token, sessionId: sessionByToken.token };
  }

  // Dev demo token check
  if (!SECURITY.isProd && (token.startsWith('demo_') || token.startsWith('test_'))) {
    return { valid: true, token, sessionId: token };
  }

  return { valid: false, reason: 'no_session' };
}

export function setMessageHandler(handler: (ws: WebSocket, token: string, data: any) => Promise<void> | void) {
  messageHandler = handler;
}

export function broadcastToSession(token: string, message: any): void {
  const clients = activeWsClients.get(token);
  if (!clients) return;
  const data = JSON.stringify(message);
  for (const client of clients) {
    if (client.readyState === WebSocket.OPEN) {
      try {
        client.send(data);
      } catch (e) {
        console.error('[WS] Send error:', e);
      }
    }
  }
}

export function broadcastAll(message: any): void {
  const data = JSON.stringify(message);
  for (const clients of activeWsClients.values()) {
    for (const client of clients) {
      if (client.readyState === WebSocket.OPEN) {
        try {
          client.send(data);
        } catch (e) {
          console.error('[WS] BroadcastAll error:', e);
        }
      }
    }
  }
}

export function getActiveConnectionsCount(): number {
  let count = 0;
  for (const clients of activeWsClients.values()) {
    count += clients.size;
  }
  return count;
}

export function attachWebSocketServer(server: http.Server): WebSocketServer {
  const wss = new WebSocketServer({ noServer: true });

  server.on('upgrade', (req, socket, head) => {
    let url: URL;
    try {
      url = new URL(req.url || '', `http://${req.headers.host || 'localhost'}`);
    } catch {
      socket.write('HTTP/1.1 400 Bad Request\r\n\r\n');
      socket.destroy();
      return;
    }

    // Only handle /ws paths, leave others (e.g. Vite HMR) to their own handlers
    if (url.pathname !== '/ws' && url.pathname !== '/ws/') {
      return;
    }

    // Origin Check
    const origin = req.headers.origin as string | undefined;
    if (!validateOrigin(origin, req.headers.host)) {
      console.warn(`[WS Origin] Rejected origin: ${origin}`);
      wss.handleUpgrade(req, socket, head, (ws) => {
        try {
          ws.close(4003, 'Forbidden Origin');
        } catch {}
      });
      return;
    }

    // IP Rate Limiting
    const ip = getClientIp(req);
    const now = Date.now();
    let tracker = ipTrackers.get(ip);
    if (!tracker) {
      tracker = { concurrent: 0, recentConnections: [] };
      ipTrackers.set(ip, tracker);
    }
    // Filter connections in the last minute
    tracker.recentConnections = tracker.recentConnections.filter(ts => now - ts < 60_000);

    if (tracker.recentConnections.length >= SECURITY.ws.connectionsPerIpPerMinute) {
      console.warn(`[WS RateLimit] IP ${ip} exceeded connection rate limit`);
      socket.write('HTTP/1.1 429 Too Many Requests\r\n\r\n');
      socket.destroy();
      return;
    }

    if (tracker.concurrent >= SECURITY.ws.concurrentPerIp) {
      console.warn(`[WS RateLimit] IP ${ip} exceeded concurrent connection limit`);
      socket.write('HTTP/1.1 429 Too Many Requests\r\n\r\n');
      socket.destroy();
      return;
    }

    // Authentication
    const cookies = parseCookies(req.headers.cookie);
    const token =
      cookies[SECURITY.cookies.ws.name] ||
      cookies[SECURITY.cookies.session.name] ||
      url.searchParams.get('token') ||
      '';

    if (SECURITY.ws.strictMode) {
      const auth = validateSessionToken(token);
      if (!auth.valid) {
        console.warn(`[WS Auth] Rejected connection without valid session: ${auth.reason}`);
        wss.handleUpgrade(req, socket, head, (ws) => {
          try {
            ws.close(4001, 'Unauthorized');
          } catch {}
        });
        return;
      }
    }

    const effectiveToken = token || 'guest';

    tracker.recentConnections.push(now);
    tracker.concurrent++;

    wss.handleUpgrade(req, socket, head, (ws) => {
      wss.emit('connection', ws, req, { token: effectiveToken, ip });
    });
  });

  wss.on('connection', (ws: WebSocket & { isAlive?: boolean }, req: http.IncomingMessage, context?: { token: string; ip: string }) => {
    const token = context?.token || 'guest';
    const ip = context?.ip || getClientIp(req);

    if (!activeWsClients.has(token)) {
      activeWsClients.set(token, new Set());
    }
    activeWsClients.get(token)!.add(ws);

    // Heartbeat setup
    ws.isAlive = true;
    ws.on('pong', () => {
      ws.isAlive = true;
    });

    // Per-client message rate limiting state
    const rateState: ClientRateState = {
      msgCountSec: 0,
      lastSecReset: Date.now(),
      msgCountMin: 0,
      lastMinReset: Date.now(),
    };

    // Connection confirmation
    try {
      ws.send(JSON.stringify({ type: 'connected', time: Date.now() }));
    } catch {}

    ws.on('message', async (raw) => {
      const now = Date.now();

      // Check payload size
      const payloadSize = Buffer.isBuffer(raw) ? raw.length : Buffer.byteLength(raw.toString());
      if (payloadSize > SECURITY.ws.maxPayloadBytes) {
        console.warn(`[WS] Payload size ${payloadSize} exceeds limit ${SECURITY.ws.maxPayloadBytes}`);
        ws.close(4008, 'Message too large');
        return;
      }

      // Check rate limit: per-second
      if (now - rateState.lastSecReset > 1000) {
        rateState.msgCountSec = 0;
        rateState.lastSecReset = now;
      }
      rateState.msgCountSec++;
      if (rateState.msgCountSec > SECURITY.ws.messagesPerSecond) {
        console.warn(`[WS RateLimit] Token ${token} exceeded messages-per-second limit`);
        ws.close(4008, 'Rate limit exceeded');
        return;
      }

      // Check rate limit: per-minute
      if (now - rateState.lastMinReset > 60_000) {
        rateState.msgCountMin = 0;
        rateState.lastMinReset = now;
      }
      rateState.msgCountMin++;
      if (rateState.msgCountMin > SECURITY.ws.messagesPerMinute) {
        console.warn(`[WS RateLimit] Token ${token} exceeded messages-per-minute limit`);
        ws.close(4008, 'Rate limit exceeded');
        return;
      }

      // Handle message
      try {
        const data = JSON.parse(raw.toString());
        if (data.type === 'ping') {
          ws.send(JSON.stringify({ type: 'pong', time: Date.now() }));
          return;
        }

        if (messageHandler) {
          await messageHandler(ws, token, data);
        }
      } catch (err) {
        console.warn('[WS] Invalid message data:', err);
      }
    });

    ws.on('close', () => {
      activeWsClients.get(token)?.delete(ws);
      if (activeWsClients.get(token)?.size === 0) {
        activeWsClients.delete(token);
      }
      const tracker = ipTrackers.get(ip);
      if (tracker && tracker.concurrent > 0) {
        tracker.concurrent--;
      }
    });

    ws.on('error', (err) => {
      console.warn('[WS Client Error]', err?.message || err);
    });
  });

  // Heartbeat interval
  if (!heartbeatInterval) {
    heartbeatInterval = setInterval(() => {
      for (const clients of activeWsClients.values()) {
        for (const ws of clients) {
          const client = ws as WebSocket & { isAlive?: boolean };
          if (client.isAlive === false) {
            console.log('[WS Heartbeat] Terminating inactive socket');
            client.terminate();
            continue;
          }
          client.isAlive = false;
          client.ping();
        }
      }
    }, SECURITY.ws.pingIntervalMs);
  }

  return wss;
}
