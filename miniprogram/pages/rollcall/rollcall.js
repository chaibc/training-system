const api = require('../../utils/request');
const auth = require('../../utils/auth');
const view = require('../../utils/view');

Page({
  data: {
    loading: true,
    submitting: false,
    blocked: false,
    lessonId: null,
    lesson: null,
    students: [],
    summary: { total: 0, present: 0, leave: 0, absent: 0, unmarked: 0 },
    done: false,
    result: null
  },

  onLoad(options) {
    if (!auth.requireTeacher()) {
      this.setData({ blocked: true, loading: false });
      return;
    }
    this.setData({ lessonId: options.lessonId });
    this.load();
  },

  async load() {
    try {
      const data = await api.get('/mini/teacher/lessons/' + this.data.lessonId);
      const students = (data.students || []).map(s => ({
        ...s,
        mark: s.attendanceStatus || 'PRESENT'
      }));
      this.setData({
        lesson: view.lesson(data.lesson),
        students,
        done: data.summary && data.summary.done,
        loading: false
      });
      this.recalc();
    } catch (error) {
      this.setData({ loading: false });
    }
  },

  recalc() {
    const students = this.data.students;
    const summary = {
      total: students.length,
      present: students.filter(s => s.mark === 'PRESENT').length,
      leave: students.filter(s => s.mark === 'LEAVE').length,
      absent: students.filter(s => s.mark === 'ABSENT').length
    };
    summary.unmarked = summary.total - summary.present - summary.leave - summary.absent;
    this.setData({ summary });
  },

  setMark(e) {
    const index = Number(e.currentTarget.dataset.index);
    const mark = e.currentTarget.dataset.mark;
    this.setData({ ['students[' + index + '].mark']: mark }, () => this.recalc());
  },

  async submit() {
    if (this.data.submitting || !this.data.students.length) return;
    this.setData({ submitting: true });
    try {
      const items = this.data.students.map(s => ({
        studentId: s.studentId,
        status: s.mark,
        note: s.mark === 'PRESENT' ? '出勤' : s.mark === 'LEAVE' ? '请假' : '缺勤'
      }));
      const result = await api.post('/mini/teacher/lessons/' + this.data.lessonId + '/rollcall', { items });
      this.setData({ done: true, result, submitting: false });
      wx.showToast({ title: '点名完成', icon: 'success' });
    } catch (error) {
      this.setData({ submitting: false });
    }
  },

  goBack() {
    const pages = getCurrentPages();
    if (pages.length > 1) wx.navigateBack();
    else wx.reLaunch({ url: '/pages/teacher-home/teacher-home' });
  },

  goWorkbench() {
    wx.reLaunch({ url: '/pages/teacher-home/teacher-home' });
  }
});
