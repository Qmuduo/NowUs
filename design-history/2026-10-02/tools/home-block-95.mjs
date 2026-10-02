function nowRow(person) {
  const moment = personMoment(person);
  const detail = [moment.stateText];
  if (moment.next) detail.push(`${moment.next.time} 开始${moment.next.label}`);
  return `<article class="now-row ${person === 'me' ? 'self' : 'partner'}">
        <div class="row-identity"><strong>${esc(moment.name)}</strong><span class="row-place">${esc(moment.profile.city)} · ${moment.date}</span></div>
        <time class="now-time">${moment.local.time}</time>
        <p class="row-state">${esc(detail.join(' · '))}</p>
      </article>`;
}
function nowStrip() {
  return `<div class="now-strip"><span class="strip-label">此刻</span>${['partner', 'me'].map(nowRow).join('')}</div>`;
}
function soloBoard() {
  return `<div class="now-strip board-solo"><h2 class="board-solo-title">先过好自己的一天</h2><p class="board-solo-text">配对并填写联系偏好后，这里会显示双方此刻与下一段共同时间。</p><button type="button" class="board-solo-action" data-action="start-setup">继续邀请伴侣 ${icon('arrow')}</button></div>`;
}
function windowBand() {
  const window = upcomingWindow();
  const missing = dataScenario() === 'missing';
  if (!window) {
    const title = missing ? `${esc(PEOPLE.partner.name)}尚未设置作息与联系偏好，未知时间不作推荐。` : '按目前的联系偏好，没有找到共同窗口。';
    const action = missing ? 'edit-note' : 'open-rhythm';
    const label = missing ? '先留一句关心' : '设置我的节奏';
    return `<section class="window-band band-empty" aria-label="下一段共同时间"><div class="band-head"><span class="band-label">下一段共同时间</span></div><p class="band-empty-text">${title}</p><div class="band-foot"><span class="band-source">${missing ? '等待彼此的节奏' : '接下来七天没有交集'}</span><button type="button" class="band-action" data-action="${action}">${label} ${icon('arrow')}</button></div></section>`;
  }
  const when = window.start <= now()
    ? '现在可以联系'
    : parts(window.start, PEOPLE.me.zone).date !== parts(now(), PEOPLE.me.zone).date
      ? `${localDayRelation(window.start, 'me')}可以联系`
      : `${minutesText(window.start - now()).replace(/\s+/g, '')}后开始`;
  const readings = ['partner', 'me'].map(person => {
    const zone = PEOPLE[person].zone;
    return `<div class="reading"><span class="reading-city">${esc(PEOPLE[person].city)}</span><time class="reading-time">${parts(window.start, zone).time}–${parts(window.end, zone).time}</time></div>`;
  }).join('');
  return `<section class="window-band" aria-label="下一段共同时间"><div class="band-head"><span class="band-label">下一段共同时间</span><span class="band-when">${esc(when)} · ${esc(minutesText(window.end - window.start).replace(/\s+/g, ''))}</span></div><div class="band-readings">${readings}</div><div class="band-foot"><span class="band-source">按双方联系偏好估计 · 尚未约定</span><button type="button" class="band-action" data-action="window-detail">${state.reminder === window.start ? '提醒已设' : '设置提醒'} ${icon('arrow')}</button></div></section>`;
}
function renderHome() {
  $('#home-view').innerHTML = `<div class="home-stack">${partnerJoined() ? nowStrip() + windowBand() : soloBoard()}${noteSection()}</div>`;
}
