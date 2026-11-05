/**
 * 学员成长档案页：按时间分组展示作品与作品总数。
 * 接口：GET /api/mini/parent/growth?studentId=
 * 参数：studentId（可选，缺省用全局当前学员）
 * 需登录：是，且必须是家长（PARENT）
 */

const api = require('../../utils/request');
const auth = require('../../utils/auth');
const view = require('../../utils/view');

Page({
  data: {
    loading: true,
    blocked: false,
    studentId: null,
    child: null,
    timeline: [],
    workCount: 0
  },

  // 家长守卫后确定 studentId：URL 参数优先，其次全局当前学员
  onLoad(options) {
    if (!auth.requireParent()) {
      this.setData({ blocked: true, loading: false });
      return;
    }
    this.setData({ studentId: options.studentId || getApp().getChild() });
    this.load();
  },

  // 拉成长档案：时间线每组内的作品再用 view.work 适配图片地址与文案
  async load() {
    if (!this.data.studentId) return;
    try {
      const data = await api.get('/mini/parent/growth', { studentId: this.data.studentId });
      const timeline = (data.timeline || []).map(group => ({
        ...group,
        works: (group.works || []).map(view.work)
      }));
      this.setData({
        child: view.child(data.child),
        timeline,
        workCount: data.workCount || 0,
        loading: false
      });
    } catch (error) {
      this.setData({ loading: false });
    }
  },

  // 进作品详情
  goWork(e) {
    wx.navigateTo({ url: '/pages/work-detail/work-detail?id=' + e.currentTarget.dataset.id });
  },

  // 回学员档案（带 studentId，避免用错全局学员）
  goProfile() {
    wx.navigateTo({ url: '/pages/child-profile/child-profile?id=' + this.data.studentId });
  },

  // 跳作品墙 tab
  goWorks() {
    wx.switchTab({ url: '/pages/works/works' });
  }
});
