const periodOf = person => {
  const hour = +parts(now(), PEOPLE[person].zone).hour;
  return hour >= 6 && hour < 18
    ? { kind: 'day', label: hour < 11 ? '早晨' : hour < 14 ? '午间' : '午后' }
    : { kind: 'night', label: hour >= 23 || hour < 6 ? '深夜' : '夜晚' };
};
function stateText(person, moment) {
  const usual = ({ '晚间休息': '休息', '午间休息': '休息', '通勤': '通勤路上' })[moment.activity.label] || moment.activity.label;
  if (moment.activity.source === '按通常作息') return person === 'partner' ? `通常在${usual}` : usual;
  if (moment.activity.source === '未知') return '此刻安排未分享';
  return moment.activity.label;
}
function axisBounds() {
  const window = upcomingWindow();
  const start = now() - 30 * 60000;
  const end = Math.max(now() + 2.5 * 3600000, window ? window.end + 30 * 60000 : 0);
  return { start, end, window, position: t => Math.min(100, Math.max(0, (t - start) / (end - start) * 100)) };
}
function trackRow(person, axis) {
  const moment = personMoment(person);
  const period = periodOf(person);
  const zone = moment.profile.zone;
  const nowLeft = axis.position(now());
  const windowLeft = axis.window ? axis.position(axis.window.start) : null;
  const windowWidth = axis.window ? axis.position(axis.window.end) - windowLeft : 0;
  const windowLabelLeft = axis.window ? Math.min(86, Math.max(windowLeft + windowWidth / 2, nowLeft + 22)) : 0;
  return `<article class="track-row ${person === 'me' ? 'self' : 'partner'}">
        <div class="track-head"><span class="avatar ${period.kind}" aria-hidden="true">${esc(moment.initial)}</span><strong>${esc(moment.name)}</strong><span class="track-place">· ${esc(moment.profile.city)} · ${moment.date}</span></div>
        <div class="track-labels">
          <time class="now-time" style="left:${nowLeft.toFixed(1)}%">${moment.local.time}</time>
          ${axis.window ? `<time class="window-time" style="left:${windowLabelLeft.toFixed(1)}%">${parts(axis.window.start, zone).time}–${parts(axis.window.end, zone).time}</time>` : ''}
        </div>
        <div class="track ${period.kind}">
          <span class="track-now" style="left:${nowLeft.toFixed(1)}%"></span>
          ${axis.window ? `<span class="track-window" style="left:${windowLeft.toFixed(1)}%;width:${Math.max(windowWidth, 6).toFixed(1)}%"></span>` : ''}
        </div>
        <p class="track-state">${esc(period.label)} · ${esc(stateText(person, moment))}</p>
      </article>`;
}
function trackBoard() {
  const axis = axisBounds();
  const missing = dataScenario() === 'missing';
  const when = axis.window
    ? `${axis.window.start <= now() ? '现在可以联系' : parts(axis.window.start, PEOPLE.me.zone).date !== parts(now(), PEOPLE.me.zone).date ? `${localDayRelation(axis.window.start, 'me')}可以联系` : `${minutesText(axis.window.start - now()).replace(/\s+/g, '')}后开始`} · ${minutesText(axis.window.end - axis.window.start).replace(/\s+/g, '')}`
    : missing ? '等待彼此的节奏' : '暂无交集';
  return `<section class="track-board" aria-label="此刻与下一段共同时间">
      <div class="board-head"><span class="board-label">此刻</span><span class="board-when">下一段共同时间 · ${esc(when)}</span></div>
      ${trackRow('partner', axis)}${trackRow('me', axis)}
    </section>`;
}
function soloBoard() {
  return `<div class="board-solo"><h2 class="board-solo-title">先过好自己的一天</h2><p class="board-solo-text">配对并填写联系偏好后，这里会画出双方此刻与下一段共同时间。</p><button type="button" class="board-solo-action" data-action="start-setup">继续邀请伴侣 ${icon('arrow')}</button></div>`;
}
function boardFoot() {
  if (!partnerJoined()) return '';
  const axis = axisBounds();
  const missing = dataScenario() === 'missing';
  const note = axis.window
    ? '按双方联系偏好估计 · 尚未约定'
    : missing ? '未知时间不会算作共同空闲。' : '按目前的联系偏好，没有找到共同窗口。';
  const action = axis.window
    ? `<button type="button" class="foot-action" data-action="window-detail">${state.reminder === axis.window.start ? '提醒已设' : '设置提醒'} ${icon('arrow')}</button>`
    : `<button type="button" class="foot-action" data-action="${missing ? 'edit-note' : 'open-rhythm'}">${missing ? '先留一句关心' : '设置我的节奏'} ${icon('arrow')}</button>`;
  return `<div class="board-foot"><span class="foot-key"><i class="key-now"></i>现在<i class="key-window"></i>共同时间</span><span class="foot-note">${note}</span>${action}</div>`;
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
  $('#home-view').innerHTML = `<div class="home-stack">${partnerJoined() ? trackBoard() + boardFoot() : soloBoard()}${noteSection()}</div>`;
}
