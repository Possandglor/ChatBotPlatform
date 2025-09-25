import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  server: {
    port: 3000,
    host: true,
    cors: true,
    proxy: {
      // Сообщения сессий напрямую к Chat Service
      '/api/v1/chat/sessions': {
        target: 'http://localhost:8091',
        changeOrigin: true,
        secure: false
      },
      // Все остальные API запросы через API Gateway
      '/api/v1': {
        target: 'http://localhost:8090',
        changeOrigin: true,
        secure: false,
        configure: (proxy, options) => {
          proxy.on('proxyReq', (proxyReq, req, res) => {
            // Ensure all HTTP methods are supported
            if (req.method === 'PUT' || req.method === 'DELETE') {
              proxyReq.method = req.method;
            }
          });
        }
      }
    }
  },
  build: {
    outDir: 'dist',
    sourcemap: true
  }
})
