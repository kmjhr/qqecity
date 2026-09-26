import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'

// ============================================================
// 管理前端 Vite 配置
// 开发环境：将 /api 代理到后端 8080
// ============================================================
export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, 'src')
    }
  },
  server: {
    port: 5174,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  },
  build: {
    outDir: 'dist',
    sourcemap: false
  }
})
