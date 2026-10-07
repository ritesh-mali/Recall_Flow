import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  build: {
    rolldownOptions: {
      external: [],
    },
    chunkSizeWarningLimit: 1000,
  },
  optimizeDeps: {
    include: ['recharts', 'react-is'],
  },
  resolve: {
    alias: {},
  },
})
