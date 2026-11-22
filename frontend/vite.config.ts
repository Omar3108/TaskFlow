import tailwindcss from '@tailwindcss/vite'
import react from '@vitejs/plugin-react'
import { loadEnv } from 'vite'
import { defineConfig } from 'vitest/config'

// https://vite.dev/config/
export default defineConfig(({ mode }) => {
  // Read the repository-level .env (one directory up), shared with docker-compose and the API.
  const env = loadEnv(mode, '..', '')

  return {
    plugins: [react(), tailwindcss()],
    server: {
      // The browser only ever talks to the Vite origin; /api is forwarded to the backend.
      // Same-origin requests mean no CORS preflight and a first-party refresh-token cookie,
      // exactly like production, where the host rewrites /api to the API server.
      proxy: {
        '/api': {
          target: env.VITE_API_PROXY_TARGET ?? 'http://localhost:8080',
          changeOrigin: true,
        },
      },
    },
    test: {
      environment: 'jsdom',
      setupFiles: './src/test/setup.ts',
      css: false,
      // `npm run test:coverage`. V8 measures the code as the engine runs it, with no
      // instrumentation step. The minimums sit a little below the current values (about 82% of
      // lines, 72% of branches), so they catch untested features rather than noise. Dragging is
      // not measurable in jsdom; the Playwright tests in /e2e cover it.
      coverage: {
        provider: 'v8',
        include: ['src/**'],
        exclude: ['src/**/*.test.{ts,tsx}', 'src/test/**', 'src/main.tsx'],
        reporter: ['text-summary', 'html'],
        thresholds: { lines: 75, statements: 75, functions: 75, branches: 65 },
      },
    },
  }
})
