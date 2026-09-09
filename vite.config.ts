import tailwindcss from '@tailwindcss/vite';
import react from '@vitejs/plugin-react';
import path from 'path';
import module from 'module';
import {defineConfig} from 'vite';

// Guard against ERR_INVALID_ARG_VALUE in Node.js v20+ when Vite plugins invoke createRequire with '.' or relative paths
if (module && typeof module.createRequire === 'function') {
  const origCreateRequire = module.createRequire;
  module.createRequire = function (filenameOrURL: any) {
    if (!filenameOrURL || filenameOrURL === '.' || (typeof filenameOrURL === 'string' && !filenameOrURL.startsWith('file:') && !path.isAbsolute(filenameOrURL))) {
      const safeTarget = typeof filenameOrURL === 'string' && filenameOrURL && filenameOrURL !== '.'
        ? path.resolve(process.cwd(), filenameOrURL)
        : path.resolve(process.cwd(), 'package.json');
      return origCreateRequire.call(this, safeTarget);
    }
    return origCreateRequire.call(this, filenameOrURL);
  };
}

export default defineConfig((async ({ command }: any) => {
  const isBuild = command === 'build';

  const plugins: any[] = [
    react() as any,
    tailwindcss() as any,
  ];

  if (isBuild) {
    const { VitePWA } = await import('vite-plugin-pwa');
    plugins.push(
      VitePWA({
        registerType: 'autoUpdate',
        includeAssets: ['favicon.ico', 'apple-touch-icon.png', 'telegram-logo.svg', 'sql-wasm.wasm', 'sw-custom.js', 'icons/*.png', 'icons/*.svg'],
        manifest: {
          id: '/',
          name: 'Telegram (DrKLO Official Build)',
          short_name: 'Telegram',
          description: 'تطبيق تيليجرام الرسمي المتقدم (Telegram_Anwer) مع دعم الأتمتة والمراقبة والرسائل الفورية.',
          theme_color: '#8A2BE2',
          background_color: '#1E1E2E',
          display: 'standalone',
          orientation: 'portrait-primary',
          start_url: '/',
          scope: '/',
          icons: [
            {
              src: '/icons/icon-192.png',
              sizes: '192x192',
              type: 'image/png',
              purpose: 'any',
            },
            {
              src: '/icons/icon-512.png',
              sizes: '512x512',
              type: 'image/png',
              purpose: 'any',
            },
            {
              src: '/icons/icon-maskable.png',
              sizes: '512x512',
              type: 'image/png',
              purpose: 'maskable',
            },
            {
              src: '/icons/icon-512.svg',
              sizes: '512x512',
              type: 'image/svg+xml',
              purpose: 'any',
            },
          ],
        },
        workbox: {
          skipWaiting: true,
          clientsClaim: true,
          importScripts: ['/sw-custom.js'],
          maximumFileSizeToCacheInBytes: 15 * 1024 * 1024,
          globPatterns: ['**/*.{js,css,html,ico,png,svg,wasm,woff,woff2}'],
          runtimeCaching: [
            {
              urlPattern: /^https:\/\/telegram\.org\/.*/i,
              handler: 'CacheFirst',
              options: {
                cacheName: 'telegram-assets-cache',
                expiration: {
                  maxEntries: 50,
                  maxAgeSeconds: 60 * 60 * 24 * 30, // 30 Days
                },
                cacheableResponse: {
                  statuses: [0, 200],
                },
              },
            },
          ],
        },
        devOptions: {
          enabled: false,
        },
      }) as any
    );
  } else {
    plugins.push({
      name: 'virtual-pwa-register-dev',
      resolveId(id: string) {
        if (id === 'virtual:pwa-register') {
          return '\0virtual:pwa-register';
        }
      },
      load(id: string) {
        if (id === '\0virtual:pwa-register') {
          return 'export function registerSW(options) { return () => {}; }';
        }
      },
    });
  }

  return {
    define: {
      global: 'globalThis',
    },
    plugins,
    resolve: {
      alias: {
        '@': path.resolve(process.cwd(), '.'),
      },
    },
    worker: {
      format: 'es',
    },
    build: {
      target: 'es2020',
      cssCodeSplit: true,
      sourcemap: false,
      chunkSizeWarningLimit: 1200,
      rollupOptions: {
        output: {
          manualChunks(id) {
            // GramJS MTProto core in a dedicated chunk
            if (id.includes('/node_modules/telegram/')) {
              return 'vendor-telegram';
            }
            // SQLite WASM library in a dedicated chunk
            if (id.includes('/node_modules/sql.js/')) {
              return 'vendor-sql';
            }
            // Virtualized lists in a dedicated chunk
            if (id.includes('/node_modules/react-window/')) {
              return 'vendor-react-window';
            }
            // React & React DOM core framework
            if (
              id.includes('/node_modules/react/') ||
              id.includes('/node_modules/react-dom/')
            ) {
              return 'vendor-react';
            }
            // Animation and charts libraries
            if (
              id.includes('/node_modules/motion/') ||
              id.includes('/node_modules/lottie-react/') ||
              id.includes('/node_modules/recharts/')
            ) {
              return 'vendor-animation-charts';
            }
            // Icons
            if (id.includes('/node_modules/lucide-react/')) {
              return 'vendor-icons';
            }
            // Sockets and Network
            if (
              id.includes('/node_modules/socket.io-client/') ||
              id.includes('/node_modules/simple-peer/')
            ) {
              return 'vendor-networking';
            }
          },
        },
      },
    },
    server: {
      // HMR is disabled in AI Studio via DISABLE_HMR env var.
      // Do not modifyâfile watching is disabled to prevent flickering during agent edits.
      hmr: process.env.DISABLE_HMR !== 'true',
      // Disable file watching when DISABLE_HMR is true to save CPU during agent edits.
      watch: process.env.DISABLE_HMR === 'true' ? null : {
        ignored: [
          '**/sessions/**',
          '**/data/**',
          '**/*.sqlite',
          '**/*.sqlite-journal',
          '**/telemetry_logs.json',
          '**/settings.json',
          '**/batches.json',
          '**/fcm_device_tokens.json',
          '**/web_push_subscriptions.json',
          '**/vapid_keys.json',
        ],
      },
    },
  };
}) as any);
