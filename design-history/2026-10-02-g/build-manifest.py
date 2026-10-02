import hashlib, json, pathlib
root = pathlib.Path(__file__).resolve().parent
skip = {'manifest.json', 'verify.py'}
files = {}
for f in sorted(root.rglob('*')):
    if f.is_file() and f.name not in skip and f.parent.name != '__pycache__':
        files[f.relative_to(root).as_posix()] = hashlib.sha256(f.read_bytes()).hexdigest()
manifest = {'batch': '2026-10-02-g', 'track': 'B cross-view consistency', 'versions': [110, 111],
            'records': {'110': '两两比较负', '111': '两两比较胜（日视图两列与首页方向对齐）'}, 'adopted': '111', 'files': files}
(root / 'manifest.json').write_text(json.dumps(manifest, ensure_ascii=False, indent=2), encoding='utf-8')
print('manifest.json written with', len(files), 'files')
