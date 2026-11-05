const WEEK = ['', '周一', '周二', '周三', '周四', '周五', '周六', '周日'];

function pad(n) {
  return n < 10 ? '0' + n : '' + n;
}

function date(value) {
  if (!value) return '';
  return String(value).slice(0, 10);
}

function shortDate(value) {
  const text = date(value);
  return text ? text.slice(5).replace('-', '.') : '';
}

function time(value) {
  if (!value) return '';
  return String(value).slice(0, 5);
}

function range(start, end) {
  if (!start || !end) return '';
  return time(start) + '–' + time(end);
}

function money(value) {
  if (value === null || value === undefined || value === '') return '—';
  return '¥' + Number(value).toFixed(0);
}

function monthDay(value) {
  const text = date(value);
  return text ? Number(text.slice(8, 10)) : '';
}

function monthLabel(value) {
  const text = date(value);
  return text ? Number(text.slice(5, 7)) + ' 月' : '';
}

function weekday(value) {
  if (!value) return '';
  const d = new Date(String(value).slice(0, 10) + 'T00:00:00');
  return WEEK[d.getDay() === 0 ? 7 : d.getDay()];
}

function weekdayCn(number) {
  return WEEK[number] || '';
}

function dateTime(value) {
  if (!value) return '';
  return String(value).slice(0, 16).replace('T', ' ');
}

function today() {
  const d = new Date();
  return d.getFullYear() + '-' + pad(d.getMonth() + 1) + '-' + pad(d.getDate());
}

function monday(base) {
  const d = base ? new Date(base + 'T00:00:00') : new Date();
  const day = d.getDay() === 0 ? 7 : d.getDay();
  d.setDate(d.getDate() - day + 1);
  return d.getFullYear() + '-' + pad(d.getMonth() + 1) + '-' + pad(d.getDate());
}

function addDays(base, days) {
  const d = new Date(base + 'T00:00:00');
  d.setDate(d.getDate() + days);
  return d.getFullYear() + '-' + pad(d.getMonth() + 1) + '-' + pad(d.getDate());
}

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
