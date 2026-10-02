const periodOf = person => {
  const hour = +parts(now(), PEOPLE[person].zone).hour;
  return hour >= 6 && hour < 18
    ? { kind: 'day', label: hour < 11 ? '早晨' : hour < 14 ? '午间' : '午后' }
    : { kind: 'night', label: hour >= 23 || hour < 6 ? '深夜' : '夜晚' };
};
function personMoment(person) {
  const profile = PEOPLE[person];
  const local = parts(now(), profile.zone);
  const weekday = new Intl.DateTimeFormat('zh-CN', { timeZone: profile.zone, weekday: 'short' }).format(now());
  const activity = routineAt(person, now(), state.scenario, overrides());
  const next = person === 'partner' && activity.source === '按通常作息' ? nextStage('partner') : null;
  return {
    person, profile, local, activity, next,
    name: person === 'me' ? '我' : profile.name,
    date: `${Number(local.date.slice(5, 7))}/${Number(local.date.slice(8, 10))} · ${weekday}`,
  };
}
function nextStage(person) {
  const current = routineAt(person, now(), state.scenario, overrides());
  for (let t = now() + 60000; t <= now() + 12 * 3600000; t += 60000) {
    const later = routineAt(person, t, state.scenario, overrides());
    if (later.label !== current.label) return { label: later.label, time: parts(t, PEOPLE[person].zone).time };
  }
  return null;
}
function statusText(person, moment) {
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
function laneRow(person, axis) {
  const moment = personMoment(person);
  const period = periodOf(person);
  const zone = moment.profile.zone;
  const nowLeft = axis.position(now());
  const wStart = axis.window ? axis.position(axis.window.start) : null;
  const wEnd = axis.window ? axis.position(axis.window.end) : null;
  const wLabelLeft = axis.window ? Math.min(82, Math.max((wStart + wEnd) / 2, nowLeft + 20)) : 0;
  return `<article class="lane-row ${person === 'me' ? 'self' : 'partner'}">
        <div class="lane-head"><strong>${esc(moment.name)} · ${esc(moment.profile.city)}</strong><time class="now-time">${moment.local.time}</time></div>
        <div class="lane-labels"><span class="lane-label-area">${axis.window ? `<time class="window-time" style="left:${wLabelLeft.toFixed(1)}%">${parts(axis.window.start, zone).time}–${parts(axis.window.end, zone).time}</time>` : ''}</span></div>
        <div class="lane"><span class="lane-track"><span class="lane-now ${period.kind}" style="left:${nowLeft.toFixed(1)}%"></span>${axis.window ? `<span class="lane-window" style="left:${wStart.toFixed(1)}%;width:${Math.max(wEnd - wStart, 7).toFixed(1)}%"></span>` : ''}</span></div>
        <p class="lane-state">${esc(moment.date)} · ${esc(period.label)} · ${esc(statusText(person, moment))}</p>
      </article>`;
}
function laneBoard() {
  const axis = axisBounds();
  const missing = dataScenario() === 'missing';
  const lead = axis.window
    ? axis.window.start <= now()
      ? '现在可以联系'
      : parts(axis.window.start, PEOPLE.me.zone).date !== parts(now(), PEOPLE.me.zone).date
        ? `${localDayRelation(axis.window.start, 'me')}可以联系`
        : `${minutesText(axis.window.start - now()).replace(/\s+/g, '')}后开始`
    : missing ? '等待彼此的节奏' : '暂无交集';
  const readings = axis.window
    ? `<p class="answer-readings">${['partner', 'me'].map(person => `<span><small>${esc(PEOPLE[person].city)}</small><time>${parts(axis.window.start, PEOPLE[person].zone).time}–${parts(axis.window.end, PEOPLE[person].zone).time}</time></span>`).join('')}</p>`
    : `<p class="answer-readings answer-readings-empty">${missing ? `${esc(PEOPLE.partner.name)}尚未设置作息与联系偏好，未知时间不作推荐。` : '按目前的联系偏好，接下来七天没有共同窗口。'}</p>`;
  const scale = `<div class="axis-scale"><span>${parts(axis.start, PEOPLE.partner.zone).time} / ${parts(axis.start, PEOPLE.me.zone).time}</span><span>${parts(axis.end, PEOPLE.partner.zone).time} / ${parts(axis.end, PEOPLE.me.zone).time}</span></div>`;
  return `<section class="lane-board" aria-label="此刻与下一段共同时间">
      ${laneRow('partner', axis)}${laneRow('me', axis)}${scale}
      <div class="window-answer"><span class="answer-label">下一段共同时间${axis.window ? ` · 持续${esc(minutesText(axis.window.end - axis.window.start).replace(/\s+/g, ''))}` : ''}</span><p class="answer-lead">${esc(lead)}</p>${readings}</div>
    </section>`;
}
function soloBoard() {
  return `<div class="board-solo"><h2 class="board-solo-title">先过好自己的一天</h2><p class="board-solo-text">配对并填写联系偏好后，这里会把两个人放到同一条时间轴上。</p><button type="button" class="board-solo-action" data-action="start-setup">继续邀请伴侣 ${icon('arrow')}</button></div>`;
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
  return `<section class="note-block" aria-label="伴侣留言"><div class="note-meta"><span class="note-mark ${periodOf('partner').kind}" aria-hidden="true">${esc(PEOPLE.partner.short)}</span><strong class="note-author">${name}</strong><time class="note-time" datetime="${new Date(initialNote.time).toISOString()}">${relativeNoteTime(initialNote.time)}</time></div><p class="note-text">${esc(initialNote.text).replace('，', '，<br>')}</p><button type="button" class="reply-field" data-action="edit-note"><span class="reply-hint">回${name}一句</span></button></section>`;
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
function renderHome() {
  $('#home-view').innerHTML = `<div class="home-stack">${partnerJoined() ? laneBoard() + boardFoot() : soloBoard()}${noteSection()}</div>`;
}
