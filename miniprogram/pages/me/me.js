/**
 * 我的（tabBar）：展示身份与学员切换、课时/出勤/请假/报名统计，提供家长功能入口与退出登录。
 * 接口：GET /api/mini/parent/overview；GET /api/public/home（联系机构时兜底取电话）
 * 参数：无
 * 需登录：否；未登录展示游客态，家长专属入口点击时才拦截
 */

const api = require('../../utils/request');
const auth = require('../../utils/auth');
const view = require('../../utils/view');

Page({
  data: {
    user: null,
    userAvatar: '我',
    guest: true,
    isTeacher: false,
    children: [],
    child: null,
    stats: {
      remainingHours: 0,
      attendanceCount: 0,
      pendingLeaves: 0,
      enrollmentCount: 0
    },
    pendingBindings: 0,
    loading: true
  },

  // 每次显示重读登录态并刷新头像；仅家长拉概览，其他人直接结束 loading
  onShow() {
    const user = auth.getUser();
    this.setData({
      user,
      userAvatar: user && user.name ? user.name.slice(0, 1) : '我',
      guest: !auth.isLoggedIn(),
      isTeacher: user && user.roleCode === 'TEACHER'
    });
    if (auth.isLoggedIn() && user.roleCode === 'PARENT') this.loadOverview();
    else this.setData({ loading: false });
  },

  // 家长概览：未绑定学员时清空统计；当前学员失效则回落第一个并写回全局
  async loadOverview() {
    try {
      const data = await api.get('/mini/parent/overview', {}, { silent: true });
      const children = (data.children || []).map(view.child);
      if (!children.length) {
        this.setData({ children: [], child: null, pendingBindings: data.pendingBindings || 0, loading: false });
        return;
      }
      // 多子女切换的核心：以全局当前学员为准，找不到（解绑/新增）时用列表第一条纠正
      const child = children.find(c => c.id === getApp().getChild()) || children[0];
      getApp().setChild(child.id);
      this.setData({
        children,
        child,
        pendingBindings: data.pendingBindings || 0,
        stats: {
          remainingHours: child.remainingHours || 0,
          attendanceCount: child.attendanceCount || 0,
          pendingLeaves: child.pendingLeaves || 0,
          enrollmentCount: child.enrollmentCount || 0
        },
        loading: false
      });
    } catch (error) {
      this.setData({ loading: false });
    }
  },

  // 切换学员：写全局+本地缓存并就地换统计，不重新请求接口；点到当前学员直接返回
  selectChild(e) {
    const id = Number(e.currentTarget.dataset.id);
    if (!this.data.child || id === this.data.child.id) return;
    const child = this.data.children.find(c => c.id === id);
    getApp().setChild(id);
    this.setData({
      child,
      stats: {
        remainingHours: child.remainingHours || 0,
        attendanceCount: child.attendanceCount || 0,
        pendingLeaves: child.pendingLeaves || 0,
        enrollmentCount: child.enrollmentCount || 0
      }
    });
  },

  // 游客点登录
  goLogin() {
    wx.navigateTo({ url: '/pages/login/login' });
  },

  // 教师账号进入教师工作台（reLaunch 清栈）
  goWorkbench() {
    wx.reLaunch({ url: '/pages/teacher-home/teacher-home' });
  },

  // 通用跳转：家长专属页面先 requireParent，避免游客进入后看到空数据
  nav(e) {
    const url = e.currentTarget.dataset.url;
    if (url === '/pages/schedule/schedule' || url === '/pages/hours/hours' || url === '/pages/attendance/attendance') {
      if (!auth.requireParent()) return;
    }
    if (url === '/pages/growth/growth' || url === '/pages/child-profile/child-profile' || url === '/pages/account/account') {
      if (!auth.requireParent()) return;
    }
    wx.navigateTo({ url });
  },

  // 报名记录入口（家长）
  goEnrollments() {
    if (!auth.requireParent()) return;
    wx.navigateTo({ url: '/pages/enrollments/enrollments' });
  },

  // 请假记录入口（家长）
  goLeaveRecords() {
    if (!auth.requireParent()) return;
    wx.navigateTo({ url: '/pages/leave-records/leave-records' });
  },

  // 联系机构：优先用首页缓存的电话，缺失时静默请求 /public/home 补一次并回写 globalData
  async contact() {
    let phone = getApp().globalData.institutionPhone;
    if (!phone) {
      try {
        const data = await api.get('/public/home', {}, { silent: true });
        phone = data.institution && data.institution.phone;
        getApp().globalData.institutionPhone = phone;
      } catch (error) {
        // ignore
      }
    }
    if (!phone) return wx.showToast({ title: '暂未维护联系电话', icon: 'none' });
    wx.makePhoneCall({ phoneNumber: phone.replace(/[^\d]/g, '') });
  },

  // 退出登录：清 Storage 与 globalData、重置页面数据后 reLaunch 回首页（tabBar 不能用 navigateTo）
  async logout() {
    const res = await wx.showModal({
      title: '退出登录',
      content: '退出后仍可浏览公开内容，课表与课时需重新登录查看。',
      confirmText: '退出',
      confirmColor: '#B4473C'
    });
    if (!res.confirm) return;
    auth.clear();
    getApp().globalData.user = null;
    this.setData({ user: null, guest: true, isTeacher: false, children: [], child: null, loading: false });
    wx.reLaunch({ url: '/pages/home/home' });
  }
});
