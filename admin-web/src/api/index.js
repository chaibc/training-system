/**
 * 接口层：按业务模块分组包装后端 REST 接口，页面只调用这里，不直接 import axios。
 *
 * <p>约定：函数名与后端 Controller 语义一致，路径均为 /api 之后的部分（baseURL 已在 http.js 配好）；
 * 返回值已被 http.js 解包为 data，调用方无需再判 code。</p>
 */
import http from './http'

// 认证模块：登录、登出、当前用户、修改密码（后端 /api/auth/**）
export const authApi = {
  // 账号密码登录：返回 token 及用户、角色、权限点（管理端与后台账号共用）
  login: (data) => http.post('/auth/login', data),
  // 退出登录：通知服务端注销当前 token，前端状态由 stores/auth.js 清理
  logout: () => http.post('/auth/logout'),
  // 当前登录用户信息 + 权限点，路由守卫在刷新后用它补全 store
  me: () => http.get('/auth/me'),
  // 修改本人密码，data 为 { oldPassword, newPassword }，原密码错误由后端返回业务异常
  changePassword: (data) => http.post('/auth/change-password', data)
}

// 数据看板模块：首页经营与教务指标汇总
export const dashboardApi = {
  // 看板汇总数据（报名、学员、课时、收入等口径由后端统一计算，前端不再二次统计）
  get: () => http.get('/admin/dashboard')
}

// 统计报表模块（后端 /api/admin/reports/**）
export const reportApi = {
  // 经营概览报表，months 控制统计的月份跨度，默认近 6 个月
  overview: (months = 6) => http.get('/admin/reports/overview', { params: { months } }),
  // 续费分析报表，data 为筛选条件（时间范围、课程等）
  renew: (data) => http.post('/admin/reports/renew', data)
}

// 用户与权限模块：后台账号、角色权限、家长微信绑定审核、审计日志、表单下拉选项
export const userApi = {
  // 后台账号分页查询，params 含页码、关键字、状态等筛选条件
  page: (params) => http.get('/admin/users/page', { params }),
  // 新建后台账号（用户名、姓名、角色、初始密码）
  create: (data) => http.post('/admin/users', data),
  // 修改后台账号信息（姓名、角色、状态等）
  update: (id, data) => http.put(`/admin/users/${id}`, data),
  // 重置指定账号密码：管理员操作，不需要原密码，新密码由后端生成或返回提示
  resetPassword: (id) => http.post(`/admin/users/${id}/reset-password`),
  // 启用/停用账号切换：同一接口在两种状态间翻转，不需要前端传目标状态
  toggleStatus: (id) => http.post(`/admin/users/${id}/toggle-status`),
  // 解绑账号上的微信（家长或教师换号时使用）
  unbindWechat: (id) => http.post(`/admin/users/${id}/unbind-wechat`),
  // 角色列表，含每个角色已分配的权限点，用于权限配置弹窗回显
  roles: () => http.get('/admin/users/roles'),
  // 保存某个角色的权限点集合（整表覆盖）
  updateRole: (roleId, data) => http.put(`/admin/users/roles/${roleId}/permissions`, data),
  // 待审核的微信绑定申请列表
  pendingBindings: () => http.get('/admin/users/bindings/pending'),
  // 已生效的微信绑定关系列表
  activeBindings: () => http.get('/admin/users/bindings/active'),
  // 审核通过绑定申请，通过后家长/教师可用微信登录
  approveBinding: (id) => http.post(`/admin/users/bindings/${id}/approve`),
  // 驳回绑定申请
  rejectBinding: (id) => http.post(`/admin/users/bindings/${id}/reject`),
  // 管理员手工补录绑定关系（家长现场办理时使用）
  addBinding: (data) => http.post('/admin/users/bindings', data),
  // 解除已生效的绑定关系，id 为绑定记录 ID
  unbindBinding: (id) => http.post(`/admin/users/bindings/${id}/unbind`),
  // 后台操作审计日志分页查询
  auditLogs: (params) => http.get('/admin/users/audit-logs', { params }),
  // 家长下拉选项（绑定、报名等表单的家长选择器）
  parentOptions: () => http.get('/admin/users/parent-options'),
  // 学员下拉选项（家长绑定学员、班级加人等场景）
  studentOptions: () => http.get('/admin/users/student-options')
}

