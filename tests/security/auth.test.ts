import { describe, it, expect, beforeEach } from 'vitest';
import request from 'supertest';
import { createTestApp } from '../helpers/app.js';
import { clearAllSessions, issueSession } from '../../server/security/sessions.js';

describe('Security: Authentication & Sessions', () => {
  let app: any;

  beforeEach(() => {
    clearAllSessions();
    app = createTestApp();
  });

  it('rejects unauthenticated requests to protected endpoints', async () => {
    const res = await request(app)
      .get('/api/protected')
      .set('Authorization', 'Bearer invalid_nonexistent_token');

    expect(res.status).toBe(401);
    expect(res.body.error).toBeDefined();
  });

  it('allows access with valid session token', async () => {
    const loginRes = await request(app)
      .post('/api/auth/login')
      .send({ token: 'test_user_session_123' });

    expect(loginRes.status).toBe(200);
    const token = loginRes.body.token;

    const protectedRes = await request(app)
      .get('/api/protected')
      .set('Authorization', `Bearer ${token}`);

    expect(protectedRes.status).toBe(200);
    expect(protectedRes.body.success).toBe(true);
  });

  it('destroys session upon logout', async () => {
    const loginRes = await request(app)
      .post('/api/auth/login')
      .send({ token: 'session_to_logout' });

    const token = loginRes.body.token;

    const logoutRes = await request(app)
      .post('/api/auth/logout')
      .set('Authorization', `Bearer ${token}`);

    expect(logoutRes.status).toBe(200);

    const postLogoutRes = await request(app)
      .get('/api/protected')
      .set('Authorization', `Bearer ${token}`);

    expect(postLogoutRes.status).toBe(401);
  });
});
