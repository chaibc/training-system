<script setup>
import zhCN from 'ant-design-vue/es/locale/zh_CN'

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

<template>
  <a-config-provider :locale="zhCN" :theme="theme">
    <router-view />
  </a-config-provider>
</template>
