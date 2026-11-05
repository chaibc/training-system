const api = require('../../utils/request');
const auth = require('../../utils/auth');
const view = require('../../utils/view');

Page({
  data: {
    loading: true,
    blocked: false,
    hasChildren: true,
    children: [],
    childId: null,
    childName: '',
    allRecords: [],
    records: [],
    filter: 'ALL'
  },

  onLoad(options) {
    if (!auth.requireParent()) {
      this.setData({ blocked: true, loading: false });
      return;
    }
    if (options.studentId) {
      this.presetStudentId = Number(options.studentId);
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
      const child = list.find(c => c.id === (this.presetStudentId || getApp().getChild())) || list[0];
      getApp().setChild(child.id);
      this.setData({
        children: list.map(view.child),
        childId: child.id,
        childName: child.name
      });
      this.loadRecords();
    } catch (error) {
      this.setData({ loading: false });
    }
  },

  async loadRecords() {
    if (!this.data.childId) return;
    this.setData({ loading: true });
    try {
      const list = (await api.get('/mini/parent/leave-records', { studentId: this.data.childId })) || [];
      const allRecords = list.map(view.leave);
      this.setData({ allRecords, loading: false });
      this.applyFilter();
    } catch (error) {
      this.setData({ loading: false });
    }
  },

  applyFilter() {
    const records = this.data.filter === 'ALL'
      ? this.data.allRecords
      : this.data.allRecords.filter(r => r.status === this.data.filter);
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

  async makeup(e) {
    const id = e.currentTarget.dataset.id;
    const res = await wx.showModal({
      title: '申请补课',
      content: '提交后由教务安排补课时间，补课不重复计费。是否继续？',
      confirmText: '提交申请',
      confirmColor: '#B4473C'
    });
    if (!res.confirm) return;
    try {
      await api.post('/mini/parent/leaves/' + id + '/makeup');
      wx.showToast({ title: '补课申请已提交', icon: 'success' });
      this.loadRecords();
    } catch (error) {
      // request 已提示
    }
  },

  goLesson(e) {
    wx.navigateTo({
      url: '/pages/lesson-detail/lesson-detail?lessonId=' + e.currentTarget.dataset.lesson + '&studentId=' + this.data.childId
    });
  },

  goLeave() {
    wx.navigateTo({ url: '/pages/leave/leave?studentId=' + this.data.childId });
  },

  goAccount() {
    wx.navigateTo({ url: '/pages/account/account' });
  }
});
