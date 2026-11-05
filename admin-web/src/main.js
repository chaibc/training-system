/**
 * 应用入口：装配 Pinia、Vue Router 与 ant-design-vue 后挂载根组件。
 */
import { createApp } from 'vue'
import { createPinia } from 'pinia'
import Antd from 'ant-design-vue'
import 'ant-design-vue/dist/reset.css'
import App from './App.vue'
import router from './router'
import './styles.css'

// 创建应用实例；以下插件顺序要求 Pinia 在 router 之前，守卫里才能拿到 store
const app = createApp(App)
// 状态管理：stores/auth.js 的登录态依赖它
app.use(createPinia())
// 路由与全局权限守卫，见 router/index.js
app.use(router)
// 全量注册 antd 组件（未做按需引入，打包体积偏大但省去逐个注册）
app.use(Antd)
// 挂载到 index.html 中的 #app 节点
app.mount('#app')
