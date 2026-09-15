/**
 * System Fixed Configuration Constants
 * Permanent default values embedded directly into the system.
 */

export const NODE_ENV = process.env.NODE_ENV || 'production';
export const NODE_VERSION = '22.14.0';
export const TELEGRAM_API_ID = Number(process.env.TELEGRAM_API_ID || 22043994);
export const TELEGRAM_API_HASH = process.env.TELEGRAM_API_HASH || '56f64582b363d367280db96586b97801';
export const VAPID_PUBLIC_KEY = process.env.VAPID_PUBLIC_KEY || 'BE36BmheMRx2GxzjWpp_4bmXq_hZg55bP_M_vNVysfnjTxns9VCI0hiCHgnRBxOURe_LoxWaAgrS9G9QZbQhOh8';
export const VAPID_PRIVATE_KEY = process.env.VAPID_PRIVATE_KEY || '13NU1_GmeL7bDQcVtlFyuKqsnnsX3Xk0yE--2rAQJw4';
export const VAPID_SUBJECT = process.env.VAPID_SUBJECT || 'mailto:anwerfoud80@gmail.com';
export const WS_STRICT_MODE = process.env.WS_STRICT_MODE ? process.env.WS_STRICT_MODE === 'true' : true;
export const COOKIE_STRICT_MODE = process.env.COOKIE_STRICT_MODE ? process.env.COOKIE_STRICT_MODE === 'true' : true;
export const SESSION_TTL_DAYS = Number(process.env.SESSION_TTL_DAYS || 30);
export const RATE_LIMIT_HTTP_PER_MIN = Number(process.env.RATE_LIMIT_HTTP_PER_MIN || 120);
export const RATE_LIMIT_HTTP_WRITE_PER_MIN = Number(process.env.RATE_LIMIT_HTTP_WRITE_PER_MIN || 30);
export const LOG_LEVEL = process.env.LOG_LEVEL || 'info';

export const SYSTEM_CONFIG = {
  NODE_ENV,
  NODE_VERSION,
  TELEGRAM_API_ID,
  TELEGRAM_API_HASH,
  VAPID_PUBLIC_KEY,
  VAPID_PRIVATE_KEY,
  VAPID_SUBJECT,
  WS_STRICT_MODE,
  COOKIE_STRICT_MODE,
  SESSION_TTL_DAYS,
  RATE_LIMIT_HTTP_PER_MIN,
  RATE_LIMIT_HTTP_WRITE_PER_MIN,
  LOG_LEVEL,
} as const;

export default SYSTEM_CONFIG;
