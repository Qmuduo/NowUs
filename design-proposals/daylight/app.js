const $ = (s, root = document) => root.querySelector(s);
const icons = {
  sun: '<circle cx="12" cy="12" r="4"/><path d="M12 2v2m0 16v2M2 12h2m16 0h2M5 5l1.5 1.5m11 11L19 19M5 19l1.5-1.5m11-11L19 5"/>',
  moon: '<path d="M20 15A8.5 8.5 0 0 1 9 4a8.5 8.5 0 1 0 11 11Z"/>',
  heart: '<path d="M20.5 5.5a5 5 0 0 0-7 0L12 7l-1.5-1.5a5 5 0 0 0-7 7L12 21l8.5-8.5a5 5 0 0 0 0-7Z"/>',
  day: '<rect x="4" y="4" width="16" height="17" rx="3"/><path d="M8 2v4m8-4v4M4 10h16m-10 4v3m4-3v3"/>',
  note: '<path d="M5 4h14a2 2 0 0 1 2 2v10a2 2 0 0 1-2 2h-8l-5 4v-4H5a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2Z"/><path d="M7 9h10M7 13h6"/>',
  settings: '<circle cx="12" cy="8" r="4"/><path d="M4 21v-2a8 8 0 0 1 16 0v2"/>',
  arrow: '<path d="m9 5 7 7-7 7"/>', back: '<path d="m15 5-7 7 7 7"/>',
  close: '<path d="m6 6 12 12M6 18 18 6"/>',
  clock: '<circle cx="12" cy="12" r="9"/><path d="M12 7v5l3 2"/>',
  link: '<path d="m10 13 4-4m-6 7-1 1a4 4 0 0 1-6-6l5-5a4 4 0 0 1 6 0m0 12a4 4 0 0 0 6 0l5-5a4 4 0 0 0-6-6l-1 1"/>',
  work: '<rect x="3" y="7" width="18" height="14" rx="3"/><path d="M8 7V4h8v3M3 12a22 22 0 0 0 18 0m-9 0v3"/>',
  cup: '<path d="M4 9h12v7a5 5 0 0 1-5 5H9a5 5 0 0 1-5-5Zm12 1h2a3 3 0 0 1 0 6h-2M7 3v3m5-3v3"/>',
  check: '<path d="m5 12 4 4L19 6"/>', edit: '<path d="m15 4 5 5M4 20l5-1L21 7l-5-5L4 14Z"/>',
  shield: '<path d="m12 2 8 4v6c0 5-8 10-8 10S4 17 4 12V6Z"/><path d="m8 12 3 3 5-6"/>',
  pause: '<path d="M8 5v14M16 5v14"/>', plus: '<path d="M12 5v14M5 12h14"/>'
};
icons.now='<path d="M4 17V10a4 4 0 0 1 8 0v4a4 4 0 0 0 8 0V7"/><path d="M12 10v4"/>';
icons.note='<path d="M5 3h14a2 2 0 0 1 2 2v11l-5 5H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2Z"/><path d="M16 21v-5h5M7 8h10M7 12h7"/>';
icons.day='<path d="M5 3v18M19 3v18M9 6h6M9 12h6M9 18h6"/><rect x="3" y="7" width="4" height="6" rx="2"/><rect x="17" y="11" width="4" height="6" rx="2"/>';
icons.link='<path d="m9 15 6-6m-7 4-2 2a3 3 0 0 0 4 4l3-3m-2-8 3-3a3 3 0 0 1 4 4l-2 2"/>';
const icon = name => `<svg viewBox="0 0 24 24" aria-hidden="true">${icons[name] || icons.clock}</svg>`;
const escape = text => String(text).replace(/[&<>"']/g, char => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[char]));
const state = {paired:true,statusUntil:0,draft:null,lastDeleted:null,page:'home', scene:'normal', note:'等你下课，想听听你今天的小事。', status:'normal', duration:'1 小时', template:'weekday', scope:'near', paused:false, invited:false, dayOffset:0, rhythm:{weekday:[['睡眠','23:30','07:30','moon'],['工作','09:00','18:00','work'],['晚餐与自己的时间','18:00','23:30','cup']],rest:[['睡眠','00:00','09:00','moon'],['自己的时间','09:00','23:30','sun']]}, contacts:{weekday:['21:00','23:00'],rest:['21:00','23:00']}};
const openedAt=Date.now();
const demoNow=()=>Date.parse(state.scene==='midnight'?'2026-10-03T16:30:00Z':'2026-10-03T12:30:00Z')+(Date.now()-openedAt);
let view;
try{const d=JSON.parse(localStorage.getItem('nowus-daylight-note-draft-v4'));if(d&&['new','edit'].includes(d.mode)&&typeof d.text==='string')state.draft=d}catch{}
function storeDraft(){try{if(state.draft)localStorage.setItem('nowus-daylight-note-draft-v4',JSON.stringify(state.draft));else localStorage.removeItem('nowus-daylight-note-draft-v4')}catch{}}
const brandLogo=()=>'<img class="brand-lockup" src="brand/logo.svg" alt="NowUs">';
function rangeLabel(start,end,who){const a=DaylightSchedule.local(start,who),b=DaylightSchedule.local(end,who);return `${a.time}–${a.date===b.date?b.time:b.date.slice(5).replace('-','/')+' '+b.time}`}
function dateLabel(date){return date.slice(5).replace('-','/')}
const annotations = {
 brand:['相接的 n 与 u，\n各自完整，也彼此相连。','夜蓝代表一方的夜晚，晨光代表另一方的白天。中间共用的一笔，是两个人愿意留给彼此的时间。'],
 home:['一眼看懂，\n两个人的此刻。','两地时钟压缩为生活背景，把对方的便签提前呈现。纸张与胶带让一句话有留下来的感觉；联系窗口紧随其后。'],
 day:['不是两张日历，\n是同一个时间。','活动高度严格按时长计算，持续活动不再被联系窗口切碎。绿色细条单独表达联系意愿；双列共享一条时间刻度。'],
 note:['一句话，\n也能让人靠近。','胶带、淡横线、折角与手写署名组成完整的便签语言。对方的便签是主角，自己的便签安静地放在下方。'],
 rhythm:['把日常设置好，\n然后回到生活里。','工作日和休息日分别设置。活动安排与愿意联系的时间独立编辑，临时变化在首页一键覆盖。'],
 pair:['把彼此的时间，\n放在一起。','邀请先说明会分享什么，再展示邀请代码。接受配对是独立步骤，未知信息不会用示例人物补齐。'],
 settings:['靠近，也保留\n自己的空间。','资料、节奏和分享控制集中在一个位置。暂停分享有明确状态，解除配对需要二次确认。']
};
const button = (action, text, cls='primary') => `<button class="${cls}" data-action="${action}">${text}</button>`;
const header = (title, sub=false) => `<header class="topbar">${sub ? `<div class="back-title"><button class="icon-button" data-page="${state.page==='rhythm'?'settings':'home'}" aria-label="返回">${icon('back')}</button><h2>${title}</h2></div>` : `<h1 class="page-title">${title}</h1>`}${!sub?`<button class="icon-button" data-page="settings" aria-label="打开我的设置">${icon('settings')}</button>`:''}</header>`;
function windowCard(){
 if(!view.available)return `<section class="window-card"><div class="window-heading">${icon('clock')} ${state.paused?'分享已暂停':'等待彼此的节奏'}</div><h3 class="window-title">${state.paused?'按照自己的节奏来':'还不知道什么时候合适'}</h3><p class="subtle">${state.paused?'恢复分享后，再查看共同联系时间。':'阿远还没有填写作息，暂时不能估计共同时间。'}</p></section>`;
 const w=view.window;
 if(!w)return `<section class="window-card"><div class="window-heading">${icon('clock')} 未来七天</div><h3 class="window-title">暂时没有重合的联系时间</h3><p class="subtle">可以调整自己的联系偏好，也可以先留一句话。</p><button class="text-action" data-page="rhythm">调整我的联系偏好 →</button></section>`;
 const mine=DaylightSchedule.local(w.start,'me'),theirs=DaylightSchedule.local(w.start,'partner');
 const minutes=Math.round((w.start-view.now)/60000),duration=Math.round((w.end-w.start)/60000),today=mine.date===view.people.me.date;
 return `<section class="window-card"><div class="window-heading">${icon('link')} ${minutes===0?'现在 · 双方愿意联系':today?minutes+' 分钟后':dateLabel(mine.date)+' · 下一段共同时间'}</div><h3 class="window-title">有 ${duration} 分钟，可以慢慢聊</h3><div class="window-times"><div><span>你 · 北京${today?'':' · '+dateLabel(mine.date)}</span><strong>${rangeLabel(w.start,w.end,'me')}</strong></div><div><span>阿远 · 纽约${today&&theirs.date===mine.date?'':' · '+dateLabel(theirs.date)}</span><strong>${rangeLabel(w.start,w.end,'partner')}</strong></div></div><div class="window-foot"><p class="tiny">按联系偏好估计，尚未约定</p><button class="window-detail" data-action="window-detail" aria-label="查看共同时间详情">${icon('arrow')}</button></div></section>`;
}
function stickyNote(compact=false){
 const mins=Math.max(0,Math.floor((view.now-Date.parse('2026-10-03T12:10Z'))/60000));const stamp=mins<60?`${mins} 分钟前`:`${Math.floor(mins/60)} 小时前`;
 const tag=compact?'button':'article';
 return `<${tag} class="paper-note ${compact?'paper-preview':'paper-full'}" ${compact?'data-page="note" aria-label="打开阿远留给你的便签"':''}>
 <span class="paper-tape" aria-hidden="true"></span>
 <span class="paper-top"><span>TO 小满</span><span>纽约 · 08:10</span></span>
 <span class="paper-message">今天的第一杯咖啡，<br>替你也喝了一口。</span>
 <span class="paper-bottom"><span class="paper-signature">阿远 <span class="ink-heart" aria-hidden="true">♡</span></span><span class="paper-time">${stamp}${compact?icon('arrow'):''}</span></span>
 <span class="paper-corner" aria-hidden="true"></span>
 </${tag}>`;
}
function home(){
 const me=view.people.me,partner=view.people.partner,cross=me.date!==partner.date;
 const statusText=view.status==='busy'?'暂时不方便':view.status==='free'?'现在愿意联系':'我的状态';
 const top=`<header class="topbar home-topbar">${brandLogo()}<div class="home-tools"><button class="status-chip ${view.status!=='normal'?'is-set':''}" data-action="status">${icon(view.status==='busy'?'pause':'sun')}${statusText}</button><button class="icon-button" data-page="settings" aria-label="打开我的设置">${icon('settings')}</button></div></header>`;
 if(!state.paired)return `${top}<h1 class="greeting">先过好自己的一天。</h1><p class="subtle">配对后，再把彼此的日常放在一起。</p><div class="solo-clock"><span>我的北京 · ${dateLabel(me.date)}</span><strong>${me.time}</strong><p>${escape(me.activity.label)} · 按通常作息</p></div><div class="empty-state"><img src="brand/mark.svg" alt=""><h3>这里，为另一个人留着。</h3><p>对方接受邀请后，才会显示城市、时间和共享内容。</p>${button('invite-start','邀请另一半')}</div><button class="secondary" data-page="rhythm">设置我的节奏</button>`;
 const sky=(who,city,name)=>{const p=view.people[who],day=p.minute>=360&&p.minute<1080;return `<section class="sky ${day?'daylight':'night'}"><div class="orb"></div><div class="city">${city}<span>${name}</span></div><p class="time">${p.time}</p><p class="date">${dateLabel(p.date)} · ${['周日','周一','周二','周三','周四','周五','周六'][new Date(p.date+'T12:00Z').getUTCDay()]}</p><p class="activity">${escape(p.activity.label)}</p><div class="landscape"></div></section>`};
 return `${top}${state.scene==='offline'?'<div class="notice">网络未连接 · 上次更新 20:10，以下资料可能已过期。<button class="text-action" data-action="retry">重试演示连接</button></div>':''}<h1 class="greeting">${cross?'你这里，已经是明天。':'你的夜晚，他的早晨。'}</h1><p class="subtle">${state.paused?'你已暂停分享':'小满与阿远'} · 各自生活，也彼此惦记</p><div class="sky-pair">${sky('me','北京','我')}${sky('partner','纽约','阿远')}</div><div class="sky-caption"><span>北京快 12 小时</span><span>${state.scene==='busy'?'阿远主动设置 · 暂不方便':'活动按通常作息'}</span></div>${state.paused?'<div class="notice">分享已暂停，便签与共同时间暂不显示。<button class="text-action" data-page="settings">管理分享</button></div>':`<div class="paper-section">${stickyNote(true)}</div>`}${windowCard()}${view.status!=='normal'?`<p class="status-expiry">${icon('clock')} ${statusText} · 北京 ${DaylightSchedule.local(state.statusUntil,'me').time} 恢复通常偏好</p>`:''}`;
}
function day(){
 if(!state.paired)return `${header('我们的一天')}<div class="empty-state"><img src="brand/mark.svg" alt=""><h3>配对后，才能对照彼此的一天。</h3><p>自己的节奏已经保留，不会显示虚构的对方资料。</p>${button('invite-start','邀请另一半')}</div>`;
 const all=state.scope==='all',begin=all?0:Math.floor((state.focusMinute??view.people.me.minute)/60)*60,finish=all?1440:Math.min(begin+240,1440),unit=all?96:112;
 const dayStart=view.day.start,label=who=>{const a=DaylightSchedule.local(dayStart+begin*60000,who),b=DaylightSchedule.local(dayStart+(finish-1)*60000,who);return dateLabel(a.date)+(a.date!==b.date?' → '+dateLabel(b.date):'')};
 const events=who=>view.day.activities[who].filter(x=>x.end>begin&&x.start<finish).map(x=>{const a=Math.max(begin,x.start),b=Math.min(finish,x.end),height=(b-a)*unit/60,time=rangeLabel(dayStart+x.start*60000,dayStart+x.end*60000,who),contact=view.day.contacts[who].map(c=>rangeLabel(dayStart+c.start*60000,dayStart+c.end*60000,who)).join('、')||'未设置愿意联系时段';return `<button class="axis-event ${x.kind} ${height<65?'short':''}" style="top:${(a-begin)*unit/60}px;height:${height}px" data-event="${escape(x.label)}" data-time="${time}" data-contact="${contact}" aria-label="${escape(x.label)}，${time}"><strong>${escape(x.label)}</strong>${height>=65?`<small>${time}</small>`:''}</button>`}).join('');
 const rails=who=>view.day.contacts[who].filter(x=>x.end>begin&&x.start<finish).map(x=>`<div class="contact-rail" style="top:${(Math.max(begin,x.start)-begin)*unit/60}px;height:${(Math.min(finish,x.end)-Math.max(begin,x.start))*unit/60}px" aria-hidden="true"></div>`).join('');
 const ticks=Array.from({length:(finish-begin)/60+1},(_,i)=>`<span class="axis-tick" style="top:${i*unit}px">${String(begin/60+i).padStart(2,'0')}</span>`).join('');
 const requested=DaylightSchedule.local(dayStart,'me');
 return `${header('我们的一天')}<p class="subtle">同一刻，各自的生活。</p><div class="date-nav"><button class="icon-button" data-action="prev-day" aria-label="前一天">${icon('back')}</button><strong>${dateLabel(requested.date)}${state.dayOffset===0?' · 今天':''}</strong><button class="icon-button" data-action="next-day" aria-label="后一天">${icon('arrow')}</button></div><div class="segment"><button data-scope="near" class="${!all?'active':''}" aria-pressed="${!all}">附近几小时</button><button data-scope="all" class="${all?'active':''}" aria-pressed="${all}">北京的全天</button></div>${state.dayOffset!==0?'<div class="notice">按当前模板预览，不是当天实际活动记录。<button class="text-action" data-action="today">回到今天</button></div>':''}<div class="timeline-head"><div><span class="avatar me">满</span><div><strong>北京 · 我</strong><small>${label('me')}</small></div></div><div><span class="avatar">远</span><div><strong>纽约 · 阿远</strong><small>${label('partner')}</small></div></div></div><div class="day-legend"><span>左侧刻度为北京时间</span><span><i class="rail-key"></i>愿意联系</span></div>${!view.available?`<div class="notice">${state.paused?'分享已暂停，恢复后可查看双人时间轴。':'对方尚未填写作息，暂不展示双人时间轴。'}</div>`:`<div class="axis-timeline" style="--hour:${unit}px;height:${(finish-begin)*unit/60}px"><div class="axis-labels">${ticks}</div><div class="axis-column">${events('me')}${rails('me')}</div><div class="axis-column">${events('partner')}${rails('partner')}</div>${state.dayOffset===0&&view.people.me.minute>=begin&&view.people.me.minute<finish?`<div class="axis-now" style="top:${(view.people.me.minute-begin)*unit/60}px"><span>此刻</span></div>`:''}</div>`}<p class="timeline-note">活动按通常作息排列；点按查看完整时间。<br>绿色细条包含临时状态调整，不代表实时在线。</p>`;
}
function note(){return `${header('留给彼此')}<p class="subtle">把想说的话，轻轻放在这里。</p>${!state.paired?'<div class="notice">尚未配对。可以先写一张便签，配对后再共享。</div>':state.paused?'<div class="notice">分享已暂停，对方留言暂不显示。</div>':`<div class="note-stage">${stickyNote()}</div><p class="note-caption quiet-caption">${icon('heart')} 看见就好，不用急着回。</p>`}<section class="own-note own-paper"><div class="own-note-heading"><span class="tiny">我的便签 · ${state.paired?'给阿远':'尚未共享'}</span>${state.note?'<button class="icon-button" data-action="edit-note" aria-label="编辑我的便签">'+icon('edit')+'</button>':''}</div><p>${state.note?escape(state.note):'今天有什么小事，想让对方知道？'}</p>${state.note?'<button class="text-action delete-note" data-action="delete-note">删除我的便签</button>':state.lastDeleted!==null?'<button class="text-action" data-action="undo-delete">撤销删除</button>':''}</section>${button('new-note',icon('edit')+(state.note?'写一张新便签':'写下第一张便签'))}<p class="note-caption">每人保留一张 · 新便签替换旧便签 · 最多 120 字</p>`}
function rhythmOverview(key){
 const n=t=>Number(t.slice(0,2))*60+Number(t.slice(3));
 const list=state.rhythm[key];
 const sleep=list.filter(r=>r[3]==='moon').reduce((sum,r)=>sum+(n(r[2])-n(r[1])+1440)%1440,0);
 const marks=list.flatMap(r=>{const a=n(r[1]),b=n(r[2]);return(a<b?[[a,b]]:[[a,1440],[0,b]]).map(([x,y])=>`<i class="routine-mark ${r[3]}" style="left:${x/14.4}%;width:${(y-x)/14.4}%" aria-hidden="true"></i>`)}).join('');
 return `<h3>睡眠 ${Number((sleep/60).toFixed(1))} 小时 <span>· ${list.length} 段日常安排</span></h3><div class="routine-track" role="img" aria-label="24 小时日常安排；睡眠 ${Number((sleep/60).toFixed(1))} 小时">${marks}</div><div class="routine-scale"><span>00</span><span>06</span><span>12</span><span>18</span><span>24</span></div>`;
}
function rhythm(){const key=state.template; const times=state.contacts[key];return `${header('我的节奏',true)}<p class="subtle">安排好平常的一天，就不用每天填写。</p><div class="segment"><button data-template="weekday" class="${key==='weekday'?'active':''}">工作日</button><button data-template="rest" class="${key==='rest'?'active':''}">休息日</button></div><section class="rhythm-summary"><p class="subtle">北京 · ${key==='weekday'?'周一至周五':'周六与周日'}</p>${rhythmOverview(key)}</section><div class="section-title"><h3>通常的一天</h3><button data-action="add-routine">+ 添加时段</button></div>${state.rhythm[key].map((r,i)=>`<button class="routine-row" data-routine="${i}"><span class="routine-icon">${icon(r[3])}</span><span class="row-body"><strong>${escape(r[0])}</strong><small>${r[1]} – ${r[2]}${r[1]>r[2]?' · 次日':''}</small></span>${icon('arrow')}</button>`).join('')}<section class="contact-box"><div class="section-title"><h3>愿意联系的时间</h3><button data-action="contact">编辑</button></div><p>${times.join(' – ')}</p><span class="tiny">这是联系偏好，不是随时回复的承诺。</span></section><p class="tiny">临时有事？在「此刻」调整状态，到期自动恢复。</p>`}
function pair(){return `${header('邀请另一半',true)}<div class="pair-art"><span class="pair-disc">${icon('moon')}</span><span class="join">${icon('link')}</span><span class="pair-disc day">${icon('sun')}</span></div><h1 class="pair-title center">从你的此刻，<br>到你们的日常。</h1><p class="subtle center">各自拥有一天，<br>也找到属于两个人的时间。</p><section class="invite-ticket"><p class="tiny">${state.invited?'邀请已建立 · 等待对方接受':'你的专属邀请 · 演示代码'}</p><p class="invite-code">NU · 824 619</p><p class="tiny">24 小时内有效 · 仅限一人接受</p></section>${button('copy-invite',icon('link')+'复制邀请代码')}<div class="share-list"><div>${icon('check')} 分享城市、通常作息和联系偏好</div><div>${icon('check')} 分享你们各自的一条当前留言</div><div>${icon('shield')} 随时暂停分享，不需要定位</div></div><button class="secondary" data-action="accept-preview">我收到了一份邀请</button><p class="note-caption">此页为流程演示，不会建立真实配对。</p>`}
function settings(){return `${header('我的',true)}<div class="settings-profile"><span class="avatar me">满</span><div><h3>小满</h3><p class="subtle">北京 · Asia/Shanghai</p></div></div><p class="kicker">我的日常</p><div class="settings-list"><button class="setting" data-page="rhythm">${icon('day')}<div><strong>我的节奏</strong><small>工作日与休息日的通常安排</small></div>${icon('arrow')}</button><button class="setting" data-action="profile">${icon('settings')}<div><strong>昵称与城市</strong><small>手动选择，不获取实时位置</small></div>${icon('arrow')}</button></div><div class="spacer"></div><div class="section-title"><h3>我们之间</h3><span class="tiny">${state.paired?'小满 · 阿远':'尚未配对'}</span></div><button class="setting" ${state.paired?'':'disabled'} data-action="sharing" role="switch" aria-checked="${state.paired&&!state.paused}" aria-label="向对方分享日常">${icon('shield')}<div><strong>${!state.paired?'尚未配对':state.paused?'分享已暂停':'正在分享日常'}</strong><small>${state.paused?'对方暂时看不到你的共享资料':'作息、联系偏好和当前留言'}</small></div><span class="switch ${state.paused?'off':''}"></span></button><button class="setting" data-page="pair">${icon('link')}<div><strong>邀请与配对流程</strong><small>查看首次使用的设计</small></div>${icon('arrow')}</button><button class="setting" ${state.paired?'':'disabled'} data-action="disconnect">${icon('pause')}<div><strong>解除配对</strong><small>自己的节奏会保留</small></div>${icon('arrow')}</button><button class="setting" data-page="brand">${icon('now')}<div><strong>应用图标与 Logo</strong><small>查看新一版品牌设计</small></div>${icon('arrow')}</button><p class="timeline-note">你的生活由你决定。<br>这里不记录在线时长，也不推测实时行踪。</p>`}
function render(){
 view=DaylightSchedule.snapshot(state,demoNow());
 state.status=view.status;$('.statusbar>span').textContent=view.people.me.time;
 $('#app').dataset.page=state.page;
 $('#app').innerHTML=({home,day,note,rhythm,pair,settings,brand}[state.page]||home)();
 $('.tabbar').innerHTML=[['home','now','此刻'],['day','day','一天'],['note','note','留话']].map(([page,ic,label])=>`<button data-page="${page}" class="${state.page===page?'active':''}" ${state.page===page?'aria-current="page"':''}>${icon(ic)}${label}</button>`).join('');
 document.querySelectorAll('.preview-nav button').forEach(el=>{el.classList.toggle('selected',el.dataset.page===state.page);el.setAttribute('aria-pressed',el.dataset.page===state.page)});
 const [title,copy]=annotations[state.page]; $('#annotation-title').innerText=title;$('#annotation-copy').textContent=copy;
}
function navigate(page){state.page=page;closeSheet();render();$('#app').scrollTop=0;history.replaceState(null,'',`#${page}`)}
let opener=null;
function showSheet(title,content){clearTimeout(toastTimer);$('.toast').hidden=true;opener=document.activeElement;$('.sheet').innerHTML=`<div class="sheet-heading"><h2 id="sheet-title">${title}</h2><button class="icon-button" data-action="close" aria-label="关闭">${icon('close')}</button></div>${content}`;$('.sheet').hidden=false;$('.scrim').hidden=false;$('#app').inert=true;$('.tabbar').inert=true;$('.sheet button, .sheet input, .sheet textarea')?.focus()}
function closeSheet(keepDraft=true){
 const form=$('#note-form');if(form&&!$('.sheet').hidden&&keepDraft){state.draft={mode:form.dataset.mode||'edit',text:$('#note-input').value};storeDraft()}
 const wasOpen=!$('.sheet').hidden;$('.sheet').hidden=true;$('.scrim').hidden=true;$('#app').inert=false;$('.tabbar').inert=false;if(wasOpen&&opener?.isConnected)opener.focus();
}
let toastTimer;function toast(text,undo=false){$('.toast').innerHTML=`<span>${escape(text)}</span>${undo?'<button data-action="undo-delete">撤销</button>':''}`;$('.toast').hidden=false;clearTimeout(toastTimer);toastTimer=setTimeout(()=>$('.toast').hidden=true,undo?7000:3500)}
function openNote(mode){const value=state.draft?.mode===mode?state.draft.text:mode==='new'?'':state.note;showSheet(state.paired?'写给阿远的便签':'写下自己的便签',`<form id="note-form" data-mode="${mode}"><label for="note-input">${state.paired?'给阿远的留言':'我的便签'}</label><textarea class="paper-input" id="note-input" placeholder="今天有什么想分享的小事？">${escape(value)}</textarea><p class="counter"><span id="note-count">${Array.from(value).length}</span> / 120</p><p class="error" role="alert"></p><button class="primary" type="submit">贴上这张便签</button></form><p class="tiny draft-hint">草稿自动保存在此浏览器，关闭也不会丢失。贴上后替换当前便签。</p>`);$('#note-input').focus()}
const timeFields=(start,end)=>`<div class="field-pair"><div><label for="start">开始时间</label><input id="start" type="time" value="${start}" required></div><div><label for="end">结束时间</label><input id="end" type="time" value="${end}" required></div></div>`;
function editRoutine(index){const r=state.rhythm[state.template][index]||['','','','sun'];showSheet(index<0?'添加时段':'编辑时段',`<form id="routine-form" data-index="${index}"><label for="routine-name">安排名称</label><input id="routine-name" value="${escape(r[0])}" maxlength="20" required>${timeFields(r[1],r[2])}<p class="error" role="alert"></p><button class="primary" type="submit">保存时段</button>${index>=0?`<button type="button" class="text-action" data-remove="${index}">删除这个时段</button>`:''}</form>`)}
const actions={
 close:closeSheet,
 'window-detail':()=>showSheet('这段时间，为什么合适？',`<div class="window-explanation"><p>你设置了愿意联系的时间，阿远也设置了自己的偏好；这里显示两段时间重合的部分。</p><p class="subtle">这只是联系机会，还没有约定。通常活动不表示实时在线，也不代表必须回复。</p></div>${button('view-day','放到一天里看看')}`),
 'view-day':()=>{if(view.window){state.dayOffset=Math.round((Date.parse(DaylightSchedule.local(view.window.start,'me').date+'T00:00Z')-Date.parse(view.people.me.date+'T00:00Z'))/86400000);state.focusMinute=DaylightSchedule.local(view.window.start,'me').minute}state.scope='near';navigate('day')},
 'new-note':()=>openNote('new'),
 'edit-note':()=>openNote('edit'),
 'delete-note':()=>showSheet('删除这张便签？',`<p class="subtle">删除后，对方将看不到你的这张便签。你可以随时再写一张。</p>${button('confirm-delete','删除留言')}${button('close','再想想','text-action')}`),
 'confirm-delete':()=>{state.lastDeleted=state.note;state.note='';closeSheet();render();toast('便签已删除',true)},
 'undo-delete':()=>{if(state.lastDeleted!==null){state.note=state.lastDeleted;state.lastDeleted=null;render();toast('便签已恢复')}},
 status:()=>showSheet('此刻，按你的节奏',`<p class="subtle">临时状态优先于通常联系偏好。</p><div class="choice-list"><button data-status="free" class="${state.status==='free'?'chosen':''}">现在愿意联系 ${icon('sun')}</button><button data-status="busy" class="${state.status==='busy'?'chosen':''}">暂时不方便 ${icon('pause')}</button><button data-status="normal" class="${state.status==='normal'?'chosen':''}">跟随通常偏好 ${icon('clock')}</button></div><label for="duration">持续时间</label><select id="duration">${['30 分钟','1 小时','3 小时'].map(t=>`<option ${state.duration===t?'selected':''}>${t}</option>`).join('')}</select>${button('save-status','保存状态')}`),
 'save-status':()=>{state.status=$('.choice-list .chosen')?.dataset.status||'normal';state.duration=$('#duration').value;state.statusUntil=demoNow()+({'30 分钟':30,'1 小时':60,'3 小时':180}[state.duration])*60000;closeSheet();render();toast(state.status==='normal'?'已恢复通常偏好':`状态已更新 · ${state.duration}后恢复（演示）`)},
 'today':()=>{state.dayOffset=0;state.focusMinute=null;render()},
 'invite-start':()=>navigate('pair'),
 'prev-day':()=>{state.dayOffset--;render()}, 'next-day':()=>{state.dayOffset++;render()},
 'add-routine':()=>editRoutine(-1),
 contact:()=>showSheet('愿意联系的时间',`<p class="subtle">和日常安排分开设置，不会表示你必须回复。</p><form id="contact-form">${timeFields(...state.contacts[state.template])}<p class="error" role="alert"></p><button class="primary" type="submit">保存联系偏好</button></form>`),
 'copy-invite':async()=>{try{await navigator.clipboard.writeText('NU-824619');state.invited=true;render();toast('演示邀请码已复制')}catch{showSheet('复制邀请代码','<label for="copy-code">长按或选中复制</label><input id="copy-code" value="NU-824619" readonly>');$('#copy-code').select()}},
 'accept-preview':()=>showSheet('接受小满的邀请',`<p class="subtle">小满在北京，想与你分享彼此的日常。</p><div class="share-list"><div>${icon('check')} 双方共享城市、作息和联系偏好</div><div>${icon('check')} 配对后共享双方当前留言</div><div>${icon('shield')} 可随时暂停分享或解除配对</div></div>${button('accept-demo','接受邀请 · 演示')}${button('close','暂时不接受','text-action')}`),
 'accept-demo':()=>{closeSheet();state.paired=true;state.scene='unknown';$('#scene').value='unknown';navigate('home');toast('演示配对成功 · 对方作息保持未知')},
 sharing:()=>{if(state.paused){state.paused=false;render();toast('演示分享已恢复')}else showSheet('暂停分享日常？',`<p class="subtle">对方暂时看不到你的作息、联系偏好和留言。你可以随时恢复。</p>${button('pause-sharing','暂停分享')}${button('close','保持分享','text-action')}`)},
 'pause-sharing':()=>{state.paused=true;closeSheet();render();toast('演示分享已暂停')},
 disconnect:()=>showSheet('解除与阿远的配对？',`<p class="subtle">双方将停止共享资料，你自己的作息会保留。再次配对需要双方重新同意。</p>${button('confirm-disconnect','解除配对 · 演示')}${button('close','保留配对','text-action')}`),
 'confirm-disconnect':()=>{closeSheet();state.paired=false;state.paused=false;state.status='normal';state.statusUntil=0;state.invited=false;navigate('home');toast('演示配对已解除，自己的节奏已保留')},
 profile:()=>showSheet('昵称与城市',`<p class="subtle">资料页设计示例。城市手动选择，无需定位权限。</p><label for="profile-name">昵称</label><input id="profile-name" value="小满" readonly><label for="profile-city">城市与时区</label><input id="profile-city" value="北京 · Asia/Shanghai" readonly>${button('close','完成查看')}`),
 retry:()=>{state.scene='normal';$('#scene').value='normal';render();toast('演示连接已恢复')}
};
document.addEventListener('click',event=>{
 const el=event.target.closest('button'); if(!el)return;
 if(el.dataset.page){state.focusMinute=null;navigate(el.dataset.page);return}
 if(el.dataset.action){actions[el.dataset.action]?.();return}
 if(el.dataset.scope){state.scope=el.dataset.scope;render();return}
 if(el.dataset.template){state.template=el.dataset.template;render();return}
 if(el.dataset.routine!==undefined){editRoutine(+el.dataset.routine);return}
 if(el.dataset.remove!==undefined){state.rhythm[state.template].splice(+el.dataset.remove,1);closeSheet();render();toast('时段已删除（演示）');return}
 if(el.dataset.status){document.querySelectorAll('.choice-list button').forEach(x=>x.classList.toggle('chosen',x===el));return}
 if(el.dataset.event)showSheet(escape(el.dataset.event),`<p class="subtle">${el.dataset.time} · 当地时间</p><div class="own-note"><p>来源：按通常作息</p><p>当天联系偏好：${el.dataset.contact||'尚未设置'}</p></div><p class="tiny">活动安排不是实时行踪；有活动，也可以选择愿意联系。</p>${button('close','知道了')}`);
});
document.addEventListener('input',e=>{if(e.target.id==='note-input'){$('#note-count').textContent=Array.from(e.target.value).length;state.draft={mode:$('#note-form').dataset.mode,text:e.target.value};storeDraft()}});
function overlaps(a,b,c,d){const n=s=>Number(s.slice(0,2))*60+Number(s.slice(3));const inside=(x,s,e)=>s<e?x>=s&&x<e:x>=s||x<e;for(let i=0;i<1440;i++)if(inside(i,n(a),n(b))&&inside(i,n(c),n(d)))return true;return false}
document.addEventListener('submit',e=>{
 e.preventDefault();const form=e.target;const error=$('.error',form);
 if(form.id==='note-form'){const value=$('#note-input').value.trim();if(!value||Array.from(value).length>120){error.textContent='请写下 1–120 个字。';return}state.note=value;state.lastDeleted=null;state.draft=null;storeDraft();closeSheet(false);render();toast('便签已贴上（演示）');return}
 if(form.id==='routine-form'||form.id==='contact-form'){
  const start=$('#start').value,end=$('#end').value;
  if(!start||!end||start===end){error.textContent='请填写不同的开始和结束时间。';return}
  if(form.id==='contact-form'){
   const sleep=state.rhythm[state.template].find(r=>r[3]==='moon');if(sleep&&overlaps(start,end,sleep[1],sleep[2])){error.textContent='联系时间与睡眠重叠，请调整。';return}state.contacts[state.template]=[start,end];
  }else{
   const index=+form.dataset.index;const list=state.rhythm[state.template];const name=$('#routine-name').value.trim();const old=list[index];const sleep=old?.[3]==='moon'||name==='睡眠';
   if(!name){error.textContent='请填写安排名称。';return}if(!sleep&&start>end){error.textContent='除睡眠外，请设置同一天内的时段。';return}
   if(list.some((r,i)=>i!==index&&overlaps(start,end,r[1],r[2]))){error.textContent='与已有安排重叠，请调整时间。';return}
   if(sleep&&overlaps(start,end,...state.contacts[state.template])){error.textContent='睡眠与联系偏好重叠，请先调整联系偏好。';return}
   const value=[name,start,end,sleep?'moon':old?.[3]||'sun'];if(index<0)list.push(value);else list[index]=value;
  }closeSheet();render();toast('已保存，首页与时间轴已同步更新');
 }
});
$('.scrim').addEventListener('click',closeSheet);
document.addEventListener('keydown',e=>{if($('.sheet').hidden)return;if(e.key==='Escape')closeSheet();if(e.key==='Tab'){const items=[...$('.sheet').querySelectorAll('button,input,textarea,select')].filter(x=>!x.disabled);const first=items[0],last=items.at(-1);if(e.shiftKey&&document.activeElement===first){e.preventDefault();last.focus()}else if(!e.shiftKey&&document.activeElement===last){e.preventDefault();first.focus()}}});
$('#scene').addEventListener('change',e=>{state.scene=e.target.value;state.dayOffset=0;state.status='normal';state.statusUntil=0;navigate('home')});
window.addEventListener('hashchange',()=>{const page=location.hash.slice(1);if(Object.hasOwn(annotations,page))navigate(page)});
if(Object.hasOwn(annotations,location.hash.slice(1)))state.page=location.hash.slice(1);
render();

setInterval(()=>{if($('.sheet').hidden&&['home','day'].includes(state.page)){const y=$('#app').scrollTop;render();$('#app').scrollTop=y}},30000);

function brand(){return `${header('图标与 Logo',true)}<div class="brand-intro"><img class="brand-app-icon" src="brand/app-icon.svg" alt="NowUs 应用图标"><h2>各自的日常，<br>相接的我们。</h2><img class="brand-sign" src="brand/logo.svg" alt="NowUs 完整标志"></div><p class="subtle">相接的 n 与 u，一笔夜蓝，一笔晨光。中间共用的部分，留给彼此。</p><div class="brand-variants"><div><img src="brand/mark.svg" alt="彩色标记"><span>彩色标记</span></div><div><img src="brand/mark-mono.svg" alt="单色标记"><span>单色版本</span></div><div class="dark"><img src="brand/mark-reverse.svg" alt="深底标记"><span>深色背景</span></div></div><a class="primary" href="brand/index.html" target="_blank" rel="noopener">打开完整品牌展示与下载</a>`}
