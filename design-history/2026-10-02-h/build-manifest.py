import hashlib, json, pathlib
root = pathlib.Path(__file__).resolve().parent
skip = {'manifest.json', 'verify.py'}
files = {}
for f in sorted(root.rglob('*')):
    if f.is_file() and f.name not in skip and f.parent.name != '__pycache__':
        files[f.relative_to(root).as_posix()] = hashlib.sha256(f.read_bytes()).hexdigest()
manifest = {'batch': '2026-10-02-h', 'round': 112, 'reviewed': False,
            'reason': '缺陷只在固定演示场景之外的场景出现，默认场景下改前改后截图逐字节相同，按现行协议无法送评',
            'verification': '五个场景全部渲染通过、无横向溢出、无运行错误', 'adopted': '112', 'files': files}
(root / 'manifest.json').write_text(json.dumps(manifest, ensure_ascii=False, indent=2), encoding='utf-8')
print('manifest.json written with', len(files), 'files')
