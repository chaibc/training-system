const api = require('../../utils/request');
const auth = require('../../utils/auth');
const view = require('../../utils/view');
const fmt = require('../../utils/format');

Page({
  data: {
    loading: true,
    blocked: false,
    hasChildren: true,
    children: [],
    studentId: null,
    child: null,
    account: {},
    ledger: [],
    renewalHint: 3
  },

  onLoad(options) {
    if (!auth.requireParent()) {
      this.setData({ blocked: true, loading: false });
      return;
    }
    this.presetStudentId = options.studentId ? Number(options.studentId) : null;
    this.loadChildren();
  },

  onPullDownRefresh() {
    this.load().finally(() => wx.stopPullDownRefresh());
  },

  async loadChildren() {
    try {
      const list = (await api.get('/mini/parent/children')) || [];
      if (!list.length) {
        this.setData({ loading: false, hasChildren: false });
        return;
      }
      const child = list.find(c => c.id === (this.presetStudentId || getApp().getChild())) || list[0];
      getApp().setChild(child.id);
      this.setData({ children: list.map(view.child), studentId: child.id });
      this.load();
    } catch (error) {
      this.setData({ loading: false });
    }
  },

  async load() {
    if (!this.data.studentId) return;
    this.setData({ loading: true });
    try {
      const data = await api.get('/mini/parent/hours', { studentId: this.data.studentId });
      const ledger = (data.ledger || []).map(item => ({
        ...item,
        changeText: (Number(item.changeHours) > 0 ? '+' : '') + Number(item.changeHours),
        positive: Number(item.changeHours) > 0,
        timeText: fmt.dateTime(item.createdAt)
      }));
      this.setData({
        child: view.child(data.child),
        account: data.account || {},
        ledger,
        renewalHint: data.renewalHint || 3,
        loading: false
      });
    } catch (error) {
      this.setData({ loading: false });
    }
  },

  selectChild(e) {
    const id = Number(e.currentTarget.dataset.id);
    if (id === this.data.studentId) return;
    getApp().setChild(id);
    this.setData({ studentId: id }, () => this.load());
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
  },

  goSchedule() {
    wx.navigateTo({ url: '/pages/schedule/schedule' });
  }
});
