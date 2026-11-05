/**
 * 应用入口：装配 Pinia、Vue Router 与 ant-design-vue 后挂载根组件。
 */
import { createApp } from 'vue'
import { createPinia } from 'pinia'
import Antd from 'ant-design-vue'
import 'ant-design-vue/dist/reset.css'
import App from './App.vue'
import router from './router'
import { useAuthStore } from './stores/auth'
import './styles.css'

// 创建应用实例；以下插件顺序要求 Pinia 在 router 之前，守卫里才能拿到 store
const app = createApp(App)
// 状态管理：stores/auth.js 的登录态依赖它
const pinia = createPinia()
app.use(pinia)
// 路由与全局权限守卫，见 router/index.js
app.use(router)
// 全量注册 antd 组件（未做按需引入，打包体积偏大但省去逐个注册）
app.use(Antd)

// 把权限判断挂到全局属性上，模板里直接写 v-if="$auth.can('courses','create')"，
// 省去每个页面重复 import store。显式传入 pinia，不依赖「当前激活实例」这种隐式状态。
app.config.globalProperties.$auth = useAuthStore(pinia)

// 挂载到 index.html 中的 #app 节点
app.mount('#app')
