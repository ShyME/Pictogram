import { sentryVitePlugin } from '@sentry/vite-plugin';
import tailwindcss from '@tailwindcss/vite';
import react from '@vitejs/plugin-react';
import { fileURLToPath } from 'node:url';
import { configDefaults, defineConfig } from 'vitest/config';

const resolve = (p: string) => fileURLToPath(new URL(p, import.meta.url));

const BACKEND = 'http://localhost:8080';

// Uploads production source maps to Sentry (ADR-0016); a no-op build locally and in CI,
// where SENTRY_AUTH_TOKEN is unset. Sentry plugin must come after every other plugin.
const shouldUploadSourceMaps = Boolean(process.env.SENTRY_AUTH_TOKEN);

export default defineConfig({
  plugins: [
    react(),
    tailwindcss(),
    ...(shouldUploadSourceMaps
      ? [
          sentryVitePlugin({
            org: process.env.SENTRY_ORG,
            project: process.env.SENTRY_PROJECT,
            authToken: process.env.SENTRY_AUTH_TOKEN,
            sourcemaps: { filesToDeleteAfterUpload: ['./dist/**/*.map'] },
          }),
        ]
      : []),
  ],
  resolve: {
    alias: {
      '@app': resolve('./src/app'),
      '@shared': resolve('./src/shared'),
      '@features': resolve('./src/features'),
      '@test-support': resolve('./test/support'),
    },
  },
  server: {
    proxy: {
      '/api': { target: BACKEND, changeOrigin: true },
      '/oauth2': { target: BACKEND, changeOrigin: true },
      '/login/oauth2': { target: BACKEND, changeOrigin: true },
    },
  },
  build: {
    sourcemap: shouldUploadSourceMaps ? 'hidden' : false,
  },
  test: {
    globals: true,
    environment: 'jsdom',
    setupFiles: ['./test/support/setup.ts'],
    css: true,
    exclude: [...configDefaults.exclude, 'e2e/**'],
  },
});
