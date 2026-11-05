/**
 * 路由表与全局前置守卫。
 *
 * <p>权限模型：meta.module 对应权限点的模块 key，meta.managerOnly 表示仅 ADMIN 角色可进入。
 * 拦截在 beforeEach 里完成；侧边菜单的显示过滤在 layouts/AdminLayout.vue 中完成，两者需保持一致。</p>
 */
import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import AdminLayout from '../layouts/AdminLayout.vue'
import Login from '../views/Login.vue'
import Dashboard from '../views/Dashboard.vue'
import Users from '../views/Users.vue'
import Students from '../views/Students.vue'
import Enrollments from '../views/Enrollments.vue'
import Courses from '../views/Courses.vue'
import BasicData from '../views/BasicData.vue'
import Reports from '../views/Reports.vue'
import Classes from '../views/Classes.vue'
import Attendance from '../views/Attendance.vue'
import Leave from '../views/Leave.vue'
import Works from '../views/Works.vue'
import Org from '../views/Org.vue'
import Settings from '../views/Settings.vue'

// 路由表：登录页独立，其余业务页都是 AdminLayout 的子路由，共用侧边栏与顶栏
const routes = [
  // 登录页：meta.public 标记为免登录，守卫对其放行
  { path: '/login', name: 'login', component: Login, meta: { public: true, title: '登录' } },
  {
    path: '/',
    // 后台主框架，子路由渲染在其 <router-view /> 中；访问根路径默认进看板
    component: AdminLayout,
    redirect: '/dashboard',
    children: [
      { path: 'dashboard', name: 'dashboard', component: Dashboard,
        // meta.title/sub 供顶栏面包屑与浏览器标题展示，module 用于权限校验
        meta: { title: '数据看板', sub: '经营与教务核心指标 · 口径与课时账户一致', module: 'dashboard' } },
      { path: 'reports', name: 'reports', component: Reports,
        // 统计报表，需要 reports 模块的 view 权限
        meta: { title: '统计报表', sub: '学员、课时、收入、教师、出勤与续费分析', module: 'reports' } },
      { path: 'students', name: 'students', component: Students,
        // 学员管理，需要 students 模块的 view 权限（敏感字段由后端脱敏）
        meta: { title: '学员管理', sub: '档案、班级、课时账户与状态管理 · 敏感信息脱敏展示', module: 'students' } },
      { path: 'enrollments', name: 'enrollments', component: Enrollments,
        // 报名与缴费，需要 enrollments 模块的 view 权限
        meta: { title: '报名与缴费', sub: '报名申请、试听线索与线下缴费登记', module: 'enrollments' } },
      { path: 'courses', name: 'courses', component: Courses,
        // 课程与定价，需要 courses 模块的 view 权限
        meta: { title: '课程与定价', sub: '课程、班型价格与内容大纲维护', module: 'courses' } },
      { path: 'classes', name: 'classes', component: Classes,
        // 班级与排课，需要 classes 模块的 view 权限
        meta: { title: '班级与排课', sub: '周模板排课、冲突校验、调课留痕', module: 'classes' } },
      { path: 'attendance', name: 'attendance', component: Attendance,
        // 点名与消课，需要 attendance 模块的 view 权限（消课会扣课时，谨慎授权）
        meta: { title: '点名与消课', sub: '点名签到、自动消课与修正', module: 'attendance' } },
      { path: 'leave', name: 'leave', component: Leave,
        // 请假审批，需要 leave 模块的 view 权限
        meta: { title: '请假审批', sub: '审批规则、扣课时判定与补课安排', module: 'leave' } },
      { path: 'works', name: 'works', component: Works,
        // 作品管理，需要 works 模块的 view 权限
        meta: { title: '作品管理', sub: '家长授权、内容审核与评论管理', module: 'works' } },
      { path: 'org', name: 'org', component: Org,
        // 机构内容，需要 org 模块的 view 权限
        meta: { title: '机构内容', sub: '机构资料、师资团队、资质与公告维护', module: 'org' } },
      { path: 'basic', name: 'basic', component: BasicData,
        // 基础数据（字典维护），需要 basic 模块的 view 权限
        meta: { title: '基础数据', sub: '课程分类、阶段、班型、教室等基础字典维护', module: 'basic' } },
      { path: 'settings', name: 'settings', component: Settings,
        // 业务规则，需要 settings 模块的 view 权限
        meta: { title: '业务规则', sub: '请假、课时、报名规则与内容维护索引', module: 'settings' } },
      { path: 'users', name: 'users', component: Users,
        // 用户与权限：managerOnly + users 权限点双重限制，非 ADMIN 即使直接改地址也会被挡回看板
        meta: { title: '用户与权限', sub: '仅管理者可进入 · 账号、角色与家长绑定审核', module: 'users', managerOnly: true } }
    ]
  },
  // 未匹配的地址统一回落到看板；若未登录，守卫会再把它转到登录页
  { path: '/:pathMatch(.*)*', redirect: '/dashboard' }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 全局前置守卫：免登录白名单 → 登录态校验 → 补全用户信息 → 管理端/模块权限校验
router.beforeEach(async (to) => {
  const auth = useAuthStore()
  // 公开页面：已登录用户访问登录页直接回看板，避免重复登录
  if (to.meta.public) {
    if (to.path === '/login' && auth.isLoggedIn) return '/dashboard'
    return true
  }
  // 未登录：带上原目标地址，登录成功后页面可回跳
  if (!auth.isLoggedIn) return { path: '/login', query: { redirect: to.fullPath } }
  // 刷新后 store 只剩 localStorage 缓存；若连缓存都没有（如手工清空），用 /auth/me 补全
  if (!auth.user) {
    try {
      await auth.fetchMe()
    } catch {
      // 拉取失败说明 token 已失效：清登录态并带 expired 标记回登录页，由登录页提示过期
      auth.logout()
      return { path: '/login', query: { expired: '1' } }
    }
  }
  // 管理端专属页面（用户与权限）非 ADMIN 一律回看板
  if (to.meta.managerOnly && !auth.isManager) return '/dashboard'
  // 模块权限：没有 view 权限的模块既不出现在菜单里，也禁止通过地址栏直达
  if (to.meta.module && !auth.can(to.meta.module, 'view')) return '/dashboard'
  return true
})

// 路由实例在 main.js 中通过 app.use(router) 注册
export default router
