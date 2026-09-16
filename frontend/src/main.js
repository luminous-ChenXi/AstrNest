import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus, { ElMessage } from 'element-plus'
import zhCn from 'element-plus/dist/locale/zh-cn.mjs'
import 'element-plus/dist/index.css'
import App from './App.vue'
import router from './router'
import chenxiFocus from './directives/chenxiFocus'
import lazyAnimate from './directives/lazyAnimate'
import logger from './utils/logger.js'
import './assets/main.css'
import './assets/styles/theme.css'

const app = createApp(App)

// 使用环境变量中的站点名称覆盖 index.html 的默认标题
const siteName = import.meta.env.VITE_SITE_NAME
if (siteName) {
  document.title = siteName
}

// 将 logger 挂载到全局
app.config.globalProperties.$logger = logger

app.config.globalProperties.$message = ElMessage

app.use(createPinia())
app.use(router)
app.use(ElementPlus, {
  locale: zhCn,
  zIndex: 11000,
  message: {
    offset: 16,
    grouping: true,
    showClose: true,
  },
})
app.directive('chenxi-focus', chenxiFocus)
app.directive('lazy-animate', lazyAnimate)
app.mount('#app')
