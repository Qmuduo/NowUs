"""Finalize this new batch only; never write earlier batch records."""
import hashlib
import json
from pathlib import Path

root = Path(__file__).resolve().parents[1]
repo = root.parents[1]
sha = lambda p: hashlib.sha256(p.read_bytes()).hexdigest()
round_dir = root / 'round-121'
hashes = {p.name: sha(p) for p in sorted((round_dir / 'source/prototype').iterdir()) if p.is_file()}
(round_dir / 'source-hashes.json').write_text(json.dumps({
    'round': 121, 'label': '本轮已评实现；比较负；未采用', 'reviewed': True,
    'adopted': False, 'adopted_round': 120, 'comparison': {'A': 120, 'B': 121},
    'result': 'A', 'score': None, 'reviewer_model': 'gpt-6.1-sol',
    'reviewer_name': 'GPT-6.1 Sol', 'reviewer_task': '/root/review_121',
    'fork_turns': 'none', 'prototype': hashes,
    'screenshots': {name: sha(round_dir / f'review/{name}.png') for name in ['A','B']},
    'prompt_sha256': sha(round_dir / 'review/prompt.txt'),
}, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
history_links = []
for batch in sorted((repo / 'design-history').iterdir()):
    if not batch.is_dir() or batch == root:
        continue
    target = 'index.html' if (batch / 'index.html').is_file() else 'README.md'
    history_links.append(f'<li><a href="../{batch.name}/{target}">{batch.name}</a></li>')
page = '''<!doctype html><html lang="zh-CN"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>NowUs · 第121轮评审</title>
<style>body{font:15px/1.65 system-ui,sans-serif;background:#f4f5f7;color:#1a1d21;max-width:1000px;margin:auto;padding:28px}h1{font-size:25px}h2{font-size:19px}a{color:#285c94}.status{background:white;border:1px solid #dce0e6;border-radius:10px;padding:16px}.cards{display:flex;flex-wrap:wrap;gap:28px}article{max-width:375px}img{width:100%;height:auto;border:1px solid #dce0e6;border-radius:10px}</style></head><body>
<h1>第 121 轮 A 胜，保留第 120 轮</h1><p class="status">GPT-6.1 Sol 独立比较选择现有版：短留言集中排列，用户确认共同时间后能直接阅读与回复。候选扩大了白卡，但没有增加清晰度。</p>
<p><a href="../../prototype/">打开正式原型</a> · <a href="round-121/review.md">本轮完整记录</a> · <a href="round-121/review/raw.txt">评论员原文</a> · <a href="thesis.md">整批论点</a></p>
<div class="cards"><article><h2>A · 第120轮（继续正式采用）</h2><p><a href="baseline-120/source/prototype/">完整源码快照</a> · <a href="final/home-evidence.json">最终实际渲染</a></p><img src="final/home.png" width="375" height="812" alt="继续采用的第120轮"></article>
<article><h2>B · 第121轮（已评，未采用）</h2><p><a href="round-121/source/prototype/">完整候选源码</a> · <a href="round-121/home-evidence.json">候选渲染证据</a></p><img src="round-121/home.png" width="375" height="812" alt="弹性高度及内容居中的第121轮候选"></article></div>
<p>新批次 2026-10-03-b：已评1/5轮，本次只执行用户要求的一轮，未自动开始第122轮。无分数；当前比较来源为GPT-6.1 Sol，旧来源保持不变。</p>
<p>正式版仍是原有32px回复触控高度；44px改善随B候选一并未采用。日／节奏视图对比度、旧favicon、导航上方空白仍是已记录的问题。功能验证与本轮局部比较不代表整体设计目标已经达到。</p>
<h2>其他历史批次</h2><ul>''' + ''.join(history_links) + '</ul></body></html>\n'
(root / 'index.html').write_text(page, encoding='utf-8')
(root / 'README.md').write_text('''# 2026-10-03-b · 第121轮完成，保留第120轮

用户授权“开启下一轮”，从已采用第120轮开始新批次。本次实际执行一轮，已评1/5，未自动追加其余轮次。

第121轮仅改留言区：白卡伸展承接剩余空间、留言与回复整体居中、回复触控提高至44px。GPT-6.1 Sol在全新上下文中独立比较选择A（第120轮），认为集中排列更便于短留言阅读和回复，B增加分量却没有提高清晰度。结果不评分；原文在 `round-121/review/raw.txt`。

正式 `prototype/` 从未被B覆盖，继续保留第120轮；最终15项浏览器回路检查、16项既有模型测试通过，15个源码文件及最终截图与A一致。B仅styles.css不同，完整源码和截图均保留，不抽取其中一个局部改动形成未评审最终版。

本轮的局部比较不表示整体目标达到。正式回复触控高度仍32px；日／节奏视图次要文字对比度不足、favicon旧色、留言与导航之间留白仍是已知未解决项。详情见 `round-121/review.md`。

`index.html` 包含本轮A/B、完整源码、评审与所有旧批次浏览入口。`baseline-120/` 是本轮前完整基线；`round-121/` 是本轮已评但未采用候选；`final/` 是保留第120轮后的实际验证。`exploration/` 为实施者两种自检布局，没有送给独立评论员、不消耗评审次数。

只读校验：`python design-history/2026-10-03-b/verify.py`，检查本批次、原有1168个历史文件以及正式原型。旧批次的评分、截图、源码和历史入口均未更改。
''', encoding='utf-8')
files = {p.relative_to(root).as_posix(): sha(p) for p in sorted(root.rglob('*')) if p.is_file() and p.name != 'manifest.json'}
(root / 'manifest.json').write_text(json.dumps({
    'date': '2026-10-03', 'round': 121, 'completed_reviews': 1, 'review_limit': 5,
    'result': 'A', 'adopted_round': 120, 'reviewer_model': 'gpt-6.1-sol', 'files': files,
}, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
print(f'Finalized {len(files)} files; adopted round 120, review 121 chooses A.')
