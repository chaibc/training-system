/**
 * 报名提交成功页：展示报名结果与后续提示。
 * 接口：无（结果由报名页写入 Storage 后带过来）
 * 参数：无（读取 hanmo_enroll_result）
 * 需登录：否（来源页已完成家长校验）
 */

Page({
  data: { result: null },

  // 读报名结果；无缓存（直接进入或已被清理）时回首页，避免空白页
  onLoad() {
    const result = wx.getStorageSync('hanmo_enroll_result');
    if (!result) {
      wx.switchTab({ url: '/pages/home/home' });
      return;
    }
    this.setData({ result });
  },

  // 用 redirectTo 替换当前页，返回时不会回到已提交的表单
  goRecords() {
    wx.redirectTo({ url: '/pages/enrollments/enrollments' });
  },

  // 回首页（tabBar 页用 switchTab）
  goHome() {
    wx.switchTab({ url: '/pages/home/home' });
  }
});
