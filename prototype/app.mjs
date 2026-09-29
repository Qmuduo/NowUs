import { PEOPLE, SCENARIOS, parts, dayBounds, routineAt as modelRoutine, commonWindows as modelWindows, activitySegments, nextWindow, scenarioOverride, offsetDifference, canContact as modelContact } from './model.mjs?v=2';
import { loadRhythm, saveRhythm, mountRhythm } from './rhythm.mjs?v=2';

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
const initialNote = { text: '今天看到一只小猫，趴在书店门口晒太阳。\n等你有空，想讲给你听。', time: Date.parse('2026-09-29T11:45:00Z') };
const state = { scenario: 'normal', tab: 'home', dayOffset: 0, dayMode:'nearby', override: null, ownNote: null, reminder: null, rhythm:loadRhythm() };
try { const saved = JSON.parse(localStorage.getItem('nowus.prototype.note.v1') || 'null'); if (saved && typeof saved.text === 'string' && Number.isFinite(saved.time)) state.ownNote = saved; } catch { /* Local storage is optional for the demo. */ }
let lastFocus = null;
let toastTimer;
let currentRows = [];
let currentWindows = [];
const now = () => Date.parse(SCENARIOS[state.scenario].now);
const overrides = () => [state.override, scenarioOverride(state.scenario)].filter(Boolean);
const routineAt = (person, instant, scenario = state.scenario, active = overrides()) => modelRoutine(person,instant,scenario,active,state.rhythm);
const canContact = (person, instant, scenario = state.scenario, active = overrides()) => modelContact(person,instant,scenario,active,state.rhythm);
const commonWindows = (range, scenario = state.scenario, active = overrides()) => modelWindows(range,scenario,active,state.rhythm);
const dateText = (time, zone) => { const p = parts(time, zone); return `${Number(p.month)}月${Number(p.day)}日`; };
const fullDateText = (time, zone) => `${dateText(time, zone)} ${parts(time, zone).time}`;
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
  return hour >= 6 && hour < 18 ? { kind: 'day', label: hour < 11 ? '早晨' : hour < 14 ? '午间' : '午后', icon: 'sun' } : { kind: 'night', label: hour >= 23 || hour < 6 ? '深夜' : '夜晚', icon: 'moon' };
}
function scenarioButtons() {
  return Object.entries(SCENARIOS).map(([id, s]) => `<button type="button" class="scenario-button" data-scenario="${id}" aria-pressed="${state.scenario === id}"><span>${esc(s.label)}</span>${state.scenario === id ? icon('check') : icon('arrow')}</button>`).join('');
}
function timePerson(person) {
  const p = PEOPLE[person], local = parts(now(), p.zone), period = stage(now(), person);
  return `<div class="time-person ${period.kind}"><div class="person-label"><span class="avatar">${p.short}</span><span>${person === 'me' ? '我' : p.name} · ${p.city}</span></div><div class="time-weather"><span>${period.label}</span>${icon(period.icon)}</div><div class="local-time">${local.time}</div><div class="local-date">${dateText(now(), p.zone)} · ${new Intl.DateTimeFormat('zh-CN', { timeZone: p.zone, weekday: 'short' }).format(now())}</div></div>`;
}
function nextStage(person) {
  const current = routineAt(person, now(), state.scenario, overrides());
  for (let t = now() + 60000; t <= now() + 12 * 3600000; t += 60000) {
    const later = routineAt(person, t, state.scenario, overrides());
    if (later.label !== current.label) return `下一阶段：${later.label} · 当地 ${parts(t, PEOPLE[person].zone).time}`;
  }
  return '以平时的生活安排为参考';
}
function upcomingWindow() {
  return nextWindow(now(),state.scenario,overrides(),state.rhythm);
}
function commonCard() {
  if (state.scenario === 'missing') return `<div class="common-card no-common"><div class="common-label">${icon('clock')}等待彼此的节奏</div><h3 class="common-title">暂时还不能推荐时间</h3><p>阿远尚未设置作息和联系偏好，未知时间不会算作共同空闲。</p><button type="button" class="text-button" data-action="edit-note">先留一句关心 ${icon('arrow')}</button></div>`;
  const window = upcomingWindow();
  const ownUnknown = canContact('me',now()) === null;
  if (!window) return `<div class="common-card no-common"><div class="common-label">${icon('heart')}我们的时间</div><h3 class="common-title">${ownUnknown ? '我的联系时间还未设置' : '接下来七天，暂时没有交集'}</h3><p>${ownUnknown ? '未知时间不作推荐。可以补充通常愿意联系的时间，也可以先留句话。' : '按目前的联系偏好，没有找到共同窗口。留句话，等彼此方便时再聊。'}</p><button type="button" class="text-button" data-action="${ownUnknown ? 'open-rhythm' : 'edit-note'}">${ownUnknown ? '设置我的节奏' : '留一句关心'} ${icon('arrow')}</button></div>`;
  const start = Math.max(now(), window.start);
  const laterDay = parts(start, PEOPLE.me.zone).date !== parts(now(), PEOPLE.me.zone).date;
  const title = window.start <= now() ? '现在处于共同可联系时段' : laterDay ? '今天接下来暂时错开' : `${minutesText(start - now())}后，可以联系`;
  const times = Object.keys(PEOPLE).map(person => `<div><small>${person === 'me' ? '我' : PEOPLE[person].name} · ${dateText(start, PEOPLE[person].zone)}</small><strong>${parts(start, PEOPLE[person].zone).time}–${parts(window.end, PEOPLE[person].zone).time}</strong></div>`).join('');
  const busy = ['me','partner'].filter(person => routineAt(person,start).kind === 'busy').map(person => person === 'me' ? '我' : PEOPLE[person].name);
  return `<div class="common-card"><div class="common-top"><span class="common-label">${icon('heart')}${window.start <= now() ? '当前共同窗口' : '下一段共同时间'}</span><span>${minutesText(window.end - start)}</span></div><h3 class="common-title">${title}</h3><div class="common-times">${times}</div>${busy.length ? `<p class="contact-context">${busy.join('、')}通常有活动安排，但这段时间单独设为可联系。</p>` : ''}${ownUnknown ? '<p class="contact-context">今天的联系偏好尚未设置，以下来自已设置的未来安排。</p>' : ''}<div class="common-footer"><span>按双方联系偏好 · 尚未约定</span><button type="button" data-action="window-detail">${state.reminder === window.start ? '已保存提醒' : '查看时段'}</button></div></div>`;
}
function noteMarkup(note, person) {
  if (!note) return `<p class="note-empty">留一句今天想告诉${PEOPLE.partner.name}的话。</p>`;
  return `<div class="note-head"><span class="avatar">${PEOPLE[person].short}</span><span>${person === 'me' ? '我留给你' : `${PEOPLE[person].name}留给我`}</span><time>${dateText(note.time, PEOPLE.me.zone)} ${parts(note.time, PEOPLE.me.zone).time}</time></div><p class="note-body">${esc(note.text)}</p>`;
}
function renderHome() {
  const other = routineAt('partner', now(), state.scenario, overrides());
  const me = routineAt('me', now(), state.scenario, overrides());
  const differentDate = parts(now(), PEOPLE.me.zone).date !== parts(now(), PEOPLE.partner.zone).date;
  $('#home-view').innerHTML = `<h1 class="screen-title">${differentDate ? '日期不同，彼此都在' : '我的夜晚，你的早晨'}</h1><div class="time-pair">${timePerson('me')}${timePerson('partner')}</div><p class="offset-caption">${icon('link')}${PEOPLE.partner.name}比我慢 ${offsetDifference(now())} 小时${differentDate ? ' · 那里还是昨天' : ''}</p><div class="section-heading"><h2>阿远的一天，走到这里</h2><button type="button" class="text-button" data-action="open-day">看时间轴 ${icon('arrow')}</button></div><div class="life-card"><div class="life-row"><div class="activity-symbol">${icon(other.icon)}</div><div class="life-content"><div class="life-line"><strong>${esc(other.source === '按通常作息' ? `这个时段通常在${other.label}` : other.label)}</strong></div><div class="life-source">${source(other)}</div><p class="life-description">${esc(other.source === '未知' ? '等对方填写后再显示，不作活动推测。' : other.end ? `主动设置持续到纽约 ${parts(other.end, PEOPLE.partner.zone).time}` : nextStage('partner'))}</p></div></div><div class="my-life">${icon(me.icon)}<span>我 · ${esc(me.label)}</span>${me.source === '主动设置' ? '<span class="source active">主动设置</span>' : ''}<button type="button" class="text-button" data-action="status">调整状态</button></div></div><button type="button" class="note-preview" data-action="read-note"><span class="note-preview-head">${icon('message')}阿远留给我的一句话 <span>${dateText(initialNote.time,PEOPLE.me.zone)} ${parts(initialNote.time,PEOPLE.me.zone).time}</span></span><span class="note-preview-text">${esc(initialNote.text)}</span><span class="note-preview-more">展开留言 ${icon('arrow')}</span></button><div class="section-heading"><h2>我们可以聊聊的时间</h2></div>${commonCard()}<div class="section-heading"><h2>我留给你的话</h2><button type="button" class="text-button" data-action="edit-note">${icon('edit')}${state.ownNote ? '编辑留言' : '写一句'}</button></div><div class="notes">${state.ownNote ? noteMarkup(state.ownNote,'me') : '<p class="note-empty">把今天的一点小事留给阿远，不必等到同时在线。</p>'}</div><p class="quiet-footnote">生活有各自的节奏，关心可以慢慢抵达。</p>`;
}
function renderDay() {
  const day = bounds();
  const nearby = state.dayMode === 'nearby' && state.dayOffset === 0;
  const range = nearby ? {start:Math.max(day.start,now()-30*60000),end:Math.min(day.end,now()+5.5*3600000)} : day;
  const scale = 2.8;
  const top = time => (time-range.start)/60000*scale;
  currentRows = ['me','partner'].flatMap(person => activitySegments(range,person,state.scenario,overrides(),state.rhythm));
  currentWindows = commonWindows(range);
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
    ticks.push(`<div class="axis-tick" style="top:${top(instant)}px" aria-hidden="true"><span>${parts(instant,PEOPLE.me.zone).time}</span><span>${parts(instant,PEOPLE.partner.zone).time}</span></div>`);
  }
  const marker = now() >= range.start && now() < range.end ? `<div class="timeline-now" id="now-marker" style="top:${top(now())}px"><span>此刻</span></div>` : '';
  const firstWindow = currentWindows.find(window => window.end > now()) || currentWindows[0];
  const windowLabel = firstWindow ? `${currentWindows.length > 1 ? `${currentWindows.length} 段 · ` : ''}${parts(firstWindow.start,PEOPLE.me.zone).time} / ${parts(firstWindow.start,PEOPLE.partner.zone).time} · ${minutesText(firstWindow.end-firstWindow.start)}` : state.scenario === 'missing' ? '资料不足，暂不能推荐' : '这段范围没有共同窗口';
  $('#day-view').innerHTML = `<h1 class="screen-title">我们的一天</h1><p class="screen-subtitle">活动各自连续，同一高度是同一时刻。</p><div class="timeline-toolbar"><div class="day-controls"><button type="button" data-action="previous-day" aria-label="查看前一天">‹</button><strong>我的 ${dateText(day.start, PEOPLE.me.zone)}</strong><button type="button" data-action="next-day" aria-label="查看后一天">›</button><button type="button" class="text-button" data-action="return-now">回到此刻</button></div><div class="day-scope"><div class="range-switch" role="group" aria-label="时间轴范围"><button type="button" data-range="nearby" aria-pressed="${nearby}">附近几小时</button><button type="button" data-range="full" aria-pressed="${!nearby}">展开全天</button></div></div><div class="timeline-people"><div><span>我 · 北京</span><small>${dateText(range.start,PEOPLE.me.zone)}</small></div><span class="axis-key">我 / 你</span><div><span>阿远 · 纽约</span><small>${dateText(range.start,PEOPLE.partner.zone)}</small></div></div><button type="button" class="window-summary" ${firstWindow ? 'data-action="timeline-window"' : 'disabled'}>${icon(firstWindow ? 'heart' : 'clock')}<span>${firstWindow ? '共同可联系 · ' : ''}${windowLabel}</span>${firstWindow ? icon('arrow') : ''}</button>${short.length ? `<button type="button" class="text-button short-detail" data-action="short-activities">查看 ${short.length} 段短活动 ${icon('arrow')}</button>` : ''}</div><div class="timeline continuous" style="height:${top(range.end)}px">${bands}${columns}<div class="timeline-axis">${ticks.join('')}</div>${marker}</div><div class="day-end"><span>${fullDateText(range.end,PEOPLE.me.zone)}</span><span>${fullDateText(range.end,PEOPLE.partner.zone)}</span></div><p class="quiet-footnote">绿色横带按联系偏好估计，尚未约定。</p>`;
}
function renderRhythm() {
  mountRhythm($('#rhythm-view'),state.rhythm,value => {
    state.rhythm = value;
    const persisted = saveRhythm(value);
    state.reminder = null;
    renderHome(); renderDay();
    toast(persisted ? '我的节奏已保存，共同时间已更新' : '本次节奏已更新，浏览器未允许保存');
  });
}
function render() {
  const savedScroll = $('#app').scrollTop;
  renderHome(); renderDay(); renderRhythm();
  $('#desktop-scenarios').innerHTML = scenarioButtons();
  setTab(state.tab, false);
  $('#app').scrollTop = savedScroll;
}
function setTab(tab, scroll = true) {
  state.tab = tab;
  for (const name of ['home', 'day','rhythm']) {
    $(`#${name}-view`).hidden = name !== tab;
    const button = $(`[data-tab="${name}"]`);
    if (name === tab) button.setAttribute('aria-current', 'page'); else button.removeAttribute('aria-current');
  }
  if (scroll) {
    $('#app').scrollTop = 0;
    if (tab === 'day' && state.dayMode === 'full') REDUCED_MOTION ? scrollToNow() : requestAnimationFrame(scrollToNow);
  }
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
function saveNoteStorage() { try { if (state.ownNote) localStorage.setItem('nowus.prototype.note.v1', JSON.stringify(state.ownNote)); else localStorage.removeItem('nowus.prototype.note.v1'); } catch { /* Session-only interaction remains usable. */ } }
function noteSheet() {
  openSheet('留一句，给你', `<p>新的留言会替换自己的上一条。没有已读，也不需要立刻回应。</p><form id="note-form"><label for="note-text">今天想告诉阿远的话</label><textarea id="note-text" rows="4" placeholder="比如：今天辛苦了，等你方便时再聊。">${esc(state.ownNote?.text || '')}</textarea><div class="counter" id="note-counter">${Array.from(state.ownNote?.text || '').length} / 120</div><p class="field-error" id="note-error" role="alert" hidden></p><div class="sheet-actions">${state.ownNote ? '<button type="button" class="secondary-button danger-button" data-action="delete-note">删除留言</button>' : ''}<button type="submit" class="primary-button">保存留言</button></div></form>`);
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
  openSheet('共同可联系时段', `<p>按双方设置的联系偏好估计，可能与实际安排不同。尚未约定，也不要求立即回复。</p>${busy ? `<p class="contact-explanation">${busy}</p>` : ''}${Object.keys(PEOPLE).map(person => `<div class="detail-line"><span>${person === 'me' ? '我' : PEOPLE[person].name} · ${PEOPLE[person].city}</span><strong>${fullDateText(effectiveStart,PEOPLE[person].zone)}<br>至 ${fullDateText(window.end,PEOPLE[person].zone)}</strong></div>`).join('')}<div class="detail-line"><span>共同时间</span><strong>${minutesText(window.end - effectiveStart)}</strong></div>${window.end > now() ? `<p style="margin-top:16px">原型只演示保存提醒，不会发送通知。</p><div class="sheet-actions"><button type="button" class="primary-button" data-remind="${window.start}">${state.reminder === window.start ? '取消这个提醒' : '给自己留个提醒'}</button></div>` : '<p style="margin-top:16px">这是按当前模板回看的时段，不是实际活动记录。</p>'}`);
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
    case 'open-day': state.dayOffset = 0; state.dayMode = 'nearby'; renderDay(); setTab('day'); break;
    case 'open-rhythm': setTab('rhythm'); break;
    case 'read-note': openSheet('阿远留给我的一句话', `<div class="expanded-note">${noteMarkup(initialNote,'partner')}</div><p class="note-local-times">发布时：北京 ${fullDateText(initialNote.time,PEOPLE.me.zone)}<br>纽约 ${fullDateText(initialNote.time,PEOPLE.partner.zone)}</p><p>没有已读，也不需要立刻回应。</p><div class="sheet-actions"><button type="button" class="primary-button" data-action="edit-note">我也留一句</button></div>`); break;
    case 'edit-note': noteSheet(); break;
    case 'status': statusSheet(); break;
    case 'window-detail': windowSheet(); break;
    case 'timeline-window': if (currentWindows.length === 1) windowSheet(currentWindows[0]); else openSheet('这段范围的共同时间', `<p>按双方联系偏好估计，尚未约定。过去的时段只按当前模板回看。</p><div class="scenario-list">${currentWindows.map((window,index) => `<button type="button" class="scenario-button" data-window-index="${index}"><span>我 ${fullDateText(window.start,PEOPLE.me.zone)}<br>阿远 ${fullDateText(window.start,PEOPLE.partner.zone)}</span><span>${minutesText(window.end-window.start)}</span></button>`).join('')}</div>`); break;
    case 'short-activities': openSheet('短活动详情', `<p>图上按真实时长显示，短活动在这里展开。</p><div class="scenario-list">${currentRows.map((row,index) => row.end-row.start < 30*60000 ? `<button type="button" class="scenario-button" data-activity="${index}">${PEOPLE[row.person].city} ${parts(row.start,PEOPLE[row.person].zone).time} · ${esc(row.activity.label)}</button>` : '').join('')}</div>`); break;
    case 'close-sheet': closeSheet(); break;
    case 'previous-day': state.dayOffset--; state.dayMode = 'full'; renderDay(); $('#app').scrollTop = 0; break;
    case 'next-day': state.dayOffset++; state.dayMode = 'full'; renderDay(); $('#app').scrollTop = 0; break;
    case 'return-now': state.dayOffset = 0; state.dayMode = 'nearby'; renderDay(); $('#app').scrollTop = 0; break;
    case 'delete-note': { const deleted = state.ownNote; state.ownNote = null; saveNoteStorage(); closeSheet(); render(); toast('留言已删除', () => { state.ownNote = deleted; saveNoteStorage(); render(); }); break; }
  }
});
$('#mobile-demo').addEventListener('click', () => openSheet('试试不同的一天', `<p>演示人物与固定时间，仅用于评审原型。操作不会发给任何人。</p><div class="scenario-list">${scenarioButtons()}</div>`));
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
function clock() { $('#device-clock').textContent = new Date().toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit', hour12: false }); }
clock(); setInterval(clock, 30000);
$('#home-tab-icon').innerHTML = icon('heart'); $('#day-tab-icon').innerHTML = icon('day');
$('#rhythm-tab-icon').innerHTML = icon('clock');
render();
