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

  onLoad(options) {
    if (!auth.requireParent()) {
      this.setData({ blocked: true, loading: false });
      return;
    }
    this.setData({ studentId: options.studentId || getApp().getChild() });
    this.load();
  },

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

  goWork(e) {
    wx.navigateTo({ url: '/pages/work-detail/work-detail?id=' + e.currentTarget.dataset.id });
  },

  goProfile() {
    wx.navigateTo({ url: '/pages/child-profile/child-profile?id=' + this.data.studentId });
  },

  goWorks() {
    wx.switchTab({ url: '/pages/works/works' });
  }
});
