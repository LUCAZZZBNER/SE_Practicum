import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
const apiTarget = process.env.VITE_DEV_API_TARGET || 'http://localhost:8081'

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    host: '0.0.0.0',
    proxy: {
      '/api': {
        target: apiTarget,
        changeOrigin: true,
      },
    },
  },
})
