const api = require('../../utils/request');
const auth = require('../../utils/auth');
const config = require('../../config');

Page({
  data: {
    loading: false,
    teacherOpen: false,
    phone: '',
    devOpen: false,
    devCode: 'seed_parent_wang',
    isDev: config.env === 'development',
    loggedIn: false
  },

  onLoad() {
    this.setData({ loggedIn: auth.isLoggedIn() });
  },

  onShow() {
    this.setData({ loggedIn: auth.isLoggedIn() });
  },

  toggleDev() {
    this.setData({ devOpen: !this.data.devOpen });
  },

  toggleTeacher() {
    this.setData({ teacherOpen: !this.data.teacherOpen });
  },

  onPhoneInput(e) {
    this.setData({ phone: e.detail.value.trim() });
  },

  onDevCodeInput(e) {
    this.setData({ devCode: e.detail.value.trim() });
  },

  wxCode() {
    return new Promise((resolve, reject) => {
      wx.login({
        success: res => {
          if (res.code) resolve(res.code);
          else reject(new Error('微信登录失败'));
        },
        fail: reject
      });
    });
  },

  async parentLogin() {
    if (this.data.loading) return;
    this.setData({ loading: true });
    try {
      const code = this.data.isDev && this.data.devOpen ? this.data.devCode : await this.wxCode();
      const data = await api.post('/auth/wx-login', { code }, { silent: true });
      this.finish(data);
    } catch (error) {
      wx.showToast({ title: (error && error.message) || '登录失败', icon: 'none' });
    } finally {
      this.setData({ loading: false });
    }
  },

  async teacherLogin() {
    if (this.data.loading) return;
    const phone = this.data.phone;
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

  browse() {
    wx.switchTab({ url: '/pages/home/home' });
  },

  enter() {
    const user = auth.getUser();
    if (!user) return;
    if (user.roleCode === 'TEACHER') wx.reLaunch({ url: '/pages/teacher-home/teacher-home' });
    else wx.switchTab({ url: '/pages/home/home' });
  }
});
