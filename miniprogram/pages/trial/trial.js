const api = require('../../utils/request');

Page({
  data: {
    loading: true,
    submitting: false,
    courses: [],
    courseNames: [],
    slots: [],
    slotNames: [],
    courseIndex: -1,
    slotIndex: -1,
    form: {
      studentName: '',
      age: '',
      parentName: '',
      phone: '',
      note: ''
    },
    agreed: false
  },

  onLoad(options) {
    this.presetCourseId = options.courseId ? Number(options.courseId) : null;
    this.load();
  },

  async load() {
    try {
      const [meta, courses] = await Promise.all([
        api.get('/public/meta'),
        api.get('/public/courses')
      ]);
      const courseNames = (courses || []).map(c => c.name);
      const slots = meta.trialSlots || [];
      let courseIndex = -1;
      if (this.presetCourseId) {
        courseIndex = (courses || []).findIndex(c => c.id === this.presetCourseId);
      }
      this.setData({
        courses: courses || [],
        courseNames,
        slots,
        slotNames: slots.map(s => s.name),
        courseIndex,
        loading: false
      });
    } catch (error) {
      this.setData({ loading: false });
    }
  },

  onInput(e) {
    const field = e.currentTarget.dataset.field;
    this.setData({ ['form.' + field]: e.detail.value });
  },

  onCourseChange(e) {
    this.setData({ courseIndex: Number(e.detail.value) });
  },

  onSlotChange(e) {
    this.setData({ slotIndex: Number(e.detail.value) });
  },

  toggleAgree() {
    this.setData({ agreed: !this.data.agreed });
  },

  validate() {
    const form = this.data.form;
    if (!form.studentName.trim()) return '请输入学员姓名';
    if (!/^1\d{10}$/.test(form.phone.trim())) return '请输入正确的家长手机号';
    if (!this.data.agreed) return '请先阅读并同意隐私说明';
    return '';
  },

  async submit() {
    if (this.data.submitting) return;
    const error = this.validate();
    if (error) {
      wx.showToast({ title: error, icon: 'none' });
      return;
    }
    this.setData({ submitting: true });
    const form = this.data.form;
    const course = this.data.courses[this.data.courseIndex];
    const slot = this.data.slots[this.data.slotIndex];
    try {
      const result = await api.post('/public/trial', {
        studentName: form.studentName.trim(),
        age: form.age.trim(),
        parentName: form.parentName.trim(),
        phone: form.phone.trim(),
        courseId: course ? course.id : null,
        slotId: slot ? slot.id : null,
        note: form.note.trim()
      });
      wx.setStorageSync('hanmo_trial_result', result);
      wx.redirectTo({ url: '/pages/trial-done/trial-done' });
    } catch (error) {
      // 错误提示已由 request 统一处理
    } finally {
      this.setData({ submitting: false });
    }
  },

  call() {
    const phone = getApp().globalData.institutionPhone;
    if (phone) wx.makePhoneCall({ phoneNumber: phone.replace(/[^\d]/g, '') });
    else wx.showToast({ title: '暂未维护联系电话', icon: 'none' });
  }
});
