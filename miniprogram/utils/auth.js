function getUser() {
  return wx.getStorageSync('hanmo_user') || null;
}

function getToken() {
  return wx.getStorageSync('hanmo_token') || '';
}

function isLoggedIn() {
  return !!getToken() && !!getUser();
}

function role() {
  const user = getUser();
  return user ? user.roleCode : '';
}

function setLogin(data) {
  wx.setStorageSync('hanmo_token', data.token);
  wx.setStorageSync('hanmo_user', {
    userId: data.userId,
    name: data.name,
    userType: data.userType,
    roleCode: data.roleCode,
    roleName: data.roleName,
    dataScope: data.dataScope,
    permissions: data.permissions || []
  });
}

function clear() {
  wx.removeStorageSync('hanmo_token');
  wx.removeStorageSync('hanmo_user');
}

function redirectLogin(message) {
  if (message) wx.showToast({ title: message, icon: 'none' });
  setTimeout(() => {
    wx.navigateTo({ url: '/pages/login/login' });
  }, message ? 350 : 0);
}

function requireLogin() {
  if (!isLoggedIn()) {
    redirectLogin('请先登录');
    return false;
  }
  return true;
}

function requireParent() {
  if (!requireLogin()) return false;
  if (role() !== 'PARENT') {
    wx.showToast({ title: '当前为教师账号，请使用教师工作台', icon: 'none' });
    return false;
  }
  return true;
}

function requireTeacher() {
  if (!requireLogin()) return false;
  if (role() !== 'TEACHER') {
    wx.showToast({ title: '当前账号不是教师', icon: 'none' });
    return false;
  }
  return true;
}

module.exports = {
  getUser,
  getToken,
  isLoggedIn,
  role,
  setLogin,
  clear,
  redirectLogin,
  requireLogin,
  requireParent,
  requireTeacher
};
