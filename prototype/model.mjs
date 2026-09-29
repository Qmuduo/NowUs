export const PEOPLE = {
  me: { name: '小满', short: '满', city: '北京', zone: 'Asia/Shanghai', role: '我' },
  partner: { name: '阿远', short: '远', city: '纽约', zone: 'America/New_York', role: '你' }
};

export const SCENARIOS = {
  normal: { label: '日常的一天', now: '2026-09-29T12:00:00Z', description: '我的夜晚，你的早晨。' },
  midnight: { label: '日期不同', now: '2026-09-29T16:30:00Z', description: '我这里是明天，你那里还是今天。' },
  none: { label: '没有共同空闲', now: '2026-09-29T12:00:00Z', description: '今天错开了，也可以留一句关心。' },
  busy: { label: '临时忙碌', now: '2026-09-29T12:00:00Z', description: '今天的节奏，有一点变化。' }
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

export function routineAt(person, instant, scenario = 'normal', overrides = null) {
  const active = activeOverride(person, instant, overrides ?? scenarioOverride(scenario));
  if (active) return { label: active.label || (active.kind === 'free' ? '现在愿意联系' : '暂时不方便'), icon: active.kind === 'free' ? 'heart' : 'book', kind: active.kind === 'free' ? 'rest' : 'busy', source: '主动设置', end: active.end };
  const minutes = parts(instant, PEOPLE[person].zone).minutes;
  const entry = routines[person].find(([start, end]) => minutes >= start && minutes < end);
  return entry ? { label: entry[2], icon: entry[3], kind: entry[4], source: '按通常作息' } : { label: '尚未设置', icon: 'clock', kind: 'unknown', source: '未知' };
}

export function canContact(person, instant, scenario = 'normal', overrides = null) {
  const active = activeOverride(person, instant, overrides ?? scenarioOverride(scenario));
  if (active) return active.kind === 'free';
  const minute = parts(instant, PEOPLE[person].zone).minutes;
  const ranges = scenario === 'none' && person === 'partner' ? [[780, 840]] : contacts[person];
  return ranges.some(([start, end]) => minute >= start && minute < end);
}

export function commonWindows(bounds, scenario = 'normal', overrides = null) {
  const windows = [];
  let start = null;
  for (let time = bounds.start; time < bounds.end; time += 60000) {
    const shared = canContact('me', time, scenario, overrides) && canContact('partner', time, scenario, overrides);
    if (shared && start === null) start = time;
    if (!shared && start !== null) { windows.push({ start, end: time }); start = null; }
  }
  if (start !== null) windows.push({ start, end: bounds.end });
  return windows;
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
