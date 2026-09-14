/**
 * server/security/config.ts
 * Central security configuration.
 */
const IS_PROD = process.env.NODE_ENV === 'production';

export const SECURITY = {
  isProd: IS_PROD,

  cookies: {
    session: {
      name: 'tg_session_id',
      httpOnly: true,
      secure: IS_PROD,
      sameSite: 'lax' as const,
      path: '/',
      maxAgeMs: parseInt(process.env.SESSION_TTL_DAYS || '30', 10) * 24 * 60 * 60 * 1000,
    },
    csrf: {
      name: 'csrf_token',
      httpOnly: false, // readable by JS (not secret)
      secure: IS_PROD,
      sameSite: 'lax' as const,
      path: '/',
      maxAgeMs: parseInt(process.env.SESSION_TTL_DAYS || '30', 10) * 24 * 60 * 60 * 1000,
    },
    ws: {
      name: 'ws_token',
      httpOnly: true,
      secure: IS_PROD,
      sameSite: 'lax' as const,
      path: '/ws',
      maxAgeMs: 24 * 60 * 60 * 1000,
    },
  },

  ws: {
    strictMode: process.env.WS_STRICT_MODE !== 'false',
    path: '/ws',
    maxPayloadBytes: 64 * 1024,
    messagesPerMinute: 30,
    messagesPerSecond: 5,
    connectionsPerIpPerMinute: 5,
    concurrentPerIp: 10,
    pingIntervalMs: 30_000,
    pongTimeoutMs: 90_000,
  },

  http: {
    rateLimitPerMin: parseInt(process.env.RATE_LIMIT_HTTP_PER_MIN || '120', 10),
    writeRateLimitPerMin: parseInt(process.env.RATE_LIMIT_HTTP_WRITE_PER_MIN || '30', 10),
    bodyLimitBytes: '1mb',
    uploadLimitBytes: 50 * 1024 * 1024, // 50 MB
  },

  origins: {
    allowList: (process.env.ALLOWED_ORIGINS || '')
      .split(',').map(s => s.trim()).filter(Boolean),
    allowLocalhostInDev: !IS_PROD,
  },
} as const;

export const COOKIE_STRICT = process.env.COOKIE_STRICT_MODE !== 'false';
