const api = require('../../utils/request');
const auth = require('../../utils/auth');
const fmt = require('../../utils/format');

Page({
  data: {
    loading: true,
    blocked: false,
    tab: 'PENDING',
    pending: [],
    history: [],
    processingId: null
  },

  onLoad() {
    if (!auth.requireTeacher()) {
      this.setData({ blocked: true, loading: false });
      return;
    }
    this.load();
  },

  onPullDownRefresh() {
    this.load().finally(() => wx.stopPullDownRefresh());
  },

  async load() {
    this.setData({ loading: true });
    try {
      const [pending, history] = await Promise.all([
        api.get('/mini/teacher/leaves/pending', {}, { silent: true }).catch(() => []),
        api.get('/mini/teacher/leaves/history', {}, { silent: true }).catch(() => [])
      ]);
      this.setData({
        pending: (pending || []).map(this.mapLeave),
        history: (history || []).map(this.mapLeave),
        loading: false
      });
    } catch (error) {
      this.setData({ loading: false });
    }
  },

  mapLeave(l) {
    return {
      ...l,
      avatarChar: (l.studentName || '学').slice(0, 1),
      submitText: fmt.dateTime(l.submittedAt),
      dateText: l.lessonDate || '',
      statusClass: l.status === 'APPROVED' ? 'green' : l.status === 'REJECTED' ? 'red' : 'gold'
    };
  },

  setTab(e) {
    this.setData({ tab: e.currentTarget.dataset.tab });
  },

  async approve(e) {
    const id = e.currentTarget.dataset.id;
    const res = await wx.showModal({
      title: '通过请假',
      content: '确认通过这条请假申请？系统将按规则同步出勤与课时。',
      confirmText: '通过',
      confirmColor: '#5C7A63'
    });
    if (!res.confirm) return;
    await this.handle(id, { approve: true });
  },

  async reject(e) {
    const id = e.currentTarget.dataset.id;
    const res = await wx.showModal({
      title: '驳回请假',
      content: '',
      editable: true,
      placeholderText: '请填写驳回原因',
      confirmText: '确认驳回',
      confirmColor: '#B4473C'
    });
    if (!res.confirm) return;
    const reason = (res.content || '').trim();
    if (!reason) {
      wx.showToast({ title: '请填写驳回原因', icon: 'none' });
      return;
    }
    await this.handle(id, { approve: false, rejectReason: reason });
  },

  async handle(id, payload) {
    if (this.data.processingId) return;
    this.setData({ processingId: id });
    try {
      await api.post('/mini/teacher/leaves/' + id + '/approve', payload);
      wx.showToast({ title: payload.approve ? '已通过' : '已驳回', icon: 'success' });
      await this.load();
    } catch (error) {
      // request 已提示
    } finally {
      this.setData({ processingId: null });
    }
  },

  goBack() {
    const pages = getCurrentPages();
    if (pages.length > 1) wx.navigateBack();
    else wx.reLaunch({ url: '/pages/teacher-home/teacher-home' });
  }
});
