/* Shared, deterministic local-demo time projection. No account or network access. */
(function(root){
 const MIN=60000, DAY=86400000;
 const zones={me:'Asia/Shanghai',partner:'America/New_York'};
 const formats=Object.fromEntries(Object.entries(zones).map(([k,timeZone])=>[k,new Intl.DateTimeFormat('en-CA',{timeZone,year:'numeric',month:'2-digit',day:'2-digit',hour:'2-digit',minute:'2-digit',hourCycle:'h23'})]));
 const cache=new Map();
 function local(t,who){
  const key=who+Math.floor(t/MIN);if(cache.has(key))return cache.get(key);
  const p=Object.fromEntries(formats[who].formatToParts(t).map(x=>[x.type,x.value]));
  const date=`${p.year}-${p.month}-${p.day}`, weekday=new Date(date+'T12:00:00Z').getUTCDay();
  const result={date,time:`${p.hour}:${p.minute}`,minute:Number(p.hour)*60+Number(p.minute),template:[0,6].includes(weekday)?'rest':'weekday'};
  if(cache.size>60000)cache.clear();cache.set(key,result);return result;
 }
 const n=t=>Number(t.slice(0,2))*60+Number(t.slice(3));
 const inside=(m,a,b)=>a<b?m>=a&&m<b:m>=a||m<b;
 const partner=[['睡眠','23:00','08:00','moon'],['早餐','08:00','09:00','cup'],['读书','09:00','11:30','sun'],['午餐','11:30','13:00','cup'],['自己的时间','13:00','18:00','sun'],['晚餐','18:00','19:00','cup'],['个人时间','19:00','23:00','sun']];
 function activity(s,t,who){
  if(who==='partner'&&(s.scene==='unknown'||s.paired===false||s.paused))return {label:'尚未分享作息',kind:'unknown'};
  const p=local(t,who), list=who==='me'?s.rhythm[p.template]:partner;
  const r=list.find(r=>inside(p.minute,n(r[1]),n(r[2])));
  return r?{label:r[0],kind:r[3]==='moon'?'sleep':r[3]==='cup'?'warm':''}:{label:'未安排',kind:'unknown'};
 }
 function willing(s,t,who,now){
  if(who==='partner'&&(s.scene==='unknown'||s.paired===false||s.paused))return false;
  if(who==='me'&&s.status!=='normal'&&s.statusUntil>now&&t>=now&&t<s.statusUntil)return s.status==='free';
  if(who==='partner'&&s.scene==='busy'&&t>=now&&t<now+3*3600000)return false;
  if(who==='partner'&&s.scene==='none')return false;
  const p=local(t,who), range=who==='me'?s.contacts[p.template]:['09:00','09:30'];
  return range&&inside(p.minute,n(range[0]),n(range[1]));
 }
 function snapshot(s,now){
  now=Math.floor(now/MIN)*MIN;
  const status=s.statusUntil>now?s.status:'normal';
  const available=s.paired!==false&&!s.paused&&s.scene!=='unknown';
  const people=Object.fromEntries(['me','partner'].map(who=>[who,{...local(now,who),activity:activity(s,now,who)}]));
  let window=null, start=null;
  if(available)for(let t=now;t<=now+7*DAY;t+=MIN){
   const common=t<now+7*DAY&&willing(s,t,'me',now)&&willing(s,t,'partner',now);
   if(common&&start===null)start=t;
   if(!common&&start!==null){window={start,end:t};break}
  }
  const dayStart=now-people.me.minute*MIN+(s.dayOffset||0)*DAY;
  const day={start:dayStart,activities:{me:[],partner:[]},contacts:{me:[],partner:[]}};
  for(const who of ['me','partner']){
   let prev='',cstart=null;
   for(let m=0;m<=1440;m++){
    if(m<1440){const a=activity(s,dayStart+m*MIN,who),key=a.label+'|'+a.kind;
     if(key!==prev){if(day.activities[who].length)day.activities[who].at(-1).end=m;day.activities[who].push({start:m,end:1440,...a});prev=key}}
    const contact=m<1440&&available&&willing(s,dayStart+m*MIN,who,now);
    if(contact&&cstart===null)cstart=m;
    if(!contact&&cstart!==null){day.contacts[who].push({start:cstart,end:m});cstart=null}
   }
  }
  return {now,status,available,people,window,day};
 }
 const api={snapshot,local};if(typeof module==='object'&&module.exports)module.exports=api;else root.DaylightSchedule=api;
})(typeof window!=='undefined'?window:globalThis);
