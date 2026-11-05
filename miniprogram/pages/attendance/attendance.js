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
      const data = await api.get('/mini/parent/attendance', { studentId: this.data.childId });
      const allRecords = (data.records || []).map(view.attendance).map(r => ({
        ...r,
        noteText: r.status === 'PRESENT' ? '出勤，已消课'
          : r.status === 'LEAVE' ? (Number(r.consumedHours) > 0 ? '请假，按规则消课' : '请假，不扣课时')
            : '缺勤，按消课处理',
        consumedText: Number(r.consumedHours) > 0 ? '−' + r.consumedHours + ' 课时' : '0 课时'
      }));
      this.setData({ allRecords, child: view.child(data.child), loading: false });
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

  goLesson(e) {
    wx.navigateTo({
      url: '/pages/lesson-detail/lesson-detail?lessonId=' + e.currentTarget.dataset.lesson + '&studentId=' + this.data.childId
    });
  },

  goSchedule() {
    wx.navigateTo({ url: '/pages/schedule/schedule' });
  },

  goAccount() {
    wx.navigateTo({ url: '/pages/account/account' });
  }
});
