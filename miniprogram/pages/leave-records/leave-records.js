/**
 * 家长请假记录页：按学员查看请假记录、筛选状态、对已批准的请假发起补课。
 * 接口：GET /api/mini/parent/children；GET /api/mini/parent/leave-records?studentId=；
 *       POST /api/mini/parent/leaves/{id}/makeup
 * 参数：studentId（可选，指定学员）
 * 需登录：是，且必须是家长（PARENT）
 */

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

  // 家长守卫；URL 带 studentId 时优先，否则用全局当前学员
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

  // 下拉重拉当前学员的请假记录
  onPullDownRefresh() {
    this.loadRecords().finally(() => wx.stopPullDownRefresh());
  },

  // 取学员列表确定当前学员（URL 指定 > 全局当前 > 第一条）
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

  // 拉该学员请假记录；allRecords 全量，records 为筛选结果
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

  // 前端筛选：ALL 显示全部，其余按 status 精确匹配
  applyFilter() {
    const records = this.data.filter === 'ALL'
      ? this.data.allRecords
      : this.data.allRecords.filter(r => r.status === this.data.filter);
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

  // 切换状态筛选（全部/待审批/已通过/已驳回）
  setFilter(e) {
    this.setData({ filter: e.currentTarget.dataset.status }, () => this.applyFilter());
  },

  // 申请补课：只提交申请，补课时间由教务安排；补课不重复计费
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

  // 进关联课次详情
  goLesson(e) {
    wx.navigateTo({
      url: '/pages/lesson-detail/lesson-detail?lessonId=' + e.currentTarget.dataset.lesson + '&studentId=' + this.data.childId
    });
  },

  // 再提一条请假（带当前学员）
  goLeave() {
    wx.navigateTo({ url: '/pages/leave/leave?studentId=' + this.data.childId });
  },

  // 去账号页处理绑定
  goAccount() {
    wx.navigateTo({ url: '/pages/account/account' });
  }
});
