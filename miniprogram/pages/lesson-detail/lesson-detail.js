/**
 * 课次详情页：展示单次课的教师/教室/时间、学员出勤与请假状态，并提供请假入口。
 * 接口：GET /api/mini/parent/lessons/{lessonId}?studentId=
 * 参数：lessonId（必填）、studentId（可选，缺省用全局当前学员）
 * 需登录：是，且必须是家长（PARENT）
 */

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

  // 家长守卫后记录 lessonId 与 studentId（缺省用全局当前学员）
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

  // 从请假页返回时静默刷新，让 canLeave / 请假状态及时更新；首次由 onLoad 加载
  onShow() {
    if (this.data.lesson && this.data.studentId) this.load(true);
  },

  // 拉课次详情；silent=true 保留旧内容只更新数据，避免 onShow 回刷时整页闪 loading
  async load(silent) {
    // studentId 缺失直接提示，不带着 undefined 请求详情
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

  // 发起请假（带课次与学员，后端会校验是否已过请假截止）
  goLeave() {
    wx.navigateTo({
      url: '/pages/leave/leave?lessonId=' + this.data.lessonId + '&studentId=' + this.data.studentId
    });
  },

  // 查看该学员请假记录
  goLeaveRecords() {
    wx.navigateTo({ url: '/pages/leave-records/leave-records?studentId=' + this.data.studentId });
  },

  // 联系机构：优先 globalData 缓存电话，缺失时静默补拉 /public/home
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
