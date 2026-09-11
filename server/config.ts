/**
 * System Fixed Configuration Constants
 * Permanent default values embedded directly in the system.
 */

export const TELEGRAM_API_ID = Number(process.env.TELEGRAM_API_ID || 22043994);
export const TELEGRAM_API_HASH = process.env.TELEGRAM_API_HASH || '56f64582b363d367280db96586b97801';

export const VAPID_PUBLIC_KEY = process.env.VAPID_PUBLIC_KEY || 'BE36BmheMRx2GxzjWpp_4bmXq_hZg55bP_M_vNVysfnjTxns9VCI0hiCHgnRBx0URe_LoxWaAgrS9G9QZbQhOh8';
export const VAPID_PRIVATE_KEY = process.env.VAPID_PRIVATE_KEY || '13NU1_GmeL7bDQcVtlFyuKqsnnsX3XkOyE--2rAQJw4';
export const VAPID_SUBJECT = process.env.VAPID_SUBJECT || 'mailto:anwerfoud80@gmail.com';

export const SYSTEM_CONFIG = {
  TELEGRAM_API_ID,
  TELEGRAM_API_HASH,
  VAPID_PUBLIC_KEY,
  VAPID_PRIVATE_KEY,
  VAPID_SUBJECT,
} as const;

export default SYSTEM_CONFIG;
