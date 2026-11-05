const api = require('../../utils/request');
const auth = require('../../utils/auth');
const view = require('../../utils/view');

Page({
  data: {
    loading: true,
    blocked: false,
    lessonId: null,
    studentId: null,
    lesson: null,
    child: null,
    leave: null,
    canLeave: false,
    leaveHint: ''
  },

  onLoad(options) {
    if (!auth.requireParent()) {
      this.setData({ blocked: true, loading: false });
      return;
    }
    this.setData({
      lessonId: options.lessonId,
      studentId: options.studentId || getApp().getChild()
    });
    this.load();
  },

  onShow() {
    if (this.data.lesson && this.data.studentId) this.load(true);
  },

  async load(silent) {
    if (!this.data.studentId) {
      wx.showToast({ title: '缺少学员信息', icon: 'none' });
      return;
    }
    if (!silent) this.setData({ loading: true });
    try {
      const data = await api.get('/mini/parent/lessons/' + this.data.lessonId, {
        studentId: this.data.studentId
      });
      this.setData({
        lesson: view.lesson(data),
        child: view.child(data.child),
        leave: data.leave,
        canLeave: data.canLeave,
        leaveHint: data.leaveHint,
        loading: false
      });
      wx.setNavigationBarTitle({ title: data.className || '课次详情' });
    } catch (error) {
      this.setData({ loading: false });
    }
  },

  goLeave() {
    wx.navigateTo({
      url: '/pages/leave/leave?lessonId=' + this.data.lessonId + '&studentId=' + this.data.studentId
    });
  },

  goLeaveRecords() {
    wx.navigateTo({ url: '/pages/leave-records/leave-records?studentId=' + this.data.studentId });
  },

  async contact() {
    let phone = getApp().globalData.institutionPhone;
    if (!phone) {
      try {
        const data = await api.get('/public/home', {}, { silent: true });
        phone = data.institution && data.institution.phone;
        getApp().globalData.institutionPhone = phone;
      } catch (error) {
        // ignore
      }
    }
    if (!phone) return wx.showToast({ title: '暂未维护联系电话', icon: 'none' });
    wx.makePhoneCall({ phoneNumber: phone.replace(/[^\d]/g, '') });
  }
});
