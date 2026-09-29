import test from 'node:test';
import assert from 'node:assert/strict';
import { parts, dayBounds, routineAt, commonWindows, SCENARIOS, PEOPLE } from './model.mjs';
import * as model from './model.mjs';

test('one instant retains different local dates across midnight', () => {
  const instant = Date.parse('2026-09-29T16:30:00Z');
  assert.equal(parts(instant, 'Asia/Shanghai').date, '2026-09-30');
  assert.equal(parts(instant, 'Asia/Shanghai').time, '00:30');
  assert.equal(parts(instant, 'America/New_York').date, '2026-09-29');
  assert.equal(parts(instant, 'America/New_York').time, '12:30');
});

test('the Beijing day has precise start and end instants', () => {
  const bounds = dayBounds(Date.parse(SCENARIOS.normal.now), PEOPLE.me.zone);
  assert.equal(new Date(bounds.start).toISOString(), '2026-09-28T16:00:00.000Z');
  assert.equal(new Date(bounds.end).toISOString(), '2026-09-29T16:00:00.000Z');
});

test('default schedules overlap at Beijing 22:00 for thirty minutes', () => {
  const now = Date.parse(SCENARIOS.normal.now);
  const windows = commonWindows(dayBounds(now, PEOPLE.me.zone), 'normal');
  const next = windows.find(w => w.end > now);
  assert.equal(parts(next.start, PEOPLE.me.zone).time, '22:00');
  assert.equal(parts(next.start, PEOPLE.partner.zone).time, '10:00');
  assert.equal((next.end - next.start) / 60000, 30);
});

test('explicit incompatible contact preferences give no common window', () => {
  const now = Date.parse(SCENARIOS.none.now);
  assert.deepEqual(commonWindows(dayBounds(now, PEOPLE.me.zone), 'none'), []);
});

test('active busy status removes overlapping minutes until its expiry', () => {
  const now = Date.parse(SCENARIOS.normal.now);
  const busy = { person: 'partner', kind: 'busy', start: now, end: Date.parse('2026-09-29T14:15:00Z') };
  const next = commonWindows(dayBounds(now, PEOPLE.me.zone), 'normal', busy).find(w => w.end > now);
  assert.equal(parts(next.start, PEOPLE.me.zone).time, '22:15');
  assert.equal((next.end - next.start) / 60000, 15);
});

test('sleep continues on both sides of midnight', () => {
  assert.equal(routineAt('me', Date.parse('2026-09-29T15:30:00Z')).label, '睡觉');
  assert.equal(routineAt('me', Date.parse('2026-09-29T16:30:00Z')).label, '睡觉');
});

test('independent activities remain continuous through partner and contact changes', () => {
  const range = { start: Date.parse('2026-09-29T12:00:00Z'), end: Date.parse('2026-09-29T15:00:00Z') };
  const segments = model.activitySegments(range, 'me');
  assert.equal(segments.length, 1);
  assert.equal(segments[0].activity.label, '晚间休息');
  assert.deepEqual({start: segments[0].start, end: segments[0].end}, range);
});

test('missing partner preferences are unknown and cannot produce a future window', () => {
  const instant = Date.parse(SCENARIOS.normal.now);
  assert.equal(model.canContact('partner', instant, 'missing'), null);
  assert.equal(model.nextWindow(instant, 'missing'), null);
  assert.equal(model.routineAt('partner', instant, 'missing').source, '未知');
});

test('no overlap today can still show the next local day with both correct dates', () => {
  const instant = Date.parse(SCENARIOS.none.now);
  const result = model.nextWindow(instant, 'none');
  assert.equal(parts(result.start, PEOPLE.me.zone).date, '2026-09-30');
  assert.equal(parts(result.start, PEOPLE.me.zone).time, '07:00');
  assert.equal(parts(result.start, PEOPLE.partner.zone).date, '2026-09-29');
  assert.equal(parts(result.start, PEOPLE.partner.zone).time, '19:00');
});

test('saved weekday and rest-day rhythms change activities and common windows', () => {
  const rhythm = structuredClone(model.DEFAULT_RHYTHM);
  rhythm.weekday.contactStart = '21:00';
  rhythm.weekday.contactEnd = '22:00';
  const instant = Date.parse(SCENARIOS.normal.now);
  assert.deepEqual(commonWindows(dayBounds(instant, PEOPLE.me.zone), 'normal', null, rhythm).filter(w => w.end > instant), []);
  assert.equal(model.routineAt('me', Date.parse('2026-10-03T02:00:00Z'), 'normal', null, rhythm).kind, 'unknown');
  assert.equal(model.routineAt('me', Date.parse('2026-10-03T02:00:00Z'), 'normal', null, rhythm).source, '未知');
  rhythm.rest.contactKnown = false;
  assert.equal(model.canContact('me', Date.parse('2026-10-03T02:00:00Z'), 'normal', null, rhythm), null);
});

test('rhythm supports midnight sleep and contact ranges while rejecting conflicts', () => {
  const draft = structuredClone(model.DEFAULT_RHYTHM.weekday);
  assert.deepEqual(model.validateRhythm(draft), []);
  draft.activityStart = '06:00';
  assert.ok(model.validateRhythm(draft).some(message => message.includes('睡眠')));
  Object.assign(draft, {sleepStart:'08:00', sleepEnd:'16:00', activityStart:'16:00',activityEnd:'20:00',contactStart:'23:00',contactEnd:'01:00'});
  assert.deepEqual(model.validateRhythm(draft), []);
  const rhythm = {weekday:draft, rest:draft};
  assert.equal(model.canContact('me', Date.parse('2026-09-29T16:30:00Z'), 'normal', null, rhythm), true);
  assert.equal(model.canContact('me', Date.parse('2026-09-29T17:00:00Z'), 'normal', null, rhythm), false);
  draft.contactStart = '09:00';
  assert.ok(model.validateRhythm(draft).some(message => message.includes('睡眠')));
});
