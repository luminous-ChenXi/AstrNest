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
  // ============================================
  // [CDN 配置 1] 设置基础路径 - 部署到 CDN 时必须修改
  // 作用：所有资源引用都会加上这个前缀
  // 示例：'https://cdn.example.com/' 或 'https://your-bucket.oss-cn-beijing.aliyuncs.com/'
  // 注意：末尾必须带斜杠 /
  // ============================================
  // 从环境变量读取，默认为根路径
  base: process.env.VITE_ASSETS_BASE_URL || '/',
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
    port: 5175,
    headers: {
      'Cache-Control': 'no-store, no-cache, must-revalidate',
      Pragma: 'no-cache',
      Expires: '0',
    },
    proxy: {
      // 开发环境同源代理：前端默认走同源相对路径（VITE_API_BASE_URL 为空时），
      // 由 dev server 转发到本地后端；生产环境由 nginx 反代 /api。
      '/api': {
        target: process.env.VITE_PROXY_TARGET || 'http://localhost:8081',
        changeOrigin: true,
      },
    },
    // 开发服务器允许的主机名。自部署/内网访问时，
    // 可通过环境变量 VITE_ALLOWED_HOSTS 追加（逗号分隔），例如：
    //   VITE_ALLOWED_HOSTS="192.168.1.20,example.com"
    allowedHosts: [
      'localhost',
      '127.0.0.1',
      ...(process.env.VITE_ALLOWED_HOSTS
        ? process.env.VITE_ALLOWED_HOSTS.split(',').map((h) => h.trim()).filter(Boolean)
        : []),
    ],
  },
  build: {
    target: 'es2020',
    sourcemap: false,
    cssCodeSplit: true,
    manifest: true,
    chunkSizeWarningLimit: 800,
    assetsInlineLimit: 4096,
    reportCompressedSize: true,
    rollupOptions: {
      output: {
        manualChunks: {
          'vendor-core': ['vue', 'vue-router', 'pinia'],
          'vendor-ui': ['element-plus', '@element-plus/icons-vue']
        },
        // ============================================
        // [CDN 配置 2] 资源文件名配置（已配置好，无需修改）
        // 作用：确保资源文件有 hash，适合 CDN 长期缓存
        // ============================================
        entryFileNames: 'assets/[name]-[hash].js',
        chunkFileNames: 'assets/[name]-[hash].js',
        assetFileNames: 'assets/[name]-[hash][extname]'
      }
    },
    esbuild: {
      drop: process.env.NODE_ENV === 'production' ? ['console', 'debugger'] : []
    }
  },
  optimizeDeps: {
    include: [
      'vue',
      'vue-router',
      'pinia',
      'element-plus',
      '@element-plus/icons-vue'
    ]
  },
  assetsInclude: ['**/*.png', '**/*.jpg', '**/*.jpeg', '**/*.gif', '**/*.webp', '**/*.svg']
})
