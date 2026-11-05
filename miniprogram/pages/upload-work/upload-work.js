/**
 * 教师上传作品页：选择学员、作品类型，上传作品图片并提交教务审核。
 * 接口：GET /api/mini/teacher/work-options；GET /api/mini/teacher/works；
 *       POST /api/upload（图片，bizType=work，返回 id/url/name/size）；POST /api/mini/teacher/works
 * 参数：无
 * 需登录：是，且必须是教师（TEACHER）
 */

const api = require('../../utils/request');
const auth = require('../../utils/auth');

Page({
  data: {
    loading: true,
    blocked: false,
    students: [],
    studentLabels: [],
    studentIndex: -1,
    selectedStudent: null,
    workTypes: [],
    workType: 'HARD',
    title: '',
    contentChar: '',
    comment: '',
    imagePath: '',
    imageUrl: '',
    uploading: false,
    authorized: false,
    submitting: false,
    myWorks: []
  },

  // 教师守卫；非教师置 blocked 不再请求
  onLoad() {
    if (!auth.requireTeacher()) {
      this.setData({ blocked: true, loading: false });
      return;
    }
    this.load();
  },

  // 并行取学员/作品类型选项与已上传作品；我的作品接口失败降级为空数组
  async load() {
    try {
      const [options, works] = await Promise.all([
        api.get('/mini/teacher/work-options'),
        api.get('/mini/teacher/works', {}, { silent: true }).catch(() => [])
      ]);
      const students = options.students || [];
      this.setData({
        students,
        // 学员下拉标注「未授权」：家长未授权展示的学员不能提交作品（validate 会再拦一次）
        studentLabels: students.map(s => s.name + ' · ' + s.className + (s.authorized ? '' : '（未授权）')),
        workTypes: options.workTypes || [],
        myWorks: (works || []).map(w => ({
          ...w,
          avatarChar: (w.title || '作').slice(0, 1)
        })),
        loading: false
      });
    } catch (error) {
      this.setData({ loading: false });
    }
  },

  // 选学员：记下标与完整对象，提交时要取 id/classId/authorized
  selectStudent(e) {
    const index = Number(e.detail.value);
    this.setData({
      studentIndex: index,
      selectedStudent: this.data.students[index] || null
    });
  },

  // 选软笔/硬笔（HARD/SOFT）
  selectType(e) {
    this.setData({ workType: e.currentTarget.dataset.type });
  },

  // 通用输入：按 data-field 直接写 data（标题/正文/点评）
  onInput(e) {
    this.setData({ [e.currentTarget.dataset.field]: e.detail.value });
  },

  // 勾选「已取得家长展示授权」，提交前必须为 true
  toggleAuthorized() {
    this.setData({ authorized: !this.data.authorized });
  },

  // 选图后立即上传换正式地址：imagePath 用于本地预览，imageUrl 才是提交给后端的值；失败清空重选
  chooseImage() {
    wx.chooseMedia({
      count: 1,
      mediaType: ['image'],
      sizeType: ['compressed'],
      sourceType: ['album', 'camera'],
      success: async res => {
        const file = res.tempFiles && res.tempFiles[0];
        if (!file) return;
        this.setData({ imagePath: file.tempFilePath, uploading: true });
        try {
          // 上传成功才保存后端返回的 url；uploading 期间禁止提交
          const data = await api.upload(file.tempFilePath, 'work');
          this.setData({ imageUrl: data.url, uploading: false });
        } catch (error) {
          this.setData({ uploading: false, imagePath: '', imageUrl: '' });
        }
      }
    });
  },

  // 预览已选图片（本地临时路径）
  previewImage() {
    if (this.data.imagePath) wx.previewImage({ current: this.data.imagePath, urls: [this.data.imagePath] });
  },

  // 提交前校验顺序：学员 → 家长授权 → 标题 → 图片已上传 → 展示授权勾选
  validate() {
    const student = this.data.selectedStudent;
    if (!student) return '请选择学员';
    if (!student.authorized) return '该学员尚无有效家长授权，请先在后台登记';
    if (!this.data.title.trim()) return '请输入作品标题';
    if (!this.data.imageUrl) return '请先上传作品图片';
    if (!this.data.authorized) return '请确认已取得家长作品展示授权';
    return '';
  },

  // 提交作品：uploading/submitting 中禁止重复提交；成功后清空表单并刷新我的作品
  async submit() {
    if (this.data.submitting || this.data.uploading) return;
    const error = this.validate();
    if (error) {
      wx.showToast({ title: error, icon: 'none' });
      return;
    }
    this.setData({ submitting: true });
    const student = this.data.selectedStudent;
    try {
      await api.post('/mini/teacher/works', {
        studentId: student.id,
        classId: student.classId,
        title: this.data.title.trim(),
        workType: this.data.workType,
        contentChar: this.data.contentChar.trim(),
        imageUrl: this.data.imageUrl,
        comment: this.data.comment.trim(),
        authorized: true
      });
      wx.showModal({
        title: '作品已提交',
        content: '作品进入教务审核，审核通过后将展示在作品墙。',
        showCancel: false,
        success: () => {
          this.setData({
            submitting: false,
            title: '',
            contentChar: '',
            comment: '',
            imagePath: '',
            imageUrl: '',
            authorized: false
          });
          this.load();
        }
      });
    } catch (error) {
      this.setData({ submitting: false });
    }
  },

  // 有上一页就返回，直接进入本页则回教师工作台
  goBack() {
    const pages = getCurrentPages();
    if (pages.length > 1) wx.navigateBack();
    else wx.reLaunch({ url: '/pages/teacher-home/teacher-home' });
  }
});
