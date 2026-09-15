import { describe, it, expect } from 'vitest';
import { evaluatePassword } from '../../src/components/PasswordStrengthIndicator';

describe('Password Strength Evaluation', () => {
  it('handles empty input gracefully', () => {
    const resAr = evaluatePassword('', true);
    expect(resAr.level).toBe('empty');
    expect(resAr.score).toBe(0);
    expect(resAr.checks.length).toBe(false);

    const resEn = evaluatePassword('', false);
    expect(resEn.level).toBe('empty');
    expect(resEn.score).toBe(0);
  });

  it('identifies very weak / common passwords', () => {
    const res = evaluatePassword('123456', true);
    expect(res.level).toBe('very-weak');
    expect(res.checks.length).toBe(false);
    expect(res.checks.symbol).toBe(false);
  });

  it('evaluates moderate/fair passwords', () => {
    const res = evaluatePassword('password123', false);
    // Has 8+ chars and numbers, but lacks uppercase and special symbols
    expect(res.score).toBeGreaterThanOrEqual(1);
    expect(res.checks.length).toBe(true);
    expect(res.checks.number).toBe(true);
    expect(res.checks.mixedCase).toBe(false);
  });

  it('evaluates strong and very strong passwords with full entropy', () => {
    const strong = evaluatePassword('ComplexP@ssw0rd!2026', false);
    expect(strong.level).toBe('very-strong');
    expect(strong.score).toBe(4);
    expect(strong.checks.length).toBe(true);
    expect(strong.checks.mixedCase).toBe(true);
    expect(strong.checks.number).toBe(true);
    expect(strong.checks.symbol).toBe(true);
  });
});
