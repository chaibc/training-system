/**
 * 家长报名页：选择学员、课程与班级后提交报名申请（提交后等待教务确认）。
 * 接口：GET /api/mini/parent/children；GET /api/mini/parent/enroll/options?studentId=；
 *       POST /api/mini/parent/enrollments
 * 参数：courseId（可选，从课程详情带入的预选课程）
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
    childName: '',
    courses: [],
    classes: [],
    classOptions: [],
    courseId: null,
    classId: null,
    course: null,
    selectedClass: null,
    amount: '—',
    hasChildren: true
  },

  // 家长守卫；预选课程先存实例上，等 options 接口返回后再匹配（避免用到下架课程）
  onLoad(options) {
    if (!auth.requireParent()) {
      this.setData({ blocked: true, loading: false });
      return;
    }
    this.presetCourseId = options.courseId ? Number(options.courseId) : null;
    this.loadChildren();
  },

  // 取学员列表并确定当前学员：全局优先、回落第一条并写回全局
  async loadChildren() {
    try {
      const list = await api.get('/mini/parent/children');
      const children = (list || []).map(view.child);
      if (!children.length) {
        this.setData({ children: [], hasChildren: false, loading: false });
        return;
      }
      const stored = getApp().getChild();
      const child = children.find(c => c.id === stored) || children[0];
      getApp().setChild(child.id);
      this.setData({ children, childId: child.id, childName: child.name });
      this.loadOptions();
    } catch (error) {
      this.setData({ loading: false });
    }
  },

  // 拉该学员可报课程与班级；选中课程按「当前 → 预选 → 第一门」顺序回落
  async loadOptions() {
    this.setData({ loading: true });
    try {
      const data = await api.get('/mini/parent/enroll/options', { studentId: this.data.childId });
      const courses = (data.courses || []).map(view.course);
      const classes = (data.classes || []).map(c => ({
        ...c,
        weekdayText: fmt.weekdayCn(c.weekday),
        timeText: fmt.range(c.startTime, c.endTime),
        // 剩余名额 ≤2 标为紧张，仅影响展示样式
        remainingLow: c.remaining !== null && c.remaining !== undefined && c.remaining <= 2
      }));
      let courseId = this.data.courseId;
      if (!courseId || !courses.some(c => c.id === courseId)) {
        courseId = this.presetCourseId && courses.some(c => c.id === this.presetCourseId)
          ? this.presetCourseId : (courses[0] ? courses[0].id : null);
      }
      // 班级按 courseId 过滤；班级未定价时金额回落到课程价文案
      const classOptions = classes.filter(c => c.courseId === courseId);
      const selectedClass = classOptions.find(c => c.id === this.data.classId) || null;
      const course = courses.find(c => c.id === courseId) || null;
      this.setData({
        courses,
        classes,
        classOptions,
        courseId,
        classId: selectedClass ? selectedClass.id : null,
        selectedClass,
        course,
        amount: selectedClass && selectedClass.price !== null ? String(selectedClass.price) : (course && course.priceText) || '—',
        loading: false
      });
    } catch (error) {
      this.setData({ loading: false });
    }
  },

  // 切换学员：清空课程/班级选择并重拉 options，因为可报课程随学员变化
  selectChild(e) {
    const childId = Number(e.currentTarget.dataset.id);
    if (childId === this.data.childId) return;
    const child = this.data.children.find(c => c.id === childId);
    getApp().setChild(childId);
    this.setData({ childId, childName: child ? child.name : '', courseId: null, classId: null }, () => this.loadOptions());
  },

  // 选课程：同步该课程的班级选项，清空已选班级并刷新金额
  selectCourse(e) {
    const courseId = Number(e.currentTarget.dataset.id);
    const course = this.data.courses.find(c => c.id === courseId);
    const classOptions = this.data.classes.filter(c => c.courseId === courseId);
    this.setData({
      courseId,
      course,
      classOptions,
      classId: null,
      selectedClass: null,
      amount: classOptions.length && classOptions[0].price !== null ? String(classOptions[0].price) : (course ? course.priceText : '—')
    });
  },

  // 选班级：有班级价用班级价，否则保留当前金额
  selectClass(e) {
    const classId = Number(e.currentTarget.dataset.id);
    const selectedClass = this.data.classOptions.find(c => c.id === classId);
    this.setData({
      classId,
      selectedClass,
      amount: selectedClass && selectedClass.price !== null ? String(selectedClass.price) : this.data.amount
    });
  },

  // 没有可报学员时去账号页绑定
  goAccount() {
    wx.navigateTo({ url: '/pages/account/account' });
  },

  // 查看课程详情
  goCourse(e) {
    wx.navigateTo({ url: '/pages/course-detail/course-detail?id=' + e.currentTarget.dataset.id });
  },

  // 提交报名：学员/课程必选，classId 可为空表示不指定班级；成功后结果写 Storage 再 redirect
  async submit() {
    if (this.data.submitting) return;
    if (!this.data.childId) return wx.showToast({ title: '请选择学员', icon: 'none' });
    if (!this.data.courseId) return wx.showToast({ title: '请选择课程', icon: 'none' });
    this.setData({ submitting: true });
    try {
      const result = await api.post('/mini/parent/enrollments', {
        studentId: this.data.childId,
        courseId: this.data.courseId,
        classId: this.data.classId || null
      });
      const child = this.data.children.find(c => c.id === this.data.childId);
      // redirectTo 不能带参数，结果通过 Storage 传给结果页；补上学员姓名供展示
      wx.setStorageSync('hanmo_enroll_result', {
        ...result,
        childName: child ? child.name : ''
      });
      wx.redirectTo({ url: '/pages/enroll-done/enroll-done' });
    } catch (error) {
      // request 已提示
    } finally {
      this.setData({ submitting: false });
    }
  }
});
