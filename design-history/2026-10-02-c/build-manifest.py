"""Build manifest.json for the 2026-10-02-c design-history archive."""
import hashlib, json, pathlib
root = pathlib.Path(__file__).resolve().parent
skip = {'manifest.json', 'verify.py'}
files = {}
for f in sorted(root.rglob('*')):
    if f.is_file() and f.name not in skip and f.parent.name != '__pycache__':
        files[f.relative_to(root).as_posix()] = hashlib.sha256(f.read_bytes()).hexdigest()
manifest = {
    'batch': '2026-10-02-c',
    'versions': [88, 104, 105, 106, 107, 108],
    'reviewerPrompt': 'docs/design-review-continuation-prompt-v2.md (v2.1)',
    'reviewerModel': 'Deepseek-V4.1-Flash',
    'records': {'88': '8.1 (用户提供的历史评分)', '104': '7', '105': '7', '106': '7',
                '107': '两两比较胜第 88 轮', '108': '两两比较胜第 107 轮'},
    'adopted': '108',
    'files': files,
}
(root / 'manifest.json').write_text(json.dumps(manifest, ensure_ascii=False, indent=2), encoding='utf-8')
print('manifest.json written with', len(files), 'files')
