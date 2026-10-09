"""Read-only integrity check for the adopted round and all prior archive bytes."""
import hashlib
import json
from pathlib import Path

root = Path(__file__).resolve().parent
repo = root.parents[2]
sha = lambda p: hashlib.sha256(p.read_bytes()).hexdigest()
data = json.loads((root / 'manifest.json').read_text(encoding='utf-8'))
source = json.loads((root / 'source-hashes.json').read_text(encoding='utf-8'))
failures = []
for relative, expected in data['files'].items():
    p = root / relative
    if not p.is_file() or sha(p) != expected:
        failures.append(f'Round 120 changed: {relative}')
for relative, expected in data['preserved_history'].items():
    p = repo / relative
    if not p.is_file() or sha(p) != expected:
        failures.append(f'Prior archive changed: {relative}')
for name, expected in source['prototype'].items():
    for folder in [repo / 'prototype', root / 'source/prototype']:
        p = folder / name
        if not p.is_file() or sha(p) != expected:
            failures.append(f'Adopted source differs: {p}')
index = root.parent / 'index.html'
if sha(index) != data['batch_index_sha256']:
    failures.append('Batch browser entry changed')
if sha(root / 'review/A.png') == sha(root / 'review/B.png'):
    failures.append('Review images are identical')
for p in [root / 'home.png', root / 'final/home.png']:
    if sha(p) != sha(root / 'review/B.png'):
        failures.append(f'Adopted screenshot differs from reviewed B: {p}')
if (root / 'review/raw.txt').read_text(encoding='utf-8').strip().splitlines()[-1] != 'B':
    failures.append('Raw review conclusion differs')
if failures:
    raise SystemExit('\n'.join(failures))
print(f"Verified {len(data['files'])} round files, {len(data['preserved_history'])} preserved history files, "
      f"{len(source['prototype'])} adopted source files, and reviewed B equals final render.")
print('Round 120: B wins, GPT-6.1 Sol. Adopted round 120; batch closed at 5/5 reviews.')
