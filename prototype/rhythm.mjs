import { DEFAULT_RHYTHM, validateRhythm } from './model.mjs?v=2';

const storageKey = 'nowus.prototype.rhythm.v1';
export function loadRhythm() {
  try {
    const saved = JSON.parse(localStorage.getItem(storageKey) || 'null');
    if (saved && ['weekday','rest'].every(key => validateRhythm(saved[key]).length === 0)) return saved;
  } catch { /* A corrupt or unavailable local store does not block the prototype. */ }
  return null;
}
export function saveRhythm(value) {
  try { localStorage.setItem(storageKey, JSON.stringify(value)); return true; } catch { return false; }
}

export function mountRhythm(container, saved, onSave) {
  const drafts = structuredClone(saved || DEFAULT_RHYTHM);
  let active = 'weekday';
  const $ = selector => container.querySelector(selector);
  const label = key => key === 'weekday' ? '工作日' : '休息日';
  const esc = value => String(value).replace(/[&<>"']/g, char => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[char]));
  const field = (name, title, value) => `<label class="time-field">${title}<input type="time" name="${name}" value="${esc(value)}" required></label>`;
  function read() {
    const form = $('#rhythm-form');
    const draft = drafts[active];
    for (const name of ['sleepStart','sleepEnd','activity','activityStart','activityEnd','contactStart','contactEnd']) draft[name] = form.elements.namedItem(name).value;
    draft.contactKnown = form.elements.namedItem('contactKnown').value === 'yes';
    return draft;
  }
  function refresh() {
    const draft = drafts[active];
    $('#activity-times').hidden = draft.activity === 'none';
    $('#contact-times').hidden = !draft.contactKnown;
    for (const input of $('#activity-times').querySelectorAll('input')) input.disabled = draft.activity === 'none';
    for (const input of $('#contact-times').querySelectorAll('input')) input.disabled = !draft.contactKnown;
    $('#rhythm-summary').textContent = `${label(active)} · 睡眠 ${draft.sleepStart}–${draft.sleepEnd} · ${draft.contactKnown ? `可联系 ${draft.contactStart}–${draft.contactEnd}` : '联系时间尚未设置'}`;
  }
  function showErrors(errors) {
    $('#rhythm-error').textContent = errors.join(' ');
    $('#rhythm-error').hidden = !errors.length;
  }
  function render() {
    const d = drafts[active];
    container.innerHTML = `<h1 class="screen-title">我的节奏</h1><p class="screen-subtitle">先填三件事，其他生活细节慢慢补充。</p><div class="rhythm-tabs" role="group" aria-label="作息模板">${['weekday','rest'].map(key => `<button type="button" data-rhythm-template="${key}" aria-pressed="${active === key}">${label(key)}</button>`).join('')}</div><p class="rhythm-context">按北京当地时间填写。工作日为周一至周五，休息日为周六、周日。</p><form id="rhythm-form" novalidate><section class="rhythm-section"><h2><span>1</span>睡眠</h2><p>可以跨过午夜，例如 23:00 到 07:00。</p><div class="time-fields">${field('sleepStart','入睡',d.sleepStart)}${field('sleepEnd','起床',d.sleepEnd)}</div></section><section class="rhythm-section"><h2><span>2</span>上班或上课</h2><label class="select-label">主要安排<select name="activity"><option value="work" ${d.activity === 'work' ? 'selected' : ''}>上班</option><option value="study" ${d.activity === 'study' ? 'selected' : ''}>上课</option><option value="none" ${d.activity === 'none' ? 'selected' : ''}>没有固定安排</option></select></label><div class="time-fields" id="activity-times">${field('activityStart','开始',d.activityStart)}${field('activityEnd','结束',d.activityEnd)}</div><p>只记录固定时段；其他时间显示为未细分安排。</p></section><section class="rhythm-section"><h2><span>3</span>愿意联系的时间</h2><p>独立于活动设置，不代表必须回复。</p><label class="select-label">联系偏好<select name="contactKnown"><option value="yes" ${d.contactKnown ? 'selected' : ''}>设置一段通常可联系时间</option><option value="no" ${!d.contactKnown ? 'selected' : ''}>暂不设置，保持未知</option></select></label><div class="time-fields" id="contact-times">${field('contactStart','开始',d.contactStart)}${field('contactEnd','结束',d.contactEnd)}</div><p>可跨午夜；请避开睡眠。上班／上课期间的设置会单独说明。</p></section><div class="rhythm-save"><p id="rhythm-summary" aria-live="polite"></p><p id="rhythm-error" class="field-error" role="alert" hidden></p><button type="submit" class="primary-button">保存我的节奏</button><p class="save-help">同时保存两套模板，替换自己的通常作息。</p></div></form>`;
    refresh();
    for (const button of container.querySelectorAll('[data-rhythm-template]')) button.addEventListener('click', () => {read(); active = button.dataset.rhythmTemplate; render();});
    $('#rhythm-form').addEventListener('input', () => {read(); refresh();});
    $('#rhythm-form').addEventListener('change', () => {read(); refresh(); showErrors(validateRhythm(drafts[active]));});
    $('#rhythm-form').addEventListener('submit', event => {
      event.preventDefault(); read();
      for (const key of ['weekday','rest']) {
        const errors = validateRhythm(drafts[key]);
        if (errors.length) {if (active !== key) {active = key; render();} showErrors(errors); $('#rhythm-error').scrollIntoView({block:'center',behavior:'auto'}); return;}
      }
      onSave(structuredClone(drafts));
    });
  }
  render();
}
