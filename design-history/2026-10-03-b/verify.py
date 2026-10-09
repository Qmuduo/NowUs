"""Read-only current-batch, old-history and adopted-source verification."""
import hashlib
import json
from pathlib import Path

root = Path(__file__).resolve().parent
repo = root.parents[1]
sha = lambda p: hashlib.sha256(p.read_bytes()).hexdigest()
manifest = json.loads((root / 'manifest.json').read_text(encoding='utf-8'))
prior = json.loads((root / 'preserved-history.json').read_text(encoding='utf-8-sig'))
baseline = json.loads((root / 'baseline-120/source-hashes.json').read_text(encoding='utf-8-sig'))
failures = []
for relative, expected in manifest['files'].items():
    p = root / relative
    if not p.is_file() or sha(p) != expected:
        failures.append(f'Current archive changed: {relative}')
for relative, expected in prior.items():
    p = repo / relative
    if not p.is_file() or sha(p) != expected:
        failures.append(f'Old history changed: {relative}')
for name, expected in baseline.items():
    for folder in [repo / 'prototype', root / 'baseline-120/source/prototype']:
        p = folder / name
        if not p.is_file() or sha(p) != expected:
            failures.append(f'Adopted source changed: {p}')
candidate = root / 'round-121/source/prototype'
changed = sorted(name for name, expected in baseline.items() if sha(candidate / name) != expected)
if changed != ['styles.css']:
    failures.append(f'Unexpected candidate changed files: {changed}')
for label, directory in [('A', 'baseline-120'), ('B', 'round-121')]:
    if sha(root / f'round-121/review/{label}.png') != sha(root / directory / 'home.png'):
        failures.append(f'Review image {label} differs from original capture')
if sha(root / 'round-121/review/A.png') == sha(root / 'round-121/review/B.png'):
    failures.append('Review images identical')
if sha(root / 'final/home.png') != sha(root / 'round-121/review/A.png'):
    failures.append('Final render differs from reviewed A')
if (root / 'round-121/review/raw.txt').read_text(encoding='utf-8').strip().splitlines()[-1] != 'A':
    failures.append('Unexpected review conclusion')
if failures:
    raise SystemExit('\n'.join(failures))
print(f"Verified {len(manifest['files'])} new archive files, {len(prior)} old history files, "
      f"{len(baseline)} adopted prototype files; candidate only changed styles.css.")
print('Round 121: A wins, GPT-6.1 Sol; adopted round remains 120. Current batch: 1/5 reviews.')
