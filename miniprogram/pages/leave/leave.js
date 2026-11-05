/**
 * 家长请假申请页：选择学员与待请假课次、请假原因，提交后按开课时间提示是否扣课时。
 * 接口：GET /api/mini/parent/children；GET /api/mini/parent/leave-options?studentId=；
 *       POST /api/mini/parent/leaves
 * 参数：lessonId（可选，预选课次）；studentId（可选，预选学员）
 * 需登录：是，且必须是家长（PARENT）
 */

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

  // 家长守卫；暂存 URL 带入的课次/学员，等 options 接口返回后再匹配
  onLoad(options) {
    if (!auth.requireParent()) {
      this.setData({ blocked: true, loading: false });
      return;
    }
    this.presetLessonId = options.lessonId ? Number(options.lessonId) : null;
    this.presetStudentId = options.studentId ? Number(options.studentId) : null;
    this.loadChildren();
  },

  // 取学员列表定位当前学员：URL 指定 > 全局当前 > 第一条，并写回全局
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

  // 拉该学员可请假课次与原因；有预选课次则直接定位到对应下拉项
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
        // cutoffHours 判断是否扣课时的截止小时数（后端配置，默认 4）
        deductHint: selectedLesson ? this.calcHint(selectedLesson, data.cutoffHours || 4) : '',
        loading: false
      });
    } catch (error) {
      this.setData({ loading: false });
    }
  },

  // 按「开课时间 - 现在」与截止小时数比较，提前告知审批通过后是否扣课时（最终以后端批准结果为准）
  calcHint(lesson, cutoff) {
    if (!lesson) return '';
    const start = new Date(lesson.lessonDate + 'T' + String(lesson.startTime).slice(0, 8));
    const hours = (start - new Date()) / 3600000;
    return hours >= cutoff
      ? '距开课超过 ' + cutoff + ' 小时，审批通过后不扣课时'
      : '距开课不足 ' + cutoff + ' 小时，审批通过后按消课处理';
  },

  // 换学员后清空已选课次/原因，URL 预选课次随之失效
  selectChild(e) {
    const id = Number(e.currentTarget.dataset.id);
    if (id === this.data.childId) return;
    getApp().setChild(id);
    this.presetLessonId = null;
    this.setData({ childId: id, lessonIndex: -1, selectedLesson: null, reasonId: null, deductHint: '' }, () => this.loadOptions());
  },

  // 选课次：同步选中项并重算扣课时提示
  selectLesson(e) {
    const index = Number(e.detail.value);
    const lesson = this.data.lessons[index];
    this.setData({
      lessonIndex: index,
      selectedLesson: lesson,
      deductHint: this.calcHint(lesson, this.data.cutoffHours)
    });
  },

  // 选请假原因（后端下发的固定选项）
  selectReason(e) {
    const id = Number(e.currentTarget.dataset.id);
    this.setData({ reasonId: id });
  },

  // 补充说明：非必填，提交时去首尾空格
  onReasonText(e) {
    this.setData({ reasonText: e.detail.value });
  },

  // 提交请假：学员/课次/原因三项必填；成功后结果写 Storage 再 redirect 到结果页
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

  // 没有可请假学员时去绑定学员
  goAccount() {
    wx.navigateTo({ url: '/pages/account/account' });
  }
});
