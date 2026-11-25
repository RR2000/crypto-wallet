import vue from '@vitejs/plugin-vue'
import { defineConfig } from 'vitest/config'
import vuetify from 'vite-plugin-vuetify'

// In sviluppo il server di autenticazione gira su :9000 e la web app lo vede su /auth dello stesso origin,
// come in produzione (dove ci pensa il reverse proxy): niente CORS, niente cookie tra domini diversi.
export default defineConfig({
  plugins: [vue(), vuetify({ autoImport: true })],
  server: {
    host: '127.0.0.1',
    port: 5173,
    strictPort: true,
    proxy: { '/auth': { target: 'http://127.0.0.1:9000', changeOrigin: false } },
  },
  test: {
    environment: 'node',
  },
})
