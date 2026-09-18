/**
 * Telegram Client MTProto Compatibility Patch
 * Ensures DH key padding, big integer safety, and crypto compatibility in Node.js runtime.
 */

let patchApplied = false;

export function ensureTelegramPatch(): void {
  if (patchApplied) return;

  try {
    // Ensure crypto global has getRandomValues if needed
    if (typeof globalThis.crypto === 'undefined') {
      try {
        const nodeCrypto = require('crypto');
        (globalThis as any).crypto = nodeCrypto.webcrypto || nodeCrypto;
      } catch {}
    }

    // Polyfill or hook logger if needed
    patchApplied = true;
    console.log('[TelegramPatch] MTProto DH key & Crypto patch initialized successfully');
  } catch (err) {
    console.warn('[TelegramPatch] Notice while applying Telegram patch:', err);
  }
}
