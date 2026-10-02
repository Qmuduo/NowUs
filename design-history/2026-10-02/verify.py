"""Verify archived bytes against manifest.json without changing any file."""
import hashlib
import json
from pathlib import Path

archive_root = Path(__file__).resolve().parent
manifest = json.loads((archive_root / 'manifest.json').read_text(encoding='utf-8'))
failures = []
for relative_path, expected in manifest['files'].items():
    file_path = archive_root / relative_path
    if not file_path.is_file():
        failures.append(f'Missing: {relative_path}')
    elif hashlib.sha256(file_path.read_bytes()).hexdigest() != expected:
        failures.append(f'Changed: {relative_path}')
if failures:
    raise SystemExit('\n'.join(failures))
print(f"Verified {len(manifest['files'])} archive files; versions 88, 94-98 preserved.")
