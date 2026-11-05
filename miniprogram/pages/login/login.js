/**
 * 登录页：微信一键登录；教师额外输入手机号匹配教师账号；开发环境提供演示账号与手填 code 入口。
 * 接口：POST /api/auth/wx-login（body: { code, phone? }）
 * 参数：无
 * 需登录：否（本页就是登录入口）
 */

const api = require('../../utils/request');
const auth = require('../../utils/auth');
const config = require('../../config');

Page({
  data: {
    loading: false,
    teacherOpen: false,
    phone: '',
    devOpen: false,
    // 开发模式默认演示 code：后端开发模式把 seed_* 直接当 openid 用，对应演示数据里的家长王女士
    devCode: 'seed_parent_wang',
    // 是否开发环境：由 config.env 决定，生产环境不展示开发面板
    isDev: config.env === 'development',
    loggedIn: false
  },

  // 进入页面先回显登录态：已登录时按钮变为「进入」，避免重复登录
  onLoad() {
    this.setData({ loggedIn: auth.isLoggedIn() });
  },

  // 返回前台时再同步一次登录态（例如从其他页退出登录后回到本页）
  onShow() {
    this.setData({ loggedIn: auth.isLoggedIn() });
  },

  // 展开/收起开发模式面板：可手填演示 code 直接登录后端种子数据
  toggleDev() {
    this.setData({ devOpen: !this.data.devOpen });
  },

  // 切换「教师登录」表单：教师必须再填手机号，后端按手机号匹配教师账号
  toggleTeacher() {
    const open = !this.data.teacherOpen;
    const patch = { teacherOpen: open };
    // 开发模式：教师表单默认切到教师演示 code，避免沿用家长 code 时后端返回
    // 409「当前微信已绑定其他账号」；收起表单时再切回家长 code
    if (this.data.isDev) {
      if (open && this.data.devCode === 'seed_parent_wang') patch.devCode = 'seed_teacher_wang';
      if (!open && this.data.devCode === 'seed_teacher_wang') patch.devCode = 'seed_parent_wang';
    }
    this.setData(patch);
  },

  // 教师手机号输入：去首尾空格后存 data.phone，供格式校验与提交
  onPhoneInput(e) {
    this.setData({ phone: e.detail.value.trim() });
  },

  // 开发模式手填 code：替换默认种子 code，便于切换不同演示账号
  onDevCodeInput(e) {
    this.setData({ devCode: e.detail.value.trim() });
  },

  // 调 wx.login 取一次性 code；拿不到 code 视为微信登录失败，交给调用方统一 Toast
  wxCode() {
    return new Promise((resolve, reject) => {
      // 只取 code，不把 code 存起来；code 一次性，重复使用会登录失败
      wx.login({
        success: res => {
          if (res.code) resolve(res.code);
          else reject(new Error('微信登录失败'));
        },
        fail: reject
      });
    });
  },

  // 家长登录：开发模式且展开面板时用演示 code，否则走真实 wx.login；登录成功后统一收尾
  async parentLogin() {
    if (this.data.loading) return;
    this.setData({ loading: true });
    try {
      const code = this.data.isDev && this.data.devOpen ? this.data.devCode : await this.wxCode();
      // 家长不带 phone：后端找不到 openid 时会自动创建家长账号
      const data = await api.post('/auth/wx-login', { code }, { silent: true });
      this.finish(data);
    } catch (error) {
      wx.showToast({ title: (error && error.message) || '登录失败', icon: 'none' });
    } finally {
      this.setData({ loading: false });
    }
  },

  // 教师登录：先本地校验手机号格式，再连同 code 提交；后端用手机号匹配教师账号
  async teacherLogin() {
    if (this.data.loading) return;
    const phone = this.data.phone;
    // 前端只校验格式，账号是否存在由后端判定（未匹配会返回 404 提示）
    if (!/^1\d{10}$/.test(phone)) {
      wx.showToast({ title: '请输入 11 位手机号', icon: 'none' });
      return;
    }
    this.setData({ loading: true });
    try {
      const code = this.data.isDev && this.data.devOpen ? this.data.devCode : await this.wxCode();
      const data = await api.post('/auth/wx-login', { code, phone }, { silent: true });
      this.finish(data);
    } catch (error) {
      wx.showToast({ title: (error && error.message) || '教师登录失败', icon: 'none' });
    } finally {
      this.setData({ loading: false });
    }
  },

  // 演示家长登录：固定用 seed_parent_wang，后端开发模式直接映射演示家长，不调微信接口
  async demoParent() {
    if (this.data.loading) return;
    this.setData({ loading: true });
    try {
      const data = await api.post('/auth/wx-login', { code: 'seed_parent_wang' }, { silent: true });
      this.finish(data);
    } catch (error) {
      wx.showToast({ title: (error && error.message) || '演示登录失败', icon: 'none' });
    } finally {
      this.setData({ loading: false });
    }
  },

  // 演示教师登录：seed_teacher_wang + 演示手机号 13800006621，后端按手机号匹配教师账号
  async demoTeacher() {
    if (this.data.loading) return;
    this.setData({ loading: true });
    try {
      const data = await api.post('/auth/wx-login', {
        code: 'seed_teacher_wang',
        phone: '13800006621'
      }, { silent: true });
      this.finish(data);
    } catch (error) {
      wx.showToast({ title: (error && error.message) || '演示登录失败', icon: 'none' });
    } finally {
      this.setData({ loading: false });
    }
  },

  // 登录收尾：凭证写 Storage 并同步 globalData；教师 reLaunch 进工作台，家长 switchTab 回首页
  finish(data) {
    auth.setLogin(data);
    const app = getApp();
    app.globalData.user = auth.getUser();
    wx.showToast({ title: '登录成功', icon: 'success' });
    setTimeout(() => {
      if (data.roleCode === 'TEACHER') {
        wx.reLaunch({ url: '/pages/teacher-home/teacher-home' });
      } else {
        wx.switchTab({ url: '/pages/home/home' });
      }
    }, 350);
  },

  // 不登录先逛逛：首页是 tabBar 页，必须 switchTab
  browse() {
    wx.switchTab({ url: '/pages/home/home' });
  },

  // 已登录时进入：按角色分流到教师工作台或家长首页
  enter() {
    const user = auth.getUser();
    if (!user) return;
    if (user.roleCode === 'TEACHER') wx.reLaunch({ url: '/pages/teacher-home/teacher-home' });
    else wx.switchTab({ url: '/pages/home/home' });
  }
});
