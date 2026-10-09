"""Build the continuation's browser entry and evidence manifest; no old-file writes."""
import hashlib
import html
import json
from pathlib import Path

root = Path(__file__).resolve().parents[1]
repo = root.parents[2]
sha = lambda p: hashlib.sha256(p.read_bytes()).hexdigest()
def link(p, label):
    relative = Path(__import__('os').path.relpath(p, root)).as_posix()
    return f'<a href="{html.escape(relative, quote=True)}">{html.escape(label)}</a>'

candidate_hashes = {p.name: sha(p) for p in sorted((root / 'candidate/source/prototype').iterdir()) if p.is_file()}
(root / 'candidate/source-hashes.json').write_text(json.dumps({
    'label': '待评候选；不是本轮已评实现', 'round': 120, 'reviewed': False,
    'reviewer_model': None, 'score': None, 'result': None, 'prototype': candidate_hashes,
}, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')

sections = []
for batch in sorted((repo / 'design-history').iterdir()):
    if not batch.is_dir():
        continue
    links = [link(batch / name, name) for name in ['index.html', 'README.md'] if (batch / name).is_file()]
    snapshots = []
    for p in sorted(batch.rglob('source/prototype/index.html')):
        snapshots.append('<li>' + link(p, p.relative_to(batch).as_posix()) + '</li>')
    screenshots = []
    for p in sorted(batch.rglob('*.png')):
        if 'source' not in p.parts and ('home' in p.name or p.parent.name == 'review'):
            screenshots.append('<li>' + link(p, p.relative_to(batch).as_posix()) + '</li>')
    sections.append(f'<details><summary>{html.escape(batch.name)}</summary><p>{" · ".join(links)}</p>'
                    + '<p>可运行完整源码</p><ul>' + ''.join(snapshots) + '</ul>'
                    + '<p>归档截图</p><ul>' + ''.join(screenshots) + '</ul></details>')
page = '''<!doctype html><html lang="zh-CN"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>NowUs · 第 120 轮待评记录</title>
<style>body{font:15px/1.65 system-ui,sans-serif;color:#1a1d21;background:#f4f5f7;margin:0;padding:28px;max-width:1000px;margin:auto}a{color:#285c94}h1{font-size:25px}h2{font-size:19px}.versions{display:flex;flex-wrap:wrap;gap:28px}.versions article{max-width:375px}img{width:100%;height:auto;border:1px solid #dce0e6;border-radius:10px}.notice,details{padding:14px 18px;background:white;border:1px solid #dce0e6;border-radius:10px;margin:12px 0}summary{cursor:pointer;font-weight:600}li{overflow-wrap:anywhere}</style></head><body>
<h1>第 120 轮候选已准备，独立评审未执行</h1><p class="notice">正式采用第 119 轮。指定 Deepseek-V4.1-Flash 在当前环境不可用；已评 4/5 轮，最后一轮未消耗。以下对照供实施者与用户查看，不给评论员。</p>
<p><a href="README.md">完整诊断与验证记录</a> · <a href="../../../prototype/">正式原型</a> · <a href="plan.md">候选决策</a></p>
<div class="versions"><article><h2>A · 第 119 轮（已评、正式采用）</h2><p><a href="baseline-119/source/prototype/">体验完整源码快照</a> · <a href="baseline-119/home-evidence.json">实际渲染证据</a></p><img src="baseline-119/home.png" width="375" height="812" alt="第119轮首页截图"></article>
<article><h2>B · 候选（未经独立评审）</h2><p><a href="candidate/source/prototype/">体验完整候选</a> · <a href="candidate/home-evidence.json">实际渲染证据</a></p><img src="candidate/home.png" width="375" height="812" alt="双方身份并入各自时间半栏的候选"></article></div>
<h2>历史浏览入口</h2>''' + ''.join(sections) + '</body></html>\n'
(root / 'index.html').write_text(page, encoding='utf-8')
plan = root / 'plan.md'
text = plan.read_text(encoding='utf-8').replace('- [ ] 仅在候选副本', '- [x] 仅在候选副本').replace('- [ ] 建立候选源码', '- [x] 建立候选源码')
plan.write_text(text, encoding='utf-8')
files = {p.relative_to(root).as_posix(): sha(p) for p in sorted(root.rglob('*')) if p.is_file() and p.name != 'manifest.json'}
(root / 'manifest.json').write_text(json.dumps({'date': '2026-10-03', 'reviewed': False, 'adopted_round': 119, 'completed_reviews': 4, 'files': files}, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
print(f'Created entry and manifest for {len(files)} files; no old archive or live prototype writes.')
