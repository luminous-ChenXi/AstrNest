import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

function disableDevCachePlugin() {
  return {
    name: 'disable-dev-cache',
    configureServer(server) {
      server.middlewares.use((req, res, next) => {
        if (req.headers['if-none-match']) {
          delete req.headers['if-none-match']
        }
        res.setHeader('Cache-Control', 'no-store, no-cache, must-revalidate')
        res.setHeader('Pragma', 'no-cache')
        res.setHeader('Expires', '0')
        next()
      })
    },
  }
}

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    vue(), 
    disableDevCachePlugin(),
    // 可选：添加样式热更优化（改二次元样式时实时生效）
    {
      name: 'style-hmr-fix',
      handleHotUpdate({ file, server }) {
        if (file.endsWith('.vue') || file.endsWith('.css') || file.endsWith('.scss')) {
          server.ws.send({ type: 'full-reload', path: '*' })
        }
      }
    }
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  server: {
    host: '0.0.0.0', // 允许局域网访问
    port: 5173,
    headers: {
      'Cache-Control': 'no-store, no-cache, must-revalidate',
      Pragma: 'no-cache',
      Expires: '0',
    },
    // allowedHosts：数组格式 + 正确的域名/IP列表
    allowedHosts: [
      'localhost', 
      '127.0.0.1',  // 修正你的 127.0.0., 错误
      '192.168.1.100', 
      'luminouschenxi.net',
      'astrnest.luminouschenxi.net',
      'backend.astrnest.luminouschenxi.net',
      'backend.astrnest.luminouschenxi.net:443' // 加入被拦截的域名
    ]
  },
  // 可选：添加二次元风格资源的别名（比如图片/字体）
  css: {
    preprocessorOptions: {
      // 如果用scss写二次元样式，这里配置全局变量（比如主色、圆角）
      scss: {
        additionalData: `
          @import "@/styles/var.scss"; // 可以定义二次元配色变量
        `
      }
    }
  }
})