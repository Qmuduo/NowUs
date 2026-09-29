export const PEOPLE = {
  me: { name: '小满', short: '满', city: '北京', zone: 'Asia/Shanghai', role: '我' },
  partner: { name: '阿远', short: '远', city: '纽约', zone: 'America/New_York', role: '你' }
};

export const SCENARIOS = {
  normal: { label: '日常的一天', now: '2026-09-29T12:00:00Z', description: '我的夜晚，你的早晨。' },
  midnight: { label: '日期不同', now: '2026-09-29T16:30:00Z', description: '我这里是明天，你那里还是今天。' },
  none: { label: '没有共同空闲', now: '2026-09-29T12:00:00Z', description: '今天错开了，也可以留一句关心。' },
  busy: { label: '临时忙碌', now: '2026-09-29T12:00:00Z', description: '今天的节奏，有一点变化。' },
  missing: { label: '对方尚未设置', now: '2026-09-29T12:00:00Z', description: '先留句话，等彼此的节奏慢慢补齐。' }
};

const routines = {
  me: [
    [0, 420, '睡觉', 'moon', 'sleep'], [420, 480, '慢慢醒来', 'sun', 'rest'],
    [480, 540, '通勤', 'train', 'busy'], [540, 720, '工作', 'case', 'busy'],
    [720, 780, '午餐', 'cup', 'rest'], [780, 1080, '工作', 'case', 'busy'],
    [1080, 1140, '晚餐', 'cup', 'rest'], [1140, 1200, '通勤', 'train', 'busy'],
    [1200, 1380, '晚间休息', 'sofa', 'rest'], [1380, 1440, '睡觉', 'moon', 'sleep']
  ],
  partner: [
    [0, 420, '睡觉', 'moon', 'sleep'], [420, 480, '早餐', 'cup', 'rest'],
    [480, 540, '通勤', 'train', 'busy'], [540, 720, '上课', 'book', 'busy'],
    [720, 780, '午间休息', 'sun', 'rest'], [780, 1020, '上课', 'book', 'busy'],
    [1020, 1080, '健身', 'activity', 'busy'], [1080, 1200, '休息', 'sofa', 'rest'],
    [1200, 1260, '晚餐', 'cup', 'rest'], [1260, 1380, '读书', 'book', 'rest'],
    [1380, 1440, '睡觉', 'moon', 'sleep']
  ]
};

const contacts = { me: [[420, 480], [1200, 1380]], partner: [[600, 630], [720, 780], [1080, 1200]] };
const formatters = new Map();

export const DEFAULT_RHYTHM = {
  weekday: {sleepStart:'23:00', sleepEnd:'07:00', activity:'work', activityStart:'09:00', activityEnd:'18:00', contactKnown:true, contactStart:'20:00', contactEnd:'23:00'},
  rest: {sleepStart:'23:00', sleepEnd:'09:00', activity:'none', activityStart:'09:00', activityEnd:'18:00', contactKnown:true, contactStart:'10:00', contactEnd:'23:00'}
};
const toMinutes = value => /^\d{2}:\d{2}$/.test(value || '') && +value.slice(0,2) < 24 && +value.slice(3) < 60 ? +value.slice(0,2) * 60 + +value.slice(3) : NaN;
const within = (minute, start, end) => start < end ? minute >= start && minute < end : minute >= start || minute < end;
export function validateRhythm(draft) {
  if (!draft || !['work','study','none'].includes(draft.activity) || typeof draft.contactKnown !== 'boolean') return ['请检查作息选项。'];
  const fields = ['sleepStart','sleepEnd', ...(draft.activity === 'none' ? [] : ['activityStart','activityEnd']), ...(draft.contactKnown ? ['contactStart','contactEnd'] : [])];
  if (fields.some(key => !Number.isFinite(toMinutes(draft[key])))) return ['请填写完整且有效的时间。'];
  const s = toMinutes(draft.sleepStart), e = toMinutes(draft.sleepEnd);
  const a = toMinutes(draft.activityStart), b = toMinutes(draft.activityEnd);
  const c = toMinutes(draft.contactStart), d = toMinutes(draft.contactEnd);
  const errors = [];
  if (s === e) errors.push('入睡与起床时间不能相同。');
  if (draft.activity !== 'none' && b <= a) errors.push('上班／上课结束时间应晚于开始时间，本版支持同日安排。');
  if (draft.contactKnown && c === d) errors.push('联系开始与结束时间不能相同。');
  let activityConflict = false, contactConflict = false;
  for (let minute = 0; minute < 1440; minute++) {
    if (!within(minute,s,e)) continue;
    if (draft.activity !== 'none' && within(minute,a,b)) activityConflict = true;
    if (draft.contactKnown && within(minute,c,d)) contactConflict = true;
  }
  if (activityConflict) errors.push('上班／上课时间与睡眠重叠，请调整。');
  if (contactConflict) errors.push('愿意联系的时间与睡眠重叠，请调整。');
  return errors;
}
export function rhythmFor(instant, rhythm) {
  const local = parts(instant, PEOPLE.me.zone);
  const weekday = new Date(Date.UTC(+local.year,+local.month-1,+local.day)).getUTCDay();
  return rhythm[weekday === 0 || weekday === 6 ? 'rest' : 'weekday'];
}

