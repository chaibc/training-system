const api = require('../../utils/request');
const auth = require('../../utils/auth');
const view = require('../../utils/view');
const fmt = require('../../utils/format');

Page({
  data: {
    id: null,
    work: null,
    comments: [],
    commentText: '',
    loading: true,
    liked: false,
    liking: false,
    submitting: false,
    isParent: false,
    isTeacher: false,
    guest: true
  },

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

  async load() {
    try {
      const data = await api.get('/public/works/' + this.data.id);
      const work = view.work(data);
      work.createdText = fmt.dateTime(data.createdAt);
      const comments = (data.comments || []).map(c => ({
        ...c,
        avatarChar: (c.userName || '家').slice(0, 1),
        timeText: fmt.dateTime(c.createdAt)
      }));
      this.setData({ work, comments, loading: false });
      wx.setNavigationBarTitle({ title: data.title || '作品详情' });
    } catch (error) {
      this.setData({ loading: false });
    }
  },

  async like() {
    if (this.data.liked || this.data.liking) return;
    this.setData({ liking: true });
    try {
      const data = await api.post('/public/works/' + this.data.id + '/like');
      this.setData({
        liked: true,
        'work.likeCount': data.likeCount,
        liking: false
      });
      wx.showToast({ title: '已点赞', icon: 'success' });
    } catch (error) {
      this.setData({ liking: false });
    }
  },

  preview() {
    if (this.data.work && this.data.work.image) {
      wx.previewImage({ current: this.data.work.image, urls: [this.data.work.image] });
    }
  },

  onCommentInput(e) {
    this.setData({ commentText: e.detail.value });
  },

  requireParent() {
    if (!auth.requireLogin()) return false;
    if (!this.data.isParent) {
      wx.showToast({ title: '教师账号不支持家长留言', icon: 'none' });
      return false;
    }
    return true;
  },

  async submitComment() {
    if (this.data.submitting) return;
    if (!this.requireParent()) return;
    const content = this.data.commentText.trim();
    if (!content) return wx.showToast({ title: '请输入留言内容', icon: 'none' });
    this.setData({ submitting: true });
    try {
      await api.post('/mini/parent/works/' + this.data.id + '/comments', { content });
      this.setData({ commentText: '', submitting: false });
      wx.showToast({ title: '留言已提交，审核后展示', icon: 'none' });
    } catch (error) {
      this.setData({ submitting: false });
    }
  },

  goLogin() {
    wx.navigateTo({ url: '/pages/login/login' });
  },

  onShareAppMessage() {
    const work = this.data.work;
    return {
      title: work ? work.title + ' · ' + work.studentName : '翰墨学堂学员作品',
      path: '/pages/work-detail/work-detail?id=' + this.data.id,
      imageUrl: work && work.image ? work.image : ''
    };
  }
});
