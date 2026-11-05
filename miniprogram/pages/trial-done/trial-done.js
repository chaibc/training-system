Page({
  data: {
    result: null
  },

  onLoad() {
    const result = wx.getStorageSync('hanmo_trial_result');
    if (!result) {
      wx.switchTab({ url: '/pages/home/home' });
      return;
    }
    this.setData({ result });
  },

  goHome() {
    wx.switchTab({ url: '/pages/home/home' });
  },

  goCourses() {
    wx.switchTab({ url: '/pages/courses/courses' });
  }
});
