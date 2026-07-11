import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
// 全局主题样式层：必须在 element-plus 默认样式之后引入，主色/圆角覆盖才会生效
import './styles/theme.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import router from './router/index.js'
import App from './App.vue'

const app = createApp(App)

// 挂载状态管理（Pinia）—— 必须在 router 之前，路由守卫里会用到 store
app.use(createPinia())
// 挂载路由
app.use(router)
// 挂载 Element Plus 组件库
app.use(ElementPlus)

// 全局注册所有 Element Plus 图标，菜单/按钮里可直接使用
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

app.mount('#app')
