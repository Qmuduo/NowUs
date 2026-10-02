import re, pathlib

style = re.search(r'<style>(.*?)</style>', pathlib.Path('design-history/2026-10-01/index.html').read_text(encoding='utf-8'), re.S).group(1)

html = """<!doctype html>
<html lang="zh-CN">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>NowUs · 设计历史 2026-10-02-d</title>
<style>__STYLE__</style>
</head>
<body>
<main class="shell">
<header class="masthead"><div><div class="wordmark">NowUs / ARCHIVE</div><h1>设计历史 · 2026-10-02-d 批次</h1><p class="intro">本批次只有一轮：第 109 轮是一次<strong>受控对照</strong>，不是改进尝试。它把第 107 轮那处从未被比较确认过的顶部改动单独撤回，与第 108 轮（当前采用版本）做两两比较，以关闭采用版本里最后一个未经确认的风险。</p><div class="links"><a href="README.md">归档说明</a><a href="summary.md">逐轮记录</a><a href="round-109/review.md">第 109 轮评审原文</a></div></div><div class="adopted">采用版本仍为<strong>第 108 轮</strong><br>第 109 轮落败，不予采用</div></header>
<p class="notice">两两比较结果：A（第 108 轮，含该改动）胜。评审回答「有实质差别」并给出像素级依据——A 里两个时间同量级、共基线、可横向比对；B 把右栏缩小却不让左栏变大，只是削弱了对照的另一半，并没有换来更突出的伴侣。<strong>第 107 轮那处改动由此被确认为真实改进。</strong></p>
<div class="toolbar"><div class="modes" role="group" aria-label="浏览方式"><button data-mode="compare" aria-pressed="true">并排对照</button><button data-mode="all" aria-pressed="false">全部版本</button></div><span class="hint">归档截图保持原样，当前首页不受切换影响。</span></div>
<section id="compare" class="comparison" aria-label="版本对照"></section>
<section id="all" class="all" aria-label="全部历史版本" hidden></section>
<footer>历史目录：design-history/2026-10-02-d/。前面几批：第 88–93 轮在 design-history/2026-10-01/，第 94–98 轮在 design-history/2026-10-02/，第 99–103 轮在 design-history/2026-10-02-b/，第 104–108 轮在 design-history/2026-10-02-c/。<code>render-evidence.json</code> 证明每个快照重新渲染后与归档截图逐字节一致。</footer>
</main>
<dialog id="preview"><div class="dialog-head"><strong id="preview-title"></strong><button id="close-preview" type="button">关闭 ×</button></div><div class="frame-well"><iframe id="prototype-frame" title="历史原型手机预览"></iframe></div><p class="dialog-note">375×812 手机视口。上面的归档截图始终是实际送评时的固定画面。</p></dialog>
<script>
const versions=[
{id:108,score:'胜',origin:'两两比较',desc:'当前采用版本：含第 107 轮的顶部改动（两列等宽、同字号、同基线、墨色深浅分主次）。',dir:'baseline-108',review:'round-109/review.md'},
{id:109,score:'负',origin:'两两比较',desc:'受控对照：单独撤回第 107 轮的顶部改动，其余与第 108 轮完全一致。落败。',dir:'round-109',review:'round-109/review.md'},
];
const root=document.querySelector('#compare');
const optionList=id=>versions.map(v=>'<option value="'+v.id+'" '+(v.id===id?'selected':'')+'>第 '+v.id+' 轮</option>').join('');
const score=v=>'<span class="score"><small>'+v.origin+'</small>'+v.score+'</span>';
const actions=v=>'<div class="actions"><button class="preview-button" data-preview="'+v.id+'">体验这一版</button><a href="'+v.dir+'/home.png" target="_blank" rel="noopener">原始截图</a><a href="'+v.review+'">评语</a><a href="'+v.dir+'/source/prototype/index.html" target="_blank" rel="noopener">源码原型 &#8599;</a></div>';
function panel(id,side){const v=versions.find(v=>v.id===Number(id));return '<article class="panel" data-side="'+side+'"><div class="panel-head"><select aria-label="'+(side==='left'?'左侧':'右侧')+'版本" data-select="'+side+'">'+optionList(v.id)+'</select>'+score(v)+'</div><p class="caption">'+v.desc+'</p><div class="image-well"><img class="phone-shot" src="'+v.dir+'/home.png" width="375" height="812" alt="第 '+v.id+' 轮截图"></div>'+actions(v)+'</article>';}
root.innerHTML=panel(108,'left')+panel(109,'right');
document.querySelector('#all').innerHTML=versions.map(v=>'<article class="tile"><div class="panel-head"><h2>第 '+v.id+' 轮</h2>'+score(v)+'</div><p class="caption">'+v.desc+'</p><div class="image-well"><img class="phone-shot" src="'+v.dir+'/home.png" width="375" height="812" loading="lazy" alt="第 '+v.id+' 轮截图"></div>'+actions(v)+'</article>').join('');
root.addEventListener('change',e=>{if(e.target.matches('[data-select]'))e.target.closest('.panel').outerHTML=panel(e.target.value,e.target.dataset.select);});
document.querySelectorAll('[data-mode]').forEach(b=>b.addEventListener('click',()=>{document.querySelectorAll('[data-mode]').forEach(x=>x.setAttribute('aria-pressed',String(x===b)));root.hidden=b.dataset.mode!=='compare';document.querySelector('#all').hidden=b.dataset.mode!=='all';}));
const dialog=document.querySelector('#preview');
document.addEventListener('click',e=>{const b=e.target.closest('[data-preview]');if(!b)return;const v=versions.find(v=>v.id===Number(b.dataset.preview));document.querySelector('#preview-title').textContent='第 '+v.id+' 轮 · 手机预览';document.querySelector('#prototype-frame').src=v.dir+'/source/prototype/?preview='+Date.now();dialog.showModal();});
document.querySelector('#close-preview').addEventListener('click',()=>dialog.close());
dialog.addEventListener('close',()=>document.querySelector('#prototype-frame').src='about:blank');
</script>
</body>
</html>
""".replace('__STYLE__', style)

pathlib.Path('design-history/2026-10-02-d/index.html').write_text(html, encoding='utf-8')
print('batch-d viewer written', len(html))
