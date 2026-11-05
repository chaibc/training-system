const api = require('../../utils/request');
const auth = require('../../utils/auth');
const view = require('../../utils/view');
const fmt = require('../../utils/format');

Page({
  data: {
    loading: true,
    blocked: false,
    hasChildren: true,
    children: [],
    childId: null,
    child: null,
    hours: {},
    lessons: [],
    weekStart: '',
    weekEnd: '',
    weekDates: [],
    selectedDate: '',
    dayLessons: [],
    weekLabel: ''
  },

  onLoad() {
    if (!auth.requireParent()) {
      this.setData({ blocked: true, loading: false });
      return;
    }
    this.loadChildren();
  },

  onShow() {
    if (this.data.childId && !this.data.loading) this.loadSchedule(false);
  },

  onPullDownRefresh() {
    this.loadSchedule(false).finally(() => wx.stopPullDownRefresh());
  },

  async loadChildren() {
    try {
      const list = (await api.get('/mini/parent/children')) || [];
      if (!list.length) {
        this.setData({ loading: false, hasChildren: false });
        return;
      }
      const child = list.find(c => c.id === getApp().getChild()) || list[0];
      getApp().setChild(child.id);
      this.setData({
        children: list.map(view.child),
        childId: child.id,
        child: view.child(child),
        weekStart: fmt.monday(),
        selectedDate: fmt.today()
      });
      this.loadSchedule();
    } catch (error) {
      this.setData({ loading: false });
    }
  },

  async loadSchedule(showLoading) {
    if (!this.data.childId) return;
    if (showLoading !== false) this.setData({ loading: true });
    try {
      const data = await api.get('/mini/parent/schedule', {
        studentId: this.data.childId,
        week: this.data.weekStart || fmt.monday()
      });
      const lessons = (data.lessons || []).map(view.lesson).map(l => ({
        ...l,
        canLeave: l.status === 'SCHEDULED' && !l.leaveStatus && this.future(l)
      }));
      const counts = {};
      lessons.forEach(l => { counts[l.lessonDate] = (counts[l.lessonDate] || 0) + 1; });
      const weekDates = fmt.weekDates(data.weekStart || this.data.weekStart).map(d => ({
        ...d,
        count: counts[d.date] || 0
      }));
      let selectedDate = this.data.selectedDate;
      if (selectedDate < data.weekStart || selectedDate > data.weekEnd) selectedDate = data.weekStart;
      const dayLessons = lessons.filter(l => l.lessonDate === selectedDate);
      this.setData({
        child: view.child(data.child),
        hours: data.hours || {},
        lessons,
        weekStart: data.weekStart,
        weekEnd: data.weekEnd,
        weekDates,
        selectedDate,
        dayLessons,
        weekLabel: fmt.shortDate(data.weekStart) + ' — ' + fmt.shortDate(data.weekEnd),
        loading: false
      });
    } catch (error) {
      this.setData({ loading: false });
    }
  },

  future(lesson) {
    if (!lesson.lessonDate || !lesson.startTime) return false;
    return new Date(lesson.lessonDate + 'T' + String(lesson.startTime).slice(0, 8)) > new Date();
  },

  selectChild(e) {
    const id = Number(e.currentTarget.dataset.id);
    if (id === this.data.childId) return;
    const child = this.data.children.find(c => c.id === id);
    getApp().setChild(id);
    this.setData({ childId: id, child: view.child(child), weekStart: fmt.monday(), selectedDate: fmt.today() }, () => this.loadSchedule());
  },

  selectDay(e) {
    const date = e.currentTarget.dataset.date;
    const dayLessons = this.data.lessons.filter(l => l.lessonDate === date);
    this.setData({ selectedDate: date, dayLessons });
  },

  changeWeek(e) {
    const delta = Number(e.currentTarget.dataset.delta) * 7;
    const weekStart = fmt.addDays(this.data.weekStart, delta);
    this.setData({ weekStart, selectedDate: weekStart }, () => this.loadSchedule());
  },

  goLesson(e) {
    wx.navigateTo({
      url: '/pages/lesson-detail/lesson-detail?lessonId=' + e.currentTarget.dataset.id + '&studentId=' + this.data.childId
    });
  },

  goLeave(e) {
    wx.navigateTo({
      url: '/pages/leave/leave?lessonId=' + e.currentTarget.dataset.id + '&studentId=' + this.data.childId
    });
  },

  goHours() {
    wx.navigateTo({ url: '/pages/hours/hours?studentId=' + this.data.childId });
  },

  goAccount() {
    wx.navigateTo({ url: '/pages/account/account' });
  }
});
