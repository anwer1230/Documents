/**
 * server/security/middleware.ts
 * Express middlewares: auth, CSRF, origin.
 */
import type { Request, Response, NextFunction } from 'express';
import { SECURITY } from './config.js';
import {
  getSessionByToken,
  timingSafeEqual,
  type SessionRecord,
} from './sessions.js';

declare global {
  namespace Express {
    interface Request {
      session?: SessionRecord;
    }
  }
}

export function requireAuth(req: Request, res: Response, next: NextFunction) {
  const token =
    req.cookies?.[SECURITY.cookies.session.name] ||
    (req.headers['authorization']?.toString().replace(/^Bearer\s+/i, '')) ||
    '';

  const session = token ? getSessionByToken(token) : null;
  if (!session) {
    return res.status(401).json({ error: 'unauthorized', code: 'NO_SESSION' });
  }
  req.session = session;
  next();
}

export function requireCsrf(req: Request, res: Response, next: NextFunction) {
  const method = req.method.toUpperCase();
  if (['GET', 'HEAD', 'OPTIONS'].includes(method)) return next();

  const session = req.session;
  if (!session) {
    return res.status(401).json({ error: 'unauthorized' });
  }

  const headerToken =
    (req.headers['x-csrf-token']?.toString()) ||
    (req.headers['x-xsrf-token']?.toString()) ||
    '';

  if (!headerToken || !timingSafeEqual(headerToken, session.csrfToken)) {
    console.warn(`[CSRF] Rejected ${method} ${req.originalUrl}`);
    return res.status(403).json({ error: 'csrf_failed' });
  }
  next();
}

export function checkOrigin(req: Request, res: Response, next: NextFunction) {
  const method = req.method.toUpperCase();
  if (['GET', 'HEAD', 'OPTIONS'].includes(method)) return next();

  const origin = (req.headers.origin as string | undefined)?.trim();
  if (!origin) return next(); // same-origin or non-browser client

  // Self-origin
  const proto = (req.headers['x-forwarded-proto'] as string)?.split(',')[0]?.trim()
    || req.protocol;
  const host = (req.headers['x-forwarded-host'] as string)?.split(',')[0]?.trim()
    || req.headers.host;
  if (host && origin === `${proto}://${host}`) return next();

  // Allow list
  if (SECURITY.origins.allowList.includes(origin)) return next();

  // Dev localhost
  if (SECURITY.origins.allowLocalhostInDev &&
      /^https?:\/\/(localhost|127\.0\.0\.1)(:\d+)?$/.test(origin)) {
    return next();
  }

  console.warn(`[Origin] Rejected ${origin} for ${method} ${req.originalUrl}`);
  return res.status(403).json({ error: 'origin_not_allowed' });
}
