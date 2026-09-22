import vue from '@vitejs/plugin-vue'
import { defineConfig } from 'vite'
import { fileURLToPath, URL } from 'node:url'

// https://vite.dev/config/
export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  server: {
    // 固定绑 IPv4：不写这行，Vite 可能只监听 ::1，浏览器用 127.0.0.1 会打不开。
    // 想让同局域网的同学访问，启动时加 --host，即 npm run dev -- --host
    host: '127.0.0.1',
    port: 5173,
    // 开发时把 /api 转发到网关，前端代码里只写 /api/xxx 就行，不用管跨域
    proxy: {
      '/api': {
        target: 'http://127.0.0.1:9090',
        changeOrigin: true,
      },
    },
  },
})
