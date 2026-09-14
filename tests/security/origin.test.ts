import { describe, it, expect, beforeEach } from 'vitest';
import request from 'supertest';
import { createTestApp } from '../helpers/app.js';
import { clearAllSessions } from '../../server/security/sessions.js';

describe('Security: Origin Validation', () => {
  let app: any;

  beforeEach(() => {
    clearAllSessions();
    app = createTestApp();
  });

  it('rejects requests from untrusted external origins on state-changing requests', async () => {
    const res = await request(app)
      .post('/api/auth/login')
      .set('Origin', 'https://malicious-attacker-site.com')
      .send({ token: 'test' });

    expect(res.status).toBe(403);
    expect(res.body.error.toLowerCase()).toContain('origin');
  });

  it('allows requests with local or trusted origin', async () => {
    const res = await request(app)
      .post('/api/auth/login')
      .set('Origin', 'http://localhost:3000')
      .send({ token: 'test_origin_ok' });

    expect(res.status).toBe(200);
  });

  it('allows requests without origin header (same-origin / native client)', async () => {
    const res = await request(app)
      .post('/api/auth/login')
      .send({ token: 'test_no_origin' });

    expect(res.status).toBe(200);
  });
});
