// 开发环境联调：微信开发者工具需勾选「不校验合法域名、web-view（业务域名）、TLS 版本以及 HTTPS 证书」
// 上线前改为 production，并把域名替换为已备案并配置到小程序后台的 HTTPS 域名
const ENV = 'development';

const CONFIG = {
  development: {
    baseUrl: 'http://127.0.0.1:8080',
    apiBase: 'http://127.0.0.1:8080/api'
  },
  production: {
    baseUrl: 'https://your-domain.com',
    apiBase: 'https://your-domain.com/api'
  }
};

module.exports = {
  env: ENV,
  ...CONFIG[ENV]
};
