const fmt = require('./format');
const req = require('./request');

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

function child(c) {
  if (!c) return c;
  return {
    ...c,
    hoursText: Number(c.remainingHours || 0).toFixed(0),
    classText: c.className || '未分班',
    relationText: c.relationText || '家长'
  };
}

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

function leave(l) {
  if (!l) return l;
  return {
    ...l,
    timeText: fmt.dateTime(l.submittedAt),
    statusClass: l.status === 'APPROVED' ? 'green' : l.status === 'REJECTED' ? 'red' : l.status === 'PENDING' ? 'gold' : 'line',
    lessonText: l.lesson ? [l.lesson.className, l.lesson.lessonDate, fmt.range(l.lesson.startTime, l.lesson.endTime)].filter(Boolean).join(' · ') : ''
  };
}

function binding(b) {
  if (!b) return b;
  return {
    ...b,
    avatarChar: (b.studentName || '学').slice(0, 1),
    statusClass: b.status === 'ACTIVE' ? 'green' : b.status === 'PENDING' || b.status === 'UNBIND_PENDING' ? 'gold' : 'line',
    canUnbind: b.status === 'ACTIVE'
  };
}

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