export function parts(instant, zone) {
  if (!formatters.has(zone)) formatters.set(zone, new Intl.DateTimeFormat('en-CA', {
    timeZone: zone, year: 'numeric', month: '2-digit', day: '2-digit',
    hour: '2-digit', minute: '2-digit', hourCycle: 'h23'
  }));
  const value = Object.fromEntries(formatters.get(zone).formatToParts(new Date(instant)).map(p => [p.type, p.value]));
  return { ...value, date: `${value.year}-${value.month}-${value.day}`, time: `${value.hour}:${value.minute}`, minutes: Number(value.hour) * 60 + Number(value.minute) };
}

function localMidnight(year, month, day, zone) {
  const target = Date.UTC(year, month - 1, day);
  let candidate = target;
  for (let i = 0; i < 4; i++) {
    const p = parts(candidate, zone);
    const represented = Date.UTC(+p.year, +p.month - 1, +p.day, +p.hour, +p.minute);
    candidate += target - represented;
  }
  return candidate;
}

export function dayBounds(instant, zone) {
  const p = parts(instant, zone);
  const nextDate = new Date(Date.UTC(+p.year, +p.month - 1, +p.day + 1));
  return {
    start: localMidnight(+p.year, +p.month, +p.day, zone),
    end: localMidnight(nextDate.getUTCFullYear(), nextDate.getUTCMonth() + 1, nextDate.getUTCDate(), zone)
  };
}

export function scenarioOverride(scenario) {
  if (scenario !== 'busy') return null;
  return { person: 'partner', kind: 'busy', label: '临时在图书馆', start: Date.parse(SCENARIOS.busy.now), end: Date.parse('2026-09-29T15:00:00Z') };
}

function activeOverride(person, instant, overrides) {
  return (Array.isArray(overrides) ? overrides : [overrides]).filter(Boolean).find(o => o.person === person && instant >= o.start && instant < o.end);
}

export function routineAt(person, instant, scenario = 'normal', overrides = null, rhythm = null) {
  const active = activeOverride(person, instant, overrides ?? scenarioOverride(scenario));
  if (active) return { label: active.label || (active.kind === 'free' ? '现在愿意联系' : '暂时不方便'), icon: active.kind === 'free' ? 'heart' : 'book', kind: active.kind === 'free' ? 'rest' : 'busy', source: '主动设置', end: active.end };
  const minutes = parts(instant, PEOPLE[person].zone).minutes;
  if (person === 'partner' && scenario === 'missing') return {label:'尚未设置作息',icon:'clock',kind:'unknown',source:'未知'};
  if (person === 'me' && rhythm) {
    const template = rhythmFor(instant,rhythm);
    if (within(minutes,toMinutes(template.sleepStart),toMinutes(template.sleepEnd))) return {label:'睡觉',icon:'moon',kind:'sleep',source:'按通常作息'};
    if (template.activity !== 'none' && within(minutes,toMinutes(template.activityStart),toMinutes(template.activityEnd))) return {label:template.activity === 'work' ? '工作' : '上课',icon:template.activity === 'work' ? 'case' : 'book',kind:'busy',source:'按通常作息'};
    return {label:'未细分安排',icon:'clock',kind:'unknown',source:'未知'};
  }
  const entry = routines[person].find(([start, end]) => minutes >= start && minutes < end);
  return entry ? { label: entry[2], icon: entry[3], kind: entry[4], source: '按通常作息' } : { label: '尚未设置', icon: 'clock', kind: 'unknown', source: '未知' };
}

