import { fileURLToPath, URL } from 'node:url'

import { defineConfig, type ProxyOptions } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'

// Backend (Spring Boot) URL for the dev-server proxy; override with CM_BACKEND_URL.
const backendUrl = process.env.CM_BACKEND_URL ?? 'http://localhost:8080'
const proxyOptions: ProxyOptions = {
  target: backendUrl,
  changeOrigin: true,
  // Proxied calls are same-origin for the browser; dropping Origin keeps the backend's CORS check
  // (meant for direct cross-origin clients) from rejecting writes when Vite runs on another port.
  configure: (proxy) => proxy.on('proxyReq', (req) => req.removeHeader('origin')),
}
const backendProxy = Object.fromEntries(['/api', '/swagger-ui', '/v3/api-docs'].map((path) => [path, proxyOptions]))

// https://vite.dev/config/
export default defineConfig({
  plugins: [vue(), vueDevTools()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  server: { port: 5173, proxy: backendProxy },
  preview: { port: 4173, proxy: backendProxy },
})
