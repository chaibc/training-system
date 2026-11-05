const api = require('../../utils/request');
const view = require('../../utils/view');
const auth = require('../../utils/auth');

Page({
  data: {
    id: null,
    course: null,
    prices: [],
    outlines: [],
    loading: true,
    isParent: false,
    isTeacher: false,
    guest: true
  },

  onLoad(options) {
    const user = auth.getUser();
    this.setData({
      id: options.id,
      guest: !auth.isLoggedIn(),
      isParent: user && user.roleCode === 'PARENT',
      isTeacher: user && user.roleCode === 'TEACHER'
    });
    this.load();
  },

  async load() {
    try {
      const data = await api.get('/public/courses/' + this.data.id);
      const course = view.course(data);
      const prices = (data.prices || []).map(p => ({
        ...p,
        unitText: p.unit === 'TIME' ? '次' : '期',
        priceText: Number(p.price || 0).toFixed(0)
      }));
      this.setData({ course, prices, outlines: data.outlines || [], loading: false });
      wx.setNavigationBarTitle({ title: course.name || '课程详情' });
    } catch (error) {
      this.setData({ loading: false });
    }
  },

  goTrial() {
    wx.navigateTo({ url: '/pages/trial/trial?courseId=' + this.data.id });
  },

  goEnroll() {
    if (!auth.requireParent()) return;
    wx.navigateTo({ url: '/pages/enroll/enroll?courseId=' + this.data.id });
  },

  goLogin() {
    wx.navigateTo({ url: '/pages/login/login' });
  },

  call() {
    const phone = getApp().globalData.institutionPhone;
    if (!phone) {
      wx.showToast({ title: '暂未维护联系电话', icon: 'none' });
      return;
    }
    wx.makePhoneCall({ phoneNumber: phone.replace(/[^\d]/g, '') });
  },

  onShareAppMessage() {
    return {
      title: this.data.course ? this.data.course.name : '翰墨学堂课程',
      path: '/pages/course-detail/course-detail?id=' + this.data.id
    };
  }
});
