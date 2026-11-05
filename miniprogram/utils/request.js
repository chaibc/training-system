const config = require('../config');

function token() {
  return wx.getStorageSync('hanmo_token') || '';
}

function fullUrl(path) {
  if (!path) return '';
  if (/^https?:\/\//.test(path)) return path;
  if (path.startsWith('/uploads/')) return config.baseUrl + path;
  return path;
}

function request(method, url, data, options) {
  const opts = options || {};
  return new Promise((resolve, reject) => {
    wx.request({
      url: config.apiBase + url,
      method,
      data,
      timeout: 15000,
      header: {
        'Content-Type': 'application/json',
        ...(token() ? { Authorization: 'Bearer ' + token() } : {})
      },
      success(res) {
        const body = res.data || {};
        if (res.statusCode >= 200 && res.statusCode < 300 && body.code === 0) {
          resolve(body.data);
          return;
        }
        if (res.statusCode === 401) {
          wx.removeStorageSync('hanmo_token');
          wx.removeStorageSync('hanmo_user');
          if (!opts.silent) {
            wx.showToast({ title: body.message || '登录已失效，请重新登录', icon: 'none' });
          }
        } else if (!opts.silent) {
          wx.showToast({ title: body.message || '请求失败', icon: 'none' });
        }
        reject({ statusCode: res.statusCode, code: body.code, message: body.message || '请求失败' });
      },
      fail(error) {
        if (!opts.silent) {
          wx.showToast({ title: '网络异常，请检查后端服务', icon: 'none' });
        }
        reject(error);
      }
    });
  });
}

function upload(filePath, bizType) {
  return new Promise((resolve, reject) => {
    wx.uploadFile({
      url: config.apiBase + '/upload',
      filePath,
      name: 'file',
      formData: { bizType: bizType || 'work' },
      header: token() ? { Authorization: 'Bearer ' + token() } : {},
      success(res) {
        let body = {};
        try {
          body = JSON.parse(res.data || '{}');
        } catch (e) {
          body = {};
        }
        if (res.statusCode === 200 && body.code === 0) {
          resolve(body.data);
          return;
        }
        wx.showToast({ title: body.message || '上传失败', icon: 'none' });
        reject(body);
      },
      fail(error) {
        wx.showToast({ title: '上传失败', icon: 'none' });
        reject(error);
      }
    });
  });
}

module.exports = {
  get: (url, data, options) => request('GET', url, data, options),
  post: (url, data, options) => request('POST', url, data, options),
  put: (url, data, options) => request('PUT', url, data, options),
  del: (url, data, options) => request('DELETE', url, data, options),
  upload,
  fullUrl,
  token
};
