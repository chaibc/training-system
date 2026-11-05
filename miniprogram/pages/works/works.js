/**
 * 作品墙（tabBar）：按全部/软笔/硬笔/本月筛选展示学员作品。
 * 接口：GET /api/public/works?type=
 * 参数：无
 * 需登录：否
 */

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

  // 进入即加载默认「全部」作品
  onLoad() {
    this.load();
  },

  // 每次切回 tab 刷新访客态（登录/退出后登录入口文案要跟着变）
  onShow() {
    this.setData({ guest: !auth.isLoggedIn() });
  },

  // 下拉按当前筛选重新加载
  onPullDownRefresh() {
    this.load().finally(() => wx.stopPullDownRefresh());
  },

  // 软笔/硬笔按 type 过滤；「本月」传 month=yyyy-MM 交给后端按 created_at 过滤，
  // 避免前端拉全量再筛导致条数失真（数据量大时尤其明显）
  async load() {
    this.setData({ loading: true });
    try {
      const filter = this.data.filter;
      const query = {};
      if (filter === 'HARD' || filter === 'SOFT') query.type = filter;
      if (filter === 'MONTH') {
        const now = new Date();
        query.month = now.getFullYear() + '-' + String(now.getMonth() + 1).padStart(2, '0');
      }
      const list = (await api.get('/public/works', query)) || [];
      const works = list.map(view.work);
      this.setData({ works, total: works.length, loading: false });
    } catch (error) {
      this.setData({ loading: false });
    }
  },

  // 切换筛选；点到当前项不重复请求
  setFilter(e) {
    const filter = e.currentTarget.dataset.filter;
    if (filter === this.data.filter) return;
    this.setData({ filter }, () => this.load());
  },

  // 进作品详情
  goWork(e) {
    wx.navigateTo({ url: '/pages/work-detail/work-detail?id=' + e.currentTarget.dataset.id });
  },

  // 游客登录入口
  goLogin() {
    wx.navigateTo({ url: '/pages/login/login' });
  },

  // 分享作品墙
  onShareAppMessage() {
    return { title: '翰墨学堂学员作品墙', path: '/pages/works/works' };
  }
});