export function canContact(person, instant, scenario = 'normal', overrides = null, rhythm = null) {
  const active = activeOverride(person, instant, overrides ?? scenarioOverride(scenario));
  if (active) return active.kind === 'free';
  const minute = parts(instant, PEOPLE[person].zone).minutes;
  if (person === 'partner' && scenario === 'missing') return null;
  if (person === 'me' && rhythm) {
    const template = rhythmFor(instant,rhythm);
    return template.contactKnown ? within(minute,toMinutes(template.contactStart),toMinutes(template.contactEnd)) : null;
  }
  const exceptionToday = scenario === 'none' && person === 'partner' && parts(instant, PEOPLE.me.zone).date === parts(Date.parse(SCENARIOS.none.now), PEOPLE.me.zone).date;
  const ranges = exceptionToday ? [[780, 840]] : contacts[person];
  return ranges.some(([start, end]) => minute >= start && minute < end);
}

export function commonWindows(bounds, scenario = 'normal', overrides = null, rhythm = null) {
  const windows = [];
  let start = null;
  for (let time = bounds.start; time < bounds.end; time += 60000) {
    const shared = canContact('me', time, scenario, overrides, rhythm) === true && canContact('partner', time, scenario, overrides, rhythm) === true;
    if (shared && start === null) start = time;
    if (!shared && start !== null) { windows.push({ start, end: time }); start = null; }
  }
  if (start !== null) windows.push({ start, end: bounds.end });
  return windows;
}

export function nextWindow(instant, scenario = 'normal', overrides = null, rhythm = null) {
  if (scenario === 'missing') return null;
  const horizon = instant + 7 * 86400000;
  let range = dayBounds(instant, PEOPLE.me.zone);
  while (range.start < horizon) {
    const window = commonWindows({...range,end:Math.min(range.end,horizon)},scenario,overrides,rhythm).find(w => w.end > instant);
    if (window) {
      while (window.end < horizon && canContact('me',window.end,scenario,overrides,rhythm) === true && canContact('partner',window.end,scenario,overrides,rhythm) === true) window.end += 60000;
      return window;
    }
    range = dayBounds(range.end + 60000, PEOPLE.me.zone);
  }
  return null;
}

export function activitySegments(range, person, scenario = 'normal', overrides = null, rhythm = null) {
  const result = [];
  let previous = '';
  for (let instant = range.start; instant < range.end; instant += 60000) {
    const activity = routineAt(person,instant,scenario,overrides,rhythm);
    const signature = `${activity.label}|${activity.kind}|${activity.source}`;
    if (signature !== previous) {
      if (result.length) result.at(-1).end = instant;
      result.push({start:instant,end:range.end,activity,person});
      previous = signature;
    }
  }
  return result;
}

export function timeline(bounds, scenario = 'normal', overrides = null) {
  const rows = [];
  let previous = '';
  for (let instant = bounds.start; instant < bounds.end; instant += 60000) {
    const me = routineAt('me', instant, scenario, overrides);
    const partner = routineAt('partner', instant, scenario, overrides);
    const shared = canContact('me', instant, scenario, overrides) && canContact('partner', instant, scenario, overrides);
    const signature = `${me.label}|${partner.label}|${shared}|${parts(instant, PEOPLE.partner.zone).date}`;
    if (signature !== previous) { rows.push({ start: instant, end: bounds.end, me, partner, shared }); if (rows.length > 1) rows.at(-2).end = instant; previous = signature; }
  }
  return rows;
}

export function offsetDifference(instant) {
  const offset = zone => {
    const p = parts(instant, zone);
    return (Date.UTC(+p.year, +p.month - 1, +p.day, +p.hour, +p.minute) - instant) / 3600000;
  };
  return offset(PEOPLE.me.zone) - offset(PEOPLE.partner.zone);
}
