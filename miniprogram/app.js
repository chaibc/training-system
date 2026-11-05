const auth = require('./utils/auth');

App({
  globalData: {
    user: null,
    selectedChildId: null
  },

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

  onShow() {
    this.globalData.user = auth.getUser();
  },

  setChild(id) {
    this.globalData.selectedChildId = id;
    wx.setStorageSync('hanmo_child', id);
  },

  getChild() {
    return this.globalData.selectedChildId || wx.getStorageSync('hanmo_child') || null;
  }
});
