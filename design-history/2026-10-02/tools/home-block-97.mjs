const periodOf = person => {
  const hour = +parts(now(), PEOPLE[person].zone).hour;
  return hour >= 6 && hour < 18
    ? { kind: 'day', label: hour < 11 ? '早晨' : hour < 14 ? '午间' : '午后' }
    : { kind: 'night', label: hour >= 23 || hour < 6 ? '深夜' : '夜晚' };
};
function personCell(person, window) {
  const moment = personMoment(person);
  const period = periodOf(person);
  const usual = ({ '晚间休息': '休息', '午间休息': '休息', '通勤': '通勤路上' })[moment.activity.label] || moment.activity.label;
  const state = moment.activity.source === '按通常作息'
    ? person === 'partner' ? `通常在${usual}` : usual
    : moment.activity.source === '未知' ? '此刻安排未分享' : moment.activity.label;
  const zone = moment.profile.zone;
  return {
    now: `<div class="person-cell ${person === 'me' ? 'self' : 'partner'}">
        <div class="row-identity"><span class="avatar ${period.kind}" aria-hidden="true">${esc(moment.initial)}</span><strong>${esc(moment.name)}</strong><span class="row-place">· ${esc(moment.profile.city)}</span></div>
        <div class="row-line"><time class="now-time">${moment.local.time}</time><span class="row-place">${moment.date}</span></div>
        <p class="row-state">${esc(period.label)} · ${esc(state)}</p>
      </div>`,
    window: window
      ? `<div class="panel-cell ${person === 'me' ? 'self' : 'partner'}"><time class="window-time">${parts(window.start, zone).time}–${parts(window.end, zone).time}</time></div>`
      : `<div class="panel-cell ${person === 'me' ? 'self' : 'partner'}"><span class="window-time window-time-empty">—:—</span></div>`,
  };
}
function pairBoard() {
  const window = upcomingWindow();
  const missing = dataScenario() === 'missing';
  const when = window
    ? `${window.start <= now() ? '现在可以联系' : parts(window.start, PEOPLE.me.zone).date !== parts(now(), PEOPLE.me.zone).date ? `${localDayRelation(window.start, 'me')}可以联系` : `${minutesText(window.start - now()).replace(/\s+/g, '')}后开始`} · ${minutesText(window.end - window.start).replace(/\s+/g, '')}`
    : missing ? '等待彼此的节奏' : '暂无交集';
  const partner = personCell('partner', window);
  const self = personCell('me', window);
  return `<section class="pair-board" aria-label="此刻与下一段共同时间">
      <span class="grid-label">此刻</span>
      <div class="panel-head"><span class="band-label">下一段共同时间</span><span class="band-when">${esc(when)}</span></div>
      ${partner.now}${partner.window}${self.now}${self.window}
    </section>`;
}
function soloBoard() {
  return `<div class="board-solo"><h2 class="board-solo-title">先过好自己的一天</h2><p class="board-solo-text">配对并填写联系偏好后，这里会并排显示双方此刻与下一段共同时间。</p><button type="button" class="board-solo-action" data-action="start-setup">继续邀请伴侣 ${icon('arrow')}</button></div>`;
}
function boardFoot() {
  if (!partnerJoined()) return '';
  const window = upcomingWindow();
  const missing = dataScenario() === 'missing';
  if (!window) return `<div class="board-foot"><span class="foot-note">${missing ? '未知时间不会算作共同空闲。' : '按目前的联系偏好，没有找到共同窗口。'}</span><button type="button" class="foot-action" data-action="${missing ? 'edit-note' : 'open-rhythm'}">${missing ? '先留一句关心' : '设置我的节奏'} ${icon('arrow')}</button></div>`;
  return `<div class="board-foot"><span class="foot-note">按双方联系偏好估计 · 尚未约定</span><button type="button" class="foot-action" data-action="window-detail">${state.reminder === window.start ? '提醒已设' : '设置提醒'} ${icon('arrow')}</button></div>`;
}
function noteMarkup(note, person) {
  if (!note) return `<p class="note-empty">留一句今天想告诉${esc(PEOPLE.partner.name)}的话。</p>`;
  return `<div class="note-head"><span class="avatar">${esc(PEOPLE[person].short)}</span><span>${person === 'me' ? '我留给你' : `${esc(PEOPLE[person].name)}留给我`}</span><time>${dateText(note.time, PEOPLE.me.zone)} ${parts(note.time, PEOPLE.me.zone).time}</time></div><p class="note-body">${esc(note.text)}</p>`;
}
function noteSection() {
  if (!partnerJoined()) return '';
  const name = esc(PEOPLE.partner.name);
  return `<section class="note-block" aria-label="伴侣留言"><div class="note-meta"><span class="avatar night" aria-hidden="true">${esc(PEOPLE.partner.short)}</span><strong class="note-author">${name}</strong><time class="note-time" datetime="${new Date(initialNote.time).toISOString()}">${relativeNoteTime(initialNote.time)}</time></div><p class="note-text">${esc(initialNote.text).replace('，', '，<br>')}</p><button type="button" class="reply-field" data-action="edit-note"><span class="reply-hint">回${name}一句</span></button></section>`;
}
function renderHome() {
  $('#home-view').innerHTML = `<div class="home-stack">${partnerJoined() ? pairBoard() + boardFoot() : soloBoard()}${noteSection()}</div>`;
}
