const api = require('../../utils/request');
const req = require('../../utils/request');

Page({
  data: {
    loading: true,
    institution: null,
    images: [],
    qualifications: []
  },

  onLoad() {
    this.load();
  },

  onPullDownRefresh() {
    this.load().finally(() => wx.stopPullDownRefresh());
  },

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
      if (institution && institution.phone) getApp().globalData.institutionPhone = institution.phone;
    } catch (error) {
      this.setData({ loading: false });
    }
  },

  call() {
    const phone = this.data.institution && this.data.institution.phone;
    if (!phone) return wx.showToast({ title: '暂未维护联系电话', icon: 'none' });
    wx.makePhoneCall({ phoneNumber: phone.replace(/[^\d]/g, '') });
  },

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

  goTeachers() {
    wx.navigateTo({ url: '/pages/teachers/teachers' });
  },

  goWorks() {
    wx.switchTab({ url: '/pages/works/works' });
  },

  previewImage(e) {
    const item = this.data.images[e.currentTarget.dataset.index];
    if (item && item.url) wx.previewImage({ current: item.url, urls: [item.url] });
  },

  onShareAppMessage() {
    return { title: '翰墨学堂 · 机构介绍', path: '/pages/about/about' };
  }
});
