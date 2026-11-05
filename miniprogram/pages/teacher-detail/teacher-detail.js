/**
 * 教师详情页：展示教师简介、擅长书体、标签与在授课程。
 * 接口：GET /api/public/teachers/{id}
 * 参数：id（教师 ID，必填）
 * 需登录：否
 */

const api = require('../../utils/request');
const view = require('../../utils/view');

Page({
  data: {
    id: null,
    teacher: null,
    courses: [],
    loading: true
  },

  // 记录教师 ID 并加载详情
  onLoad(options) {
    this.setData({ id: options.id });
    this.load();
  },

  // 拉教师详情；tags 逗号分隔、styles 斜杠分隔，需转成数组供 WXML 遍历
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

  // 进教师所授课程详情
  goCourse(e) {
    wx.navigateTo({ url: '/pages/course-detail/course-detail?id=' + e.currentTarget.dataset.id });
  },

  // 试听预约（公开表单）
  goTrial() {
    wx.navigateTo({ url: '/pages/trial/trial' });
  },

  // 分享教师详情（带教师 ID）
  onShareAppMessage() {
    return {
      title: this.data.teacher ? this.data.teacher.name + ' · 翰墨学堂教师' : '翰墨学堂师资',
      path: '/pages/teacher-detail/teacher-detail?id=' + this.data.id
    };
  }
});
