import { resolve } from 'path'
import vue from '@vitejs/plugin-vue'
import { defineConfig } from 'vite'

function pathResolve(dir) {
  return resolve(__dirname, '.', dir)
}

export default defineConfig(({ mode }) => {
  const isProd = mode === 'production'
  
  return {
    plugins: [vue()],
    resolve: {
      alias: {
        '@': resolve(__dirname, 'src'),
      },
    },
    server: {
      port: 5175,
      proxy: {
        '/api': {
          target: 'http://localhost:9201',
          changeOrigin: true,
        },
        '/user': { target: 'http://localhost:9201', changeOrigin: true },
        '/project': { target: 'http://localhost:9201', changeOrigin: true },
        '/project-management': { target: 'http://localhost:9201', changeOrigin: true },
        '/project-config': { target: 'http://localhost:9201', changeOrigin: true },
        '/project-snapshot': { target: 'http://localhost:9201', changeOrigin: true },
        '/project-spec': { target: 'http://localhost:9201', changeOrigin: true },
        '/project-spec-data': { target: 'http://localhost:9201', changeOrigin: true },
        '/files': { target: 'http://localhost:9201', changeOrigin: true },
        '/file-upload': { target: 'http://localhost:9201', changeOrigin: true },
        '/xpt': { target: 'http://localhost:9201', changeOrigin: true },
        '/acrf': { target: 'http://localhost:9201', changeOrigin: true },
        '/p21-spec': { target: 'http://localhost:9201', changeOrigin: true },
        '/export': { target: 'http://localhost:9201', changeOrigin: true },
        '/planInfo': { target: 'http://localhost:9201', changeOrigin: true },
        '/visitConfig': { target: 'http://localhost:9201', changeOrigin: true },
        '/crf': { target: 'http://localhost:9201', changeOrigin: true },
        '/sdrg': { target: 'http://localhost:9201', changeOrigin: true },
      }
    },
    optimizeDeps: {
      include: ['luckyexcel', 'jszip'],
    },
    build: {
      outDir: 'dist',
      assetsDir: 'assets',
      minify: 'terser',
      terserOptions: {
        compress: {
          drop_console: true,
          drop_debugger: true,
        },
      },
    }
  }
})
