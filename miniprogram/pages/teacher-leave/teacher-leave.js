/**
 * 教师请假审批页：查看待审批与历史请假，通过或驳回（驳回必须填原因）。
 * 接口：GET /api/mini/teacher/leaves/pending；GET /api/mini/teacher/leaves/history；
 *       POST /api/mini/teacher/leaves/{id}/approve
 * 参数：无
 * 需登录：是，且必须是教师（TEACHER）
 */

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

  // 教师守卫；非教师置 blocked 不再请求
  onLoad() {
    if (!auth.requireTeacher()) {
      this.setData({ blocked: true, loading: false });
      return;
    }
    this.load();
  },

  // 下拉同时刷新待审批与历史两个列表
  onPullDownRefresh() {
    this.load().finally(() => wx.stopPullDownRefresh());
  },

  // 两个列表并行拉取；单个接口失败降级为空数组，避免整页报错
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

  // 列表项加工：头像首字、提交时间、状态样式；历史与待审批共用
  mapLeave(l) {
    return {
      ...l,
      avatarChar: (l.studentName || '学').slice(0, 1),
      submitText: fmt.dateTime(l.submittedAt),
      dateText: l.lessonDate || '',
      statusClass: l.status === 'APPROVED' ? 'green' : l.status === 'REJECTED' ? 'red' : 'gold'
    };
  },

  // 切换「待审批/历史」tab，仅切换展示不重新请求（两个列表已同时加载）
  setTab(e) {
    this.setData({ tab: e.currentTarget.dataset.tab });
  },

  // 通过请假：二次确认后提交 approve=true，后端按规则同步出勤与课时
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

  // 驳回请假：editable 弹窗收集原因，原因为空不提交
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

  // 审批提交：processingId 兼作防重复点击锁；成功后重新拉取两个列表
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

  // 有上一页就返回，直接进入本页则回教师工作台
  goBack() {
    const pages = getCurrentPages();
    if (pages.length > 1) wx.navigateBack();
    else wx.reLaunch({ url: '/pages/teacher-home/teacher-home' });
  }
});
