/**
 * 试听预约页：填写学员/家长信息、选择课程与试听时段，提交试听申请。
 * 接口：GET /api/public/meta（试听时段 trialSlots）、GET /api/public/courses；POST /api/public/trial
 * 参数：courseId（可选，预选课程）
 * 需登录：否（公开表单，不校验登录）
 */

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

  // 记录 URL 预选课程；并行拉元数据与课程后再定位下拉下标
  onLoad(options) {
    this.presetCourseId = options.courseId ? Number(options.courseId) : null;
    this.load();
  },

  // meta.trialSlots 为可预约时段；课程列表用于课程下拉，预选课程不在列表时下标保持 -1
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

  // 通用输入：按 data-field 写入 form 对应字段
  onInput(e) {
    const field = e.currentTarget.dataset.field;
    this.setData({ ['form.' + field]: e.detail.value });
  },

  // 选课程：记下标，提交时再取 id（未选为 -1）
  onCourseChange(e) {
    this.setData({ courseIndex: Number(e.detail.value) });
  },

  // 选试听时段：记下标，提交时再取 id
  onSlotChange(e) {
    this.setData({ slotIndex: Number(e.detail.value) });
  },

  // 勾选/取消隐私说明同意，提交前校验
  toggleAgree() {
    this.setData({ agreed: !this.data.agreed });
  },

  // 表单校验：姓名必填、家长手机号 1 开头 11 位、必须同意隐私说明；返回错误文案，空串表示通过
  validate() {
    const form = this.data.form;
    if (!form.studentName.trim()) return '请输入学员姓名';
    if (!/^1\d{10}$/.test(form.phone.trim())) return '请输入正确的家长手机号';
    if (!this.data.agreed) return '请先阅读并同意隐私说明';
    return '';
  },

  // 提交试听：先校验；课程/时段未选传 null（允许只留联系方式）；结果写 Storage 后 redirect
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

  // 联系机构：电话来自首页缓存，缺失时只提示
  call() {
    const phone = getApp().globalData.institutionPhone;
    if (phone) wx.makePhoneCall({ phoneNumber: phone.replace(/[^\d]/g, '') });
    else wx.showToast({ title: '暂未维护联系电话', icon: 'none' });
  }
});
