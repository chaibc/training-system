/**
 * 家长课表页：按周展示学员课表，支持切周/选日期、切换学员、进课次详情与请假。
 * 接口：GET /api/mini/parent/children；GET /api/mini/parent/schedule?studentId=&week=
 * 参数：无（学员来自全局当前学员）
 * 需登录：是，且必须是家长（PARENT）
 */

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

  // 家长守卫；通过后先取学员列表，再按本周加载课表
  onLoad() {
    if (!auth.requireParent()) {
      this.setData({ blocked: true, loading: false });
      return;
    }
    this.loadChildren();
  },

  // 请假/切学员后返回时静默刷新；首次进入由 loadChildren 负责加载，避免重复请求
  onShow() {
    if (this.data.childId && !this.data.loading) this.loadSchedule(false);
  },

  // 下拉刷新当前周课表（不显示整页 loading）
  onPullDownRefresh() {
    this.loadSchedule(false).finally(() => wx.stopPullDownRefresh());
  },

  // 取学员列表定位当前学员；初始化本周周一（weekStart）与选中日期（今天）
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

  // 拉指定周课表；showLoading=false 用于 onShow/下拉刷新，保留旧内容避免闪骨架
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
        // 仅「已排课 + 未请假 + 开课时间未到」的课次允许发起请假
        canLeave: l.status === 'SCHEDULED' && !l.leaveStatus && this.future(l)
      }));
      // 按日期统计当天课次数，用于周条上的数量角标
      const counts = {};
      lessons.forEach(l => { counts[l.lessonDate] = (counts[l.lessonDate] || 0) + 1; });
      const weekDates = fmt.weekDates(data.weekStart || this.data.weekStart).map(d => ({
        ...d,
        count: counts[d.date] || 0
      }));
      let selectedDate = this.data.selectedDate;
      // 选中日期不在本周时（切周/跨周刷新）回落到周一
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

  // 判断课次是否还没开始；拼 T + 时分秒避免不同基础库对日期字符串解析不一致
  future(lesson) {
    if (!lesson.lessonDate || !lesson.startTime) return false;
    return new Date(lesson.lessonDate + 'T' + String(lesson.startTime).slice(0, 8)) > new Date();
  },

  // 切换学员：写全局+缓存，重置到本周与今天后重新加载
  selectChild(e) {
    const id = Number(e.currentTarget.dataset.id);
    if (id === this.data.childId) return;
    const child = this.data.children.find(c => c.id === id);
    getApp().setChild(id);
    this.setData({ childId: id, child: view.child(child), weekStart: fmt.monday(), selectedDate: fmt.today() }, () => this.loadSchedule());
  },

  // 选日期：只切换展示的当天课次，不请求接口
  selectDay(e) {
    const date = e.currentTarget.dataset.date;
    const dayLessons = this.data.lessons.filter(l => l.lessonDate === date);
    this.setData({ selectedDate: date, dayLessons });
  },

  // 切周：data-delta 为 ±1，换算成 ±7 天重新拉取，并把选中日期落到新周一
  changeWeek(e) {
    const delta = Number(e.currentTarget.dataset.delta) * 7;
    const weekStart = fmt.addDays(this.data.weekStart, delta);
    this.setData({ weekStart, selectedDate: weekStart }, () => this.loadSchedule());
  },

  // 进课次详情（带 lessonId 与当前 studentId）
  goLesson(e) {
    wx.navigateTo({
      url: '/pages/lesson-detail/lesson-detail?lessonId=' + e.currentTarget.dataset.id + '&studentId=' + this.data.childId
    });
  },

  // 直接从课表发起请假（带课次与学员）
  goLeave(e) {
    wx.navigateTo({
      url: '/pages/leave/leave?lessonId=' + e.currentTarget.dataset.id + '&studentId=' + this.data.childId
    });
  },

  // 查看该学员课时账户
  goHours() {
    wx.navigateTo({ url: '/pages/hours/hours?studentId=' + this.data.childId });
  },

  // 去账号页处理绑定
  goAccount() {
    wx.navigateTo({ url: '/pages/account/account' });
  }
});
