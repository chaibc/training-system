/**
 * 作品详情页：展示作品大图、作者与留言，支持点赞、家长留言、分享。
 * 接口：GET /api/public/works/{id}；POST /api/public/works/{id}/like；
 *       POST /api/mini/parent/works/{id}/comments
 * 参数：id（作品 ID，必填）
 * 需登录：否；点赞/留言需要登录，留言仅限家长
 */

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

  // 记录作品 ID 与访客/家长/教师身份，控制登录与留言入口展示
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

  // 公开作品详情：图片由 view.work 补全；留言补头像字与时间文案
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

  // 点赞：liked/liking 双重防重复点击；成功后只回写 likeCount，不重新拉详情
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

  // 预览作品大图
  preview() {
    if (this.data.work && this.data.work.image) {
      wx.previewImage({ current: this.data.work.image, urls: [this.data.work.image] });
    }
  },

  // 留言输入
  onCommentInput(e) {
    this.setData({ commentText: e.detail.value });
  },

  // 留言权限：先登录，再要求家长身份（教师账号不能给学员作品留言）
  requireParent() {
    if (!auth.requireLogin()) return false;
    if (!this.data.isParent) {
      wx.showToast({ title: '教师账号不支持家长留言', icon: 'none' });
      return false;
    }
    return true;
  },

  // 提交留言：内容非空才提交；留言需审核后才展示，所以提示文案不是「成功」
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

  // 游客登录入口
  goLogin() {
    wx.navigateTo({ url: '/pages/login/login' });
  },

  // 分享作品：标题带作品名与学员名，图片用作品图（有才带）
  onShareAppMessage() {
    const work = this.data.work;
    return {
      title: work ? work.title + ' · ' + work.studentName : '翰墨学堂学员作品',
      path: '/pages/work-detail/work-detail?id=' + this.data.id,
      imageUrl: work && work.image ? work.image : ''
    };
  }
});
