/**
 * 教师列表页：展示全部教师与擅长书体，点击进教师详情。
 * 接口：GET /api/public/teachers
 * 参数：无
 * 需登录：否
 */

const api = require('../../utils/request');

Page({
  data: {
    loading: true,
    teachers: []
  },

  // 进入即加载教师列表
  onLoad() {
    this.load();
  },

  // 下拉重新加载，结束后收起动画
  onPullDownRefresh() {
    this.load().finally(() => wx.stopPullDownRefresh());
  },

  // 拉教师列表；补头像首字、书体文案、标签数组供卡片展示
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

  // 进教师详情
  goTeacher(e) {
    wx.navigateTo({ url: '/pages/teacher-detail/teacher-detail?id=' + e.currentTarget.dataset.id });
  },

  // 试听预约入口
  goTrial() {
    wx.navigateTo({ url: '/pages/trial/trial' });
  }
});
