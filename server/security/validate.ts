/**
 * server/security/validate.ts
 * Unified input validation & sanitization.
 */
export function isNonEmptyString(v: any, max = 1000): v is string {
  return typeof v === 'string' && v.trim().length > 0 && v.length <= max;
}

export function isSafeId(v: any): v is string {
  return typeof v === 'string' && /^[A-Za-z0-9_\-.:]{1,128}$/.test(v);
}

export function sanitizeText(v: string): string {
  return v.replace(/[\u0000-\u0008\u000B\u000C\u000E-\u001F]/g, '');
}
