/**
 * 登录态工具：本地 token/用户快照的读写、角色判断、未登录跳转与页面守卫。
 * 存储键：hanmo_token（JWT）、hanmo_user（登录返回的用户资料快照）
 * 接口：无（登录请求由 pages/login 发起）
 * 需登录：本文件不强制；页面在 onLoad 里调用 requireLogin/requireParent/requireTeacher 做守卫
 */

function getUser() {
  return wx.getStorageSync('hanmo_user') || null;
}

// 读取本地 JWT；未登录返回空串，request 据此决定是否带 Authorization 头
function getToken() {
  return wx.getStorageSync('hanmo_token') || '';
}

// token 与用户快照必须同时存在才算登录；401 时 request 会把两者一起清掉
function isLoggedIn() {
  return !!getToken() && !!getUser();
}

// 取角色编码（PARENT / TEACHER / ADMIN 等），未登录返回空串
function role() {
  const user = getUser();
  return user ? user.roleCode : '';
}

// 登录成功落盘：token 单独存，其余字段与 /auth/wx-login 返回的 LoginResponse 一一对应
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

// 退出登录：只清本地凭证，不请求后端（小程序端 token 由客户端丢弃即可）
function clear() {
  wx.removeStorageSync('hanmo_token');
  wx.removeStorageSync('hanmo_user');
}

// 跳登录页：带提示时延迟 350ms，避免 Toast 还没显示就被页面切换盖掉
function redirectLogin(message) {
  if (message) wx.showToast({ title: message, icon: 'none' });
  setTimeout(() => {
    wx.navigateTo({ url: '/pages/login/login' });
  }, message ? 350 : 0);
}

// 页面守卫：未登录则提示并跳登录，返回 false，调用方应立即 return
function requireLogin() {
  if (!isLoggedIn()) {
    redirectLogin('请先登录');
    return false;
  }
  return true;
}

// 家长页守卫：先要求登录，再校验 roleCode；教师账号提示改用教师工作台
function requireParent() {
  if (!requireLogin()) return false;
  if (role() !== 'PARENT') {
    wx.showToast({ title: '当前为教师账号，请使用教师工作台', icon: 'none' });
    return false;
  }
  return true;
}

// 教师页守卫：先要求登录，再校验 roleCode 是否为 TEACHER
function requireTeacher() {
  if (!requireLogin()) return false;
  if (role() !== 'TEACHER') {
    wx.showToast({ title: '当前账号不是教师', icon: 'none' });
    return false;
  }
  return true;
}

// 对外暴露读取、写入与三类守卫方法，页面不直接操作 Storage 键名
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
