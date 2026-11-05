/**
 * 日期/金额格式化工具：纯函数，无副作用，供页面与 view.js 复用。
 * 接口：无
 * 需登录：否
 */

const WEEK = ['', '周一', '周二', '周三', '周四', '周五', '周六', '周日'];

// 两位补零，供 today/monday/addDays 拼 yyyy-MM-dd
function pad(n) {
  return n < 10 ? '0' + n : '' + n;
}

// 取日期部分 yyyy-MM-dd：直接截前 10 位，兼容带 T 的 ISO 与「yyyy-MM-dd HH:mm:ss」
function date(value) {
  if (!value) return '';
  return String(value).slice(0, 10);
}

// 月日简写 03.08，用于卡片日期角标
function shortDate(value) {
  const text = date(value);
  return text ? text.slice(5).replace('-', '.') : '';
}

// 取 HH:mm：兼容「09:00」与「09:00:00」两种后端时间格式
function time(value) {
  if (!value) return '';
  return String(value).slice(0, 5);
}

// 时间区间文案，如 09:00–10:30（中间用 en dash）
function range(start, end) {
  if (!start || !end) return '';
  return time(start) + '–' + time(end);
}

// 金额文案：空值显示「—」，否则取整到元并加 ¥（课程价格没有小数）
function money(value) {
  if (value === null || value === undefined || value === '') return '—';
  return '¥' + Number(value).toFixed(0);
}

// 只取「日」，用于出勤日历格子
function monthDay(value) {
  const text = date(value);
  return text ? Number(text.slice(8, 10)) : '';
}

// 月份标签，如 3 月
function monthLabel(value) {
  const text = date(value);
  return text ? Number(text.slice(5, 7)) + ' 月' : '';
}

// 由日期算星期文案；补 T00:00:00 是为了绕开 iOS 对 yyyy-MM-dd 的解析兼容问题
function weekday(value) {
  if (!value) return '';
  const d = new Date(String(value).slice(0, 10) + 'T00:00:00');
  return WEEK[d.getDay() === 0 ? 7 : d.getDay()];
}

// 由 1-7 的数字直接取星期文案，用于班级排课的 weekday 字段
function weekdayCn(number) {
  return WEEK[number] || '';
}

// yyyy-MM-dd HH:mm，把 ISO 的 T 换成空格
function dateTime(value) {
  if (!value) return '';
  return String(value).slice(0, 16).replace('T', ' ');
}

// 今天日期 yyyy-MM-dd（按设备本地时区）
function today() {
  const d = new Date();
  return d.getFullYear() + '-' + pad(d.getMonth() + 1) + '-' + pad(d.getDate());
}

// 取所在周的周一：getDay() 周日为 0，需按 7 处理，否则会算到下一周
function monday(base) {
  const d = base ? new Date(base + 'T00:00:00') : new Date();
  const day = d.getDay() === 0 ? 7 : d.getDay();
  d.setDate(d.getDate() - day + 1);
  return d.getFullYear() + '-' + pad(d.getMonth() + 1) + '-' + pad(d.getDate());
}

// 日期加减：借 Date 处理跨月/跨年，结果仍是 yyyy-MM-dd
function addDays(base, days) {
  const d = new Date(base + 'T00:00:00');
  d.setDate(d.getDate() + days);
  return d.getFullYear() + '-' + pad(d.getMonth() + 1) + '-' + pad(d.getDate());
}

// 生成一周 7 天的展示数据：日期、几号、周几、是否今天，供课表周条渲染
function weekDates(mondayText) {
  return Array.from({ length: 7 }).map((_, i) => {
    const value = addDays(mondayText, i);
    return {
      date: value,
      day: Number(value.slice(8, 10)),
      week: WEEK[i + 1],
      today: value === today()
    };
  });
}

module.exports = {
  date,
  shortDate,
  time,
  range,
  money,
  monthDay,
  monthLabel,
  weekday,
  weekdayCn,
  dateTime,
  today,
  monday,
  addDays,
  weekDates,
  pad
};
