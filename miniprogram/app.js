/**
 * 小程序入口：注册全局 App，维护「登录用户」与「当前选中学员」两个全局状态。
 * 接口：无（本文件不直接发请求，登录/业务接口由各页面自行调用）
 * 参数：无
 * 需登录：否；登录态由 utils/auth 从 Storage 读取后同步到 globalData
 */

const auth = require('./utils/auth');

App({
  // 全局状态：user 是登录用户快照；selectedChildId 是家长端当前学员（多子女共用）
  globalData: {
    user: null,
    selectedChildId: null
  },

  // 启动时把本地登录用户同步到 globalData，并注册新版本下载完成后的重启提示
  onLaunch() {
    this.globalData.user = auth.getUser();
    const updateManager = wx.getUpdateManager && wx.getUpdateManager();
    if (updateManager) {
      updateManager.onUpdateReady(() => {
        wx.showModal({
          title: '发现新版本',
          content: '新版本已准备好，是否重启应用？',
          success: res => {
            if (res.confirm) updateManager.applyUpdate();
          }
        });
      });
    }
  },

  // 每次回到前台重新读 Storage，避免别处登录/退出后 globalData 仍是旧用户
  onShow() {
    this.globalData.user = auth.getUser();
  },

  // 切换当前学员：同时写 globalData 与本地缓存 hanmo_child，小程序重启后仍保持选择
  setChild(id) {
    this.globalData.selectedChildId = id;
    wx.setStorageSync('hanmo_child', id);
  },

  // 取当前学员：内存优先、缓存兜底；返回 null 表示未选择或不适用（教师端不使用）
  getChild() {
    return this.globalData.selectedChildId || wx.getStorageSync('hanmo_child') || null;
  }
});
