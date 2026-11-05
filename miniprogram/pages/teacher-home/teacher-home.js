/**
 * 教师工作台：今日/本周课次、待审批请假、待审核作品等概览，提供点名、审批、上传作品入口。
 * 接口：GET /api/mini/teacher/overview；GET /api/mini/teacher/leaves/pending
 * 参数：无
 * 需登录：是，且必须是教师（TEACHER）
 */

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

  // 教师守卫；非教师置 blocked 只展示无权限提示
  onLoad() {
    if (!auth.requireTeacher()) {
      this.setData({ blocked: true, loading: false });
      return;
    }
    this.load();
  },

  // 从点名/审批/上传页返回时静默刷新概览；首次加载由 onLoad 负责
  onShow() {
    if (this.data.teacher && !this.data.loading) this.load(true);
  },

  // 下拉重拉概览与待审批请假
  onPullDownRefresh() {
    this.load().finally(() => wx.stopPullDownRefresh());
  },

  // 并行取概览与待审批请假；请假接口失败降级为空数组，不影响工作台主体
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
        // 工作台只预览前 3 条，完整列表去教师请假审批页
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

  // 点名入口：今天没课（无 lessonId）时只提示，不跳转
  goRollcall(e) {
    const id = e.currentTarget.dataset.id;
    if (!id) {
      wx.showToast({ title: '今天暂无可点名的课程', icon: 'none' });
      return;
    }
    wx.navigateTo({ url: '/pages/rollcall/rollcall?lessonId=' + id });
  },

  // 请假审批页
  goLeave() {
    wx.navigateTo({ url: '/pages/teacher-leave/teacher-leave' });
  },

  // 上传学员作品页
  goUpload() {
    wx.navigateTo({ url: '/pages/upload-work/upload-work' });
  },
  // 教师登录入口（未登录时工作台为 blocked 态）
  goLogin() {
    wx.navigateTo({ url: '/pages/login/login' });
  },

  // 回家长端首页 tab
  goHome() {
    wx.switchTab({ url: '/pages/home/home' });
  },

  // 退出登录：清凭证与 globalData 后 reLaunch 回首页
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
