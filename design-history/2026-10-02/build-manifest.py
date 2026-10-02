"""Build manifest.json for the 2026-10-02 design-history archive."""
import hashlib, json, pathlib

root = pathlib.Path(__file__).resolve().parent
skip = {'manifest.json', 'verify.py'}
files = {}
for f in sorted(root.rglob('*')):
    if f.is_file() and f.name not in skip and f.parent.name != '__pycache__':
        files[f.relative_to(root).as_posix()] = hashlib.sha256(f.read_bytes()).hexdigest()

manifest = {
    'batch': '2026-10-02',
    'versions': [88, 94, 95, 96, 97, 98],
    'reviewerModel': 'Deepseek-V4.1-Flash',
    'scores': {'88': '8.1 (用户提供的历史评分)', '94': '6', '95': '6', '96': '6', '97': '6', '98': '6'},
    'adopted': '88',
    'currentPrototypeUnchanged': True,
    'reviewerPromptUnchanged': True,
    'files': files,
}
(root / 'manifest.json').write_text(json.dumps(manifest, ensure_ascii=False, indent=2), encoding='utf-8')
print('manifest.json written with', len(files), 'files')
