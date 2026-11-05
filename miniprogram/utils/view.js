/**
 * 视图模型适配层：把后端原始字段加工成 WXML 可直接用的展示字段（文案、样式 class、图片绝对地址）。
 * 接口：无（纯数据转换；work() 会调用 request.fullUrl 补全图片地址）
 * 需登录：否
 */

const fmt = require('./format');
const req = require('./request');

// 课程卡片：封面渐变、标签数组、价格文案（价格为 null 显示「咨询」）、年龄段/课时/阶段拼接
function course(c) {
  if (!c) return c;
  return {
    ...c,
    coverStyle: 'background:linear-gradient(' + (c.coverColor || '145deg,#75422F,#A05B40') + ');',
    tagsList: (c.tags || '').split(',').filter(Boolean),
    priceText: c.price === null || c.price === undefined ? '咨询' : fmt.money(c.price),
    priceUnit: c.priceNote || '',
    metaText: [c.ageRange, c.lessons ? c.lessons + ' 课时' : '', c.stageName].filter(Boolean).join(' · ')
  };
}

// 作品卡片：图片补全为绝对地址，日期/软硬笔文案，无正文时用「墨」占位，liked 为本地初始态
function work(w) {
  if (!w) return w;
  return {
    ...w,
    image: req.fullUrl(w.imageUrl),
    hasImage: !!w.imageUrl,
    dateText: fmt.shortDate(w.createdAt),
    typeText: w.workType === 'SOFT' ? '软笔' : '硬笔',
    displayChar: w.contentChar || '墨',
    liked: false
  };
}

// 课次：时间区间、日期/星期文案；按 status 与 attendanceStatus 映射颜色 class 供 WXSS 使用
function lesson(l) {
  if (!l) return l;
  const statusClass = l.status === 'DONE' ? 'green' : l.status === 'CANCELLED' ? 'line' : 'red';
  return {
    ...l,
    timeText: fmt.range(l.startTime, l.endTime),
    startText: fmt.time(l.startTime),
    endText: fmt.time(l.endTime),
    dateText: fmt.shortDate(l.lessonDate),
    weekdayText: fmt.weekday(l.lessonDate),
    statusClass,
    attendanceClass: l.attendanceStatus === 'PRESENT' ? 'green' : l.attendanceStatus === 'LEAVE' ? 'gold' : l.attendanceStatus === 'ABSENT' ? 'red' : 'line',
    teacherRoom: [l.teacherName, l.classroomName].filter(Boolean).join(' · ')
  };
}

// 学员：剩余课时取整为字符串、未分班兜底、与当前家长的关系文案
function child(c) {
  if (!c) return c;
  return {
    ...c,
    hoursText: Number(c.remainingHours || 0).toFixed(0),
    classText: c.className || '未分班',
    relationText: c.relationText || '家长'
  };
}

// 报名记录：金额/提交时间文案；只有待确认、待支付两种状态允许取消
function enrollment(e) {
  if (!e) return e;
  return {
    ...e,
    amountText: fmt.money(e.amount),
    timeText: fmt.dateTime(e.submittedAt),
    canCancel: e.status === 'PENDING_CONFIRM' || e.status === 'PENDING_PAYMENT',
    statusClass: e.status === 'ENROLLED' ? 'green' : e.status === 'CANCELLED' ? 'line' : e.status === 'PENDING_PAYMENT' ? 'gold' : 'red'
  };
}

// 请假记录：提交时间、状态样式；关联课次拼成「班级 · 日期 · 时间」摘要
function leave(l) {
  if (!l) return l;
  return {
    ...l,
    timeText: fmt.dateTime(l.submittedAt),
    statusClass: l.status === 'APPROVED' ? 'green' : l.status === 'REJECTED' ? 'red' : l.status === 'PENDING' ? 'gold' : 'line',
    lessonText: l.lesson ? [l.lesson.className, l.lesson.lessonDate, fmt.range(l.lesson.startTime, l.lesson.endTime)].filter(Boolean).join(' · ') : ''
  };
}

// 家长-学员绑定：头像首字、状态样式；仅 ACTIVE 状态可发起解绑
function binding(b) {
  if (!b) return b;
  return {
    ...b,
    avatarChar: (b.studentName || '学').slice(0, 1),
    statusClass: b.status === 'ACTIVE' ? 'green' : b.status === 'PENDING' || b.status === 'UNBIND_PENDING' ? 'gold' : 'line',
    canUnbind: b.status === 'ACTIVE'
  };
}

// 出勤记录：日历式「月/日」、时间区间、状态样式（请假金色、缺勤红色）
function attendance(a) {
  if (!a) return a;
  return {
    ...a,
    day: fmt.monthDay(a.lessonDate),
    month: fmt.monthLabel(a.lessonDate),
    timeText: fmt.range(a.startTime, a.endTime),
    statusClass: a.status === 'PRESENT' ? 'green' : a.status === 'LEAVE' ? 'gold' : 'red'
  };
}

module.exports = {
  course,
  work,
  lesson,
  child,
  enrollment,
  leave,
  binding,
  attendance
};
