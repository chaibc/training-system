const api = require('../../utils/request');
const auth = require('../../utils/auth');
const view = require('../../utils/view');
const req = require('../../utils/request');

Page({
  data: {
    loading: true,
    institution: null,
    courses: [],
    teachers: [],
    works: [],
    notices: [],
    qualifications: [],
    images: [],
    user: null,
    greeting: '您好',
    guest: true,
    isTeacher: false,
    child: null,
    pendingLeaves: 0,
    pendingBindings: 0
  },

  onLoad() {
    this.loadPublic();
  },

  onShow() {
    const user = auth.getUser();
    this.setData({
      user,
      guest: !auth.isLoggedIn(),
      isTeacher: user && user.roleCode === 'TEACHER'
    });
    if (auth.isLoggedIn() && user.roleCode === 'PARENT') this.loadOverview();
  },

  onPullDownRefresh() {
    Promise.all([this.loadPublic(), this.loadOverview()]).finally(() => wx.stopPullDownRefresh());
  },

  async loadPublic() {
    try {
      const data = await api.get('/public/home', {}, { silent: true });
      const inst = data.institution || null;
      const images = (inst && inst.images ? inst.images : []).map((img, index) => ({
        ...img,
        url: req.fullUrl(img.imageUrl),
        char: ['室', '帖', '作'][index % 3],
        color: ['#6B503A', '#4C5544', '#76524A'][index % 3]
      }));
      this.setData({
        institution: inst,
        courses: (data.courses || []).map(view.course),
        teachers: (data.teachers || []).map(t => ({
          ...t,
          avatarChar: (t.name || '师').slice(0, 1),
          styleText: t.styles || t.coursesText || ''
        })),
        works: (data.works || []).map(view.work).slice(0, 4),
        notices: data.notices || [],
        qualifications: data.qualifications || [],
        images,
        loading: false
      });
      if (inst && inst.phone) getApp().globalData.institutionPhone = inst.phone;
    } catch (error) {
      this.setData({ loading: false });
    }
  },

  async loadOverview() {
    try {
      const data = await api.get('/mini/parent/overview', {}, { silent: true });
      const hour = new Date().getHours();
      const greeting = hour < 11 ? '早上好' : hour < 14 ? '中午好' : hour < 18 ? '下午好' : '晚上好';
      const child = data.children && data.children.length
        ? view.child(data.children.find(c => c.id === getApp().getChild()) || data.children[0]) : null;
      if (child) getApp().setChild(child.id);
      this.setData({
        greeting,
        child,
        pendingLeaves: data.pendingLeaves || 0,
        pendingBindings: data.pendingBindings || 0
      });
    } catch (error) {
      // 静默失败，不影响公开首页
    }
  },

  goCourses() {
    wx.switchTab({ url: '/pages/courses/courses' });
  },

  goWorks() {
    wx.switchTab({ url: '/pages/works/works' });
  },

  goTrial() {
    wx.navigateTo({ url: '/pages/trial/trial' });
  },

  goTeachers() {
    wx.navigateTo({ url: '/pages/teachers/teachers' });
  },

  goAbout() {
    wx.navigateTo({ url: '/pages/about/about' });
  },

  goLogin() {
    wx.navigateTo({ url: '/pages/login/login' });
  },

  goCourse(e) {
    wx.navigateTo({ url: '/pages/course-detail/course-detail?id=' + e.currentTarget.dataset.id });
  },

  goTeacher(e) {
    wx.navigateTo({ url: '/pages/teacher-detail/teacher-detail?id=' + e.currentTarget.dataset.id });
  },

  goWork(e) {
    wx.navigateTo({ url: '/pages/work-detail/work-detail?id=' + e.currentTarget.dataset.id });
  },

  goSchedule() {
    if (!auth.requireParent()) return;
    wx.navigateTo({ url: '/pages/schedule/schedule' });
  },

  goHours() {
    if (!auth.requireParent()) return;
    wx.navigateTo({ url: '/pages/hours/hours' });
  },

  goEnrollments() {
    if (!auth.requireParent()) return;
    wx.navigateTo({ url: '/pages/enrollments/enrollments' });
  },

  goLeaveRecords() {
    if (!auth.requireParent()) return;
    wx.navigateTo({ url: '/pages/leave-records/leave-records' });
  },

  goWorkbench() {
    wx.reLaunch({ url: '/pages/teacher-home/teacher-home' });
  },

  showNotice(e) {
    const notice = this.data.notices[e.currentTarget.dataset.index];
    if (!notice) return;
    wx.showModal({
      title: notice.title,
      content: notice.content || '',
      showCancel: false,
      confirmText: '知道了'
    });
  },

  call() {
    const phone = this.data.institution && this.data.institution.phone;
    if (!phone) {
      wx.showToast({ title: '暂未维护联系电话', icon: 'none' });
      return;
    }
    wx.makePhoneCall({ phoneNumber: phone.replace(/[^\d]/g, '') });
  },

  openMap() {
    const inst = this.data.institution;
    if (!inst || !inst.latitude || !inst.longitude) {
      wx.showToast({ title: '暂未维护地图坐标', icon: 'none' });
      return;
    }
    wx.openLocation({
      latitude: Number(inst.latitude),
      longitude: Number(inst.longitude),
      name: inst.name || '翰墨学堂',
      address: inst.address || '',
      scale: 16
    });
  },

  previewWork(e) {
    const item = this.data.works[e.currentTarget.dataset.index];
    if (item && item.image) {
      wx.previewImage({ current: item.image, urls: [item.image] });
    } else {
      wx.navigateTo({ url: '/pages/work-detail/work-detail?id=' + item.id });
    }
  },

  onShareAppMessage() {
    return {
      title: '翰墨学堂 · 一笔一画，写好中国字',
      path: '/pages/home/home'
    };
  }
});
