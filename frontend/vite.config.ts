import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      "/api": "http://localhost:21001"
    }
  },
  build: {
    outDir: "../src/main/resources/frontend/dist"
  }
})
