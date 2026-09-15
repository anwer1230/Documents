/**
 * WebSocket Concurrent Connection & Rate Limit Load Test
 * Usage: node tests/load/ws-connections.js [TARGET_URL] [NUM_CLIENTS]
 */

import { WebSocket } from 'ws';

const TARGET_URL = process.argv[2] || 'ws://localhost:3000/api/ws';
const NUM_CLIENTS = parseInt(process.argv[3] || '50', 10);
const DURATION_MS = 10000;

console.log(`Starting WebSocket Load Test against ${TARGET_URL}`);
console.log(`Simulating ${NUM_CLIENTS} concurrent connections for ${DURATION_MS / 1000}s`);

let connected = 0;
let failed = 0;
let messagesReceived = 0;
const clients = [];

for (let i = 0; i < NUM_CLIENTS; i++) {
  try {
    const ws = new WebSocket(TARGET_URL, {
      headers: {
        Origin: 'http://localhost:3000',
      },
    });

    ws.on('open', () => {
      connected++;
      ws.send(JSON.stringify({ type: 'ping' }));
    });

    ws.on('message', () => {
      messagesReceived++;
    });

    ws.on('error', () => {
      failed++;
    });

    clients.push(ws);
  } catch (err) {
    failed++;
  }
}

setTimeout(() => {
  console.log('--- Load Test Results ---');
  console.log(`Connected: ${connected}`);
  console.log(`Failed / Rejected: ${failed}`);
  console.log(`Messages Received: ${messagesReceived}`);

  clients.forEach((c) => {
    try {
      c.close();
    } catch {}
  });

  process.exit(failed > (NUM_CLIENTS * 0.5) ? 1 : 0);
}, DURATION_MS);