// 学员模块：档案 CRUD、课时账户调整、下拉选项
export const studentApi = {
  // 学员分页查询，params 支持关键字、班级、状态等筛选
  page: (params) => http.get('/admin/students/page', { params }),
  // 学员详情，聚合班级、课时账户与上课记录，供详情抽屉展示
  detail: (id) => http.get(`/admin/students/${id}`),
  // 新建学员档案
  create: (data) => http.post('/admin/students', data),
  // 修改学员档案
  update: (id, data) => http.put(`/admin/students/${id}`, data),
  // 调整课时余额（正负均可），后端会写课时流水，属于有副作用的操作
  adjustHours: (id, data) => http.post(`/admin/students/${id}/adjust-hours`, data),
  // 学员下拉选项，用于报名、调班、点名等选择器
  options: () => http.get('/admin/students/options')
}

// 报名与缴费模块：报名申请、试听线索、线下缴费登记
export const enrollmentApi = {
  // 新建报名申请（前台/小程序提交与后台代报名走同一接口）
  create: (data) => http.post('/admin/enrollments', data),
  // 报名分页查询
  page: (params) => http.get('/admin/enrollments/page', { params }),
  // 确认报名：data 可为空对象，后端按默认参数确认（占位、生成待缴费记录）
  confirm: (id, data) => http.post(`/admin/enrollments/${id}/confirm`, data || {}),
  // 登记线下缴费，data 含缴费方式、金额、备注等
  pay: (id, data) => http.post(`/admin/enrollments/${id}/pay`, data),
  // 取消报名，后端按规则处理已缴费用与班位释放
  cancel: (id) => http.post(`/admin/enrollments/${id}/cancel`),
  // 试听线索分页查询
  trials: (params) => http.get('/admin/enrollments/trials/page', { params }),
  // 记录试听线索的跟进结果（沟通内容、下次跟进时间）
  followUp: (id, data) => http.post(`/admin/enrollments/trials/${id}/follow-up`, data),
  // 缴费记录分页查询
  payments: (params) => http.get('/admin/enrollments/payments/page', { params })
}

// 基础字典模块：分类、阶段、班型、教室、请假原因、缴费方式、试听时段
// 这些字典后端结构一致，因此用同一个对象承载通用 CRUD，endpoint 决定具体字典
export const basicApi = {
  // 以下为各字典的后端根路径，页面拿到后直接交给 page/list/create/update/remove 复用
  categories: '/admin/categories',
  stages: '/admin/stages',
  classTypes: '/admin/class-types',
  classrooms: '/admin/classrooms',
  leaveReasons: '/admin/leave-reasons',
  paymentMethods: '/admin/payment-methods',
  trialSlots: '/admin/trial-slots',
  // 字典分页查询
  page: (endpoint, params) => http.get(`${endpoint}/page`, { params }),
  // 字典全量列表，主要用于下拉选择（不分页）
  list: (endpoint) => http.get(`${endpoint}/list`),
  // 新增字典项
  create: (endpoint, data) => http.post(endpoint, data),
  // 修改字典项
  update: (endpoint, id, data) => http.put(`${endpoint}/${id}`, data),
  // 删除字典项，是否被引用由后端校验并给出提示
  remove: (endpoint, id) => http.delete(`${endpoint}/${id}`)
}

// 课程模块（课程主体；价格与大纲见 priceApi / outlineApi）
export const courseApi = {
  // 课程分页查询
  page: (params) => http.get('/admin/courses/page', { params }),
  // 课程全量列表，用于班级、价格、大纲等表单的课程选择
  list: () => http.get('/admin/courses/list'),
  // 新建课程
  create: (data) => http.post('/admin/courses', data),
  // 修改课程
  update: (id, data) => http.put(`/admin/courses/${id}`, data),
  // 删除课程（后端级联删除该课程的价格与大纲）
  remove: (id) => http.delete(`/admin/courses/${id}`)
}

// 课程价格模块：同一课程可按班型、课时包维护多档价格
export const priceApi = {
  // 价格分页查询
  page: (params) => http.get('/admin/course-prices/page', { params }),
  // 新增班型价格
  create: (data) => http.post('/admin/course-prices', data),
  // 修改班型价格
  update: (id, data) => http.put(`/admin/course-prices/${id}`, data),
  // 删除班型价格
  remove: (id) => http.delete(`/admin/course-prices/${id}`)
}

// 课程大纲模块：课程内容章节的维护与排序
export const outlineApi = {
  // 大纲分页查询
  page: (params) => http.get('/admin/course-outlines/page', { params }),
  // 新增大纲阶段
  create: (data) => http.post('/admin/course-outlines', data),
  // 修改大纲阶段（切换显示/隐藏也走这里）
  update: (id, data) => http.put(`/admin/course-outlines/${id}`, data),
  // 删除大纲阶段
  remove: (id) => http.delete(`/admin/course-outlines/${id}`)
}

