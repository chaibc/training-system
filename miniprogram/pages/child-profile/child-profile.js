const api = require('../../utils/request');
const auth = require('../../utils/auth');
const view = require('../../utils/view');
const fmt = require('../../utils/format');

Page({
  data: {
    loading: true,
    blocked: false,
    childId: null,
    child: null,
    hours: {},
    classes: [],
    recent: [],
    editOpen: false,
    saving: false,
    form: { gender: '', birthDate: '', school: '', grade: '' },
    genders: ['男', '女']
  },

  onLoad(options) {
    if (!auth.requireParent()) {
      this.setData({ blocked: true, loading: false });
      return;
    }
    this.setData({ childId: options.id || getApp().getChild() });
    this.load();
  },

  async load() {
    if (!this.data.childId) {
      this.setData({ loading: false });
      return;
    }
    try {
      const data = await api.get('/mini/parent/children/' + this.data.childId);
      const child = view.child(data);
      const recent = (data.recentAttendance || []).map(view.attendance);
      const classes = (data.classes || []).map(c => ({
        ...c,
        weekdayText: fmt.weekdayCn(c.weekday),
        timeText: fmt.range(c.startTime, c.endTime)
      }));
      this.setData({
        child,
        hours: data.hourAccount || {},
        classes,
        recent,
        loading: false,
        form: {
          gender: data.gender || '',
          birthDate: data.birthDate || '',
          school: data.school || '',
          grade: data.grade || ''
        }
      });
    } catch (error) {
      this.setData({ loading: false });
    }
  },

  openEdit() {
    this.setData({ editOpen: true });
  },

  closeEdit() {
    this.setData({ editOpen: false });
  },

  onField(e) {
    this.setData({ ['form.' + e.currentTarget.dataset.field]: e.detail.value });
  },

  onGender(e) {
    this.setData({ 'form.gender': this.data.genders[Number(e.detail.value)] });
  },

  onBirth(e) {
    this.setData({ 'form.birthDate': e.detail.value });
  },

  async save() {
    if (this.data.saving) return;
    this.setData({ saving: true });
    try {
      await api.put('/mini/parent/children/' + this.data.childId, this.data.form);
      wx.showToast({ title: '资料已更新', icon: 'success' });
      this.setData({ editOpen: false });
      this.load();
    } catch (error) {
      // request 已提示
    } finally {
      this.setData({ saving: false });
    }
  },

  goHours() {
    wx.navigateTo({ url: '/pages/hours/hours?studentId=' + this.data.childId });
  },

  goGrowth() {
    wx.navigateTo({ url: '/pages/growth/growth?studentId=' + this.data.childId });
  },

  goSchedule() {
    wx.navigateTo({ url: '/pages/schedule/schedule' });
  },

  goAccount() {
    wx.navigateTo({ url: '/pages/account/account' });
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
