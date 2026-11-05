/**
 * 机构介绍页：展示机构简介、环境图片与资质证书，支持拨号、地图导航与分享。
 * 接口：GET /api/public/home（institution 机构信息、images 环境图、qualifications 资质）
 * 参数：无
 * 需登录：否（公开页）
 */

const api = require('../../utils/request');
const req = require('../../utils/request');

Page({
  data: {
    loading: true,
    institution: null,
    images: [],
    qualifications: []
  },

  // 进入页面即拉取机构公开信息
  onLoad() {
    this.load();
  },

  // 下拉刷新后无论成功失败都要 stopPullDownRefresh，否则动画不消失
  onPullDownRefresh() {
    this.load().finally(() => wx.stopPullDownRefresh());
  },

  // 拉机构信息；环境图地址用 fullUrl 补全，再按序号循环取占位字与主题色（纯展示）
  async load() {
    try {
      const data = await api.get('/public/home');
      const institution = data.institution || null;
      const images = (institution && institution.images ? institution.images : []).map((img, index) => ({
        ...img,
        url: req.fullUrl(img.imageUrl),
        char: ['室', '帖', '作'][index % 3],
        color: ['#6B503A', '#4C5544', '#76524A'][index % 3]
      }));
      this.setData({
        institution,
        images,
        qualifications: data.qualifications || [],
        loading: false
      });
      // 电话写入 globalData，其他页面「联系机构」可直接复用
      if (institution && institution.phone) getApp().globalData.institutionPhone = institution.phone;
    } catch (error) {
      this.setData({ loading: false });
    }
  },

  // 拨号：机构未维护电话时只提示；拨号前去掉非数字字符
  call() {
    const phone = this.data.institution && this.data.institution.phone;
    if (!phone) return wx.showToast({ title: '暂未维护联系电话', icon: 'none' });
    wx.makePhoneCall({ phoneNumber: phone.replace(/[^\d]/g, '') });
  },

  // 打开地图：缺少经纬度不调起，避免定位到 (0,0)
  openMap() {
    const inst = this.data.institution;
    if (!inst || !inst.latitude || !inst.longitude) return wx.showToast({ title: '暂未维护地图坐标', icon: 'none' });
    wx.openLocation({
      latitude: Number(inst.latitude),
      longitude: Number(inst.longitude),
      name: inst.name || '翰墨学堂',
      address: inst.address || '',
      scale: 16
    });
  },

  // 跳师资列表
  goTeachers() {
    wx.navigateTo({ url: '/pages/teachers/teachers' });
  },

  // 跳作品墙（tabBar 页用 switchTab）
  goWorks() {
    wx.switchTab({ url: '/pages/works/works' });
  },

  // 预览环境图：只预览当前一张，无地址时不响应
  previewImage(e) {
    const item = this.data.images[e.currentTarget.dataset.index];
    if (item && item.url) wx.previewImage({ current: item.url, urls: [item.url] });
  },

  // 分享机构介绍页
  onShareAppMessage() {
    return { title: '翰墨学堂 · 机构介绍', path: '/pages/about/about' };
  }
});
