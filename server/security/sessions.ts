/**
 * server/security/sessions.ts
 * In-memory session store. Replace with Redis/SQLite in production.
 */
import crypto from 'crypto';
import type { Response } from 'express';
import { SECURITY } from './config.js';

export interface SessionRecord {
  token: string;
  userId: string;
  csrfToken: string;
  wsToken: string;
  createdAt: number;
  expiresAt: number;
  lastSeenAt: number;
  userAgent?: string;
  ip?: string;
}

const sessions = new Map<string, SessionRecord>();

export function generateSecureToken(bytes = 32): string {
  return crypto.randomBytes(bytes).toString('hex');
}

export function timingSafeEqual(a: string, b: string): boolean {
  const ba = Buffer.from(a);
  const bb = Buffer.from(b);
  if (ba.length !== bb.length) return false;
  return crypto.timingSafeEqual(ba, bb);
}

export function issueSession(
  res: Response,
  userId: string,
  meta: { userAgent?: string; ip?: string } = {}
): SessionRecord {
  const sessionToken = userId || generateSecureToken(32);
  const csrfToken = generateSecureToken(32);
  const wsToken = generateSecureToken(32);
  const now = Date.now();

  const record: SessionRecord = {
    token: sessionToken,
    userId,
    csrfToken,
    wsToken,
    createdAt: now,
    expiresAt: now + SECURITY.cookies.session.maxAgeMs,
    lastSeenAt: now,
    userAgent: meta.userAgent,
    ip: meta.ip,
  };

  sessions.set(sessionToken, record);

  if (res && typeof res.cookie === 'function') {
    res.cookie(SECURITY.cookies.session.name, sessionToken, {
      httpOnly: SECURITY.cookies.session.httpOnly,
      secure: SECURITY.cookies.session.secure,
      sameSite: SECURITY.cookies.session.sameSite,
      path: SECURITY.cookies.session.path,
      maxAge: SECURITY.cookies.session.maxAgeMs,
    });

    res.cookie(SECURITY.cookies.csrf.name, csrfToken, {
      httpOnly: SECURITY.cookies.csrf.httpOnly,
      secure: SECURITY.cookies.csrf.secure,
      sameSite: SECURITY.cookies.csrf.sameSite,
      path: SECURITY.cookies.csrf.path,
      maxAge: SECURITY.cookies.csrf.maxAgeMs,
    });

    res.cookie(SECURITY.cookies.ws.name, wsToken, {
      httpOnly: SECURITY.cookies.ws.httpOnly,
      secure: SECURITY.cookies.ws.secure,
      sameSite: SECURITY.cookies.ws.sameSite,
      path: SECURITY.cookies.ws.path,
      maxAge: SECURITY.cookies.ws.maxAgeMs,
    });
  }

  return record;
}

export function getSessionByToken(token: string): SessionRecord | null {
  const s = sessions.get(token);
  if (!s) return null;
  if (s.expiresAt < Date.now()) {
    sessions.delete(token);
    return null;
  }
  s.lastSeenAt = Date.now();
  return s;
}

export function getSessionByWsToken(wsToken: string): SessionRecord | null {
  for (const s of sessions.values()) {
    if (s.wsToken === wsToken) {
      if (s.expiresAt < Date.now()) return null;
      return s;
    }
  }
  return null;
}

export function destroySession(res: Response, sessionToken: string): void {
  sessions.delete(sessionToken);
  const clearOpts = {
    httpOnly: SECURITY.cookies.session.httpOnly,
    secure: SECURITY.cookies.session.secure,
    sameSite: SECURITY.cookies.session.sameSite,
    path: SECURITY.cookies.session.path,
  };
  if (res && typeof res.clearCookie === 'function') {
    res.clearCookie(SECURITY.cookies.session.name, clearOpts);
    res.clearCookie(SECURITY.cookies.csrf.name, { ...clearOpts, httpOnly: false });
    res.clearCookie(SECURITY.cookies.ws.name, { ...clearOpts, path: '/ws' });
  }
}

export function clearAllSessions(): void {
  sessions.clear();
}

export function revokeAllSessionsForUser(userId: string): number {
  let n = 0;
  for (const [token, s] of sessions.entries()) {
    if (s.userId === userId) { sessions.delete(token); n++; }
  }
  return n;
}

export function getAllSessions(): SessionRecord[] {
  return Array.from(sessions.values());
}

// Periodic cleanup
setInterval(() => {
  const now = Date.now();
  for (const [token, s] of sessions.entries()) {
    if (s.expiresAt < now) sessions.delete(token);
  }
}, 60 * 60 * 1000);
