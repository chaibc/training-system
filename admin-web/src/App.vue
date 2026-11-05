/**
 * 应用根组件：只负责全局 antd 主题与中文语言包，具体页面由 router-view 渲染。
 */
<script setup>
import zhCN from 'ant-design-vue/es/locale/zh_CN'

// 全局主题令牌：赭红主色 + 米白底色，改这里会影响全站所有 antd 组件的配色与圆角
const theme = {
  token: {
    colorPrimary: '#B4473C',
    colorInfo: '#2E6A9E',
    colorSuccess: '#5C7A63',
    colorWarning: '#C2A05E',
    colorError: '#B4473C',
    colorBgBase: '#FFFDF8',
    colorText: '#232019',
    colorTextSecondary: '#5F594E',
    colorBorder: '#E7DFD0',
    borderRadius: 8,
    fontFamily: '"PingFang SC","Microsoft YaHei",system-ui,sans-serif',
    fontSize: 13
  },
  components: {
    Table: { headerBg: '#F9F5EC', headerColor: '#5F594E', rowHoverBg: '#FBF7EF' },
    Card: { colorBorderSecondary: '#E7DFD0' },
    Button: { controlHeight: 32 }
  }
}
</script>

<!-- 全局配置：注入品牌主题与中文语言包（分页、日期、空状态等内置文案） -->
<template>
  <a-config-provider :locale="zhCN" :theme="theme">
    <!-- 登录页与后台主框架都渲染在这里，具体路由见 router/index.js -->
    <router-view />
  </a-config-provider>
</template>
