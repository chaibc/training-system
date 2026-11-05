/**
 * 教师点名页：为某课次逐人标记出勤/请假/缺勤，汇总后一次性提交。
 * 接口：GET /api/mini/teacher/lessons/{lessonId}；POST /api/mini/teacher/lessons/{lessonId}/rollcall
 * 参数：lessonId（必填）
 * 需登录：是，且必须是教师（TEACHER）
 */

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

  // 教师守卫；未通过置 blocked，不再请求课次
  onLoad(options) {
    if (!auth.requireTeacher()) {
      this.setData({ blocked: true, loading: false });
      return;
    }
    this.setData({ lessonId: options.lessonId });
    this.load();
  },

  // 拉课次与学员名单；已有出勤状态作为默认标记，summary.done 表示该课次已完成点名
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

  // 按当前标记重算汇总；unmarked = 总数 - 三种已标记，保证分组数字自洽
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

  // 单条改标记：用带下标的 setData 路径只更新该行，再重算汇总
  setMark(e) {
    const index = Number(e.currentTarget.dataset.index);
    const mark = e.currentTarget.dataset.mark;
    this.setData({ ['students[' + index + '].mark']: mark }, () => this.recalc());
  },

  // 提交点名：空名单不提交；note 是给后端记录的中文说明
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

  // 有上一页就返回，直接进入本页（分享/扫码）则回工作台
  goBack() {
    const pages = getCurrentPages();
    if (pages.length > 1) wx.navigateBack();
    else wx.reLaunch({ url: '/pages/teacher-home/teacher-home' });
  },

  // 点名完成后回教师工作台
  goWorkbench() {
    wx.reLaunch({ url: '/pages/teacher-home/teacher-home' });
  }
});
