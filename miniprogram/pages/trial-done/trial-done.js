/**
 * 试听预约成功页：展示预约结果。
 * 接口：无（结果由试听页写入 Storage）
 * 参数：无（读取 hanmo_trial_result）
 * 需登录：否
 */

Page({
  data: {
    result: null
  },

  // 读预约结果；无缓存（直接进入）时回首页，避免空白页
  onLoad() {
    const result = wx.getStorageSync('hanmo_trial_result');
    if (!result) {
      wx.switchTab({ url: '/pages/home/home' });
      return;
    }
    this.setData({ result });
  },

  // 回首页（tabBar 页用 switchTab）
  goHome() {
    wx.switchTab({ url: '/pages/home/home' });
  },

  // 继续看课程（tabBar 页用 switchTab）
  goCourses() {
    wx.switchTab({ url: '/pages/courses/courses' });
  }
});
