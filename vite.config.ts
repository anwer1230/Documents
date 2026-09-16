import tailwindcss from '@tailwindcss/vite';
import react from '@vitejs/plugin-react';
import path from 'path';
import http from 'http';
import { spawn, type ChildProcess } from 'child_process';
import { defineConfig, type Plugin } from 'vite';

let flaskProcess: ChildProcess | null = null;
let isStartingFlask = false;
let isFlaskReady = false;

function ensureFlask(): Promise<boolean> {
  if (isFlaskReady && flaskProcess && !flaskProcess.killed) {
    return Promise.resolve(true);
  }
  return new Promise((resolve) => {
    if (flaskProcess && !flaskProcess.killed && isFlaskReady) {
      resolve(true);
      return;
    }
    if (isStartingFlask) {
      setTimeout(() => resolve(isFlaskReady), 1500);
      return;
    }
    isStartingFlask = true;

    const check = http.get('http://127.0.0.1:5000/api/system_health', () => {
      isStartingFlask = false;
      isFlaskReady = true;
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
          GROQ_API_KEY: process.env.GROQ_API_KEY || '',
        },
        stdio: 'inherit',
      });

      flaskProcess = p;

      p.on('error', (err) => {
        console.error('❌ [Vite] Failed to start Flask process:', err);
        flaskProcess = null;
        isStartingFlask = false;
        isFlaskReady = false;
        resolve(false);
      });

      p.on('exit', (code) => {
        console.warn(`⚠️ [Vite] Flask process exited with code ${code}`);
        flaskProcess = null;
        isStartingFlask = false;
        isFlaskReady = false;
      });

      let attempts = 0;
      const poll = setInterval(() => {
        attempts++;
        const probe = http.get('http://127.0.0.1:5000/api/system_health', () => {
          clearInterval(poll);
          isStartingFlask = false;
          isFlaskReady = true;
          resolve(true);
        });
        probe.on('error', () => {
          if (attempts >= 40) {
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

        // دمج أصول Vite و Socket.IO ليتم التعامل معها مباشرة بواسطة خادم Vite ومحول الـ WebSocket
        if (
          url.startsWith('/@') ||
          url.startsWith('/src/') ||
          url.startsWith('/node_modules/') ||
          url.startsWith('/__vite') ||
          url.startsWith('/socket.io')
        ) {
          return next();
        }

        if (!isFlaskReady) {
          await ensureFlask();
        }

        // تنقية Hop-by-Hop headers لمنع تلف اتصالات HTTP Keep-Alive
        const reqHeaders: Record<string, string | string[] | undefined> = { ...req.headers };
        delete reqHeaders['host'];
        delete reqHeaders['connection'];
        delete reqHeaders['keep-alive'];
        delete reqHeaders['upgrade'];
        delete reqHeaders['http2-settings'];
        delete reqHeaders['transfer-encoding'];
        reqHeaders['host'] = '127.0.0.1:5000';

        const options: http.RequestOptions = {
          hostname: '127.0.0.1',
          port: 5000,
          path: url,
          method: req.method,
          headers: reqHeaders,
        };

        const proxyReq = http.request(options, (proxyRes) => {
          const resHeaders = { ...proxyRes.headers };
          delete resHeaders['connection'];
          delete resHeaders['keep-alive'];
          delete resHeaders['transfer-encoding'];
          delete resHeaders['upgrade'];
          res.writeHead(proxyRes.statusCode || 200, resHeaders);
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
            res.end(`<!DOCTYPE html><html dir="rtl" lang="ar"><head>
  <meta charset="utf-8">
  <title>مركز سرعة انجاز - جاري تشغيل الخادم</title>
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <style>
    body { font-family: system-ui, -apple-system, sans-serif; background: #0b1426; color: #fff; display: flex; align-items: center; justify-content: center; height: 100vh; margin: 0; text-align: center; }
    .box { padding: 32px; background: rgba(255,255,255,0.05); border-radius: 16px; border: 1px solid rgba(255,255,255,0.1); max-width: 400px; width: 90%; }
    .spinner { border: 4px solid rgba(255,255,255,0.1); border-left-color: #3b82f6; border-radius: 50%; width: 44px; height: 44px; animation: spin 1s linear infinite; margin: 0 auto 16px; }
    @keyframes spin { to { transform: rotate(360deg); } }
    h2 { margin: 0 0 8px; font-size: 1.25rem; }
    p { margin: 0; color: rgba(255,255,255,0.7); font-size: 0.95rem; }
  </style>
  <script>
    let isTransitioning = false;
    const pollInterval = setInterval(async () => {
      if (isTransitioning) return;
      try {
        const r = await fetch('/api/system_health?_t=' + Date.now());
        if (r.ok) {
          isTransitioning = true;
          clearInterval(pollInterval);
          const pageRes = await fetch('/?_ready=1');
          if (pageRes.ok) {
            const html = await pageRes.text();
            document.open();
            document.write(html);
            document.close();
          }
        }
      } catch(e) {}
    }, 1200);
  </script>
</head>
<body>
  <div class="box">
    <div class="spinner"></div>
    <h2>جاري تشغيل خادم المنصة...</h2>
    <p>يرجى الانتظار لحظات، يتم الاتصال تلقائياً...</p>
  </div>
</body>
</html>`);
            return;
          }
          next();
        });

        req.on('error', (err) => {
          proxyReq.destroy(err);
        });
        res.on('close', () => {
          proxyReq.destroy();
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
      allowedHosts: true as true,
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
