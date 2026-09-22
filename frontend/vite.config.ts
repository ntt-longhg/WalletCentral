import path from 'path';
import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import tailwindcss from '@tailwindcss/vite';

// https://vite.dev/config/
export default defineConfig({
  plugins: [react(), tailwindcss()],
  resolve: {
    alias: {
      '@': path.resolve(import.meta.dirname, './src'),
    },
  },
  define: {
    global: 'globalThis',
  },
  server: {
    port: 5173,
    proxy: {
      '/api': {
        configure: (proxy, options) => {
          proxy.on('proxyReq', (proxyReq, req, res) => {
            // Lấy domain hiện tại từ header 'host' (ví dụ: ://domain.com hoặc localhost:5173)
            const host = req.headers.host;
            // Xác định giao thức http hay https
            const protocol = req.headers['x-forwarded-proto'] || 'http';

            // Tạo ra chuỗi tương đương window.location.origin
            const origin = `${protocol}://${host}`;

            // Gán đè target cho request này (Ví dụ backend chạy cùng domain nhưng khác cổng hoặc route)
            // Lưu ý: Nếu backend chạy hoàn toàn cùng cổng/domain với frontend thì không cần proxy.
            // Đoạn này cấu hình nếu bạn muốn hướng target về chính domain đó:
            options.target = origin;
          });
        },
        changeOrigin: true,
      },
      '/ws': {
        configure: (proxy, options) => {
          proxy.on('proxyReq', (proxyReq, req, res) => {
            const host = req.headers.host;
            const protocol = req.headers['x-forwarded-proto'] || 'http';
            const origin = `${protocol}://${host}`;
            options.target = origin;
          });
        },
        changeOrigin: true,
        ws: true,
      },
    },
  },
});
