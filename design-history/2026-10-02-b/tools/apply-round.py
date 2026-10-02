"""Swap the home block of the current prototype for the given round's implementation."""
import pathlib, sys

n = sys.argv[1]
root = pathlib.Path('.')
p = root / 'prototype' / 'app.mjs'
lines = p.read_text(encoding='utf-8').split('\n')

start = next(i for i, l in enumerate(lines) if l.startswith('const periodOf') or l.startswith('function nowCard(') or l.startswith('function laneRow(') or l.startswith('function trackRow(') or l.startswith('function personCell(') or l.startswith('function nowRow(') or l.startswith('function timePerson('))
end = next(i for i, l in enumerate(lines) if l.startswith('function renderHome() {'))
assert lines[end + 2] == '}', repr(lines[end + 2])
block = (root / 'design-history' / '2026-10-02-b' / 'tools' / f'home-block-{n}.mjs').read_text(encoding='utf-8').rstrip('\n').split('\n')
lines = lines[:start] + block + lines[end + 3:]
p.write_text('\n'.join(lines), encoding='utf-8')

c = root / 'prototype' / 'styles.css'
text = c.read_text(encoding='utf-8')
cl = text.split('\n')
cs = next(i for i, l in enumerate(cl) if l.startswith('.view#home-view {'))
ce = next(i for i, l in enumerate(cl) if l.startswith('.reply-hint {'))
cblock = (root / 'design-history' / '2026-10-02-b' / 'tools' / f'home-block-{n}.css').read_text(encoding='utf-8').rstrip('\n').split('\n')
cl = cl[:cs] + cblock + cl[ce + 1:]
c.write_text('\n'.join(cl), encoding='utf-8')
print(f'round {n} applied')
