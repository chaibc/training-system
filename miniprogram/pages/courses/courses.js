const api = require('../../utils/request');
const view = require('../../utils/view');
const auth = require('../../utils/auth');

Page({
  data: {
    loading: true,
    categories: [],
    courses: [],
    categoryId: null,
    keyword: '',
    guest: true,
    total: 0
  },

  onLoad() {
    this.loadMeta();
    this.loadCourses();
  },

  onShow() {
    this.setData({ guest: !auth.isLoggedIn() });
  },

  onPullDownRefresh() {
    Promise.all([this.loadMeta(), this.loadCourses()]).finally(() => wx.stopPullDownRefresh());
  },

  async loadMeta() {
    try {
      const data = await api.get('/public/meta', {}, { silent: true });
      this.setData({ categories: data.categories || [] });
    } catch (error) {
      // 分类加载失败不阻塞课程列表
    }
  },

  async loadCourses() {
    this.setData({ loading: true });
    try {
      const query = {};
      if (this.data.categoryId) query.categoryId = this.data.categoryId;
      if (this.data.keyword) query.keyword = this.data.keyword;
      const list = await api.get('/public/courses', query);
      const courses = (list || []).map(view.course);
      this.setData({ courses, total: courses.length, loading: false });
    } catch (error) {
      this.setData({ loading: false });
    }
  },

  selectCategory(e) {
    const id = e.currentTarget.dataset.id;
    this.setData({ categoryId: id === 'all' ? null : id }, () => this.loadCourses());
  },

  onKeyword(e) {
    this.setData({ keyword: e.detail.value });
  },

  search() {
    this.loadCourses();
  },

  clearSearch() {
    this.setData({ keyword: '' }, () => this.loadCourses());
  },

  goCourse(e) {
    wx.navigateTo({ url: '/pages/course-detail/course-detail?id=' + e.currentTarget.dataset.id });
  },

  goTrial() {
    wx.navigateTo({ url: '/pages/trial/trial' });
  },

  goLogin() {
    wx.navigateTo({ url: '/pages/login/login' });
  },

  onShareAppMessage() {
    return { title: '翰墨学堂课程体系', path: '/pages/courses/courses' };
  }
});
