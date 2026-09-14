import express from 'express';
import cookieParser from 'cookie-parser';
import cors from 'cors';
import helmet from 'helmet';
import rateLimit from 'express-rate-limit';
import { requireAuth, requireCsrf, checkOrigin } from '../../server/security/middleware.js';
import { issueSession, destroySession, getSessionByToken, clearAllSessions } from '../../server/security/sessions.js';
import { SECURITY } from '../../server/security/config.js';
import { sanitizeText, isNonEmptyString } from '../../server/security/validate.js';

export function createTestApp() {
  const app = express();

  app.use(helmet({
    contentSecurityPolicy: false,
    crossOriginEmbedderPolicy: false,
  }));

  const generalLimiter = rateLimit({
    windowMs: 60 * 1000,
    max: 10, // low max for test verification
    standardHeaders: true,
    legacyHeaders: false,
  });

  const writeLimiter = rateLimit({
    windowMs: 60 * 1000,
    max: 5,
    standardHeaders: true,
    legacyHeaders: false,
  });

  app.use('/api/limited', generalLimiter);
  app.use('/api/write-limited', writeLimiter);
  app.use('/api/', checkOrigin);

  app.use(cors({ origin: true, credentials: true }));
  app.use(express.json({ limit: SECURITY.http.bodyLimitBytes }));
  app.use(cookieParser());

  // Session Token Middleware
  app.use((req, res, next) => {
    let token =
      req.cookies?.[SECURITY.cookies.session.name] ||
      (req.headers['authorization']?.toString().replace(/^Bearer\s+/i, '')) ||
      (req.headers['x-session-token'] as string) ||
      (req.body && typeof req.body === 'object' && (req.body.sessionToken as string)) ||
      (req.query && typeof req.query.sessionToken === 'string' && req.query.sessionToken) ||
      (req.query && typeof req.query.token === 'string' && req.query.token);

    let session = token ? getSessionByToken(token) : null;

    req.session = session || undefined;
    (req as any).sessionToken = token;
    next();
  });

  // Endpoints for testing
  app.get('/api/csrf-token', (req, res) => {
    res.json({ csrfToken: req.session?.csrfToken || '' });
  });

  app.post('/api/auth/login', (req, res) => {
    const { token = 'user_' + Date.now() } = req.body;
    const session = issueSession(res, token);
    res.json({ success: true, token: session.token, csrfToken: session.csrfToken });
  });

  app.post('/api/auth/logout', (req, res) => {
    const token = (req as any).sessionToken;
    if (token) destroySession(res, token);
    res.json({ success: true });
  });

  app.get('/api/protected', requireAuth, (req, res) => {
    res.json({ success: true, user: (req as any).sessionToken });
  });

  app.post('/api/protected/mutation', requireAuth, requireCsrf, (req, res) => {
    res.json({ success: true, data: req.body });
  });

  app.get('/api/limited/ping', (req, res) => {
    res.json({ status: 'ok' });
  });

  app.post('/api/write-limited/send', (req, res) => {
    res.json({ status: 'sent' });
  });

  return app;
}
