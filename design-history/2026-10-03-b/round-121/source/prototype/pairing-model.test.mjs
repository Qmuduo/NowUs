import test from 'node:test';
import assert from 'node:assert/strict';
import * as pairing from './pairing-model.mjs';
import { DEFAULT_RHYTHM } from './model.mjs';

const instant = Date.parse('2026-09-29T12:00:00Z');
const me = {name:'小满', cityId:'beijing'};
const partner = {name:'阿远', cityId:'new-york'};

test('profiles require a known city and a short plain-text nickname', () => {
  assert.deepEqual(pairing.validateProfile(me), []);
  assert.ok(pairing.validateProfile({...me,name:'  '}).length);
  assert.ok(pairing.validateProfile({...me,name:'长'.repeat(21)}).length);
  assert.ok(pairing.validateProfile({...me,cityId:'made-up'}).length);
  assert.deepEqual(pairing.validateProfile({...me,name:'<满>'}), []);
});
test('a new invitation has no invented partner or partner schedule', () => {
  const record = pairing.createConnection(me,DEFAULT_RHYTHM,instant);
  assert.equal(record.status,'waiting');
  assert.equal(record.partner,null);
  assert.equal(record.invite.expiresAt,instant+86400000);
  assert.ok(pairing.validConnection(record));
});
test('acceptance is explicit and leaves partner rhythm unknown', () => {
  const record = pairing.createConnection(me,DEFAULT_RHYTHM,instant);
  const accepted = pairing.acceptInvite(record,partner,' demo-nowus ',instant+1);
  assert.equal(accepted.status,'paired');
  assert.equal(accepted.partner.ready,false);
  assert.equal(record.status,'waiting');
  assert.ok(pairing.validConnection(accepted));
  assert.throws(()=>pairing.acceptInvite(accepted,partner,'DEMO-NOWUS',instant+2),/已被接受/);
});
test('wrong, revoked or expired invitations cannot establish a pair', () => {
  const record = pairing.createConnection(me,DEFAULT_RHYTHM,instant);
  assert.throws(()=>pairing.acceptInvite(record,partner,'BAD',instant+1),/代码/);
  assert.throws(()=>pairing.acceptInvite(record,partner,'DEMO-NOWUS',record.invite.expiresAt),/过期/);
  assert.throws(()=>pairing.acceptInvite({...record,invite:{...record.invite,revoked:true}},partner,'DEMO-NOWUS',instant+1),/撤销/);
});
test('corrupt local records are rejected rather than showing false partner data', () => {
  const record = pairing.createConnection(me,DEFAULT_RHYTHM,instant);
  assert.equal(pairing.validConnection({...record,status:'paired'}),false);
  assert.equal(pairing.validConnection({...record,me:{name:'小满',cityId:'unknown'}}),false);
  assert.equal(pairing.validConnection({...record,rhythm:{weekday:null,rest:null}}),false);
  assert.equal(pairing.validConnection(null),false);
});
