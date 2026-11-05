const api = require('../../utils/request');
const auth = require('../../utils/auth');
const view = require('../../utils/view');

Page({
  data: {
    loading: true,
    blocked: false,
    children: [],
    childId: null,
    childName: '',
    allRecords: [],
    records: [],
    filter: 'ALL',
    hasChildren: true
  },

  onLoad() {
    if (!auth.requireParent()) {
      this.setData({ blocked: true, loading: false });
      return;
    }
    this.loadChildren();
  },

  onPullDownRefresh() {
    this.loadRecords().finally(() => wx.stopPullDownRefresh());
  },

  async loadChildren() {
    try {
      const list = (await api.get('/mini/parent/children')) || [];
      if (!list.length) {
        this.setData({ loading: false, hasChildren: false });
        return;
      }
      const child = list.find(c => c.id === getApp().getChild()) || list[0];
      getApp().setChild(child.id);
      this.setData({ children: list.map(view.child), childId: child.id, childName: child.name });
      this.loadRecords();
    } catch (error) {
      this.setData({ loading: false });
    }
  },

  async loadRecords() {
    if (!this.data.childId) return;
    this.setData({ loading: true });
    try {
      const list = (await api.get('/mini/parent/enrollments', { studentId: this.data.childId })) || [];
      const allRecords = list.map(view.enrollment);
      this.setData({ allRecords, loading: false });
      this.applyFilter();
    } catch (error) {
      this.setData({ loading: false });
    }
  },

  applyFilter() {
    const filter = this.data.filter;
    const records = filter === 'ALL'
      ? this.data.allRecords
      : this.data.allRecords.filter(r => r.status === filter);
    this.setData({ records });
  },

  selectChild(e) {
    const id = Number(e.currentTarget.dataset.id);
    if (id === this.data.childId) return;
    const child = this.data.children.find(c => c.id === id);
    getApp().setChild(id);
    this.setData({ childId: id, childName: child ? child.name : '', filter: 'ALL' }, () => this.loadRecords());
  },

  setFilter(e) {
    this.setData({ filter: e.currentTarget.dataset.status }, () => this.applyFilter());
  },

  async cancel(e) {
    const id = e.currentTarget.dataset.id;
    const res = await wx.showModal({
      title: '取消报名申请',
      content: '确认取消这条尚未确认的报名申请吗？如已与教务沟通，请忽略。',
      confirmText: '确认取消',
      confirmColor: '#B4473C'
    });
    if (!res.confirm) return;
    try {
      await api.post('/mini/parent/enrollments/' + id + '/cancel');
      wx.showToast({ title: '已取消', icon: 'success' });
      this.loadRecords();
    } catch (error) {
      // request 已提示
    }
  },

  goCourse(e) {
    wx.navigateTo({ url: '/pages/course-detail/course-detail?id=' + e.currentTarget.dataset.id });
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

  goEnroll() {
    wx.navigateTo({ url: '/pages/enroll/enroll' });
  }
});
