/**
 * 家长端课时账户页：展示学员剩余课时、课时流水与续费提示。
 * 接口：GET /api/mini/parent/children；GET /api/mini/parent/hours?studentId=
 * 参数：studentId（可选，优先于全局当前学员）
 * 需登录：是，且必须是家长（PARENT）
 */

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

  // 家长守卫；暂存 URL 指定学员，等学员列表返回后再匹配（列表里没有则回落第一条）
  onLoad(options) {
    if (!auth.requireParent()) {
      this.setData({ blocked: true, loading: false });
      return;
    }
    this.presetStudentId = options.studentId ? Number(options.studentId) : null;
    this.loadChildren();
  },

  // 下拉只重拉课时与流水，结束后收起动画
  onPullDownRefresh() {
    this.load().finally(() => wx.stopPullDownRefresh());
  },

  // 取学员列表定位当前学员：URL 指定 > 全局当前 > 第一条，并写回全局
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

  // 拉课时账户与流水；changeHours 正负决定文案符号与颜色，时间转 yyyy-MM-dd HH:mm
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

  // 切换学员：写全局+缓存后重拉课时数据
  selectChild(e) {
    const id = Number(e.currentTarget.dataset.id);
    if (id === this.data.studentId) return;
    getApp().setChild(id);
    this.setData({ studentId: id }, () => this.load());
  },

  // 联系机构续费：优先 globalData 缓存电话，缺失时静默补拉 /public/home
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

  // 跳课表页
  goSchedule() {
    wx.navigateTo({ url: '/pages/schedule/schedule' });
  }
});
