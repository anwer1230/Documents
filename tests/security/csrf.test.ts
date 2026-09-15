import { describe, it, expect, beforeEach } from 'vitest';
import request from 'supertest';
import { createTestApp } from '../helpers/app.js';
import { clearAllSessions } from '../../server/security/sessions.js';

describe('Security: CSRF Protection', () => {
  let app: any;

  beforeEach(() => {
    clearAllSessions();
    app = createTestApp();
  });

  it('rejects state-changing requests without CSRF token', async () => {
    const loginRes = await request(app)
      .post('/api/auth/login')
      .send({ token: 'csrf_test_user' });

    const token = loginRes.body.token;

    const res = await request(app)
      .post('/api/protected/mutation')
      .set('Authorization', `Bearer ${token}`)
      .send({ message: 'attack payload' });

    expect(res.status).toBe(403);
    expect(res.body.error.toLowerCase()).toContain('csrf');
  });

  it('rejects state-changing requests with invalid CSRF token', async () => {
    const loginRes = await request(app)
      .post('/api/auth/login')
      .send({ token: 'csrf_test_user_2' });

    const token = loginRes.body.token;

    const res = await request(app)
      .post('/api/protected/mutation')
      .set('Authorization', `Bearer ${token}`)
      .set('X-CSRF-Token', 'invalid_bogus_csrf_token')
      .send({ message: 'attack payload' });

    expect(res.status).toBe(403);
  });

  it('accepts state-changing requests with matching CSRF token', async () => {
    const loginRes = await request(app)
      .post('/api/auth/login')
      .send({ token: 'csrf_test_user_3' });

    const { token, csrfToken } = loginRes.body;

    const res = await request(app)
      .post('/api/protected/mutation')
      .set('Authorization', `Bearer ${token}`)
      .set('X-CSRF-Token', csrfToken)
      .send({ message: 'safe data' });

    expect(res.status).toBe(200);
    expect(res.body.success).toBe(true);
  });
});
