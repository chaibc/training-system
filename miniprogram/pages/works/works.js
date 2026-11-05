const api = require('../../utils/request');
const view = require('../../utils/view');
const auth = require('../../utils/auth');

Page({
  data: {
    loading: true,
    works: [],
    filter: 'ALL',
    guest: true,
    total: 0,
    notices: []
  },

  onLoad() {
    this.load();
  },

  onShow() {
    this.setData({ guest: !auth.isLoggedIn() });
  },

  onPullDownRefresh() {
    this.load().finally(() => wx.stopPullDownRefresh());
  },

  async load() {
    this.setData({ loading: true });
    try {
      const filter = this.data.filter;
      const query = filter === 'HARD' || filter === 'SOFT' ? { type: filter } : {};
      let list = (await api.get('/public/works', query)) || [];
      if (filter === 'MONTH') {
        const now = new Date();
        const prefix = now.getFullYear() + '-' + String(now.getMonth() + 1).padStart(2, '0');
        list = list.filter(w => String(w.createdAt || '').startsWith(prefix));
      }
      const works = list.map(view.work);
      this.setData({ works, total: works.length, loading: false });
    } catch (error) {
      this.setData({ loading: false });
    }
  },

  setFilter(e) {
    const filter = e.currentTarget.dataset.filter;
    if (filter === this.data.filter) return;
    this.setData({ filter }, () => this.load());
  },

  goWork(e) {
    wx.navigateTo({ url: '/pages/work-detail/work-detail?id=' + e.currentTarget.dataset.id });
  },

  goLogin() {
    wx.navigateTo({ url: '/pages/login/login' });
  },

  onShareAppMessage() {
    return { title: '翰墨学堂学员作品墙', path: '/pages/works/works' };
  }
});
