import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    // §4.5 design_us15_us17.md — FE và API phải same-site để cookie lv_pb
    // (HttpOnly) được gửi kèm request của thẻ <video>.
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
})
