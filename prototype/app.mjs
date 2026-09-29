import { PEOPLE, SCENARIOS, parts, dayBounds, routineAt, commonWindows, timeline, scenarioOverride, offsetDifference, canContact } from './model.mjs';

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
const state = { scenario: 'normal', tab: 'home', dayOffset: 0, override: null, ownNote: null, reminder: null };
try { const saved = JSON.parse(localStorage.getItem('nowus.prototype.note.v1') || 'null'); if (saved && typeof saved.text === 'string' && Number.isFinite(saved.time)) state.ownNote = saved; } catch { /* Local storage is optional for the demo. */ }
let lastFocus = null;
let toastTimer;
let currentRows = [];
let currentWindows = [];
const now = () => Date.parse(SCENARIOS[state.scenario].now);
const overrides = () => [state.override, scenarioOverride(state.scenario)].filter(Boolean);
const dateText = (time, zone) => { const p = parts(time, zone); return `${Number(p.month)}月${Number(p.day)}日`; };
const fullDateText = (time, zone) => `${dateText(time, zone)} ${parts(time, zone).time}`;
const minutesText = ms => ms >= 3600000 && ms % 3600000 === 0 ? `${ms / 3600000} 小时` : `${Math.round(ms / 60000)} 分钟`;
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
  return commonWindows(dayBounds(now(), PEOPLE.me.zone), state.scenario, overrides()).find(w => w.end > now());
}
function commonCard() {
  const window = upcomingWindow();
  if (!window) return `<div class="common-card no-common"><div class="common-label">${icon('heart')}我们的时间</div><h3 class="common-title">今天的空闲，暂时错开了</h3><p>按目前安排，今天没有共同可联系时间。留句话，等彼此方便时再聊。</p><button type="button" class="text-button" data-action="edit-note">留一句关心 ${icon('arrow')}</button></div>`;
  const start = Math.max(now(), window.start);
  const title = window.start <= now() ? '现在，刚好都有空' : `${minutesText(start - now())}后，时间交汇`;
  const times = Object.keys(PEOPLE).map(person => `<div><small>${person === 'me' ? '我' : PEOPLE[person].name} · ${dateText(start, PEOPLE[person].zone)}</small><strong>${parts(start, PEOPLE[person].zone).time}–${parts(window.end, PEOPLE[person].zone).time}</strong></div>`).join('');
  return `<div class="common-card"><div class="common-top"><span class="common-label">${icon('heart')}下一段共同时间</span><span>${minutesText(window.end - start)}</span></div><h3 class="common-title">${title}</h3><div class="common-times">${times}</div><div class="common-footer"><span>按联系偏好估计 · 尚未约定</span><button type="button" data-action="window-detail">${state.reminder === window.start ? '已保存提醒' : '看看这段时间'}</button></div></div>`;
}
function noteMarkup(note, person) {
  if (!note) return `<p class="note-empty">留一句今天想告诉${PEOPLE.partner.name}的话。</p>`;
  return `<div class="note-head"><span class="avatar">${PEOPLE[person].short}</span><span>${person === 'me' ? '我留给你' : `${PEOPLE[person].name}留给我`}</span><time>${dateText(note.time, PEOPLE.me.zone)} ${parts(note.time, PEOPLE.me.zone).time}</time></div><p class="note-body">${esc(note.text)}</p>`;
}
function renderHome() {
  const other = routineAt('partner', now(), state.scenario, overrides());
  const me = routineAt('me', now(), state.scenario, overrides());
  const differentDate = parts(now(), PEOPLE.me.zone).date !== parts(now(), PEOPLE.partner.zone).date;
  $('#home-view').innerHTML = `<h1 class="screen-title">${differentDate ? '日期不同，彼此都在' : '我的夜晚，你的早晨'}</h1>${state.scenario === 'normal' ? '' : `<p class="screen-subtitle">${esc(SCENARIOS[state.scenario].description)}</p>`}<div class="time-pair">${timePerson('me')}${timePerson('partner')}</div><p class="offset-caption">${icon('link')}${PEOPLE.partner.name}比我慢 ${offsetDifference(now())} 小时${differentDate ? ' · 那里还是昨天' : ''}</p><div class="section-heading"><h2>你的一天，走到这里</h2><button type="button" class="text-button" data-action="open-day">看完整一天 ${icon('arrow')}</button></div><div class="life-card"><div class="life-row"><div class="activity-symbol">${icon(other.icon)}</div><div class="life-content"><div class="life-line"><strong>${esc(other.source === '主动设置' ? other.label : `通常在${other.label}`)}</strong>${source(other)}</div><p class="life-description">${esc(other.end ? `设置持续到当地 ${parts(other.end, PEOPLE.partner.zone).time}` : nextStage('partner'))}</p></div></div><div class="my-life">${icon(me.icon)}<span>我 · ${esc(me.label)}</span>${me.source === '主动设置' ? '<span class="source active">主动设置</span>' : ''}<button type="button" class="text-button" data-action="status">调整状态</button></div></div><div class="section-heading"><h2>我们可以聊聊的时间</h2></div>${commonCard()}<div class="section-heading"><h2>给你留句话</h2><button type="button" class="text-button" data-action="edit-note">${icon('edit')}${state.ownNote ? '编辑留言' : '写一句'}</button></div><div class="notes">${noteMarkup(initialNote, 'partner')}<div class="own-note">${noteMarkup(state.ownNote, 'me')}</div></div><p class="quiet-footnote">生活有各自的节奏，关心可以慢慢抵达。</p>`;
}
function renderDay() {
  const range = bounds();
  currentRows = timeline(range, state.scenario, overrides());
  currentWindows = commonWindows(range, state.scenario, overrides());
  const positionScale = 2.8;
  const rows = currentRows.map((row, index) => {
    const blocks = ['me', 'partner'].map(person => {
      const local = parts(row.start, PEOPLE[person].zone), previousDate = index > 0 ? parts(currentRows[index - 1].start, PEOPLE[person].zone).date : '';
      const date = index === 0 || local.date !== previousDate ? `<span class="block-date">${dateText(row.start, PEOPLE[person].zone)}</span>` : '';
      return `<button type="button" class="timeline-block ${row[person].kind}" data-activity="${index}" data-person="${person}" aria-label="${PEOPLE[person].city} ${dateText(row.start, PEOPLE[person].zone)} ${local.time} ${esc(row[person].label)}，查看详情"><span class="block-time">${local.time}${date}</span><span class="block-label">${icon(row[person].icon)}${esc(row[person].label)}</span>${row.shared ? '<span class="block-contact">都愿意联系</span>' : ''}</button>`;
    });
    return `<div class="timeline-row ${row.shared ? 'shared' : ''}" style="height:${(row.end - row.start) / 60000 * positionScale}px">${blocks[0]}<div class="timeline-middle" aria-hidden="true"></div>${blocks[1]}</div>`;
  }).join('');
  const marker = now() >= range.start && now() < range.end ? `<div class="timeline-now" id="now-marker" style="top:${(now() - range.start) / 60000 * positionScale}px"><span>此刻</span></div>` : '';
  $('#day-view').innerHTML = `<h1 class="screen-title">我们的一天</h1><p class="screen-subtitle">同一位置，同一个时刻，两种生活。</p><div class="day-controls"><button type="button" data-action="previous-day" aria-label="查看前一天">‹</button><strong>我的 ${dateText(range.start, PEOPLE.me.zone)}</strong><button type="button" data-action="next-day" aria-label="查看后一天">›</button></div><div class="day-legend"><span><i class="legend-dot"></i>共同可联系</span><span>按各自当地时间显示</span></div><div class="timeline-people"><div><span class="avatar">满</span><span>我 · 北京</span></div><span></span><div><span>阿远 · 纽约</span><span class="avatar">远</span></div></div><div class="timeline">${rows}${marker}</div><div class="day-end"><span>${fullDateText(range.end, PEOPLE.me.zone)}</span><span>${fullDateText(range.end, PEOPLE.partner.zone)}</span></div><p class="quiet-footnote">${currentWindows.length ? `这一天有 ${currentWindows.length} 段共同时间` : '这一天没有共同可联系时间'}</p><div class="return-now"><button type="button" class="pill-button" data-action="return-now">回到此刻</button></div>`;
}
function render() {
  const savedScroll = $('#app').scrollTop;
  renderHome(); renderDay();
  $('#desktop-scenarios').innerHTML = scenarioButtons();
  setTab(state.tab, false);
  $('#app').scrollTop = savedScroll;
}
function setTab(tab, scroll = true) {
  state.tab = tab;
  for (const name of ['home', 'day']) {
    $(`#${name}-view`).hidden = name !== tab;
    const button = $(`[data-tab="${name}"]`);
    if (name === tab) button.setAttribute('aria-current', 'page'); else button.removeAttribute('aria-current');
  }
  if (scroll) {
    $('#app').scrollTop = 0;
    if (tab === 'day') REDUCED_MOTION ? scrollToNow() : requestAnimationFrame(scrollToNow);
  }
}
function scrollToNow() {
  const marker = $('#now-marker');
  if (marker) $('#app').scrollTop = marker.offsetTop + marker.parentElement.offsetTop - 180;
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
  const effectiveStart = Math.max(now(), window.start);
  openSheet('这段时间，属于我们', `<p>双方都标记了愿意联系。这是时间建议，尚未和对方约定。</p>${Object.keys(PEOPLE).map(person => `<div class="detail-line"><span>${person === 'me' ? '我' : PEOPLE[person].name} · ${PEOPLE[person].city}</span><strong>${dateText(effectiveStart, PEOPLE[person].zone)}<br>${parts(effectiveStart, PEOPLE[person].zone).time}–${parts(window.end, PEOPLE[person].zone).time}</strong></div>`).join('')}<div class="detail-line"><span>共同时间</span><strong>${minutesText(window.end - effectiveStart)}</strong></div><p style="margin-top:16px">原型只演示保存提醒，不会发送通知。</p><div class="sheet-actions"><button type="button" class="primary-button" data-remind="${window.start}">${state.reminder === window.start ? '取消这个提醒' : '给自己留个提醒'}</button></div>`);
}
function activitySheet(index, person) {
  const row = currentRows[index], activity = row[person], p = PEOPLE[person];
  openSheet(`${p.role} · ${activity.label}`, `<p>${activity.source === '主动设置' ? '这是主动分享的临时状态。' : '这是通常作息，可能和此刻的实际安排不同。'}</p><div class="detail-line"><span>${p.city}当地时间</span><strong>${fullDateText(row.start, p.zone)}<br>至 ${fullDateText(row.end, p.zone)}</strong></div><div class="detail-line"><span>信息来源</span><strong>${activity.source}</strong></div><div class="detail-line"><span>联系偏好</span><strong>${canContact(person, row.start, state.scenario, overrides()) ? '愿意联系' : '暂不方便'}</strong></div>${row.shared ? '<p style="margin-top:16px">这段时间双方都愿意联系。</p>' : ''}<div class="sheet-actions"><button type="button" class="primary-button" data-action="close-sheet">知道了</button></div>`);
}

