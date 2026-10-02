import hashlib, json, pathlib
root = pathlib.Path(__file__).resolve().parent
skip = {'manifest.json', 'verify.py'}
files = {}
for f in sorted(root.rglob('*')):
    if f.is_file() and f.name not in skip and f.parent.name != '__pycache__':
        files[f.relative_to(root).as_posix()] = hashlib.sha256(f.read_bytes()).hexdigest()
manifest = {'batch': '2026-10-02-k', 'round': 114, 'reviewed': False,
            'reason': '缺陷只在「先改过节奏」的状态下出现，默认场景下改前改后截图逐字节相同；送评需第三次协议扩展（轨道 A 增加该状态）',
            'verification': '首页空状态分支正确显示、无横向溢出、无运行错误', 'adopted': '114', 'files': files}
(root / 'manifest.json').write_text(json.dumps(manifest, ensure_ascii=False, indent=2), encoding='utf-8')
print('manifest.json written with', len(files), 'files')
