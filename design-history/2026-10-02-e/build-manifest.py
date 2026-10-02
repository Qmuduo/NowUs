import hashlib, json, pathlib
root = pathlib.Path(__file__).resolve().parent
skip = {'manifest.json', 'verify.py'}
files = {}
for f in sorted(root.rglob('*')):
    if f.is_file() and f.name not in skip and f.parent.name != '__pycache__':
        files[f.relative_to(root).as_posix()] = hashlib.sha256(f.read_bytes()).hexdigest()
manifest = {
    'batch': '2026-10-02-e', 'versions': [108, 110],
    'kind': 'controlled comparison (not an improvement attempt)',
    'reviewerPrompt': 'docs/design-review-continuation-prompt-v2.md (v2.2)',
    'reviewerModel': 'Deepseek-V4.1-Flash',
    'records': {'108': '两两比较负', '110': '两两比较胜（恢复估计来源与约定状态）'},
    'adopted': '110', 'files': files,
}
(root / 'manifest.json').write_text(json.dumps(manifest, ensure_ascii=False, indent=2), encoding='utf-8')
print('manifest.json written with', len(files), 'files')
