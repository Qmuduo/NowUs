import test from 'node:test';
import assert from 'node:assert/strict';
import { parts, dayBounds, routineAt, commonWindows, SCENARIOS, PEOPLE } from './model.mjs';

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
