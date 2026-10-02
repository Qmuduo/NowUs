import pathlib, sys

# --- app.mjs ---
p = pathlib.Path('prototype/app.mjs')
lines = p.read_text(encoding='utf-8').split('\n')

assert lines[74].startswith('function source(activity)'), lines[74][:40]
assert lines[75].startswith('function stage(instant, person)'), lines[75][:40]
assert lines[79].startswith('function scenarioButtons()'), lines[79][:40]
assert lines[82].startswith('function timePerson(person)'), lines[82][:40]
assert lines[144].strip() == 'function renderHome() {', lines[144][:40]
assert lines[159] == '}', repr(lines[159])
assert lines[160].startswith('function renderDay()'), lines[160][:40]

# drop the now-unused source() and stage() helpers
lines = lines[:74] + lines[79:]
# indices shift by 5
start = 82 - 5
end = 144 - 5
assert lines[start].startswith('function timePerson(person)'), lines[start][:40]
assert lines[end].strip() == 'function renderHome() {', lines[end][:40]
assert lines[end + 15] == '}', repr(lines[end + 15])

block = pathlib.Path('design-history/2026-10-02-b/tools/home-block-99.mjs').read_text(encoding='utf-8').rstrip('\n').split('\n')
lines = lines[:start] + block + lines[end + 16:]
p.write_text('\n'.join(lines), encoding='utf-8')

# --- styles.css ---
c = pathlib.Path('prototype/styles.css')
text = c.read_text(encoding='utf-8')
cl = text.split('\n')
tail = next(i for i, l in enumerate(cl) if l.startswith('/* Round 88:'))
cl = cl[:tail]
assert cl[140].startswith('.view#home-view {'), cl[140][:40]
assert cl[281].startswith('#home-view .quiet-footnote'), cl[281][:60]
assert cl[282].startswith('.tabbar {'), cl[282][:30]
cblock = pathlib.Path('design-history/2026-10-02-b/tools/home-block-99.css').read_text(encoding='utf-8').rstrip('\n').split('\n')
cl = cl[:140] + cblock + cl[282:]
text = '\n'.join(cl)

old = '  --font-time: "Segoe UI", "Arial", sans-serif;\n'
assert old in text
text = text.replace(old, old
    + '  --font-serif: Georgia, "Times New Roman", "Noto Serif SC", "Songti SC", "STSong", "SimSun", serif;\n'
    + '  --band: #2A6046;\n  --band-ink: #F2F6F1;\n  --band-soft: rgba(242, 246, 241, .84);\n  --band-line: rgba(242, 246, 241, .22);\n', 1)

# clean the <=350px media query of home rules that no longer exist
dead = [
    '  .time-person { padding-left: 12px; padding-right: 12px; }\n',
    '  .local-time { font-size: 29px; }\n',
    '  .person-label { gap: 5px; font-size: 10.5px; }\n',
    '  .time-person { padding-left: 10px; padding-right: 10px; }\n',
    '  .local-date { font-size: 10.5px; }\n',
    '  .note-preview-head > span { margin-left: 21px; }\n',
    '  .common-times { gap: 8px; }\n',
    '  .common-times > div + div { padding-left: 10px; }\n',
    '  .common-times strong { font-size: 14px; }\n',
    '  #home-view .life-state strong { font-size: 16px; }\n',
    '  #home-view .common-times { gap: 7px; }\n',
    '  #home-view .common-times > div + div { padding-left: 7px; }\n',
    '  #home-view .common-times strong { font-size: 16px; }\n',
    '  #home-view .common-title { font-size: 21px; }\n',
    '  #home-view .time-pair .partner-person .local-time { font-size: 46px; }\n',
    '  #home-view .time-pair .self-person .local-time { font-size: 32px; }\n',
    '  .common-footer { flex-wrap: wrap; }\n',
]
for d in dead:
    assert d in text, d
    text = text.replace(d, '', 1)
text = text.replace('  .life-line { flex-wrap: wrap; }\n',
    '  .band-lead { font-size: 26px; }\n  .now-time { font-size: 25px; }\n  .note-text { font-size: 18px; }\n  .life-line { flex-wrap: wrap; }\n', 1)
c.write_text(text, encoding='utf-8')

print('spliced')
