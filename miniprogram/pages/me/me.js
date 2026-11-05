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

  async loadOverview() {
    try {
      const data = await api.get('/mini/parent/overview', {}, { silent: true });
      const children = (data.children || []).map(view.child);
      if (!children.length) {
        this.setData({ children: [], child: null, pendingBindings: data.pendingBindings || 0, loading: false });
        return;
      }
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

  goLogin() {
    wx.navigateTo({ url: '/pages/login/login' });
  },

  goWorkbench() {
    wx.reLaunch({ url: '/pages/teacher-home/teacher-home' });
  },

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

  goEnrollments() {
    if (!auth.requireParent()) return;
    wx.navigateTo({ url: '/pages/enrollments/enrollments' });
  },

  goLeaveRecords() {
    if (!auth.requireParent()) return;
    wx.navigateTo({ url: '/pages/leave-records/leave-records' });
  },

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