// 班级模块：班级 CRUD 与班级学员增删
export const classApi = {
  // 班级分页查询
  page: (params) => http.get('/admin/classes/page', { params }),
  // 班级全量列表，用于报名、调课、点名等场景的班级选择
  list: () => http.get('/admin/classes/list'),
  // 新建班级
  create: (data) => http.post('/admin/classes', data),
  // 修改班级
  update: (id, data) => http.put(`/admin/classes/${id}`, data),
  // 删除班级（后端校验是否仍有在读学员或未结束课次）
  remove: (id) => http.delete(`/admin/classes/${id}`),
  // 班级在读学员列表
  students: (id) => http.get(`/admin/classes/${id}/students`),
  // 向班级添加学员，请求体只带 studentId
  addStudent: (id, studentId) => http.post(`/admin/classes/${id}/students`, { studentId }),
  // 将学员移出班级：linkId 是班级-学员关联记录的 ID，不是学员 ID
  removeStudent: (id, linkId) => http.delete(`/admin/classes/${id}/students/${linkId}`)
}

// 课次模块：具体某天的上课记录（由班级周模板生成，也可手工增删改）
export const lessonApi = {
  // 课次分页查询
  page: (params) => http.get('/admin/lessons/page', { params }),
  // 课次列表，用于调课、补课等需要选课次的场景
  list: () => http.get('/admin/lessons/list'),
  // 新建课次（后端校验教师/教室/学员时间冲突）
  create: (data) => http.post('/admin/lessons', data),
  // 修改课次（同样会做冲突校验）
  update: (id, data) => http.put(`/admin/lessons/${id}`, data),
  // 删除课次
  remove: (id) => http.delete(`/admin/lessons/${id}`)
}

// 作品模块：家长提交作品的查询、审核与维护
export const workApi = {
  // 作品分页查询，params 支持关键字与审核状态
  page: (params) => http.get('/admin/works/page', { params }),
  // 作品上传：走作品专用上传接口（会同时落作品记录），不用通用 uploadApi
  create: (data) => http.post('/admin/works/upload', data),
  // 审核作品，status 为审核结果（通过/驳回），只有通过的作品才会对外展示
  review: (id, status) => http.post(`/admin/works/${id}/review`, { status }),
  // 修改作品信息（标题、简介、教师点评等）
  update: (id, data) => http.put(`/admin/works/${id}`, data),
  // 删除作品
  remove: (id) => http.delete(`/admin/works/${id}`)
}

// 通用上传模块：图片等静态资源上传，返回可访问的 URL
export const uploadApi = {
  // 上传单个文件：bizType 让后端按业务分目录存放，默认按作品图处理
  image: (file, bizType = 'work') => {
    const form = new FormData()
    form.append('file', file)
    // 传空字符串可跳过业务类型，由后端落到默认目录
    if (bizType) form.append('bizType', bizType)
    // 用 FormData 提交，浏览器会自动带上 multipart 边界，不能手动设置 Content-Type
    return http.post('/upload', form)
  }
}

// 公告模块：机构公告的发布与维护
export const noticeApi = {
  // 公告分页查询
  page: (params) => http.get('/admin/notices/page', { params }),
  // 新增公告
  create: (data) => http.post('/admin/notices', data),
  // 修改公告（发布/隐藏切换也走这里）
  update: (id, data) => http.put(`/admin/notices/${id}`, data),
  // 删除公告
  remove: (id) => http.delete(`/admin/notices/${id}`)
}

// 机构资质模块：营业执照、办学许可等资质材料的维护
export const qualificationApi = {
  // 资质分页查询
  page: (params) => http.get('/admin/qualifications/page', { params }),
  // 新增资质
  create: (data) => http.post('/admin/qualifications', data),
  // 修改资质（切换小程序展示也走这里）
  update: (id, data) => http.put(`/admin/qualifications/${id}`, data),
  // 删除资质
  remove: (id) => http.delete(`/admin/qualifications/${id}`)
}

// 业务规则模块：请假、课时、报名等规则的读取与修改
export const ruleApi = {
  // 规则分页查询
  page: (params) => http.get('/admin/rules/page', { params }),
  // 规则全量列表：规则总条数很少，规则页一次取完
  list: () => http.get('/admin/rules/list'),
  // 规则一般由后端初始化，前端只改不增删，因此没有 create/remove
  update: (id, data) => http.put(`/admin/rules/${id}`, data)
}

