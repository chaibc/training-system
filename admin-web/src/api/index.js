import http from './http'

export const authApi = {
  login: (data) => http.post('/auth/login', data),
  logout: () => http.post('/auth/logout'),
  me: () => http.get('/auth/me'),
  changePassword: (data) => http.post('/auth/change-password', data)
}

export const dashboardApi = {
  get: () => http.get('/admin/dashboard')
}

export const reportApi = {
  overview: (months = 6) => http.get('/admin/reports/overview', { params: { months } }),
  renew: (data) => http.post('/admin/reports/renew', data)
}

export const userApi = {
  page: (params) => http.get('/admin/users/page', { params }),
  create: (data) => http.post('/admin/users', data),
  update: (id, data) => http.put(`/admin/users/${id}`, data),
  resetPassword: (id) => http.post(`/admin/users/${id}/reset-password`),
  toggleStatus: (id) => http.post(`/admin/users/${id}/toggle-status`),
  unbindWechat: (id) => http.post(`/admin/users/${id}/unbind-wechat`),
  roles: () => http.get('/admin/users/roles'),
  updateRole: (roleId, data) => http.put(`/admin/users/roles/${roleId}/permissions`, data),
  pendingBindings: () => http.get('/admin/users/bindings/pending'),
  activeBindings: () => http.get('/admin/users/bindings/active'),
  approveBinding: (id) => http.post(`/admin/users/bindings/${id}/approve`),
  rejectBinding: (id) => http.post(`/admin/users/bindings/${id}/reject`),
  addBinding: (data) => http.post('/admin/users/bindings', data),
  unbindBinding: (id) => http.post(`/admin/users/bindings/${id}/unbind`),
  auditLogs: (params) => http.get('/admin/users/audit-logs', { params }),
  parentOptions: () => http.get('/admin/users/parent-options'),
  studentOptions: () => http.get('/admin/users/student-options')
}

export const studentApi = {
  page: (params) => http.get('/admin/students/page', { params }),
  detail: (id) => http.get(`/admin/students/${id}`),
  create: (data) => http.post('/admin/students', data),
  update: (id, data) => http.put(`/admin/students/${id}`, data),
  adjustHours: (id, data) => http.post(`/admin/students/${id}/adjust-hours`, data),
  options: () => http.get('/admin/students/options')
}

export const enrollmentApi = {
  create: (data) => http.post('/admin/enrollments', data),
  page: (params) => http.get('/admin/enrollments/page', { params }),
  confirm: (id, data) => http.post(`/admin/enrollments/${id}/confirm`, data || {}),
  pay: (id, data) => http.post(`/admin/enrollments/${id}/pay`, data),
  cancel: (id) => http.post(`/admin/enrollments/${id}/cancel`),
  trials: (params) => http.get('/admin/enrollments/trials/page', { params }),
  followUp: (id, data) => http.post(`/admin/enrollments/trials/${id}/follow-up`, data),
  payments: (params) => http.get('/admin/enrollments/payments/page', { params })
}

export const basicApi = {
  categories: '/admin/categories',
  stages: '/admin/stages',
  classTypes: '/admin/class-types',
  classrooms: '/admin/classrooms',
  leaveReasons: '/admin/leave-reasons',
  paymentMethods: '/admin/payment-methods',
  trialSlots: '/admin/trial-slots',
  page: (endpoint, params) => http.get(`${endpoint}/page`, { params }),
  list: (endpoint) => http.get(`${endpoint}/list`),
  create: (endpoint, data) => http.post(endpoint, data),
  update: (endpoint, id, data) => http.put(`${endpoint}/${id}`, data),
  remove: (endpoint, id) => http.delete(`${endpoint}/${id}`)
}

export const courseApi = {
  page: (params) => http.get('/admin/courses/page', { params }),
  list: () => http.get('/admin/courses/list'),
  create: (data) => http.post('/admin/courses', data),
  update: (id, data) => http.put(`/admin/courses/${id}`, data),
  remove: (id) => http.delete(`/admin/courses/${id}`)
}

export const priceApi = {
  page: (params) => http.get('/admin/course-prices/page', { params }),
  create: (data) => http.post('/admin/course-prices', data),
  update: (id, data) => http.put(`/admin/course-prices/${id}`, data),
  remove: (id) => http.delete(`/admin/course-prices/${id}`)
}

export const outlineApi = {
  page: (params) => http.get('/admin/course-outlines/page', { params }),
  list: (courseId) => http.get('/admin/course-outlines/list', { params: { courseId } }),
  create: (data) => http.post('/admin/course-outlines', data),
  update: (id, data) => http.put(`/admin/course-outlines/${id}`, data),
  remove: (id) => http.delete(`/admin/course-outlines/${id}`)
}

