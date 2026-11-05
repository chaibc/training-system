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

  onLoad(options) {
    if (!auth.requireParent()) {
      this.setData({ blocked: true, loading: false });
      return;
    }
    this.presetCourseId = options.courseId ? Number(options.courseId) : null;
    this.loadChildren();
  },

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

  async loadOptions() {
    this.setData({ loading: true });
    try {
      const data = await api.get('/mini/parent/enroll/options', { studentId: this.data.childId });
      const courses = (data.courses || []).map(view.course);
      const classes = (data.classes || []).map(c => ({
        ...c,
        weekdayText: fmt.weekdayCn(c.weekday),
        timeText: fmt.range(c.startTime, c.endTime),
        remainingLow: c.remaining !== null && c.remaining !== undefined && c.remaining <= 2
      }));
      let courseId = this.data.courseId;
      if (!courseId || !courses.some(c => c.id === courseId)) {
        courseId = this.presetCourseId && courses.some(c => c.id === this.presetCourseId)
          ? this.presetCourseId : (courses[0] ? courses[0].id : null);
      }
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

  selectChild(e) {
    const childId = Number(e.currentTarget.dataset.id);
    if (childId === this.data.childId) return;
    const child = this.data.children.find(c => c.id === childId);
    getApp().setChild(childId);
    this.setData({ childId, childName: child ? child.name : '', courseId: null, classId: null }, () => this.loadOptions());
  },

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

  selectClass(e) {
    const classId = Number(e.currentTarget.dataset.id);
    const selectedClass = this.data.classOptions.find(c => c.id === classId);
    this.setData({
      classId,
      selectedClass,
      amount: selectedClass && selectedClass.price !== null ? String(selectedClass.price) : this.data.amount
    });
  },

  goAccount() {
    wx.navigateTo({ url: '/pages/account/account' });
  },

  goCourse(e) {
    wx.navigateTo({ url: '/pages/course-detail/course-detail?id=' + e.currentTarget.dataset.id });
  },

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
