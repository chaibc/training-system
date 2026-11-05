/**
 * 家长报名记录页：按学员查看报名申请，支持按状态筛选与取消未确认的申请。
 * 接口：GET /api/mini/parent/children；GET /api/mini/parent/enrollments?studentId=；
 *       POST /api/mini/parent/enrollments/{id}/cancel
 * 参数：无（学员来自全局当前学员）
 * 需登录：是，且必须是家长（PARENT）
 */

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

  // 家长守卫：未通过置 blocked，不再请求数据
  onLoad() {
    if (!auth.requireParent()) {
      this.setData({ blocked: true, loading: false });
      return;
    }
    this.loadChildren();
  },

  // 下拉重拉当前学员的报名记录
  onPullDownRefresh() {
    this.loadRecords().finally(() => wx.stopPullDownRefresh());
  },

  // 取学员列表确定当前学员（全局优先、回落第一条），再加载记录
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

  // 拉该学员全部报名记录；allRecords 为全量，records 为筛选后的展示数据
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

  // 前端筛选：ALL 显示全部，其余按 status 精确匹配；不重新请求接口
  applyFilter() {
    const filter = this.data.filter;
    const records = filter === 'ALL'
      ? this.data.allRecords
      : this.data.allRecords.filter(r => r.status === filter);
    this.setData({ records });
  },

  // 切换学员：写全局+缓存、重置筛选后重载
  selectChild(e) {
    const id = Number(e.currentTarget.dataset.id);
    if (id === this.data.childId) return;
    const child = this.data.children.find(c => c.id === id);
    getApp().setChild(id);
    this.setData({ childId: id, childName: child ? child.name : '', filter: 'ALL' }, () => this.loadRecords());
  },

  // 切换状态筛选（全部/待确认/待缴费/已入班）
  setFilter(e) {
    this.setData({ filter: e.currentTarget.dataset.status }, () => this.applyFilter());
  },

  // 取消报名：仅未确认状态开放（按钮由 view.enrollment.canCancel 控制），确认后刷新
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

  // 再看课程详情
  goCourse(e) {
    wx.navigateTo({ url: '/pages/course-detail/course-detail?id=' + e.currentTarget.dataset.id });
  },

  // 联系机构：优先 globalData 缓存电话，缺失时静默请求 /public/home 补一次
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

  // 继续报名其他课程
  goEnroll() {
    wx.navigateTo({ url: '/pages/enroll/enroll' });
  }
});
