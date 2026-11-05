const api = require('../../utils/request');
const view = require('../../utils/view');

Page({
  data: {
    id: null,
    teacher: null,
    courses: [],
    loading: true
  },

  onLoad(options) {
    this.setData({ id: options.id });
    this.load();
  },

  async load() {
    try {
      const data = await api.get('/public/teachers/' + this.data.id);
      this.setData({
        teacher: {
          ...data,
          avatarChar: (data.name || '师').slice(0, 1),
          tagsList: (data.tags || '').split(',').filter(Boolean),
          stylesList: (data.styles || '').split('/').map(s => s.trim()).filter(Boolean)
        },
        courses: (data.courses || []).map(view.course),
        loading: false
      });
      wx.setNavigationBarTitle({ title: data.name || '教师详情' });
    } catch (error) {
      this.setData({ loading: false });
    }
  },

  goCourse(e) {
    wx.navigateTo({ url: '/pages/course-detail/course-detail?id=' + e.currentTarget.dataset.id });
  },

  goTrial() {
    wx.navigateTo({ url: '/pages/trial/trial' });
  },

  onShareAppMessage() {
    return {
      title: this.data.teacher ? this.data.teacher.name + ' · 翰墨学堂教师' : '翰墨学堂师资',
      path: '/pages/teacher-detail/teacher-detail?id=' + this.data.id
    };
  }
});
