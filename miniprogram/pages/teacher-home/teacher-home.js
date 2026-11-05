const api = require('../../utils/request');
const auth = require('../../utils/auth');
const view = require('../../utils/view');
const fmt = require('../../utils/format');

Page({
  data: {
    loading: true,
    blocked: false,
    teacher: null,
    today: '',
    todayLessons: [],
    weekLessons: [],
    pendingLeaves: [],
    pendingLeaveCount: 0,
    pendingWorks: 0,
    classCount: 0,
    studentCount: 0
  },

  onLoad() {
    if (!auth.requireTeacher()) {
      this.setData({ blocked: true, loading: false });
      return;
    }
    this.load();
  },

  onShow() {
    if (this.data.teacher && !this.data.loading) this.load(true);
  },

  onPullDownRefresh() {
    this.load().finally(() => wx.stopPullDownRefresh());
  },

  async load(silent) {
    if (!silent) this.setData({ loading: true });
    try {
      const [data, leaves] = await Promise.all([
        api.get('/mini/teacher/overview'),
        api.get('/mini/teacher/leaves/pending', {}, { silent: true }).catch(() => [])
      ]);
      this.setData({
        teacher: data.teacher,
        today: data.today,
        todayLessons: (data.todayLessons || []).map(view.lesson),
        weekLessons: (data.weekLessons || []).map(view.lesson),
        pendingLeaves: (leaves || []).slice(0, 3).map(l => ({
          ...l,
          avatarChar: (l.studentName || '学').slice(0, 1)
        })),
        pendingLeaveCount: data.pendingLeaves || 0,
        pendingWorks: data.pendingWorks || 0,
        classCount: data.classCount || 0,
        studentCount: data.studentCount || 0,
        loading: false
      });
    } catch (error) {
      this.setData({ loading: false });
    }
  },

  goRollcall(e) {
    const id = e.currentTarget.dataset.id;
    if (!id) {
      wx.showToast({ title: '今天暂无可点名的课程', icon: 'none' });
      return;
    }
    wx.navigateTo({ url: '/pages/rollcall/rollcall?lessonId=' + id });
  },

  goLeave() {
    wx.navigateTo({ url: '/pages/teacher-leave/teacher-leave' });
  },

  goUpload() {
    wx.navigateTo({ url: '/pages/upload-work/upload-work' });
  },

  goWeek() {
    wx.navigateTo({ url: '/pages/schedule/schedule' });
  },

  goLogin() {
    wx.navigateTo({ url: '/pages/login/login' });
  },

  goHome() {
    wx.switchTab({ url: '/pages/home/home' });
  },

  async logout() {
    const res = await wx.showModal({
      title: '退出登录',
      content: '退出后需重新使用教师手机号登录。',
      confirmText: '退出',
      confirmColor: '#B4473C'
    });
    if (!res.confirm) return;
    auth.clear();
    getApp().globalData.user = null;
    wx.reLaunch({ url: '/pages/home/home' });
  }
});
