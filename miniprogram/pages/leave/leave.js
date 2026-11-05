const api = require('../../utils/request');
const auth = require('../../utils/auth');
const view = require('../../utils/view');
const fmt = require('../../utils/format');

Page({
  data: {
    loading: true,
    submitting: false,
    blocked: false,
    children: [],
    childId: null,
    child: null,
    lessons: [],
    lessonLabels: [],
    lessonIndex: -1,
    selectedLesson: null,
    reasons: [],
    reasonId: null,
    reasonText: '',
    cutoffHours: 4,
    ruleText: '',
    deductHint: ''
  },

  onLoad(options) {
    if (!auth.requireParent()) {
      this.setData({ blocked: true, loading: false });
      return;
    }
    this.presetLessonId = options.lessonId ? Number(options.lessonId) : null;
    this.presetStudentId = options.studentId ? Number(options.studentId) : null;
    this.loadChildren();
  },

  async loadChildren() {
    try {
      const list = (await api.get('/mini/parent/children')) || [];
      if (!list.length) {
        this.setData({ loading: false });
        return;
      }
      const child = list.find(c => c.id === (this.presetStudentId || getApp().getChild())) || list[0];
      getApp().setChild(child.id);
      this.setData({ children: list.map(view.child), childId: child.id });
      this.loadOptions();
    } catch (error) {
      this.setData({ loading: false });
    }
  },

  async loadOptions() {
    if (!this.data.childId) return;
    this.setData({ loading: true });
    try {
      const data = await api.get('/mini/parent/leave-options', { studentId: this.data.childId });
      const lessons = (data.lessons || []).map(view.lesson);
      const lessonLabels = lessons.map(l => l.className + ' · ' + l.dateText + ' ' + l.timeText);
      let lessonIndex = -1;
      if (this.presetLessonId) {
        lessonIndex = lessons.findIndex(l => l.id === this.presetLessonId);
      }
      const selectedLesson = lessonIndex >= 0 ? lessons[lessonIndex] : null;
      this.setData({
        child: view.child(data.child),
        lessons,
        lessonLabels,
        lessonIndex,
        selectedLesson,
        reasons: data.reasons || [],
        cutoffHours: data.cutoffHours || 4,
        ruleText: data.ruleText || '',
        deductHint: selectedLesson ? this.calcHint(selectedLesson, data.cutoffHours || 4) : '',
        loading: false
      });
    } catch (error) {
      this.setData({ loading: false });
    }
  },

  calcHint(lesson, cutoff) {
    if (!lesson) return '';
    const start = new Date(lesson.lessonDate + 'T' + String(lesson.startTime).slice(0, 8));
    const hours = (start - new Date()) / 3600000;
    return hours >= cutoff
      ? '距开课超过 ' + cutoff + ' 小时，审批通过后不扣课时'
      : '距开课不足 ' + cutoff + ' 小时，审批通过后按消课处理';
  },

  selectChild(e) {
    const id = Number(e.currentTarget.dataset.id);
    if (id === this.data.childId) return;
    getApp().setChild(id);
    this.presetLessonId = null;
    this.setData({ childId: id, lessonIndex: -1, selectedLesson: null, reasonId: null, deductHint: '' }, () => this.loadOptions());
  },

  selectLesson(e) {
    const index = Number(e.detail.value);
    const lesson = this.data.lessons[index];
    this.setData({
      lessonIndex: index,
      selectedLesson: lesson,
      deductHint: this.calcHint(lesson, this.data.cutoffHours)
    });
  },

  selectReason(e) {
    const id = Number(e.currentTarget.dataset.id);
    this.setData({ reasonId: id });
  },

  onReasonText(e) {
    this.setData({ reasonText: e.detail.value });
  },

  async submit() {
    if (this.data.submitting) return;
    if (!this.data.childId) return wx.showToast({ title: '请选择学员', icon: 'none' });
    if (!this.data.selectedLesson) return wx.showToast({ title: '请选择请假课次', icon: 'none' });
    if (!this.data.reasonId) return wx.showToast({ title: '请选择请假原因', icon: 'none' });
    this.setData({ submitting: true });
    try {
      const result = await api.post('/mini/parent/leaves', {
        studentId: this.data.childId,
        lessonId: this.data.selectedLesson.id,
        reasonId: this.data.reasonId,
        reasonText: this.data.reasonText.trim()
      });
      wx.setStorageSync('hanmo_leave_result', {
        ...result,
        childName: this.data.child ? this.data.child.name : ''
      });
      wx.redirectTo({ url: '/pages/leave-done/leave-done' });
    } catch (error) {
      // request 已提示
    } finally {
      this.setData({ submitting: false });
    }
  },

  goAccount() {
    wx.navigateTo({ url: '/pages/account/account' });
  }
});
