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

const routes = [
  { path: '/login', name: 'login', component: Login, meta: { public: true, title: '登录' } },
  {
    path: '/',
    component: AdminLayout,
    redirect: '/dashboard',
    children: [
      { path: 'dashboard', name: 'dashboard', component: Dashboard,
        meta: { title: '数据看板', sub: '经营与教务核心指标 · 口径与课时账户一致', module: 'dashboard' } },
      { path: 'reports', name: 'reports', component: Reports,
        meta: { title: '统计报表', sub: '学员、课时、收入、教师、出勤与续费分析', module: 'reports' } },
      { path: 'students', name: 'students', component: Students,
        meta: { title: '学员管理', sub: '档案、班级、课时账户与状态管理 · 敏感信息脱敏展示', module: 'students' } },
      { path: 'enrollments', name: 'enrollments', component: Enrollments,
        meta: { title: '报名与缴费', sub: '报名申请、试听线索与线下缴费登记', module: 'enrollments' } },
      { path: 'courses', name: 'courses', component: Courses,
        meta: { title: '课程与定价', sub: '课程、班型价格与内容大纲维护', module: 'courses' } },
      { path: 'classes', name: 'classes', component: Classes,
        meta: { title: '班级与排课', sub: '周模板排课、冲突校验、调课留痕', module: 'classes' } },
      { path: 'attendance', name: 'attendance', component: Attendance,
        meta: { title: '点名与消课', sub: '点名签到、自动消课与修正', module: 'attendance' } },
      { path: 'leave', name: 'leave', component: Leave,
        meta: { title: '请假审批', sub: '审批规则、扣课时判定与补课安排', module: 'leave' } },
      { path: 'works', name: 'works', component: Works,
        meta: { title: '作品管理', sub: '家长授权、内容审核与评论管理', module: 'works' } },
      { path: 'org', name: 'org', component: Org,
        meta: { title: '机构内容', sub: '机构资料、师资团队、资质与公告维护', module: 'org' } },
      { path: 'basic', name: 'basic', component: BasicData,
        meta: { title: '基础数据', sub: '课程分类、阶段、班型、教室等基础字典维护', module: 'basic' } },
      { path: 'settings', name: 'settings', component: Settings,
        meta: { title: '业务规则', sub: '请假、课时、报名规则与内容维护索引', module: 'settings' } },
      { path: 'users', name: 'users', component: Users,
        meta: { title: '用户与权限', sub: '仅管理者可进入 · 账号、角色与家长绑定审核', module: 'users', managerOnly: true } }
    ]
  },
  { path: '/:pathMatch(.*)*', redirect: '/dashboard' }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach(async (to) => {
  const auth = useAuthStore()
  if (to.meta.public) {
    if (to.path === '/login' && auth.isLoggedIn) return '/dashboard'
    return true
  }
  if (!auth.isLoggedIn) return { path: '/login', query: { redirect: to.fullPath } }
  if (!auth.user) {
    try {
      await auth.fetchMe()
    } catch {
      auth.logout()
      return { path: '/login', query: { expired: '1' } }
    }
  }
  if (to.meta.managerOnly && !auth.isManager) return '/dashboard'
  if (to.meta.module && !auth.can(to.meta.module, 'view')) return '/dashboard'
  return true
})

export default router
