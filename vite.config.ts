import tailwindcss from '@tailwindcss/vite';
import react from '@vitejs/plugin-react';
import path from 'path';
import {defineConfig} from 'vite';
import settingsSearchPlugin from './plugins/settingsSearchPlugin';

export default defineConfig(() => {
  return {
    plugins: [react(), tailwindcss(), settingsSearchPlugin(__dirname)],
    build: {
      sourcemap: true,
      chunkSizeWarningLimit: 1000,
      rollupOptions: {
        output: {
          manualChunks(id) {
            if (id.includes('node_modules')) {
              if (id.includes('react') || id.includes('scheduler') || id.includes('motion')) {
                return 'vendor-ui';
              }
              if (id.includes('lucide-react')) {
                return 'vendor-icons';
              }
              if (id.includes('lottie-web')) {
                return 'vendor-lottie';
              }
              if (id.includes('tesseract.js')) {
                return 'vendor-ocr';
              }
            }
          },
        },
        onwarn(warning, warn) {
          if (warning.code === 'EVAL' && warning.id?.includes('lottie-web')) {
            return;
          }
          warn(warning);
        },
      },
    },
    resolve: {
      alias: {
        '@': path.resolve(__dirname, '.'),
      },
    },
    server: {
      // HMR is disabled in AI Studio via DISABLE_HMR env var.
      // Do not modifyâfile watching is disabled to prevent flickering during agent edits.
      hmr: process.env.DISABLE_HMR !== 'true',
      // Disable file watching when DISABLE_HMR is true to save CPU during agent edits.
      watch: process.env.DISABLE_HMR === 'true' ? null : {},
    },
  };
});