document.addEventListener('click', event => {
  const button = event.target.closest('button'); if (!button) return;
  if (button.dataset.tab) { setTab(button.dataset.tab); return; }
  if (button.dataset.scenario) { state.scenario = button.dataset.scenario; state.dayOffset = 0; state.override = null; state.reminder = null; closeSheet(); render(); $('#app').scrollTop = 0; if (state.tab === 'day') REDUCED_MOTION ? scrollToNow() : requestAnimationFrame(scrollToNow); return; }
  if (button.dataset.status) {
    if (button.dataset.status === 'reset') state.override = null;
    else state.override = { person: 'me', kind: button.dataset.status, start: now(), end: now() + Number($('#status-duration').value) * 60000 };
    closeSheet(); render(); toast(state.override ? '临时状态已更新' : '已恢复通常作息'); return;
  }
  if (button.dataset.remind) { const instant = Number(button.dataset.remind); state.reminder = state.reminder === instant ? null : instant; closeSheet(); render(); toast(state.reminder ? '已保存演示提醒，不会发送通知' : '已取消演示提醒'); return; }
  if (button.dataset.activity) { activitySheet(Number(button.dataset.activity), button.dataset.person); return; }
  switch (button.dataset.action) {
    case 'open-day': state.dayOffset = 0; renderDay(); setTab('day'); break;
    case 'edit-note': noteSheet(); break;
    case 'status': statusSheet(); break;
    case 'window-detail': windowSheet(); break;
    case 'close-sheet': closeSheet(); break;
    case 'previous-day': state.dayOffset--; renderDay(); $('#app').scrollTop = 0; break;
    case 'next-day': state.dayOffset++; renderDay(); $('#app').scrollTop = 0; break;
    case 'return-now': state.dayOffset = 0; renderDay(); scrollToNow(); break;
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
render();