// 教师模块：师资档案维护（教师登录走微信，不在此创建后台账号）
export const teacherApi = {
  // 教师分页查询
  page: (params) => http.get('/admin/teachers/page', { params }),
  // 教师全量列表，用于班级排课与各类教师下拉
  list: () => http.get('/admin/teachers/list'),
  // 新建教师档案
  create: (data) => http.post('/admin/teachers', data),
  // 修改教师档案（启停也走这里）
  update: (id, data) => http.put(`/admin/teachers/${id}`, data),
  // 后端未提供 DELETE /api/admin/teachers/{id}（教师只停用不删除），故不导出 remove
}

// 点名与消课模块：按课次取花名册、提交点名，历史记录单独查询
export const attendanceApi = {
  // 点名记录分页查询（历史查询用）
  page: (params) => http.get('/admin/attendance-records/page', { params }),
  // 点名记录全量列表，点名页用它展示最近记录
  list: () => http.get('/admin/attendance-records/list'),
  // 待点名/已点名课次列表，params 支持日期、班级等筛选
  lessons: (params) => http.get('/admin/workflow/lessons', { params }),
  // 指定课次的学员花名册（含已有出勤状态，用于修正已点过的课）
  roster: (id) => http.get(`/admin/workflow/lessons/${id}/roster`),
  // 提交点名并消课：items 为每个学员的出勤状态；同一课次重复提交走修正逻辑，不会重复扣课时
  rollcall: (id, items) => http.post(`/admin/workflow/lessons/${id}/rollcall`, { items })
}

// 请假审批模块：请假单查询、审批通过/驳回与补课安排
export const leaveApi = {
  // 请假单分页查询
  page: (params) => http.get('/admin/leave/page', { params }),
  // 请假状态统计（指标卡用全量口径，不受分页影响）
  stats: () => http.get('/admin/leave/stats'),
  // 修改请假单（调整原因、时间等）
  update: (id, data) => http.put(`/admin/leave/${id}`, data),
  // 通过请假审批，是否扣课时由后端按请假规则判定
  approve: (id) => http.post(`/admin/workflow/leaves/${id}/approve`, { approve: true }),
  // 驳回请假申请，与通过共用同一个审批接口，靠 approve: false 区分
  reject: (id, reason) => http.post(`/admin/workflow/leaves/${id}/approve`, { approve: false, rejectReason: reason }),
  // 为已批准的请假安排补课，data 含补课课次等信息
  arrangeMakeup: (id, data) => http.post(`/admin/workflow/leaves/${id}/makeup`, data)
}

// 作品授权模块：家长对作品公开展示的授权记录
export const workAuthApi = {
  // 授权记录分页查询
  page: (params) => http.get('/admin/work-auths/page', { params }),
  // 补录授权（线下签署授权书后在系统里登记）
  record: (data) => http.post('/admin/work-auths/record', data),
  // 撤销授权，撤销后对应作品应停止对外展示
  revoke: (id) => http.post(`/admin/work-auths/${id}/revoke`),
  // 修改授权记录（授权范围、授权书地址等）
  update: (id, data) => http.put(`/admin/work-auths/${id}`, data)
}

// 作品评论模块：作品下的评论查询与审核处理
export const workCommentApi = {
  // 评论分页查询
  page: (params) => http.get('/admin/work-comments/page', { params }),
  // 修改评论：审核通过、隐藏都走这个更新接口
  update: (id, data) => http.put(`/admin/work-comments/${id}`, data)
}

// 机构内容模块：机构基本信息与机构图片（环境图、师资展示图）
export const orgApi = {
  // 读取机构资料（名称、地址、联系方式、简介等）
  info: () => http.get('/admin/org/info'),
  // 保存机构资料（整表单提交）
  save: (data) => http.put('/admin/org/info', data),
  // 机构图片列表（含排序），小程序端会按此顺序展示
  images: () => http.get('/admin/institution-images/list'),
  // 新增机构图片
  createImage: (data) => http.post('/admin/institution-images', data),
  // 修改机构图片（图片说明、排序）
  updateImage: (id, data) => http.put(`/admin/institution-images/${id}`, data),
  // 删除机构图片
  removeImage: (id) => http.delete(`/admin/institution-images/${id}`)
}

// 兼容别名：历史代码可能以 qualificationApi2 引用同一对象
export const qualificationApi2 = qualificationApi
