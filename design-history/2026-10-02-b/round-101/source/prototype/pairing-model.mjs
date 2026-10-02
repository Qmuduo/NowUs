import { validateRhythm } from './model.mjs?v=3';

export const CITIES = {
  beijing:{label:'北京',zone:'Asia/Shanghai'}, shanghai:{label:'上海',zone:'Asia/Shanghai'},
  'new-york':{label:'纽约',zone:'America/New_York'}, london:{label:'伦敦',zone:'Europe/London'},
  paris:{label:'巴黎',zone:'Europe/Paris'}, tokyo:{label:'东京',zone:'Asia/Tokyo'}, sydney:{label:'悉尼',zone:'Australia/Sydney'}
};
export function validateProfile(profile) {
  const errors = [];
  if (typeof profile?.name !== 'string' || !profile.name.trim()) errors.push('请填写一个昵称。');
  else if (Array.from(profile.name.trim()).length > 20) errors.push('昵称请控制在 20 个字符以内。');
  if (!Object.hasOwn(CITIES,profile?.cityId || '')) errors.push('请选择列表中的城市。');
  return errors;
}
const validRhythm = rhythm => rhythm && ['weekday','rest'].every(key => validateRhythm(rhythm[key]).length === 0);
export function createConnection(me, rhythm, clock = Date.now()) {
  const errors = validateProfile(me);
  if (errors.length || !validRhythm(rhythm) || !Number.isFinite(clock)) throw new Error(errors[0] || '请检查作息与时间。');
  return {version:1,status:'waiting',me:{name:me.name.trim(),cityId:me.cityId},rhythm:structuredClone(rhythm),partner:null,note:null,invite:{code:'DEMO-NOWUS',expiresAt:clock+86400000,revoked:false}};
}
export function inviteError(record, code, clock = Date.now()) {
  if (record.status === 'paired') return '这份邀请已被接受，不能重复配对。';
  if (record.invite.revoked) return '这份邀请已撤销，请返回重新建立邀请。';
  if (clock >= record.invite.expiresAt) return '这份邀请已过期，请返回重新建立邀请。';
  if (String(code).trim().toUpperCase() !== record.invite.code) return '邀请代码不匹配，请检查演示代码。';
  return '';
}
export function acceptInvite(record, profile, code, clock = Date.now()) {
  const error = inviteError(record,code,clock) || validateProfile(profile)[0];
  if (error) throw new Error(error);
  return {...structuredClone(record),status:'paired',partner:{name:profile.name.trim(),cityId:profile.cityId,ready:false}};
}
export function validConnection(record) {
  if (!record || record.version !== 1 || !['waiting','paired'].includes(record.status)) return false;
  if (validateProfile(record.me).length || !validRhythm(record.rhythm)) return false;
  if (record.status === 'waiting' ? record.partner !== null : validateProfile(record.partner).length || typeof record.partner.ready !== 'boolean') return false;
  if (record.invite?.code !== 'DEMO-NOWUS' || !Number.isFinite(record.invite?.expiresAt) || typeof record.invite?.revoked !== 'boolean') return false;
  if (record.note !== null && (typeof record.note?.text !== 'string' || !Number.isFinite(record.note?.time))) return false;
  return true;
}
