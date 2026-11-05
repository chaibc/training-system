const api = require('../../utils/request');

Page({
  data: {
    loading: true,
    teachers: []
  },

  onLoad() {
    this.load();
  },

  onPullDownRefresh() {
    this.load().finally(() => wx.stopPullDownRefresh());
  },

  async load() {
    try {
      const list = (await api.get('/public/teachers')) || [];
      this.setData({
        teachers: list.map(t => ({
          ...t,
          avatarChar: (t.name || '师').slice(0, 1),
          styleText: t.styles || '',
          tagsList: (t.tags || '').split(',').filter(Boolean)
        })),
        loading: false
      });
    } catch (error) {
      this.setData({ loading: false });
    }
  },

  goTeacher(e) {
    wx.navigateTo({ url: '/pages/teacher-detail/teacher-detail?id=' + e.currentTarget.dataset.id });
  },

  goTrial() {
    wx.navigateTo({ url: '/pages/trial/trial' });
  }
});
