import hashlib, json, pathlib
root = pathlib.Path(__file__).resolve().parent
skip = {'manifest.json', 'verify.py'}
files = {}
for f in sorted(root.rglob('*')):
    if f.is_file() and f.name not in skip and f.parent.name != '__pycache__':
        files[f.relative_to(root).as_posix()] = hashlib.sha256(f.read_bytes()).hexdigest()
manifest = {'batch': '2026-10-02-l', 'round': 114, 'reviewed': True,
            'protocol': 'usage path fixed as a full task loop; no further extensions',
            'records': {'113': '两两比较负', '114': '两两比较胜'},
            'residual': '保存节奏后的 Toast 写「共同时间已更新」，与结论块的空状态矛盾',
            'adopted': '114', 'files': files}
(root / 'manifest.json').write_text(json.dumps(manifest, ensure_ascii=False, indent=2), encoding='utf-8')
print('manifest.json written with', len(files), 'files')
