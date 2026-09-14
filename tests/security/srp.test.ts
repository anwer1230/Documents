import { describe, it, expect } from 'vitest';
import { computeSrp, hashPassword, verifyPasswordHash } from '../../server/security/srp.js';

describe('Security: SRP and Password Verification', () => {
  it('hashes passwords with salt and verifies successfully', () => {
    const pwd = 'CorrectHorseBatteryStaple#2026';
    const hash = hashPassword(pwd);

    expect(hash).toContain(':');
    expect(verifyPasswordHash(pwd, hash)).toBe(true);
    expect(verifyPasswordHash('WrongPassword', hash)).toBe(false);
  });

  it('computes SRP parameters safely without throwing', () => {
    const res = computeSrp({
      password: 'MySecretPassword123!',
      srpB: '0123456789abcdef0123456789abcdef',
      srpId: 1001,
      algo: {
        g: 3,
        p: 'c71caeb9c6b1c9048e6c522f70f13f73980d40238e3e21c14934d037563d930f48198a0aa7c14058229493d22530f4dbfa336f6e0ac925139543ced40779804ffffffffffffffffff',
        salt1: 'salt1hexvalue',
        salt2: 'salt2hexvalue',
      },
    });

    expect(res).toBeDefined();
    expect(res.A).toBeDefined();
    expect(res.M1).toBeDefined();
  });
});
