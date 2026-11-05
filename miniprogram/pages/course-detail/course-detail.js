/**
 * 课程详情页：展示课程介绍、价格方案与课程大纲，引导试听/报名。
 * 接口：GET /api/public/courses/{id}
 * 参数：id（课程 ID，必填）
 * 需登录：否；报名入口要求家长登录，游客点报名会被守卫拦下
 */

const api = require('../../utils/request');
const view = require('../../utils/view');
const auth = require('../../utils/auth');

Page({
  data: {
    id: null,
    course: null,
    prices: [],
    outlines: [],
    loading: true,
    isParent: false,
    isTeacher: false,
    guest: true
  },

  // 记录课程 ID 与访客/家长/教师身份，用于控制报名、登录入口的展示与跳转
  onLoad(options) {
    const user = auth.getUser();
    this.setData({
      id: options.id,
      guest: !auth.isLoggedIn(),
      isParent: user && user.roleCode === 'PARENT',
      isTeacher: user && user.roleCode === 'TEACHER'
    });
    this.load();
  },

  // 拉课程详情；价格方案的 unit 由 TIME/其他转成「次/期」，大纲直接透传
  async load() {
    try {
      const data = await api.get('/public/courses/' + this.data.id);
      const course = view.course(data);
      const prices = (data.prices || []).map(p => ({
        ...p,
        unitText: p.unit === 'TIME' ? '次' : '期',
        priceText: Number(p.price || 0).toFixed(0)
      }));
      this.setData({ course, prices, outlines: data.outlines || [], loading: false });
      // 标题用课程名，接口失败时保留默认标题
      wx.setNavigationBarTitle({ title: course.name || '课程详情' });
    } catch (error) {
      this.setData({ loading: false });
    }
  },

  // 试听预约：带上 courseId 预选本课程
  goTrial() {
    wx.navigateTo({ url: '/pages/trial/trial?courseId=' + this.data.id });
  },

  // 报名：requireParent 未通过会自动跳登录或提示改用教师工作台
  goEnroll() {
    if (!auth.requireParent()) return;
    wx.navigateTo({ url: '/pages/enroll/enroll?courseId=' + this.data.id });
  },

  // 游客点登录
  goLogin() {
    wx.navigateTo({ url: '/pages/login/login' });
  },

  // 联系机构：电话来自首页写入的 globalData.institutionPhone；直接进本页时可能为空
  call() {
    const phone = getApp().globalData.institutionPhone;
    if (!phone) {
      wx.showToast({ title: '暂未维护联系电话', icon: 'none' });
      return;
    }
    wx.makePhoneCall({ phoneNumber: phone.replace(/[^\d]/g, '') });
  },

  // 分享课程详情（带课程 ID）
  onShareAppMessage() {
    return {
      title: this.data.course ? this.data.course.name : '翰墨学堂课程',
      path: '/pages/course-detail/course-detail?id=' + this.data.id
    };
  }
});
