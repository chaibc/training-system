const view = require('../../utils/view');

Page({
  data: { result: null, lesson: null },

  onLoad() {
    const result = wx.getStorageSync('hanmo_leave_result');
    if (!result) {
      wx.switchTab({ url: '/pages/home/home' });
      return;
    }
    this.setData({ result, lesson: result.lesson ? view.lesson(result.lesson) : null });
  },

  goRecords() {
    wx.redirectTo({ url: '/pages/leave-records/leave-records' });
  },

  goSchedule() {
    wx.redirectTo({ url: '/pages/schedule/schedule' });
  }
});
