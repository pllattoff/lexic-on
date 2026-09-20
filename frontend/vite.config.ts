import tailwindcss from '@tailwindcss/vite'
import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

const LOCAL_BACKEND_URL = 'http://localhost:8080'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: {
    proxy: {
      '/api': { target: LOCAL_BACKEND_URL },
      '/oauth2': { target: LOCAL_BACKEND_URL },
      '/login': { target: LOCAL_BACKEND_URL },
      '/logout': { target: LOCAL_BACKEND_URL },
    },
  },
})
