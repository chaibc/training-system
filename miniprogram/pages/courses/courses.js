/**
 * 课程列表页（tabBar）：按分类与关键字筛选课程。
 * 接口：GET /api/public/meta（分类）；GET /api/public/courses?categoryId=&keyword=
 * 参数：无
 * 需登录：否
 */

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

  // 分类与课程并行加载，互不阻塞
  onLoad() {
    this.loadMeta();
    this.loadCourses();
  },

  // 每次切回 tab 刷新访客态（登录/退出后按钮文案要跟着变）
  onShow() {
    this.setData({ guest: !auth.isLoggedIn() });
  },

  // 下拉同时刷新分类与列表，两者都结束再收起动画
  onPullDownRefresh() {
    Promise.all([this.loadMeta(), this.loadCourses()]).finally(() => wx.stopPullDownRefresh());
  },

  // 分类加载失败静默忽略，不阻塞课程主列表
  async loadMeta() {
    try {
      const data = await api.get('/public/meta', {}, { silent: true });
      this.setData({ categories: data.categories || [] });
    } catch (error) {
      // 分类加载失败不阻塞课程列表
    }
  },

  // 按分类/关键字查询课程；接口一次返回全部，无分页与上拉加载，total 即本次结果条数
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

  // 选分类：data-id 为 all 时置空 categoryId，查询参数不带该字段
  selectCategory(e) {
    const id = e.currentTarget.dataset.id;
    this.setData({ categoryId: id === 'all' ? null : id }, () => this.loadCourses());
  },

  // 关键字输入只更新 data，不即时请求
  onKeyword(e) {
    this.setData({ keyword: e.detail.value });
  },

  // 点搜索/回车才按关键字查询
  search() {
    this.loadCourses();
  },

  // 清空关键字并立即重新加载
  clearSearch() {
    this.setData({ keyword: '' }, () => this.loadCourses());
  },

  // 进课程详情
  goCourse(e) {
    wx.navigateTo({ url: '/pages/course-detail/course-detail?id=' + e.currentTarget.dataset.id });
  },

  // 试听预约（公开页）
  goTrial() {
    wx.navigateTo({ url: '/pages/trial/trial' });
  },

  // 游客登录入口
  goLogin() {
    wx.navigateTo({ url: '/pages/login/login' });
  },

  // 分享课程体系页
  onShareAppMessage() {
    return { title: '翰墨学堂课程体系', path: '/pages/courses/courses' };
  }
});
