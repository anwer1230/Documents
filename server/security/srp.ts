/**
 * server/security/srp.ts
 * SRP helper for 2FA password handling with secure-remote-password.
 */
import crypto from 'crypto';
import srpClient from 'secure-remote-password/client.js';

export function hashPassword(password: string): string {
  const salt = crypto.randomBytes(16).toString('hex');
  const hash = crypto.scryptSync(password, salt, 64).toString('hex');
  return `${salt}:${hash}`;
}

export function verifyPasswordHash(password: string, combinedHash: string): boolean {
  try {
    const [salt, key] = combinedHash.split(':');
    if (!salt || !key) return false;
    const keyBuffer = Buffer.from(key, 'hex');
    const derivedBuffer = crypto.scryptSync(password, salt, 64);
    return crypto.timingSafeEqual(keyBuffer, derivedBuffer);
  } catch {
    return false;
  }
}

export function computeSrp(params: {
  password: string;
  srpB: string;
  srpId: bigint | number;
  algo: any;
}) {
  const salt = params.algo?.salt ? Buffer.from(params.algo.salt).toString('hex') : '';
  const clientEphemeral = srpClient.generateEphemeral();
  const privateKey = srpClient.derivePrivateKey(salt, 'telegram', params.password);
  
  let session: any = null;
  try {
    session = srpClient.deriveSession(
      clientEphemeral.secret,
      params.srpB,
      salt,
      'telegram',
      privateKey
    );
  } catch (err) {
    console.warn('[SRP] Session derivation fallback:', err);
  }

  return {
    A: clientEphemeral.public,
    M1: session?.proof || '',
  };
}
