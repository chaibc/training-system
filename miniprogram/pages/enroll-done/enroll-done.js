Page({
  data: { result: null },

  onLoad() {
    const result = wx.getStorageSync('hanmo_enroll_result');
    if (!result) {
      wx.switchTab({ url: '/pages/home/home' });
      return;
    }
    this.setData({ result });
  },

  goRecords() {
    wx.redirectTo({ url: '/pages/enrollments/enrollments' });
  },

  goHome() {
    wx.switchTab({ url: '/pages/home/home' });
  }
});
