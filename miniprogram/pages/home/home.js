/**
 * 首页（tabBar）：机构介绍、课程/教师/作品/公告等公开内容；家长登录后额外展示学员概览与待办角标。
 * 接口：GET /api/public/home（公开聚合）；GET /api/mini/parent/overview（家长概览，silent）
 * 参数：无
 * 需登录：否；只有家长会请求 overview
 */

const api = require('../../utils/request');
const auth = require('../../utils/auth');
const view = require('../../utils/view');
const req = require('../../utils/request');

Page({
  data: {
    loading: true,
    institution: null,
    courses: [],
    teachers: [],
    works: [],
    notices: [],
    qualifications: [],
    images: [],
    user: null,
    greeting: '您好',
    guest: true,
    isTeacher: false,
    child: null,
    pendingLeaves: 0,
    pendingBindings: 0
  },

  // 首次进入只拉公开聚合内容；家长概览放在 onShow，保证登录返回后能补上
  onLoad() {
    this.loadPublic();
  },

  // 每次显示都重算游客/教师态；家长额外拉 overview（学员、待审批请假、待处理绑定）
  onShow() {
    const user = auth.getUser();
    this.setData({
      user,
      guest: !auth.isLoggedIn(),
      isTeacher: user && user.roleCode === 'TEACHER'
    });
    if (auth.isLoggedIn() && user.roleCode === 'PARENT') this.loadOverview();
  },

  // 下拉同时刷新公开内容与概览；未登录时 overview 会 401 但 silent，不影响首页；两者结束才收起动画
  onPullDownRefresh() {
    Promise.all([this.loadPublic(), this.loadOverview()]).finally(() => wx.stopPullDownRefresh());
  },

  // 公开首页聚合：机构信息、课程、教师、作品（只取前 4 条）、公告、资质
  async loadPublic() {
    try {
      const data = await api.get('/public/home', {}, { silent: true });
      const inst = data.institution || null;
      const images = (inst && inst.images ? inst.images : []).map((img, index) => ({
        ...img,
        // 环境图片补全为绝对地址；占位字与主题色按序号循环取，纯展示用
        url: req.fullUrl(img.imageUrl),
        char: ['室', '帖', '作'][index % 3],
        color: ['#6B503A', '#4C5544', '#76524A'][index % 3]
      }));
      this.setData({
        institution: inst,
        courses: (data.courses || []).map(view.course),
        teachers: (data.teachers || []).map(t => ({
          ...t,
          avatarChar: (t.name || '师').slice(0, 1),
          styleText: t.styles || t.coursesText || ''
        })),
        works: (data.works || []).map(view.work).slice(0, 4),
        notices: data.notices || [],
        qualifications: data.qualifications || [],
        images,
        loading: false
      });
      // 机构电话缓存到 globalData，其他页面「联系机构」可直接用，避免重复请求
      if (inst && inst.phone) getApp().globalData.institutionPhone = inst.phone;
    } catch (error) {
      this.setData({ loading: false });
    }
  },

  // 家长概览：按小时生成问候语；当前学员优先用全局值，失效则回落第一个并写回全局
  async loadOverview() {
    try {
      const data = await api.get('/mini/parent/overview', {}, { silent: true });
      const hour = new Date().getHours();
      const greeting = hour < 11 ? '早上好' : hour < 14 ? '中午好' : hour < 18 ? '下午好' : '晚上好';
      const child = data.children && data.children.length
        ? view.child(data.children.find(c => c.id === getApp().getChild()) || data.children[0]) : null;
      // 学员列表变化（解绑/新增）后把全局当前学员纠正为有效值
      if (child) getApp().setChild(child.id);
      this.setData({
        greeting,
        child,
        pendingLeaves: data.pendingLeaves || 0,
        pendingBindings: data.pendingBindings || 0
      });
    } catch (error) {
      // 静默失败，不影响公开首页
    }
  },

  // 跳课程列表 tab
  goCourses() {
    wx.switchTab({ url: '/pages/courses/courses' });
  },

  // 跳作品墙 tab
  goWorks() {
    wx.switchTab({ url: '/pages/works/works' });
  },

  // 跳试听预约页（公开表单）
  goTrial() {
    wx.navigateTo({ url: '/pages/trial/trial' });
  },

  // 跳教师列表页
  goTeachers() {
    wx.navigateTo({ url: '/pages/teachers/teachers' });
  },

  // 跳机构介绍页
  goAbout() {
    wx.navigateTo({ url: '/pages/about/about' });
  },

  // 跳登录页
  goLogin() {
    wx.navigateTo({ url: '/pages/login/login' });
  },

  // 按 data-id 进课程详情
  goCourse(e) {
    wx.navigateTo({ url: '/pages/course-detail/course-detail?id=' + e.currentTarget.dataset.id });
  },

  // 按 data-id 进教师详情
  goTeacher(e) {
    wx.navigateTo({ url: '/pages/teacher-detail/teacher-detail?id=' + e.currentTarget.dataset.id });
  },

  // 按 data-id 进作品详情
  goWork(e) {
    wx.navigateTo({ url: '/pages/work-detail/work-detail?id=' + e.currentTarget.dataset.id });
  },

  // 家长功能入口：统一先过 requireParent，游客/教师会被提示并拦住
  goSchedule() {
    if (!auth.requireParent()) return;
    wx.navigateTo({ url: '/pages/schedule/schedule' });
  },

  // 课时账户（家长）
  goHours() {
    if (!auth.requireParent()) return;
    wx.navigateTo({ url: '/pages/hours/hours' });
  },

  // 报名记录（家长）
  goEnrollments() {
    if (!auth.requireParent()) return;
    wx.navigateTo({ url: '/pages/enrollments/enrollments' });
  },

  // 请假记录（家长）
  goLeaveRecords() {
    if (!auth.requireParent()) return;
    wx.navigateTo({ url: '/pages/leave-records/leave-records' });
  },

  // 教师入口：reLaunch 清空页面栈进工作台，避免返回到家长首页
  goWorkbench() {
    wx.reLaunch({ url: '/pages/teacher-home/teacher-home' });
  },

  // 公告弹窗：按 data-index 取公告标题与正文，只读展示
  showNotice(e) {
    const notice = this.data.notices[e.currentTarget.dataset.index];
    if (!notice) return;
    wx.showModal({
      title: notice.title,
      content: notice.content || '',
      showCancel: false,
      confirmText: '知道了'
    });
  },

  // 拨号：机构未维护电话时只提示；调用前去掉号码里的非数字字符
  call() {
    const phone = this.data.institution && this.data.institution.phone;
    if (!phone) {
      wx.showToast({ title: '暂未维护联系电话', icon: 'none' });
      return;
    }
    wx.makePhoneCall({ phoneNumber: phone.replace(/[^\d]/g, '') });
  },

  // 打开地图：缺少经纬度时不调起地图，避免定位到 (0,0)
  openMap() {
    const inst = this.data.institution;
    if (!inst || !inst.latitude || !inst.longitude) {
      wx.showToast({ title: '暂未维护地图坐标', icon: 'none' });
      return;
    }
    wx.openLocation({
      latitude: Number(inst.latitude),
      longitude: Number(inst.longitude),
      name: inst.name || '翰墨学堂',
      address: inst.address || '',
      scale: 16
    });
  },

  // 作品有图直接系统预览，无图则跳作品详情
  previewWork(e) {
    const item = this.data.works[e.currentTarget.dataset.index];
    // data-index 可能越界（列表刷新后旧节点仍被点击），先判空再取字段，避免 item.id 抛 TypeError
    if (!item) return;
    if (item.image) {
      wx.previewImage({ current: item.image, urls: [item.image] });
      return;
    }
    wx.navigateTo({ url: '/pages/work-detail/work-detail?id=' + item.id });
  },

  // 分享首页
  onShareAppMessage() {
    return {
      title: '翰墨学堂 · 一笔一画，写好中国字',
      path: '/pages/home/home'
    };
  }
});
