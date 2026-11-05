/**
 * 家长端出勤记录页：按学员查看历史出勤（出勤/请假/缺勤），支持状态筛选与进课次详情。
 * 接口：GET /api/mini/parent/children；GET /api/mini/parent/attendance?studentId=
 * 参数：无（学员来自全局当前学员，失效时用列表第一条）
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

  // 家长守卫：未通过时置 blocked 只展示无权限态，不请求数据
  onLoad() {
    if (!auth.requireParent()) {
      this.setData({ blocked: true, loading: false });
      return;
    }
    this.loadChildren();
  },

  // 下拉只重拉当前学员的出勤记录，结束后收起动画
  onPullDownRefresh() {
    this.loadRecords().finally(() => wx.stopPullDownRefresh());
  },

  // 先取学员列表确定 childId（全局优先、回落第一条并写回全局），再加载出勤
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

  // 按当前学员拉出勤；allRecords 存全量，records 存筛选后的展示数据
  async loadRecords() {
    if (!this.data.childId) return;
    this.setData({ loading: true });
    try {
      const data = await api.get('/mini/parent/attendance', { studentId: this.data.childId });
      const allRecords = (data.records || []).map(view.attendance).map(r => ({
        ...r,
        // 消课文案由后端 consumedHours 推导：请假是否扣课时取决于是否在截止时间前申请
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

  // 前端筛选：ALL 显示全部，其余按 status 精确匹配；不重新请求接口
  applyFilter() {
    const records = this.data.filter === 'ALL'
      ? this.data.allRecords
      : this.data.allRecords.filter(r => r.status === this.data.filter);
    this.setData({ records });
  },

  // 切换学员：写全局+缓存、重置筛选后重载记录
  selectChild(e) {
    const id = Number(e.currentTarget.dataset.id);
    if (id === this.data.childId) return;
    const child = this.data.children.find(c => c.id === id);
    getApp().setChild(id);
    this.setData({ childId: id, childName: child ? child.name : '', filter: 'ALL' }, () => this.loadRecords());
  },

  // 切换状态筛选（ALL/PRESENT/LEAVE/ABSENT）
  setFilter(e) {
    this.setData({ filter: e.currentTarget.dataset.status }, () => this.applyFilter());
  },

  // 进课次详情，带上 lessonId 与当前 studentId
  goLesson(e) {
    wx.navigateTo({
      url: '/pages/lesson-detail/lesson-detail?lessonId=' + e.currentTarget.dataset.lesson + '&studentId=' + this.data.childId
    });
  },

  // 跳课表页
  goSchedule() {
    wx.navigateTo({ url: '/pages/schedule/schedule' });
  },

  // 跳账号/绑定页（用于处理未绑定学员）
  goAccount() {
    wx.navigateTo({ url: '/pages/account/account' });
  }
});
