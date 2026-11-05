/**
 * 学员档案页：展示单个学员的基本资料、课时账户、在读班级与最近出勤，可编辑基础信息。
 * 接口：GET /api/mini/parent/children/{id}；PUT /api/mini/parent/children/{id}
 * 参数：id（可选，缺省用全局当前学员）
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
    childId: null,
    child: null,
    hours: {},
    classes: [],
    recent: [],
    editOpen: false,
    saving: false,
    form: { gender: '', birthDate: '', school: '', grade: '' },
    genders: ['男', '女']
  },

  // 家长守卫后确定 childId：URL 参数优先，其次全局当前学员
  onLoad(options) {
    if (!auth.requireParent()) {
      this.setData({ blocked: true, loading: false });
      return;
    }
    this.setData({ childId: options.id || getApp().getChild() });
    this.load();
  },

  // 拉学员详情；没有 childId 时直接结束 loading，避免骨架屏常驻
  async load() {
    if (!this.data.childId) {
      this.setData({ loading: false });
      return;
    }
    try {
      const data = await api.get('/mini/parent/children/' + this.data.childId);
      const child = view.child(data);
      const recent = (data.recentAttendance || []).map(view.attendance);
      // 班级的 weekday 是 1-7 数字、时间带秒，需转成「周几」「HH:mm–HH:mm」
      const classes = (data.classes || []).map(c => ({
        ...c,
        weekdayText: fmt.weekdayCn(c.weekday),
        timeText: fmt.range(c.startTime, c.endTime)
      }));
      this.setData({
        child,
        hours: data.hourAccount || {},
        classes,
        recent,
        loading: false,
        form: {
          gender: data.gender || '',
          birthDate: data.birthDate || '',
          school: data.school || '',
          grade: data.grade || ''
        }
      });
    } catch (error) {
      this.setData({ loading: false });
    }
  },

  // 打开编辑弹层（form 已在 load 时用后端值初始化）
  openEdit() {
    this.setData({ editOpen: true });
  },

  // 关闭弹层，不提交
  closeEdit() {
    this.setData({ editOpen: false });
  },

  // 通用字段输入：按 data-field 写入 form
  onField(e) {
    this.setData({ ['form.' + e.currentTarget.dataset.field]: e.detail.value });
  },

  // 性别 picker：存「男/女」原文
  onGender(e) {
    this.setData({ 'form.gender': this.data.genders[Number(e.detail.value)] });
  },

  // 出生日期 picker：存 yyyy-MM-dd
  onBirth(e) {
    this.setData({ 'form.birthDate': e.detail.value });
  },

  // 保存资料：提交整个 form（后端只接受性别/生日/学校/年级）；成功后关弹层并回读
  async save() {
    if (this.data.saving) return;
    this.setData({ saving: true });
    try {
      await api.put('/mini/parent/children/' + this.data.childId, this.data.form);
      wx.showToast({ title: '资料已更新', icon: 'success' });
      this.setData({ editOpen: false });
      this.load();
    } catch (error) {
      // request 已提示
    } finally {
      this.setData({ saving: false });
    }
  },

  // 查看该学员课时账户（带 studentId，避免用错全局学员）
  goHours() {
    wx.navigateTo({ url: '/pages/hours/hours?studentId=' + this.data.childId });
  },

  // 查看该学员成长档案
  goGrowth() {
    wx.navigateTo({ url: '/pages/growth/growth?studentId=' + this.data.childId });
  },

  // 查看课表（使用全局当前学员）
  goSchedule() {
    wx.navigateTo({ url: '/pages/schedule/schedule' });
  },

  // 进入账号/绑定管理
  goAccount() {
    wx.navigateTo({ url: '/pages/account/account' });
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
  }
});
