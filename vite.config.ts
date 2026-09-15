import tailwindcss from '@tailwindcss/vite';
import react from '@vitejs/plugin-react';
import path from 'path';
import http from 'http';
import { spawn } from 'child_process';
import { defineConfig, type Plugin } from 'vite';

let flaskSpawned = false;
function ensureFlask() {
  if (flaskSpawned) return;
  const check = http.get('http://127.0.0.1:5000/', () => {
    flaskSpawned = true;
  });
  check.on('error', () => {
    if (!flaskSpawned) {
      flaskSpawned = true;
      console.log('🚀 [Vite] Starting Python Flask backend on port 5000...');
      const p = spawn('python3', ['app.py'], {
        env: { ...process.env, PORT: '5000' },
        stdio: 'inherit',
      });
      p.on('exit', () => {
        flaskSpawned = false;
      });
    }
  });
}

function flaskIntegrationPlugin(): Plugin {
  return {
    name: 'flask-integration',
    configureServer(server) {
      ensureFlask();
      server.middlewares.use((req, res, next) => {
        const url = req.url || '/';
        if (url.startsWith('/@') || url.startsWith('/src/') || url.startsWith('/node_modules/')) {
          return next();
        }

        const options: http.RequestOptions = {
          hostname: '127.0.0.1',
          port: 5000,
          path: url,
          method: req.method,
          headers: {
            ...req.headers,
            host: '127.0.0.1:5000',
          },
        };

        const proxyReq = http.request(options, (proxyRes) => {
          res.writeHead(proxyRes.statusCode || 200, proxyRes.headers);
          proxyRes.pipe(res, { end: true });
        });

        proxyReq.on('error', () => {
          next();
        });

        req.pipe(proxyReq, { end: true });
      });
    },
  };
}

export default defineConfig(() => {
  return {
    plugins: [react(), tailwindcss(), flaskIntegrationPlugin()],
    resolve: {
      alias: {
        '@': path.resolve(__dirname, '.'),
      },
    },
    server: {
      hmr: process.env.DISABLE_HMR !== 'true',
      watch: process.env.DISABLE_HMR === 'true' ? null : {},
      proxy: {
        '/socket.io': {
          target: 'http://127.0.0.1:5000',
          ws: true,
          changeOrigin: true,
        },
      },
    },
  };
});
