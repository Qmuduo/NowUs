import re, pathlib

style = re.search(r'<style>(.*?)</style>', pathlib.Path('design-history/2026-10-01/index.html').read_text(encoding='utf-8'), re.S).group(1)

versions = [
    (88, '8.1', '用户历史评分', '对照版本：本批次两次两两比较的基准，两次都落败。', 'baseline-88', ''),
    (104, '7', 'Deepseek-V4.1-Flash', '把显示级文字从「2小时」换成两地当地时段；倒计时降为标签。', 'round-104', 'round-104/review.md'),
    (105, '7', 'Deepseek-V4.1-Flash', '窗口块标签从「共同空闲」改为「可联系的时间」，消除与「09:00 开始上课」的矛盾。', 'round-105', 'round-105/review.md'),
    (106, '7', 'Deepseek-V4.1-Flash', '「2小时后开始」提到区块标题行，与「设置提醒」同级。', 'round-106', 'round-106/review.md'),
    (107, '两两胜', 'Deepseek-V4.1-Flash', '顶部两列时间做成可比较的一对（同字号同基线、等宽、以墨色深浅分主次）。两两比较胜第 88 轮。', 'round-107', 'round-107/review.md'),
    (108, '两两胜', 'Deepseek-V4.1-Flash', '留言入口收成自包含的行内动作，箭头紧跟文案；省出的空间留给留言。两两比较胜第 107 轮。', 'round-108', 'round-108/review.md'),
]

entries = []
for i, s, o, d, dr, rv in versions:
    entry = "{id:%d,score:'%s',origin:'%s',desc:'%s',dir:'%s'" % (i, s, o, d, dr)
    if rv:
        entry += ",review:'%s'" % rv
    entries.append(entry + "},")
opts = '\n'.join(entries)

script = """
const versions=[
__OPTS__
];
const root=document.querySelector('#compare');
const optionList=id=>versions.map(v=>'<option value="'+v.id+'" '+(v.id===id?'selected':'')+'>第 '+v.id+' 轮'+(v.id===88?' · 对照':'')+'</option>').join('');
const score=v=>'<span class="score"><small>'+v.origin+'</small>'+v.score+'<small> / 10</small></span>';
const actions=v=>'<div class="actions"><button class="preview-button" data-preview="'+v.id+'">体验这一版</button><a href="'+v.dir+'/home.png" target="_blank" rel="noopener">原始截图</a><a href="'+(v.review||('round-'+v.id+'/review.md'))+'">评语</a><a href="'+v.dir+'/source/prototype/index.html" target="_blank" rel="noopener">源码原型 &#8599;</a></div>';
function panel(id,side){const v=versions.find(v=>v.id===Number(id));return '<article class="panel" data-side="'+side+'"><div class="panel-head"><select aria-label="'+(side==='left'?'左侧':'右侧')+'版本" data-select="'+side+'">'+optionList(v.id)+'</select>'+score(v)+'</div><p class="caption">'+v.desc+'</p><div class="image-well"><img class="phone-shot" src="'+v.dir+'/home.png" width="375" height="812" alt="第 '+v.id+' 轮实际评审截图"></div>'+actions(v)+'</article>';}
root.innerHTML=panel(88,'left')+panel(108,'right');
document.querySelector('#all').innerHTML=versions.map(v=>'<article class="tile"><div class="panel-head"><h2>第 '+v.id+' 轮</h2>'+score(v)+'</div><p class="caption">'+v.desc+'</p><div class="image-well"><img class="phone-shot" src="'+v.dir+'/home.png" width="375" height="812" loading="lazy" alt="第 '+v.id+' 轮实际评审截图"></div>'+actions(v)+'</article>').join('');
root.addEventListener('change',e=>{if(e.target.matches('[data-select]'))e.target.closest('.panel').outerHTML=panel(e.target.value,e.target.dataset.select);});
document.querySelectorAll('[data-mode]').forEach(b=>b.addEventListener('click',()=>{document.querySelectorAll('[data-mode]').forEach(x=>x.setAttribute('aria-pressed',String(x===b)));root.hidden=b.dataset.mode!=='compare';document.querySelector('#all').hidden=b.dataset.mode!=='all';}));
const dialog=document.querySelector('#preview');
document.addEventListener('click',e=>{const b=e.target.closest('[data-preview]');if(!b)return;const v=versions.find(v=>v.id===Number(b.dataset.preview));document.querySelector('#preview-title').textContent='第 '+v.id+' 轮 · 手机预览';document.querySelector('#prototype-frame').src=v.dir+'/source/prototype/?preview='+Date.now();dialog.showModal();});
document.querySelector('#close-preview').addEventListener('click',()=>dialog.close());
dialog.addEventListener('close',()=>document.querySelector('#prototype-frame').src='about:blank');
""".replace('__OPTS__', opts)

head = """<!doctype html>
<html lang="zh-CN">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>NowUs · 设计历史 2026-10-02-c</title>
<style>__STYLE__</style>
</head>
<body>
<main class="shell">
<header class="masthead"><div><div class="wordmark">NowUs / ARCHIVE</div><h1>设计历史 · 2026-10-02-c 批次</h1><p class="intro">本批次 5 个新候选（第 104–108 轮），加上作为起点与对照版本的第 88 轮。每版都保留送评截图、完整源码快照、评审原文与来源。所有新评审由 Deepseek-V4.1-Flash 在全新上下文中完成。</p><div class="links"><a href="README.md">归档说明</a><a href="summary.md">逐轮记录</a><a href="retrospective.md">本批次复盘</a><a href="continuation-prompt-at-archive.md">批次提示词原文</a></div></div><div class="adopted">本批次结果<strong>第 108 轮</strong>两两比较 2 胜 0 负</div></header>
<p class="notice">本批次用 v2 提示词：第 104–106 轮为绝对分（各 7）；因 105、106 两轮停在 7 且各指出不同的一处，按预先定好的规则 3.8，第 107、108 轮改为强制两两比较、不出绝对分——两次比较都由当轮候选胜出。</p>
<div class="toolbar"><div class="modes" role="group" aria-label="浏览方式"><button data-mode="compare" aria-pressed="true">并排对照</button><button data-mode="all" aria-pressed="false">全部版本</button></div><span class="hint">归档截图保持原样，当前首页不受切换影响。</span></div>
<section id="compare" class="comparison" aria-label="版本对照"></section>
<section id="all" class="all" aria-label="全部历史版本" hidden></section>
<footer>历史目录：design-history/2026-10-02-c/。第 88–93 轮在 design-history/2026-10-01/，第 94–98 轮在 design-history/2026-10-02/，第 99–103 轮在 design-history/2026-10-02-b/。每版 <code>source/prototype/</code> 为完整快照；<code>render-evidence.json</code> 证明每个快照重新渲染后与归档截图逐字节一致。交互预览需通过本地 HTTP 服务打开。</footer>
</main>
<dialog id="preview"><div class="dialog-head"><strong id="preview-title"></strong><button id="close-preview" type="button">关闭 ×</button></div><div class="frame-well"><iframe id="prototype-frame" title="历史原型手机预览"></iframe></div><p class="dialog-note">375×812 手机视口。预览会读取浏览器已有的演示记录；上面的归档截图始终是实际送评时的固定画面。</p></dialog>
<script>__SCRIPT__</script>
</body>
</html>
"""

html = head.replace('__STYLE__', style).replace('__SCRIPT__', script)
pathlib.Path('design-history/2026-10-02-c/index.html').write_text(html, encoding='utf-8')
print('viewer written', len(html))
