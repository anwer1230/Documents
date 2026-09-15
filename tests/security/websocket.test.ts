import { describe, it, expect, beforeAll, afterAll } from 'vitest';
import http from 'http';
import { WebSocket } from 'ws';
import express from 'express';
import { attachWebSocketServer } from '../../server/ws-security.js';
import { issueSession, clearAllSessions } from '../../server/security/sessions.js';

describe('Security: WebSocket Server Hardening', () => {
  let server: http.Server;
  let port: number;

  beforeAll(async () => {
    clearAllSessions();
    const app = express();
    server = http.createServer(app);
    attachWebSocketServer(server);

    await new Promise<void>((resolve) => {
      server.listen(0, '127.0.0.1', () => {
        const address = server.address() as any;
        port = address.port;
        resolve();
      });
    });
  });

  afterAll(async () => {
    await new Promise<void>((resolve) => server.close(() => resolve()));
  });

  it('rejects connection without authentication token with code 4001', async () => {
    const ws = new WebSocket(`ws://127.0.0.1:${port}/ws`);

    const closeCode = await new Promise<number>((resolve) => {
      ws.on('close', (code) => resolve(code));
      ws.on('error', () => {});
    });

    expect(closeCode).toBe(4001);
  });

  it('rejects connection from unauthorized foreign origin with code 4003', async () => {
    // Issue a valid session first
    const mockRes: any = { cookie: () => {} };
    const session = issueSession(mockRes, 'valid_ws_user');

    const ws = new WebSocket(`ws://127.0.0.1:${port}/ws?token=${session.token}`, {
      headers: {
        Origin: 'https://evil-attacker.com',
      },
    });

    const closeCode = await new Promise<number>((resolve) => {
      ws.on('close', (code) => resolve(code));
      ws.on('error', () => {});
    });

    expect(closeCode).toBe(4003);
  });

  it('accepts connection with valid token and authorized origin', async () => {
    const mockRes: any = { cookie: () => {} };
    const session = issueSession(mockRes, 'authorized_ws_user');

    const ws = new WebSocket(`ws://127.0.0.1:${port}/ws?token=${session.token}`, {
      headers: {
        Origin: `http://127.0.0.1:${port}`,
      },
    });

    const isConnected = await new Promise<boolean>((resolve) => {
      ws.on('open', () => {
        ws.close();
        resolve(true);
      });
      ws.on('error', () => resolve(false));
      ws.on('close', () => resolve(false));
    });

    expect(isConnected).toBe(true);
  });
});
