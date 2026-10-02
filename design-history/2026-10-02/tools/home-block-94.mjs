function personMoment(person) {
  const profile = PEOPLE[person];
  const local = parts(now(), profile.zone);
  const weekday = new Intl.DateTimeFormat('zh-CN', { timeZone: profile.zone, weekday: 'short' }).format(now());
  const activity = routineAt(person, now(), state.scenario, overrides());
  const usual = ({ '早餐': '吃早餐', '晚餐': '吃晚餐' })[activity.label] || activity.label;
  const stateText = activity.source === '按通常作息'
    ? (activity.label === '通勤' ? '通常在通勤路上' : `平时在${usual}`)
    : activity.source === '未知' ? '此刻安排未分享' : `此刻 · ${activity.label}`;
  const next = person === 'partner' && activity.source === '按通常作息' ? nextStage('partner') : null;
  return {
    person, profile, local, activity, stateText, next,
    period: stage(now(), person),
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
function boardRow(person, window) {
  const moment = personMoment(person);
  const detail = [moment.stateText];
  if (moment.next) detail.push(`${moment.next.time} 开始${moment.next.label}`);
  const zone = moment.profile.zone;
  const windowCell = window
    ? `<time class="window-time">${parts(window.start, zone).time}–${parts(window.end, zone).time}</time>`
    : '<span class="window-time window-time-empty">—:—</span>';
  return `<article class="board-row ${person === 'me' ? 'self' : 'partner'}">
        <div class="row-identity"><strong>${esc(moment.name)}</strong><span class="row-place">${icon(moment.period.icon)}${esc(moment.profile.city)} · ${moment.date}</span></div>
        <time class="now-time">${moment.local.time}</time>
        <span class="row-bridge" aria-hidden="true"><i></i>${icon('arrow')}</span>
        ${windowCell}
        <p class="row-state">${esc(detail.join(' · '))}</p>
      </article>`;
}
function boardSection() {
  if (!partnerJoined()) return `<section class="moment-board board-solo"><h2 class="board-solo-title">先过好自己的一天</h2><p class="board-solo-text">配对并填写联系偏好后，这里会并排显示双方此刻与下一段共同时间。</p><button type="button" class="board-solo-action" data-action="start-setup">继续邀请伴侣 ${icon('arrow')}</button></section>`;
  const window = upcomingWindow();
  const missing = dataScenario() === 'missing';
  const when = window
    ? window.start <= now()
      ? '现在可以联系'
      : parts(window.start, PEOPLE.me.zone).date !== parts(now(), PEOPLE.me.zone).date
        ? `${localDayRelation(window.start, 'me')}可以联系`
        : `${minutesText(window.start - now()).replace(/\s+/g, '')}后开始`
    : missing ? '等待彼此节奏' : '暂无交集';
  const howLong = window ? `持续${minutesText(window.end - window.start).replace(/\s+/g, '')}` : missing ? '未知时间不作推荐' : '接下来七天没有共同窗口';
  const head = `<div class="board-head"><span class="head-now">此刻</span><span class="head-window"><strong>下一段共同时间</strong><span>${esc(when)} · ${esc(howLong)}</span></span></div>`;
  const rows = ['partner', 'me'].map(person => boardRow(person, window)).join('');
  const foot = window
    ? `<span class="foot-note">按双方联系偏好估计 · 尚未约定</span><button type="button" class="foot-action" data-action="window-detail">${state.reminder === window.start ? '提醒已设' : '设置提醒'} ${icon('arrow')}</button>`
    : missing
      ? `<span class="foot-note">${esc(PEOPLE.partner.name)}尚未设置作息与联系偏好。</span><button type="button" class="foot-action" data-action="edit-note">先留一句关心 ${icon('arrow')}</button>`
      : `<span class="foot-note">按目前的联系偏好，暂时没有共同窗口。</span><button type="button" class="foot-action" data-action="open-rhythm">设置我的节奏 ${icon('arrow')}</button>`;
  return `<section class="moment-board" aria-label="此刻与下一段共同时间">${head}<div class="board-rows">${rows}</div><div class="board-foot">${foot}</div></section>`;
}
function noteMarkup(note, person) {
  if (!note) return `<p class="note-empty">留一句今天想告诉${esc(PEOPLE.partner.name)}的话。</p>`;
  return `<div class="note-head"><span class="avatar">${esc(PEOPLE[person].short)}</span><span>${person === 'me' ? '我留给你' : `${esc(PEOPLE[person].name)}留给我`}</span><time>${dateText(note.time, PEOPLE.me.zone)} ${parts(note.time, PEOPLE.me.zone).time}</time></div><p class="note-body">${esc(note.text)}</p>`;
}
function noteSection() {
  if (!partnerJoined()) return '';
  const name = esc(PEOPLE.partner.name);
  return `<section class="note-block" aria-label="伴侣留言"><div class="note-meta"><strong class="note-author">${name}</strong><time class="note-time" datetime="${new Date(initialNote.time).toISOString()}">${relativeNoteTime(initialNote.time)}</time></div><p class="note-text">${esc(initialNote.text)}</p><button type="button" class="reply-field" data-action="edit-note"><span class="reply-hint">回${name}一句</span><span class="reply-arrow" aria-hidden="true">${icon('arrow')}</span></button></section>`;
}
function renderHome() {
  $('#home-view').innerHTML = `<div class="home-stack">${boardSection()}${noteSection()}</div>`;
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
