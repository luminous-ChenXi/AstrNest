import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus, { ElMessage } from 'element-plus'
import zhCn from 'element-plus/dist/locale/zh-cn.mjs'
import 'element-plus/dist/index.css'
import App from './App.vue'
import router from './router'
import chenxiFocus from './directives/chenxiFocus'
import lazyAnimate from './directives/lazyAnimate'
import './assets/main.css'
import './assets/styles/theme.css'

const app = createApp(App)

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
