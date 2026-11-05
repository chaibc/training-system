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

  onLoad() {
    if (!auth.requireTeacher()) {
      this.setData({ blocked: true, loading: false });
      return;
    }
    this.load();
  },

  async load() {
    try {
      const [options, works] = await Promise.all([
        api.get('/mini/teacher/work-options'),
        api.get('/mini/teacher/works', {}, { silent: true }).catch(() => [])
      ]);
      const students = options.students || [];
      this.setData({
        students,
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

  selectStudent(e) {
    const index = Number(e.detail.value);
    this.setData({
      studentIndex: index,
      selectedStudent: this.data.students[index] || null
    });
  },

  selectType(e) {
    this.setData({ workType: e.currentTarget.dataset.type });
  },

  onInput(e) {
    this.setData({ [e.currentTarget.dataset.field]: e.detail.value });
  },

  toggleAuthorized() {
    this.setData({ authorized: !this.data.authorized });
  },

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
          const data = await api.upload(file.tempFilePath, 'work');
          this.setData({ imageUrl: data.url, uploading: false });
        } catch (error) {
          this.setData({ uploading: false, imagePath: '', imageUrl: '' });
        }
      }
    });
  },

  previewImage() {
    if (this.data.imagePath) wx.previewImage({ current: this.data.imagePath, urls: [this.data.imagePath] });
  },

  validate() {
    const student = this.data.selectedStudent;
    if (!student) return '请选择学员';
    if (!student.authorized) return '该学员尚无有效家长授权，请先在后台登记';
    if (!this.data.title.trim()) return '请输入作品标题';
    if (!this.data.imageUrl) return '请先上传作品图片';
    if (!this.data.authorized) return '请确认已取得家长作品展示授权';
    return '';
  },

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

  goBack() {
    const pages = getCurrentPages();
    if (pages.length > 1) wx.navigateBack();
    else wx.reLaunch({ url: '/pages/teacher-home/teacher-home' });
  }
});
