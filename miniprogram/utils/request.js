/**
 * 网络请求封装：拼 API 前缀、注入 Bearer Token、解析统一响应结构，并提供上传与图片地址补全。
 * 接口：不绑定具体 URL；get/post/put/del 的 url 为 config.apiBase 之后的路径（如 /public/home）。
 * 需登录：否；无 token 时不带 Authorization 头，由后端对受保护接口返回 401。
 */

const config = require('../config');

// 每次请求现取 JWT，登录/退出后无需重启小程序也不会用到旧 token
function token() {
  return wx.getStorageSync('hanmo_token') || '';
}

// 图片地址补全规则：
//  1) 空值返回空串；
//  2) http(s) 开头的绝对地址原样返回；
//  3) 仅 /uploads/ 开头的相对路径拼 config.baseUrl（后端把上传目录静态映射在根路径）；
//  4) 其他相对路径按原样返回，不做拼接（避免把业务路径误当静态资源）。
function fullUrl(path) {
  if (!path) return '';
  if (/^https?:\/\//.test(path)) return path;
  if (path.startsWith('/uploads/')) return config.baseUrl + path;
  return path;
}

// 通用请求：Promise 化 wx.request；成功 resolve 业务 data，失败 reject { statusCode, code, message }。
// options.silent = true 时抑制 Toast（公开页/后台刷新用），但 reject 行为不变。
function request(method, url, data, options) {
  const opts = options || {};
  return new Promise((resolve, reject) => {
    wx.request({
      url: config.apiBase + url,
      method,
      data,
      // 统一 15 秒超时；超时/断网走 fail 回调，统一提示「网络异常」
      timeout: 15000,
      header: {
        'Content-Type': 'application/json',
        // 已登录才注入 Bearer Token；公开接口不带也能访问
        ...(token() ? { Authorization: 'Bearer ' + token() } : {})
      },
      success(res) {
        const body = res.data || {};
        // 统一响应结构 { code, message, data }：HTTP 2xx 且 code === 0 才算成功
        // 成功时只把 body.data 交给页面，页面不感知外层包装
        if (res.statusCode >= 200 && res.statusCode < 300 && body.code === 0) {
          resolve(body.data);
          return;
        }
        // 401 = token 缺失/过期：清掉本地凭证，避免页面继续按已登录渲染；
        // 这里只提示不自动跳转，跳登录页由各页面守卫决定（公开页可继续浏览）。
        if (res.statusCode === 401) {
          wx.removeStorageSync('hanmo_token');
          wx.removeStorageSync('hanmo_user');
          if (!opts.silent) {
            wx.showToast({ title: body.message || '登录已失效，请重新登录', icon: 'none' });
          }
        } else if (!opts.silent) {
          wx.showToast({ title: body.message || '请求失败', icon: 'none' });
        }
        // reject 保留后端 message 与业务 code，便于页面按 code 做特殊分支
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

// 上传文件：wx.uploadFile，字段名固定 file，bizType 默认 work；后端 /api/upload 返回 { id, url, name, size }（页面只用 url）。
// 上传失败没有 silent 选项，失败一律 Toast。
function upload(filePath, bizType) {
  return new Promise((resolve, reject) => {
    // 上传走独立通道，header 只带 Authorization，Content-Type 由 wx.uploadFile 自动生成 multipart
    wx.uploadFile({
      url: config.apiBase + '/upload',
      filePath,
      name: 'file',
      formData: { bizType: bizType || 'work' },
      header: token() ? { Authorization: 'Bearer ' + token() } : {},
      success(res) {
        // uploadFile 的 res.data 是字符串，必须手动 JSON.parse；解析失败按空对象处理，避免抛异常
        let body = {};
        try {
          body = JSON.parse(res.data || '{}');
        } catch (e) {
          body = {};
        }
        // 上传接口只认 200 + code === 0（与普通请求的 2xx 判定略有不同）
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

// 对外暴露：四个 HTTP 方法 + upload（上传）+ fullUrl（图片补全）+ token（读 token）
module.exports = {
  get: (url, data, options) => request('GET', url, data, options),
  post: (url, data, options) => request('POST', url, data, options),
  put: (url, data, options) => request('PUT', url, data, options),
  del: (url, data, options) => request('DELETE', url, data, options),
  upload,
  fullUrl,
  token
};
