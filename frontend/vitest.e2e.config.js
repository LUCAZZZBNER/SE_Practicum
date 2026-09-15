import { defineConfig } from 'vitest/config'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  test: {
    environment: 'jsdom',
    environmentOptions: {
      jsdom: { url: process.env.VITE_E2E_ORIGIN || 'http://127.0.0.1:5173/' },
    },
    setupFiles: ['./src/tests/setup.js'],
    include: ['./src/tests/e2e/**/*.spec.js'],
  },
})
