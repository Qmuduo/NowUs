import { PEOPLE, SCENARIOS, parts, dayBounds, routineAt as modelRoutine, commonWindows as modelWindows, activitySegments, nextWindow, scenarioOverride, offsetDifference, canContact as modelContact } from './model.mjs?v=3';
import { loadRhythm, saveRhythm, mountRhythm } from './rhythm.mjs?v=3';
import { CITIES } from './pairing-model.mjs?v=3';
import { loadConnection, saveConnection, mountOnboarding } from './onboarding.mjs?v=3';

const $ = selector => document.querySelector(selector);
const REDUCED_MOTION = matchMedia('(prefers-reduced-motion: reduce)').matches;
const esc = value => String(value).replace(/[&<>"']/g, char => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[char]));
const paths = {
  heart: '<path d="M20.8 4.6a5.5 5.5 0 0 0-7.8 0L12 5.7l-1.1-1.1a5.5 5.5 0 0 0-7.8 7.8L12 21l8.8-8.6a5.5 5.5 0 0 0 0-7.8Z"/>',
  sun: '<circle cx="12" cy="12" r="4"/><path d="M12 2v2m0 16v2M2 12h2m16 0h2M5 5l1.4 1.4m11.2 11.2L19 19M5 19l1.4-1.4M17.6 6.4 19 5"/>',
  moon: '<path d="M20.8 13A9 9 0 0 1 11 3.2 9 9 0 1 0 20.8 13Z"/>',
  clock: '<circle cx="12" cy="12" r="9"/><path d="M12 7v5l3 2"/>',
  book: '<path d="M3 4.5c4-1 7-.6 9 1.4 2-2 5-2.4 9-1.4V20c-4-1-7-.6-9 1-2-1.6-5-2-9-1V4.5ZM12 6v15"/>',
  train: '<rect x="5" y="3" width="14" height="15" rx="4"/><path d="M5 10h14M8 21l2-3m6 3-2-3M9 14h.01M15 14h.01"/>',
  case: '<rect x="3" y="7" width="18" height="14" rx="3"/><path d="M8 7V5a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2M3 12c5 3 13 3 18 0M12 12v4"/>',
  cup: '<path d="M4 8h13v8a4 4 0 0 1-4 4H8a4 4 0 0 1-4-4V8Zm13 1h2a3 3 0 0 1 0 6h-2M7 3v2m4-2v2m4-2v2"/>',
  sofa: '<path d="M5 11V8a3 3 0 0 1 3-3h8a3 3 0 0 1 3 3v3M5 16h14M5 11a2 2 0 0 0-3 2v5h20v-5a2 2 0 0 0-3-2M5 18v3m14-3v3"/>',
  activity: '<path d="M3 8v8m3-11v14m12-14v14m3-11v8M6 12h12"/>',
  arrow: '<path d="m9 5 7 7-7 7"/>',
  link: '<path d="m10 13 4-4m-6 7-1 1a4 4 0 0 1-6-6l4-4a4 4 0 0 1 6 0m2 1 1-1a4 4 0 0 1 6 6l-4 4a4 4 0 0 1-6 0"/>',
  day: '<path d="M8 3v3m8-3v3M3 10h18"/><rect x="3" y="5" width="18" height="16" rx="3"/><path d="M7 14h3m4 0h3m-10 4h3"/>',
  check: '<path d="m5 12 4 4L19 6"/>',
  edit: '<path d="m16 3 5 5-12 12-6 1 1-6L16 3Zm-2 2 5 5"/>',
  message: '<path d="M21 11.5a8.4 8.4 0 0 1-9 8.5 9.5 9.5 0 0 1-4-.8L3 21l1.8-5A8.3 8.3 0 0 1 3 11.5 8.4 8.4 0 0 1 12 3a8.4 8.4 0 0 1 9 8.5Z"/>'
};
const icon = name => `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">${paths[name] || paths.clock}</svg>`;
const initialNote = { text: '我找到你说的那家唱片店了，地址发你，周末一起去？', time: Date.parse('2026-09-29T11:45:00Z') };
const samplePeople = structuredClone(PEOPLE);
const state = { scenario: 'normal', tab: 'home', dayOffset: 0, dayMode:'nearby', override: null, ownNote: null, reminder: null, rhythm:loadRhythm(), connection:loadConnection(),setupOpen:false };
try { const saved = JSON.parse(localStorage.getItem('nowus.prototype.note.v1') || 'null'); if (saved && typeof saved.text === 'string' && Number.isFinite(saved.time)) state.ownNote = saved; } catch { /* Local storage is optional for the demo. */ }
function applyConnection(record) {
  state.connection = record;
  for (const person of ['me','partner']) {
    const profile = record?.[person];
    const details = profile ? {name:profile.name,short:Array.from(profile.name)[0],city:CITIES[profile.cityId].label,zone:CITIES[profile.cityId].zone} : record && person === 'partner' ? {name:'对方',short:'?',city:'等待加入',zone:samplePeople.partner.zone} : samplePeople[person];
    Object.assign(PEOPLE[person],details);
  }
  if (record) {state.rhythm = record.rhythm; state.ownNote = record.note;}
}
if (state.connection) applyConnection(state.connection);
let lastFocus = null;
let toastTimer;
let currentRows = [];
let currentWindows = [];
const now = () => Date.parse(SCENARIOS[state.scenario].now);
const overrides = () => [state.override, state.connection && (!partnerJoined() || !state.connection.partner.ready) ? null : scenarioOverride(state.scenario)].filter(Boolean);
const partnerJoined = () => !state.connection || state.connection.status === 'paired';
const dataScenario = (scenario = state.scenario) => state.connection && (!partnerJoined() || !state.connection.partner.ready) ? 'missing' : scenario;
const routineAt = (person, instant, scenario = state.scenario, active = overrides()) => modelRoutine(person,instant,dataScenario(scenario),active,state.rhythm);
const canContact = (person, instant, scenario = state.scenario, active = overrides()) => modelContact(person,instant,dataScenario(scenario),active,state.rhythm);
const commonWindows = (range, scenario = state.scenario, active = overrides()) => modelWindows(range,dataScenario(scenario),active,state.rhythm);
const dateText = (time, zone) => { const p = parts(time, zone); return `${Number(p.month)}月${Number(p.day)}日`; };
const fullDateText = (time, zone) => `${dateText(time, zone)} ${parts(time, zone).time}`;
function relativeNoteTime(time) {
  const minutes = Math.max(0, Math.floor((now() - time) / 60000));
  if (minutes < 1) return '刚刚';
  if (minutes < 60) return `${minutes}分钟前`;
  const hours = Math.floor(minutes / 60);
  if (hours < 24) return `${hours}小时前`;
  const days = Math.floor(hours / 24);
  if (days === 1) return '昨天';
  if (days < 7) return `${days}天前`;
  return fullDateText(time, PEOPLE.me.zone);
}
const minutesText = ms => {
  const minutes = Math.round(ms/60000), hours = Math.floor(minutes/60), remainder = minutes % 60;
  return hours ? `${hours} 小时${remainder ? ` ${remainder} 分钟` : ''}` : `${minutes} 分钟`;
};
const bounds = () => {
  const base = dayBounds(now(), PEOPLE.me.zone);
  return dayBounds(base.start + state.dayOffset * 86400000 + 12 * 3600000, PEOPLE.me.zone);
};

function source(activity) { return `<span class="source ${activity.source === '主动设置' ? 'active' : ''}">${esc(activity.source)}</span>`; }
function stage(instant, person) {
  const hour = +parts(instant, PEOPLE[person].zone).hour;
  if (hour >= 5 && hour < 8) return { kind: 'dawn', label: '清晨', icon: 'sun' };
  if (hour >= 8 && hour < 17) return { kind: 'day', label: '白天', icon: 'sun' };
  if (hour >= 17 && hour < 20) return { kind: 'dusk', label: '黄昏', icon: 'sun' };
  return { kind: 'night', label: '夜晚', icon: 'moon' };
}
function scenarioButtons() {
  return Object.entries(SCENARIOS).map(([id, s]) => `<button type="button" class="scenario-button" data-scenario="${id}" aria-pressed="${state.scenario === id}"><span>${esc(s.label)}</span>${state.scenario === id ? icon('check') : icon('arrow')}</button>`).join('');
}
function timePerson(person) {
  if (person === 'partner' && !partnerJoined()) return '<div class="time-person waiting-person"><div class="person-label"><span class="avatar">?</span><span>等待对方加入</span></div><div class="local-time">—:—</div><p class="local-date">加入后显示城市、日期与时间</p></div>';
  const p = PEOPLE[person], local = parts(now(), p.zone), period = stage(now(), person);
  const sameLocalDate = person === 'me' && partnerJoined() && local.date === parts(now(), PEOPLE.partner.zone).date;
  const activity = routineAt(person, now(), state.scenario, overrides());
  const next = person === 'partner' && activity.source === '按通常作息' ? nextStage('partner') : null;
  const usualLabel = ({ '早餐': '吃早餐', '晚餐': '吃晚餐' })[activity.label] || activity.label;
  const activityText = activity.source === '按通常作息' ? (activity.label === '通勤' ? '通常在通勤路上' : `平时在${usualLabel}`) : activity.source === '未知' ? '此刻安排未分享' : `此刻 · ${activity.label}`;
  const stateLine = person === 'partner'
    ? `<div class="partner-state"><div class="partner-state-current"><strong>${esc(activityText)}</strong></div>${next ? `<div class="partner-state-next"><time>${next.time}</time><span>开始${esc(next.label)}</span></div>` : ''}</div><div class="routine-source">${activity.source === '未知' ? '<span>尚未分享此刻安排</span>' : source(activity)}${activity.end ? `<span>持续到 ${p.city} ${parts(activity.end, p.zone).time}</span>` : ''}</div>`
    : `<div class="time-weather"><strong class="self-activity">${esc(activity.source === '未知' ? period.label : activity.label)}</strong></div><div class="person-routine self-routine"><strong>${esc(activity.label)}</strong><button type="button" data-action="status">调整</button></div><div class="routine-source">${activity.source === '主动设置' ? source(activity) : ''}</div>`;
  const role = person === 'me' ? 'self' : 'partner';
  const localDate = parts(now(), p.zone);
  const weekday = new Intl.DateTimeFormat('zh-CN', { timeZone: p.zone, weekday: 'short' }).format(now());
  const compactDate = `${Number(localDate.date.slice(5, 7))}/${Number(localDate.date.slice(8, 10))}`;
  return `<div class="time-person ${period.kind} ${role}-person${sameLocalDate ? ' same-date' : ''}"><div class="person-label"><span class="person-name">${person === 'me' ? '我' : esc(p.name)}</span><span class="person-city">· ${p.city}</span></div><div class="person-date">${period.label} · ${compactDate} ${weekday}</div><div class="local-time">${local.time.replace(':', '<span class="time-colon" aria-hidden="true">:</span>')}</div>${stateLine}</div>`;
}
function nextStage(person) {
  const current = routineAt(person, now(), state.scenario, overrides());
  for (let t = now() + 60000; t <= now() + 12 * 3600000; t += 60000) {
    const later = routineAt(person, t, state.scenario, overrides());
    if (later.label !== current.label) return { label: later.label, time: parts(t, PEOPLE[person].zone).time };
  }
  return null;
}
function localDayRelation(instant, person) {
  const zone = PEOPLE[person].zone;
  const dayOffset = Math.round((dayBounds(instant, zone).start - dayBounds(now(), zone).start) / 86400000);
  if (dayOffset === 0) return '今天';
  if (dayOffset === 1) return '明天';
  if (dayOffset === -1) return '昨天';
  return dateText(instant, zone);
}
function upcomingWindow() {
  return nextWindow(now(),dataScenario(),overrides(),state.rhythm);
}
function commonCard() {
  if (!partnerJoined()) return `<div class="common-card no-common"><div class="common-label">${icon('link')}等待对方加入</div><h3 class="common-title">先过好自己的一天</h3><p>配对并填写联系偏好后，才能对照共同时间。</p><button type="button" class="text-button" data-action="start-setup">继续邀请 ${icon('arrow')}</button></div>`;
  if (dataScenario() === 'missing') return `<div class="common-card no-common"><div class="common-label">${icon('clock')}等待彼此的节奏</div><h3 class="common-title">暂时还不能推荐时间</h3><p>${esc(PEOPLE.partner.name)}尚未设置作息和联系偏好，未知时间不会算作共同空闲。</p><button type="button" class="text-button" data-action="edit-note">先留一句关心 ${icon('arrow')}</button></div>`;
  const window = upcomingWindow();
  const ownUnknown = canContact('me',now()) === null;
  if (ownUnknown) return `<div class="common-card no-common"><div class="common-label">${icon('clock')}我的联系时间还未设置</div><h3 class="common-title">先告诉我什么时候方便</h3><p>未知时间不会算作共同空闲。补充通常愿意联系的时间之后，这里才会给出下一段共同时间。</p><button type="button" class="text-button" data-action="open-rhythm">设置我的节奏 ${icon('arrow')}</button></div>`;
  if (!window) return `<div class="common-card no-common"><div class="common-label">${icon('heart')}我们的时间</div><h3 class="common-title">${ownUnknown ? '我的联系时间还未设置' : '接下来七天，暂时没有交集'}</h3><p>${ownUnknown ? '未知时间不作推荐。可以补充通常愿意联系的时间，也可以先留句话。' : '按目前的联系偏好，没有找到共同窗口。留句话，等彼此方便时再聊。'}</p><button type="button" class="text-button" data-action="${ownUnknown ? 'open-rhythm' : 'edit-note'}">${ownUnknown ? '设置我的节奏' : '留一句关心'} ${icon('arrow')}</button></div>`;
  const start = Math.max(now(), window.start);
  const laterDay = parts(start, PEOPLE.me.zone).date !== parts(now(), PEOPLE.me.zone).date;
  const whenText = window.start <= now()
    ? '现在可以联系'
    : laterDay
      ? `${localDayRelation(start, 'me')}可以联系`
      : `${minutesText(start - now()).replace(/\s+/g, '')}后开始`;
  const times = ['partner', 'me'].map(person => {
    const zone = PEOPLE[person].zone;
    const day = localDayRelation(start, person);
    const startLocal = parts(start, zone), endLocal = parts(window.end, zone);
    const nextDay = startLocal.date !== endLocal.date ? '<span class="end-next-day">次日</span>' : '';
    return `<span class="local-window ${person}"><small>${esc(PEOPLE[person].city)}${day === '今天' ? '' : ` · ${day}`}</small><strong>${startLocal.time}–${endLocal.time}${nextDay}</strong></span>`;
  }).join('');
  return `<div class="common-card"><div class="common-top"><span class="common-label">可联系的时间<span class="common-when">${esc(whenText)}</span></span><button type="button" class="common-action" data-action="window-detail">${state.reminder === window.start ? '提醒已设' : '设置提醒'} ${icon('arrow')}</button></div><div class="common-times" role="group" aria-label="双方当地的联系时段">${times}</div><p class="common-source">按双方联系偏好估计，尚未约定</p></div>`;
}
function noteMarkup(note, person) {
  if (!note) return `<p class="note-empty">留一句今天想告诉${esc(PEOPLE.partner.name)}的话。</p>`;
  return `<div class="note-head"><span class="avatar">${esc(PEOPLE[person].short)}</span><span>${person === 'me' ? '我留给你' : `${esc(PEOPLE[person].name)}留给我`}</span><time>${dateText(note.time, PEOPLE.me.zone)} ${parts(note.time, PEOPLE.me.zone).time}</time></div><p class="note-body">${esc(note.text)}</p>`;
}
function renderHome() {
  const joined = partnerJoined();
  const ownDate = parts(now(),PEOPLE.me.zone).date, otherDate = parts(now(),PEOPLE.partner.zone).date;
  const differentDate = joined && ownDate !== otherDate;
  const partnerMoment = stage(now(),'partner'), myMoment = stage(now(),'me');
  const partnerPeriod = partnerMoment.label, myPeriod = myMoment.label;
  const heading = !joined ? '先安顿好自己的一天' : differentDate ? '日期不同，彼此都在' : partnerPeriod === myPeriod ? `我们都在${partnerPeriod}` : `你的${partnerPeriod}，我的${myPeriod}`;
  const headingMarkup = joined
    ? `<h1 class="home-thesis home-thesis-pair"><span><strong>${esc(PEOPLE.partner.name)}</strong><small> · ${esc(PEOPLE.partner.city)}</small></span><span><strong>我</strong><small> · ${esc(PEOPLE.me.city)}</small></span></h1>`
    : `<h1 class="home-thesis">${heading}</h1>`;
  const partnerNote = partnerJoined() ? `<button type="button" class="note-preview" data-action="read-note"><span class="note-preview-head"><span class="note-from"><span class="note-author">${esc(PEOPLE.partner.name)}</span><time datetime="${new Date(initialNote.time).toISOString()}">${relativeNoteTime(initialNote.time)}</time></span></span><span class="note-preview-text">${esc(initialNote.text).replace('，', '，<br>')}</span><span class="note-preview-more">展开留言 ${icon('arrow')}</span></button>` : '';
  const momentSection = `<div class="home-stack"><div class="moment-group">${headingMarkup}<div class="time-pair ${partnerMoment.kind}-pair}${joined ? ' identity-heading' : ''}">${timePerson('partner')}${timePerson('me')}</div></div></div>`;
  const connectionSection = `<div class="connection-panel"><div class="home-contact">${commonCard()}</div></div>`;
  const noteSection = `<div class="note-section">${partnerNote}<button type="button" class="note-compose" data-action="edit-note"><span class="note-compose-placeholder">回${esc(PEOPLE.partner.name)}一句</span>${icon('arrow')}</button></div>`;
  $('#home-view').innerHTML = `${momentSection}${connectionSection}${noteSection}`;
}
function renderDay() {
  if (!partnerJoined()) {
    $('#day-view').innerHTML = `<h1 class="screen-title">我们的一天</h1><p class="screen-subtitle">两份节奏，需要两个人来分享。</p><div class="setup-empty"><span class="avatar">?</span><h2>对方尚未加入</h2><p>你的${PEOPLE.me.city}作息已保存。等对方选择城市并分享作息后，这里才会出现对照时间轴。</p><button type="button" class="primary-button" data-action="start-setup">继续邀请伴侣</button></div>`;
    currentRows = []; currentWindows = []; return;
  }
  const day = bounds();
  const nearby = state.dayMode === 'nearby' && state.dayOffset === 0;
  const range = nearby ? {start:Math.max(day.start,now()-30*60000),end:Math.min(day.end,now()+5.5*3600000)} : day;
  const scale = 2.8;
  const top = time => (time-range.start)/60000*scale;
  currentRows = ['me','partner'].flatMap(person => activitySegments(range,person,dataScenario(),overrides(),state.rhythm));
  currentWindows = commonWindows(range);
  const dayWindows = commonWindows(day);
  const short = currentRows.filter(row => row.end-row.start < 30*60000);
  const columns = ['me','partner'].map(person => `<div class="activity-column ${person}">${currentRows.map((row,index) => {
    if (row.person !== person) return '';
    const height = (row.end-row.start)/60000*scale;
    const name = `${PEOPLE[person].city} ${fullDateText(row.start,PEOPLE[person].zone)} ${row.activity.label}，${row.activity.source}`;
    if (height < 84) return `<div class="timeline-short ${row.activity.kind}" style="top:${top(row.start)}px;height:${height}px" title="${esc(name)}" aria-hidden="true"></div>`;
    return `<button type="button" class="timeline-block ${row.activity.kind}${height < 100 ? ' compact' : ''}" data-activity="${index}" aria-label="${esc(name)}，查看详情" style="top:${top(row.start)}px;height:${height-6}px"><span class="block-time">${parts(row.start,PEOPLE[person].zone).time}–${parts(row.end,PEOPLE[person].zone).time}</span>${parts(row.start,PEOPLE[person].zone).date !== parts(row.end,PEOPLE[person].zone).date ? `<span class="block-date">${dateText(row.start,PEOPLE[person].zone)} → ${dateText(row.end,PEOPLE[person].zone)}</span>` : ''}<span class="block-label">${icon(row.activity.icon)}${esc(row.activity.label)}</span><span class="block-source">${row.activity.source === '主动设置' ? '主动设置' : row.activity.source === '未知' ? '未知' : '通常安排'}</span></button>`;
  }).join('')}</div>`).join('');
  const bands = currentWindows.map(window => `<div class="shared-band" style="top:${top(window.start)}px;height:${(window.end-window.start)/60000*scale}px" aria-hidden="true"></div>`).join('');
  const ticks = [];
  for (let instant = range.start; instant < range.end; instant += 30*60000) {
    if (instant !== range.start && parts(instant,PEOPLE.me.zone).minutes % 60 !== 0) continue;
    ticks.push(`<div class="axis-tick" style="top:${top(instant)}px" aria-hidden="true"><span>${parts(instant,PEOPLE.partner.zone).time}</span><span>${parts(instant,PEOPLE.me.zone).time}</span></div>`);
  }
  const marker = now() >= range.start && now() < range.end ? `<div class="timeline-now" id="now-marker" style="top:${top(now())}px"><span>此刻</span></div>` : '';
  const firstWindow = currentWindows.find(window => window.end > now()) || currentWindows[0];
  const windowLabel = firstWindow ? `${currentWindows.length > 1 ? `${currentWindows.length} 段 · ` : ''}${parts(firstWindow.start,PEOPLE.partner.zone).time} / ${parts(firstWindow.start,PEOPLE.me.zone).time} · ${minutesText(firstWindow.end-firstWindow.start)}` : dataScenario() === 'missing' ? '资料不足，暂不能推荐' : dayWindows.length ? `这段范围没有共同窗口 · 全天还有 ${dayWindows.length} 段` : '这段范围没有共同窗口';
  $('#day-view').innerHTML = `<h1 class="screen-title">我们的一天</h1><p class="screen-subtitle">活动各自连续，同一高度是同一时刻。</p><div class="timeline-toolbar"><div class="day-controls"><button type="button" data-action="previous-day" aria-label="查看前一天">‹</button><strong>我的 ${dateText(day.start, PEOPLE.me.zone)}</strong><button type="button" data-action="next-day" aria-label="查看后一天">›</button><button type="button" class="text-button" data-action="return-now">回到此刻</button></div><div class="day-scope"><div class="range-switch" role="group" aria-label="时间轴范围"><button type="button" data-range="nearby" aria-pressed="${nearby}">附近几小时</button><button type="button" data-range="full" aria-pressed="${!nearby}">展开全天</button></div></div><div class="timeline-people"><div><span>${esc(PEOPLE.partner.name)} · ${PEOPLE.partner.city}</span><small>${dateText(range.start,PEOPLE.partner.zone)}</small></div><span class="axis-key">你 / 我</span><div><span>我 · ${PEOPLE.me.city}</span><small>${dateText(range.start,PEOPLE.me.zone)}</small></div></div><button type="button" class="window-summary" ${firstWindow ? 'data-action="timeline-window"' : dayWindows.length ? 'data-range="full"' : 'disabled'}>${icon(firstWindow ? 'heart' : 'clock')}<span>${firstWindow ? '共同可联系 · ' : ''}${windowLabel}</span>${firstWindow || dayWindows.length ? icon('arrow') : ''}</button>${short.length ? `<button type="button" class="text-button short-detail" data-action="short-activities">查看 ${short.length} 段短活动 ${icon('arrow')}</button>` : ''}</div><div class="timeline continuous" style="height:${top(range.end)}px">${bands}${columns}<div class="timeline-axis">${ticks.join('')}</div>${marker}</div><div class="day-end"><span>${fullDateText(range.end,PEOPLE.me.zone)}</span><span>${fullDateText(range.end,PEOPLE.partner.zone)}</span></div><p class="quiet-footnote">绿色横带按联系偏好估计，尚未约定。</p>`;
}
function renderRhythm() {
  mountRhythm($('#rhythm-view'),state.rhythm,value => {
    state.rhythm = value;
    const persisted = state.connection ? saveConnection(state.connection = {...state.connection,rhythm:value}) : saveRhythm(value);
    state.reminder = null;
    renderHome(); renderDay();
    toast(persisted ? '我的节奏已保存，共同时间已更新' : '本次节奏已更新，浏览器未允许保存');
  },{city:PEOPLE.me.city});
}
function render() {
  clock();
  const savedScroll = $('#app').scrollTop;
  renderHome(); renderDay(); renderRhythm();
  $('#desktop-scenarios').innerHTML = scenarioButtons();
  setTab(state.tab, false);
  $('#app').scrollTop = savedScroll;
}
function setTab(tab, scroll = true) {
  state.tab = tab;
  for (const name of ['home', 'day','rhythm']) {
    $(`#${name}-view`).hidden = state.setupOpen || name !== tab;
    const button = $(`[data-tab="${name}"]`);
    if (name === tab) button.setAttribute('aria-current', 'page'); else button.removeAttribute('aria-current');
  }
  if (scroll) {
    $('#app').scrollTop = 0;
    if (tab === 'day' && state.dayMode === 'full') REDUCED_MOTION ? scrollToNow() : requestAnimationFrame(scrollToNow);
  }
}
function finishSetup(record = state.connection) {
  if (record) applyConnection(record);
  state.setupOpen = false; $('.phone').classList.remove('setup-mode');
  $('#setup-view').hidden = true; $('#tabbar').hidden = false; $('#mobile-demo').hidden = false; $('#exit-setup').hidden = true;
  $('.review-panel').inert = false;
  state.scenario = 'normal'; state.override = null; state.reminder = null; state.dayOffset = 0; state.dayMode = 'nearby'; state.tab = 'home';
  render(); $('#app').scrollTop = 0;
}
function startSetup() {
  closeSheet();
  state.setupOpen = true; $('.phone').classList.add('setup-mode');
  for (const id of ['home','day','rhythm']) $(`#${id}-view`).hidden = true;
  $('#setup-view').hidden = false; $('#tabbar').hidden = true; $('#mobile-demo').hidden = true; $('#exit-setup').hidden = false;
  $('.review-panel').inert = true;
  mountOnboarding($('#setup-view'),state.connection || loadConnection(),{onChange:applyConnection,onFinish:finishSetup,notify:toast},state.rhythm);
  $('#app').scrollTop = 0; $('#app').focus();
}
function useSample() {
  state.connection = null; applyConnection(null); state.rhythm = loadRhythm(); state.ownNote = null;
  try {const note = JSON.parse(localStorage.getItem('nowus.prototype.note.v1') || 'null'); if (note && typeof note.text === 'string' && Number.isFinite(note.time)) state.ownNote = note;} catch { /* Optional original sample state. */ }
  finishSetup(null);
}
function scrollToNow() {
  const marker = $('#now-marker');
  if (marker) $('#app').scrollTop = marker.offsetTop + marker.parentElement.offsetTop - ($('.timeline-toolbar')?.offsetHeight || 0) - 90;
}
function openSheet(title, html) {
  lastFocus = document.activeElement;
  $('#sheet-title').textContent = title;
  $('#sheet-content').innerHTML = html;
  $('#sheet').hidden = false; $('#scrim').hidden = false;
  for (const selector of ['#app', '#app-header', '#tabbar', '.review-panel']) $(selector).inert = true;
  $('#close-sheet').focus();
}
function closeSheet() {
  $('#sheet').hidden = true; $('#scrim').hidden = true;
  for (const selector of ['#app', '#app-header', '#tabbar', '.review-panel']) $(selector).inert = false;
  if (lastFocus?.isConnected) lastFocus.focus(); else $(`[data-tab="${state.tab}"]`).focus();
}
function toast(text, undo = null) {
  clearTimeout(toastTimer);
  $('#toast').replaceChildren(document.createTextNode(text));
  if (undo) { const button = document.createElement('button'); button.type = 'button'; button.textContent = '撤销'; button.addEventListener('click', () => { undo(); $('#toast').hidden = true; }); $('#toast').append(button); }
  $('#toast').hidden = false;
  toastTimer = setTimeout(() => { $('#toast').hidden = true; }, undo ? 7000 : 3000);
}
function saveNoteStorage() { if (state.connection) {state.connection = {...state.connection,note:state.ownNote}; saveConnection(state.connection); return;} try { if (state.ownNote) localStorage.setItem('nowus.prototype.note.v1', JSON.stringify(state.ownNote)); else localStorage.removeItem('nowus.prototype.note.v1'); } catch { /* Session-only interaction remains usable. */ } }
function noteSheet() {
  openSheet('留一句，给你', `<p>新的留言会替换自己的上一条。没有已读，也不需要立刻回应。</p><form id="note-form"><label for="note-text">今天想告诉${esc(PEOPLE.partner.name)}的话</label><textarea id="note-text" rows="4" placeholder="比如：今天辛苦了，等你方便时再聊。">${esc(state.ownNote?.text || '')}</textarea><div class="counter" id="note-counter">${Array.from(state.ownNote?.text || '').length} / 120</div><p class="field-error" id="note-error" role="alert" hidden></p><div class="sheet-actions">${state.ownNote ? '<button type="button" class="secondary-button danger-button" data-action="delete-note">删除留言</button>' : ''}<button type="submit" class="primary-button">保存留言</button></div></form>`);
  $('#note-text').focus();
  $('#note-text').addEventListener('input', () => { $('#note-counter').textContent = `${Array.from($('#note-text').value).length} / 120`; $('#note-error').hidden = true; });
  $('#note-form').addEventListener('submit', event => {
    event.preventDefault(); const text = $('#note-text').value.trim();
    if (!text || Array.from(text).length > 120) { $('#note-error').textContent = !text ? '先写一句想告诉对方的话。' : '请把留言缩短到 120 字以内。'; $('#note-error').hidden = false; return; }
    state.ownNote = { text, time: now() }; saveNoteStorage(); closeSheet(); render(); toast('留言已保存在这个原型里');
  });
}
function statusSheet() {
  openSheet('调整我的状态', `<p>只改变接下来的一段时间，到期恢复通常作息。</p><label for="status-duration">持续多久</label><select class="duration-field" id="status-duration"><option value="30">30 分钟</option><option value="60">1 小时</option><option value="180">3 小时</option></select><div class="status-options"><button type="button" class="status-choice" data-status="free">${icon('heart')}<span><strong>现在愿意联系</strong><small>将这段时间标为可联系</small></span></button><button type="button" class="status-choice" data-status="busy">${icon('moon')}<span><strong>暂时不方便</strong><small>这段时间不推荐联系</small></span></button><button type="button" class="status-choice" data-status="reset">${icon('clock')}<span><strong>恢复通常作息</strong><small>取消我的临时状态</small></span></button></div>`);
}
function windowSheet(window = upcomingWindow()) {
  if (!window) return;
  const effectiveStart = now() >= window.start && now() < window.end ? now() : window.start;
  const busy = ['me','partner'].filter(person => routineAt(person,effectiveStart).kind === 'busy').map(person => `${person === 'me' ? '我' : PEOPLE[person].name}通常在${routineAt(person,effectiveStart).label}，但单独把这段时间设为可联系。`).join(' ');
  openSheet('共同可联系时段', `<p>按双方设置的联系偏好估计，可能与实际安排不同。尚未约定，也不要求立即回复。</p>${busy ? `<p class="contact-explanation">${esc(busy)}</p>` : ''}${Object.keys(PEOPLE).map(person => `<div class="detail-line"><span>${person === 'me' ? '我' : esc(PEOPLE[person].name)} · ${PEOPLE[person].city}</span><strong>${fullDateText(effectiveStart,PEOPLE[person].zone)}<br>至 ${fullDateText(window.end,PEOPLE[person].zone)}</strong></div>`).join('')}<div class="detail-line"><span>共同时间</span><strong>${minutesText(window.end - effectiveStart)}</strong></div>${window.end > now() ? `<p style="margin-top:16px">原型只演示保存提醒，不会发送通知。</p><div class="sheet-actions"><button type="button" class="primary-button" data-remind="${window.start}">${state.reminder === window.start ? '取消这个提醒' : '给自己留个提醒'}</button></div>` : '<p style="margin-top:16px">这是按当前模板回看的时段，不是实际活动记录。</p>'}`);
}
function activitySheet(index) {
  const row = currentRows[index], {activity,person} = row, p = PEOPLE[person];
  const contactRanges = [];
  let start = null, unknown = false;
  for (let time = row.start; time <= row.end; time += 60000) {
    const value = time < row.end ? canContact(person,time) : false;
    if (value === null) unknown = true;
    if (value === true && start === null) start = time;
    if (value !== true && start !== null) {contactRanges.push(`${parts(start,p.zone).time}–${parts(time,p.zone).time}`); start = null;}
  }
  openSheet(`${person === 'me' ? '我' : p.name} · ${activity.label}`, `<p>${activity.source === '主动设置' ? '这是主动分享的临时状态。' : activity.source === '未知' ? '尚未设置，不推测这段时间的活动。' : '这是通常安排，可能与实际生活不同。'}</p><div class="detail-line"><span>${p.city}当地时间</span><strong>${fullDateText(row.start, p.zone)}<br>至 ${fullDateText(row.end, p.zone)}</strong></div><div class="detail-line"><span>信息来源</span><strong>${activity.source}</strong></div><div class="detail-line"><span>单独设置的联系偏好</span><strong>${contactRanges.length ? `${contactRanges.join('、')} 可联系` : unknown ? '尚未设置' : '未设为可联系'}</strong></div>${activity.kind === 'busy' && contactRanges.length ? '<p style="margin-top:16px">活动与联系偏好独立：有活动安排也可以单独标记一段可联系时间。</p>' : ''}<div class="sheet-actions"><button type="button" class="primary-button" data-action="close-sheet">知道了</button></div>`);
}

document.addEventListener('click', event => {
  const button = event.target.closest('button'); if (!button) return;
  if (button.dataset.tab) { setTab(button.dataset.tab); return; }
  if (button.dataset.scenario) { state.scenario = button.dataset.scenario; state.dayOffset = 0; state.dayMode = 'nearby'; state.override = null; state.reminder = null; closeSheet(); render(); $('#app').scrollTop = 0; return; }
  if (button.dataset.range) {state.dayMode = button.dataset.range; if (state.dayMode === 'nearby') state.dayOffset = 0; renderDay(); $('#app').scrollTop = 0; return;}
  if (button.dataset.windowIndex) {windowSheet(currentWindows[Number(button.dataset.windowIndex)]); return;}
  if (button.dataset.status) {
    if (button.dataset.status === 'reset') state.override = null;
    else state.override = { person: 'me', kind: button.dataset.status, start: now(), end: now() + Number($('#status-duration').value) * 60000 };
    closeSheet(); render(); toast(state.override ? '临时状态已更新' : '已恢复通常作息'); return;
  }
  if (button.dataset.remind) { const instant = Number(button.dataset.remind); state.reminder = state.reminder === instant ? null : instant; closeSheet(); render(); toast(state.reminder ? '已保存演示提醒，不会发送通知' : '已取消演示提醒'); return; }
  if (button.dataset.activity) { activitySheet(Number(button.dataset.activity)); return; }
  switch (button.dataset.action) {
    case 'start-setup': startSetup(); break;
    case 'use-sample': closeSheet(); useSample(); break;
    case 'open-day': state.dayOffset = 0; state.dayMode = 'nearby'; renderDay(); setTab('day'); break;
    case 'open-rhythm': setTab('rhythm'); break;
    case 'read-note': openSheet(`${PEOPLE.partner.name}留给我的一句话`, `<div class="expanded-note">${noteMarkup(initialNote,'partner')}</div><p class="note-local-times">发布时：${PEOPLE.me.city} ${fullDateText(initialNote.time,PEOPLE.me.zone)}<br>${PEOPLE.partner.city} ${fullDateText(initialNote.time,PEOPLE.partner.zone)}</p><p>没有已读，也不需要立刻回应。</p><div class="sheet-actions"><button type="button" class="primary-button" data-action="edit-note">我也留一句</button></div>`); break;
    case 'edit-note': noteSheet(); break;
    case 'status': statusSheet(); break;
    case 'window-detail': windowSheet(); break;
    case 'timeline-window': if (currentWindows.length === 1) windowSheet(currentWindows[0]); else openSheet('这段范围的共同时间', `<p>按双方联系偏好估计，尚未约定。过去的时段只按当前模板回看。</p><div class="scenario-list">${currentWindows.map((window,index) => `<button type="button" class="scenario-button" data-window-index="${index}"><span>我 ${fullDateText(window.start,PEOPLE.me.zone)}<br>${esc(PEOPLE.partner.name)} ${fullDateText(window.start,PEOPLE.partner.zone)}</span><span>${minutesText(window.end-window.start)}</span></button>`).join('')}</div>`); break;
    case 'short-activities': openSheet('短活动详情', `<p>图上按真实时长显示，短活动在这里展开。</p><div class="scenario-list">${currentRows.map((row,index) => row.end-row.start < 30*60000 ? `<button type="button" class="scenario-button" data-activity="${index}">${PEOPLE[row.person].city} ${parts(row.start,PEOPLE[row.person].zone).time} · ${esc(row.activity.label)}</button>` : '').join('')}</div>`); break;
    case 'close-sheet': closeSheet(); break;
    case 'previous-day': state.dayOffset--; state.dayMode = 'full'; renderDay(); $('#app').scrollTop = 0; break;
    case 'next-day': state.dayOffset++; state.dayMode = 'full'; renderDay(); $('#app').scrollTop = 0; break;
    case 'return-now': state.dayOffset = 0; state.dayMode = 'nearby'; renderDay(); $('#app').scrollTop = 0; break;
    case 'delete-note': { const deleted = state.ownNote; state.ownNote = null; saveNoteStorage(); closeSheet(); render(); toast('留言已删除', () => { state.ownNote = deleted; saveNoteStorage(); render(); }); break; }
  }
});
$('#mobile-demo').addEventListener('click', () => openSheet('体验原型', `<p>演示人物与固定时间，仅用于评审原型。操作不会发给任何人。</p><div class="scenario-list"><button type="button" class="scenario-button" data-action="start-setup">体验首次使用与配对 ${icon('arrow')}</button><button type="button" class="scenario-button" data-action="use-sample">查看双人示例（保留首次使用记录）</button>${scenarioButtons()}</div>`));
$('#exit-setup').addEventListener('click', () => finishSetup());
$('#close-sheet').addEventListener('click', closeSheet);
$('#scrim').addEventListener('click', closeSheet);
document.addEventListener('keydown', event => {
  if ($('#sheet').hidden) return;
  if (event.key === 'Escape') { event.preventDefault(); closeSheet(); }
  if (event.key === 'Tab') {
    const focusable = [...$('#sheet').querySelectorAll('button:not(:disabled), textarea, select, input, a[href]')].filter(el => !el.hidden);
    const first = focusable[0], last = focusable.at(-1);
    if (event.shiftKey && document.activeElement === first) { event.preventDefault(); last.focus(); }
    else if (!event.shiftKey && document.activeElement === last) { event.preventDefault(); first.focus(); }
  }
});
function clock() { $('#device-clock').textContent = parts(now(), PEOPLE.me.zone).time; }
clock();
$('#home-tab-icon').innerHTML = icon('heart'); $('#day-tab-icon').innerHTML = icon('day');
$('#rhythm-tab-icon').innerHTML = icon('clock');
render();
if (new URLSearchParams(location.search).get('flow') === 'setup') startSetup();
