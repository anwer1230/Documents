import tailwindcss from '@tailwindcss/vite';
import react from '@vitejs/plugin-react';
import path from 'path';
import http from 'http';
import { spawn, type ChildProcess } from 'child_process';
import { defineConfig, type Plugin } from 'vite';

let flaskProcess: ChildProcess | null = null;
let isStartingFlask = false;

function ensureFlask(): Promise<boolean> {
  return new Promise((resolve) => {
    if (flaskProcess && !flaskProcess.killed) {
      resolve(true);
      return;
    }
    if (isStartingFlask) {
      setTimeout(() => resolve(true), 1500);
      return;
    }
    isStartingFlask = true;

    const check = http.get('http://127.0.0.1:5000/api/system_health', () => {
      isStartingFlask = false;
      resolve(true);
    });

    check.on('error', () => {
      console.log('🚀 [Vite] Starting Python Flask backend on port 5000...');
      const p = spawn('python3', ['app.py'], {
        cwd: __dirname,
        env: {
          ...process.env,
          PORT: '5000',
          PYTHONUNBUFFERED: '1',
          SESSION_SECRET: process.env.SESSION_SECRET || 'abu_malk_stable_session_secret_2026',
        },
        stdio: 'inherit',
      });
      flaskProcess = p;

      p.on('error', (err) => {
        console.error('❌ [Vite] Failed to start Flask process:', err);
        flaskProcess = null;
        isStartingFlask = false;
        resolve(false);
      });

      p.on('exit', (code) => {
        console.warn(`⚠️ [Vite] Flask process exited with code ${code}`);
        flaskProcess = null;
        isStartingFlask = false;
      });

      let attempts = 0;
      const poll = setInterval(() => {
        attempts++;
        const probe = http.get('http://127.0.0.1:5000/', () => {
          clearInterval(poll);
          isStartingFlask = false;
          resolve(true);
        });
        probe.on('error', () => {
          if (attempts >= 30) {
            clearInterval(poll);
            isStartingFlask = false;
            resolve(false);
          }
        });
      }, 500);
    });
  });
}

function flaskIntegrationPlugin(): Plugin {
  return {
    name: 'flask-integration',
    configureServer(server) {
      ensureFlask();
      server.middlewares.use(async (req, res, next) => {
        const url = req.url || '/';
        if (
          url.startsWith('/@') ||
          url.startsWith('/src/') ||
          url.startsWith('/node_modules/') ||
          url.startsWith('/__vite')
        ) {
          return next();
        }

        await ensureFlask();

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

        proxyReq.on('error', (err) => {
          console.error(`⚠️ [Vite Proxy Error] ${req.method} ${url}:`, err.message);
          if (url.startsWith('/api/') || url.startsWith('/admin/') || url.startsWith('/tools/')) {
            if (!res.headersSent) {
              res.writeHead(503, { 'Content-Type': 'application/json; charset=utf-8' });
              res.end(
                JSON.stringify({
                  success: false,
                  message: 'الخادم الخلفي قيد التشغيل أو التهيئة، يرجى المحاولة بعد لحظات...',
                })
              );
            }
            return;
          }
          if (!res.headersSent && (url === '/' || url.startsWith('/?'))) {
            res.writeHead(200, { 'Content-Type': 'text/html; charset=utf-8' });
            res.end(`<!DOCTYPE html>
<html dir="rtl" lang="ar">
<head>
  <meta charset="utf-8">
  <title>مركز سرعة انجاز - جاري تشغيل الخادم</title>
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <meta http-equiv="refresh" content="2">
  <style>
    body { font-family: system-ui, -apple-system, sans-serif; background: #0b1426; color: #fff; display: flex; align-items: center; justify-content: center; height: 100vh; margin: 0; text-align: center; }
    .box { padding: 32px; background: rgba(255,255,255,0.05); border-radius: 16px; border: 1px solid rgba(255,255,255,0.1); max-width: 400px; width: 90%; }
    .spinner { border: 4px solid rgba(255,255,255,0.1); border-left-color: #3b82f6; border-radius: 50%; width: 44px; height: 44px; animation: spin 1s linear infinite; margin: 0 auto 16px; }
    @keyframes spin { to { transform: rotate(360deg); } }
    h2 { margin: 0 0 8px; font-size: 1.25rem; }
    p { margin: 0; color: rgba(255,255,255,0.7); font-size: 0.95rem; }
  </style>
</head>
<body>
  <div class="box">
    <div class="spinner"></div>
    <h2>جاري تشغيل خادم المنصة...</h2>
    <p>يرجى الانتظار لحظات، سيتم التحميل تلقائياً</p>
  </div>
</body>
</html>`);
            return;
          }
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
      host: '0.0.0.0',
      port: 3000,
      allowedHosts: true,
      cors: true,
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
