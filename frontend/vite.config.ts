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
    },
  }
})
