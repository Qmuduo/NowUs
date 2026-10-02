import { CITIES, validateProfile, createConnection, acceptInvite, inviteError, validConnection } from './pairing-model.mjs?v=3';
import { DEFAULT_RHYTHM } from './model.mjs?v=3';
import { mountRhythm } from './rhythm.mjs?v=3';

const storageKey = 'nowus.prototype.connection.v1';
const esc = value => String(value).replace(/[&<>"']/g, char => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[char]));
export function loadConnection() {
  try {const record = JSON.parse(localStorage.getItem(storageKey) || 'null'); return validConnection(record) ? record : null;} catch {return null;}
}
export function saveConnection(record) {
  try {localStorage.setItem(storageKey,JSON.stringify(record)); return true;} catch {return false;}
}

export function mountOnboarding(container, existing, {onChange,onFinish,notify}, seedRhythm = null) {
  let record = existing ? structuredClone(existing) : null;
  let step = record ? record.status === 'paired' ? 4 : 3 : 1;
  let receiving = false;
  let restarting = false;
  let draft = record?.me || {name:'',cityId:'beijing'};
  let rhythm = record?.rhythm || seedRhythm || DEFAULT_RHYTHM;
  const $ = selector => container.querySelector(selector);
  const cityOptions = selected => Object.entries(CITIES).map(([id,city]) => `<option value="${id}" ${selected === id ? 'selected' : ''}>${city.label}</option>`).join('');
  const profileFields = (value, receiver = false) => `<label class="setup-label">${receiver ? '对方的演示昵称' : '你的昵称'}<input name="nickname" type="text" value="${esc(value.name)}" placeholder="希望对方怎样叫你" autocomplete="off"></label><label class="setup-label">${receiver ? '对方选择的城市' : '你所在的城市'}<select name="city">${cityOptions(value.cityId)}</select></label>`;
  function commit(value) {
    record = value;
    if (!saveConnection(record)) notify('本次设置已更新，浏览器未允许保存');
    onChange(record);
  }
  function error(text) {$('#setup-error').textContent = text; $('#setup-error').hidden = !text;}
  function shell(body) {
    container.innerHTML = `<div class="setup-top"><button type="button" class="text-button" data-setup-back ${step === 1 || step === 4 ? 'hidden' : ''}>‹ 上一步</button><span>首次使用 · ${step}/4</span></div><div class="setup-progress" aria-label="第 ${step} 步，共 4 步">${[1,2,3,4].map(n => `<span class="${n <= step ? 'done' : ''}"></span>`).join('')}</div>${body}`;
    $('[data-setup-back]')?.addEventListener('click', () => {if (receiving) receiving = false; else step--; render();});
  }
  function render() {
    if (step === 1) {
      shell(`<div class="setup-intro"><span class="setup-kicker">先从你的一天开始</span><h1 class="screen-title">你在哪里，<br>我们就从哪里开始。</h1><p>选择城市，让彼此的时间有一个真实的坐标。</p></div><form id="profile-form" novalidate>${profileFields(draft)}<p class="setup-help">无需定位。旅行时可以再调整城市。</p><p id="setup-error" class="field-error" role="alert" hidden></p><div class="setup-actions"><button type="submit" class="primary-button">继续，设置我的节奏</button></div></form><p class="prototype-note">首次使用演示，数据只保存在这个浏览器。</p>`);
      $('#profile-form').addEventListener('submit',event => {
        event.preventDefault();
        const profile = {name:$('#profile-form').elements.nickname.value,cityId:$('#profile-form').elements.city.value};
        const errors = validateProfile(profile); if (errors.length) {error(errors.join(' ')); return;}
        draft = {...profile,name:profile.name.trim()}; step = 2; render();
      });
    } else if (step === 2) {
      shell('<div id="setup-rhythm"></div>');
      mountRhythm($('#setup-rhythm'),rhythm,value => {rhythm = value; const next = createConnection(draft,rhythm); if (record?.status === 'waiting') next.note = record.note; commit(next); step = 3; render();},{city:CITIES[draft.cityId].label,saveLabel:'保存节奏，邀请伴侣',saveHelp:'先保存自己的安排，等对方加入后再对照。'});
    } else if (step === 3 && receiving) {
      const issue = inviteError(record,record.invite.code);
      shell(`<div class="setup-intro"><span class="setup-kicker">邀请接收页 · 本地演示</span><h1 class="screen-title">${esc(record.me.name)}想和你<br>看见彼此的一天。</h1><p>来自 ${CITIES[record.me.cityId].label} 的邀请。接受后双方可以查看昵称、城市、作息、主动状态与当前留言。</p></div><form id="receiver-form" novalidate>${profileFields({name:'阿远',cityId:'new-york'},true)}<label class="setup-label">演示邀请代码<input name="code" type="text" value="${record.invite.code}" autocomplete="off"></label><p class="setup-help">可以暂停分享或解除配对。未知的作息会保持未知。</p><p id="setup-error" class="field-error" role="alert" ${issue ? '' : 'hidden'}>${esc(issue)}</p><div class="setup-actions"><button type="submit" class="primary-button" ${issue ? 'disabled' : ''}>模拟对方接受邀请</button><button type="button" class="secondary-button" id="decline-invite">暂不接受，返回邀请页</button></div></form>`);
      $('#decline-invite').addEventListener('click',()=>{receiving=false;render();});
      $('#receiver-form').addEventListener('submit',event => {
        event.preventDefault();
        const form = $('#receiver-form');
        try {commit(acceptInvite(record,{name:form.elements.nickname.value,cityId:form.elements.city.value},form.elements.code.value)); step = 4; receiving = false; render();} catch (failure) {error(failure.message);}
      });
    } else if (step === 3) {
      const issue = inviteError(record,record.invite.code);
      shell(`<div class="setup-intro"><span class="setup-kicker">你的一天，已经安顿好了</span><h1 class="screen-title">邀请一个人，<br>把两种节奏放在一起。</h1><p>可以先进入自己的首页，等对方方便时再加入。</p></div><div class="invite-card"><div class="invite-person"><span class="avatar">${esc(Array.from(record.me.name)[0])}</span><strong>${esc(record.me.name)}</strong><span>${CITIES[record.me.cityId].label}</span></div><p class="invite-state">${issue ? esc(issue) : '演示邀请有效 · 24 小时'}</p><div class="invite-code">${record.invite.code}</div><p>本地演示代码，不会生成真实链接或发送邀请。</p></div><div class="setup-actions"><button type="button" class="primary-button" id="preview-receiver" ${issue ? 'disabled' : ''}>查看邀请接收页</button><button type="button" class="secondary-button" id="later-invite">稍后邀请，进入我的首页</button><button type="button" class="text-button" id="renew-invite">${issue ? '重新建立演示邀请' : '撤销这份演示邀请'}</button></div><p class="prototype-note">等待加入时，不会显示虚构的对方信息。</p>`);
      $('#preview-receiver').addEventListener('click',()=>{receiving=true;render();});
      $('#later-invite').addEventListener('click',()=>onFinish(record));
      $('#renew-invite').addEventListener('click',()=>{commit({...record,invite:issue ? {...record.invite,revoked:false,expiresAt:Date.now()+86400000} : {...record.invite,revoked:true}});render();});
    } else {
      shell(`<div class="setup-intro"><span class="setup-kicker">演示配对成功</span><h1 class="screen-title">两种生活，<br>开始有了交点。</h1><p>${esc(record.partner.name)}已加入。${record.partner.ready ? '现在可以按示例作息对照双方的一天。' : '对方还没填写作息，活动与共同时间会保持未知。'}</p></div><div class="paired-summary">${[record.me,record.partner].map((profile,index) => `<div class="paired-person ${index ? 'partner' : 'me'}"><span class="avatar">${esc(Array.from(profile.name)[0])}</span><strong>${esc(profile.name)}</strong><span>${CITIES[profile.cityId].label}</span><small>${index ? record.partner.ready ? '已填示例作息' : '作息尚未填写' : '自己的节奏已保存'}</small></div>`).join('')}</div><div class="setup-actions"><button type="button" class="primary-button" id="finish-setup">进入我们的首页</button>${!record.partner.ready ? '<button type="button" class="secondary-button" id="simulate-rhythm">模拟对方填好示例作息</button>' : ''}</div><p class="prototype-note">这是同一浏览器中的演示配对，没有建立真实账号或双方同步。</p>`);
      $('.setup-actions').insertAdjacentHTML('beforeend','<button type="button" class="text-button" id="restart-setup">重新体验首次使用</button>');
      $('#restart-setup').addEventListener('click',()=>{restarting=true;record=null;draft={name:'',cityId:'beijing'};rhythm=DEFAULT_RHYTHM;step=1;render();});
      $('#finish-setup').addEventListener('click',()=>onFinish(record));
      $('#simulate-rhythm')?.addEventListener('click',()=>{commit({...record,partner:{...record.partner,ready:true}});render();});
    }
    if (restarting && step === 1) $('.prototype-note').textContent = '保存新节奏后会替换本地演示配对。退出则保留已有记录。';
    container.closest('main').scrollTop = 0;
  }
  render();
}