export const classApi = {
  page: (params) => http.get('/admin/classes/page', { params }),
  list: () => http.get('/admin/classes/list'),
  create: (data) => http.post('/admin/classes', data),
  update: (id, data) => http.put(`/admin/classes/${id}`, data),
  remove: (id) => http.delete(`/admin/classes/${id}`),
  students: (id) => http.get(`/admin/classes/${id}/students`),
  addStudent: (id, studentId) => http.post(`/admin/classes/${id}/students`, { studentId }),
  removeStudent: (id, linkId) => http.delete(`/admin/classes/${id}/students/${linkId}`)
}

export const lessonApi = {
  page: (params) => http.get('/admin/lessons/page', { params }),
  list: () => http.get('/admin/lessons/list'),
  create: (data) => http.post('/admin/lessons', data),
  update: (id, data) => http.put(`/admin/lessons/${id}`, data),
  remove: (id) => http.delete(`/admin/lessons/${id}`)
}

export const workApi = {
  page: (params) => http.get('/admin/works/page', { params }),
  create: (data) => http.post('/admin/works/upload', data),
  review: (id, status) => http.post(`/admin/works/${id}/review`, { status }),
  update: (id, data) => http.put(`/admin/works/${id}`, data),
  remove: (id) => http.delete(`/admin/works/${id}`)
}

export const uploadApi = {
  image: (file, bizType = 'work') => {
    const form = new FormData()
    form.append('file', file)
    if (bizType) form.append('bizType', bizType)
    return http.post('/upload', form)
  }
}

export const noticeApi = {
  page: (params) => http.get('/admin/notices/page', { params }),
  create: (data) => http.post('/admin/notices', data),
  update: (id, data) => http.put(`/admin/notices/${id}`, data),
  remove: (id) => http.delete(`/admin/notices/${id}`)
}

export const qualificationApi = {
  page: (params) => http.get('/admin/qualifications/page', { params }),
  create: (data) => http.post('/admin/qualifications', data),
  update: (id, data) => http.put(`/admin/qualifications/${id}`, data),
  remove: (id) => http.delete(`/admin/qualifications/${id}`)
}

export const ruleApi = {
  page: (params) => http.get('/admin/rules/page', { params }),
  list: () => http.get('/admin/rules/list'),
  update: (id, data) => http.put(`/admin/rules/${id}`, data)
}

export const teacherApi = {
  page: (params) => http.get('/admin/teachers/page', { params }),
  list: () => http.get('/admin/teachers/list'),
  create: (data) => http.post('/admin/teachers', data),
  update: (id, data) => http.put(`/admin/teachers/${id}`, data),
  remove: (id) => http.delete(`/admin/teachers/${id}`)
}

export const attendanceApi = {
  page: (params) => http.get('/admin/attendance-records/page', { params }),
  list: () => http.get('/admin/attendance-records/list'),
  lessons: (params) => http.get('/admin/workflow/lessons', { params }),
  roster: (id) => http.get(`/admin/workflow/lessons/${id}/roster`),
  rollcall: (id, items) => http.post(`/admin/workflow/lessons/${id}/rollcall`, { items })
}

export const leaveApi = {
  page: (params) => http.get('/admin/leave/page', { params }),
  update: (id, data) => http.put(`/admin/leave/${id}`, data),
  approve: (id) => http.post(`/admin/workflow/leaves/${id}/approve`, { approve: true }),
  reject: (id, reason) => http.post(`/admin/workflow/leaves/${id}/approve`, { approve: false, rejectReason: reason }),
  arrangeMakeup: (id, data) => http.post(`/admin/workflow/leaves/${id}/makeup`, data)
}

export const workAuthApi = {
  page: (params) => http.get('/admin/work-auths/page', { params }),
  record: (data) => http.post('/admin/work-auths/record', data),
  revoke: (id) => http.post(`/admin/work-auths/${id}/revoke`),
  update: (id, data) => http.put(`/admin/work-auths/${id}`, data)
}

export const workCommentApi = {
  page: (params) => http.get('/admin/work-comments/page', { params }),
  update: (id, data) => http.put(`/admin/work-comments/${id}`, data)
}

export const orgApi = {
  info: () => http.get('/admin/org/info'),
  save: (data) => http.put('/admin/org/info', data),
  images: () => http.get('/admin/institution-images/list'),
  createImage: (data) => http.post('/admin/institution-images', data),
  updateImage: (id, data) => http.put(`/admin/institution-images/${id}`, data),
  removeImage: (id) => http.delete(`/admin/institution-images/${id}`)
}

export const qualificationApi2 = qualificationApi
