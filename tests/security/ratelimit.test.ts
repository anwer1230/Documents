import { describe, it, expect } from 'vitest';
import request from 'supertest';
import { createTestApp } from '../helpers/app.js';

describe('Security: HTTP Rate Limiting', () => {
  it('blocks excessive requests after limit is reached with HTTP 429', async () => {
    const app = createTestApp();

    // writeLimiter limit is set to 5 in test helper
    const responses: any[] = [];
    for (let i = 0; i < 7; i++) {
      const res = await request(app).post('/api/write-limited/send');
      responses.push(res);
    }

    const successful = responses.filter((r) => r.status === 200);
    const blocked = responses.filter((r) => r.status === 429);

    expect(successful.length).toBe(5);
    expect(blocked.length).toBe(2);
  });
});
