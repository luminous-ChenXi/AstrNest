import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import zhCn from 'element-plus/dist/locale/zh-cn.mjs'
import 'element-plus/dist/index.css'
import App from './App.vue'
import router from './router'
import chenxiFocus from './directives/chenxiFocus'
import lazyAnimate from './directives/lazyAnimate'
import './assets/main.css'

const app = createApp(App)

app.use(createPinia())
app.use(router)
app.use(ElementPlus, { locale: zhCn })
app.directive('chenxi-focus', chenxiFocus)
app.directive('lazy-animate', lazyAnimate)
app.mount('#app')
