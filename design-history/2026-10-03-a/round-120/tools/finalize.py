"""Finalize only round 120 metadata and the new batch browser entry."""
import hashlib
import json
from pathlib import Path

root = Path(__file__).resolve().parents[1]
batch = root.parent
repo = root.parents[2]
prep = batch / 'continuation-120'
sha = lambda p: hashlib.sha256(p.read_bytes()).hexdigest()
hashes = {p.name: sha(p) for p in sorted((root / 'source/prototype').iterdir()) if p.is_file()}
(root / 'source-hashes.json').write_text(json.dumps({
    'round': 120, 'label': '本轮已评实现；用户偏好与独立比较均选择B；正式采用',
    'reviewed': True, 'reviewer_model': 'gpt-6.1-sol', 'reviewer_name': 'GPT-6.1 Sol',
    'fork_turns': 'none', 'reviewer_task': '/root/review_120', 'result': 'B', 'score': None,
    'comparison': {'A': 119, 'B': 120}, 'prototype': hashes,
    'screenshots': {'A': sha(root / 'review/A.png'), 'B': sha(root / 'review/B.png')},
    'prompt_sha256': sha(root / 'review/prompt.txt'),
}, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
old = json.loads((prep / 'prior-history-hashes.json').read_text(encoding='utf-8-sig'))
frozen = json.loads((prep / 'manifest.json').read_text(encoding='utf-8'))
for relative, expected in frozen['files'].items():
    if sha(prep / relative) != expected:
        raise SystemExit(f'Preparation changed: {relative}')
    old[(prep / relative).relative_to(repo).as_posix()] = expected
old[(prep / 'manifest.json').relative_to(repo).as_posix()] = sha(prep / 'manifest.json')
history = []
for folder in sorted((repo / 'design-history').iterdir()):
    if not folder.is_dir() or folder == batch:
        continue
    target = 'index.html' if (folder / 'index.html').is_file() else 'README.md'
    history.append(f'<li><a href="../{folder.name}/{target}">{folder.name}</a></li>')
page = '''<!doctype html><html lang="zh-CN"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>NowUs · 第120轮正式采用</title>
<style>body{font:15px/1.65 system-ui,sans-serif;background:#f4f5f7;color:#1a1d21;margin:auto;max-width:1000px;padding:28px}h1{font-size:25px}h2{font-size:19px}a{color:#285c94}.cards{display:flex;flex-wrap:wrap;gap:28px}article{max-width:375px}img{width:100%;height:auto;border:1px solid #dce0e6;border-radius:10px}.status{padding:16px;background:white;border:1px solid #dce0e6;border-radius:10px}table{border-collapse:collapse;width:100%;margin:20px 0}td,th{padding:9px;text-align:left;border-bottom:1px solid #dce0e6}li{overflow-wrap:anywhere}</style></head><body>
<h1>第 120 轮 B 胜，正式采用</h1><p class="status">用户偏好 B；全新上下文的 GPT-6.1 Sol 独立比较同样选择 B。本批次已完成 5/5 轮，到此收束。</p>
<p><a href="../../prototype/">打开正式原型</a> · <a href="round-120/review.md">本轮结论与采用依据</a> · <a href="round-120/review/raw.txt">评论员原文</a> · <a href="thesis.md">整批论点</a></p>
<div class="cards"><article><h2>A · 第 119 轮</h2><p><a href="round-119/source/prototype/">完整源码快照</a> · <a href="round-119/review.md">原始评审</a></p><img src="round-120/review/A.png" width="375" height="812" alt="第119轮首页"></article>
<article><h2>B · 第 120 轮（已评、正式采用）</h2><p><a href="round-120/source/prototype/">完整源码快照</a> · <a href="round-120/final/home-evidence.json">正式版渲染证据</a></p><img src="round-120/final/home.png" width="375" height="812" alt="第120轮正式首页"></article></div>
<table><thead><tr><th>轮次</th><th>原始比较结果</th><th>来源</th></tr></thead><tbody>
<tr><td><a href="round-116/review.md">116</a></td><td>B 胜</td><td>DeepSeek-V4.1-Flash</td></tr>
<tr><td><a href="round-117/review.md">117</a></td><td>无实质差别，保留</td><td>DeepSeek-V4.1-Flash</td></tr>
<tr><td><a href="round-118/review.md">118</a></td><td>无实质差别，回退</td><td>DeepSeek-V4.1-Flash</td></tr>
<tr><td><a href="round-119/review.md">119</a></td><td>B 胜</td><td>DeepSeek-V4.1-Flash</td></tr>
<tr><td><a href="round-120/review.md">120</a></td><td>B 胜，正式采用</td><td>GPT-6.1 Sol（本轮由用户授权切换）</td></tr></tbody></table>
<p>正式版截图与送评 B 哈希相同；16 项既有测试、15 项实际回路检查通过。日／节奏视图的次要文字对比度、旧 favicon、留言下方留白及回复触控高度仍是已记录的未解决项，本轮没有追加设计修改。</p>
<p><a href="continuation-120/">冻结的送评前准备记录（保留当时状态）</a> · <a href="continuation-120/index.html#history">准备阶段历史浏览入口</a></p>
<h2>既有各批次</h2><ul>''' + ''.join(history) + '</ul></body></html>\n'
# The batch has no previous index; subsequent runs update this new entry only.
(batch / 'index.html').write_text(page, encoding='utf-8')
files = {p.relative_to(root).as_posix(): sha(p) for p in sorted(root.rglob('*')) if p.is_file() and p.name != 'manifest.json'}
(root / 'manifest.json').write_text(json.dumps({
    'reviewed': True, 'adopted_round': 120, 'completed_reviews': 5,
    'reviewer_model': 'gpt-6.1-sol', 'result': 'B', 'files': files,
    'preserved_history': old, 'batch_index_sha256': sha(batch / 'index.html'),
}, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
print(f'Finalized {len(files)} round files and recorded {len(old)} preserved earlier files.')
