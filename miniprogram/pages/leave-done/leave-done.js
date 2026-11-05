/**
 * 请假提交成功页：展示请假结果与关联课次。
 * 接口：无（结果由请假页写入 Storage）
 * 参数：无（读取 hanmo_leave_result）
 * 需登录：否（来源页已完成家长校验）
 */

const view = require('../../utils/view');

Page({
  data: { result: null, lesson: null },

  // 读请假结果；无缓存时回首页；关联课次用 view.lesson 补时间/星期文案
  onLoad() {
    const result = wx.getStorageSync('hanmo_leave_result');
    if (!result) {
      wx.switchTab({ url: '/pages/home/home' });
      return;
    }
    this.setData({ result, lesson: result.lesson ? view.lesson(result.lesson) : null });
  },

  // 查看请假记录（redirectTo 替换当前页）
  goRecords() {
    wx.redirectTo({ url: '/pages/leave-records/leave-records' });
  },

  // 回课表（tabBar 之外，用 redirectTo 避免栈里残留结果页）
  goSchedule() {
    wx.redirectTo({ url: '/pages/schedule/schedule' });
  }
});
