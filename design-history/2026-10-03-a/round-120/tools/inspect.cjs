// Read-only browser inspection. All outputs stay in this round directory.
const { chromium } = require('C:/Users/MUDUO/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/playwright');
const fs = require('node:fs');
const path = require('node:path');
const [source, output] = process.argv.slice(2);
if (!source || !output) throw Error('Usage: node inspect.cjs <repository-relative-source> <output-directory>');
const root = path.resolve(__dirname, '..');
const folder = path.resolve(output);
if (!folder.startsWith(root + path.sep)) throw Error('Output must stay inside round-120');
fs.mkdirSync(folder, { recursive: true });
(async () => {
  const browser = await chromium.launch({ headless: true });
  const context = await browser.newContext({ viewport: { width: 375, height: 812 }, deviceScaleFactor: 1, locale: 'zh-CN', timezoneId: 'Asia/Shanghai' });
  const page = await context.newPage();
  const errors = [];
  page.on('pageerror', e => errors.push(e.message));
  await page.route('**/*', r => r.continue({ headers: { ...r.request().headers(), 'cache-control': 'no-cache', pragma: 'no-cache' } }));
  const response = await page.goto('http://127.0.0.1:8765/' + source.replaceAll('\\', '/') + '/?inspect=' + Date.now(), { waitUntil: 'networkidle' });
  if (response.status() !== 200) throw Error('Source HTTP status ' + response.status());
  await page.evaluate(() => document.fonts.ready);
  await page.screenshot({ path: path.join(folder, 'home.png') });
  const cdp = await context.newCDPSession(page);
  await cdp.send('DOM.enable');
  await cdp.send('CSS.enable');
  const documentNode = await cdp.send('DOM.getDocument');
  const { nodeId } = await cdp.send('DOM.querySelector', { nodeId: documentNode.root.nodeId, selector: '.time-pair' });
  const matched = await cdp.send('CSS.getMatchedStylesForNode', { nodeId });
  const marginRules = matched.matchedCSSRules.flatMap(({ rule, matchingSelectors }) => {
    const properties = rule.style.cssProperties.filter(p => /^(margin|margin-top|margin-block|margin-block-start)$/.test(p.name) && !p.implicit);
    return properties.length ? [{ selector: matchingSelectors.map(i => rule.selectorList.selectors[i].text).join(', '), properties, line: (rule.style.range?.startLine ?? -1) + 1, stylesheet: rule.styleSheetId }] : [];
  });
  function inspectScreen() {
    const rect = s => document.querySelector(s)?.getBoundingClientRect().toJSON();
    const pair = document.querySelector('.time-pair');
    const heading = rect('.home-thesis'), pairRect = rect('.time-pair'), note = rect('.note-section'), nav = rect('.tabbar');
    return { viewport: { width: innerWidth, height: innerHeight, dpr: devicePixelRatio, zoom: visualViewport.scale }, text: document.querySelector('.view:not([hidden])').innerText, rects: Object.fromEntries(['#app', '#home-view', '.home-thesis', '.time-pair', '.connection-panel', '.note-section', '.note-preview', '.note-compose', '.tabbar'].map(s => [s, rect(s)])), pairClass: pair?.className, computedMarginTop: pair && getComputedStyle(pair).marginTop, gaps: { headingToPair: heading && pairRect ? pairRect.top - heading.bottom : null, noteToNav: note && nav ? nav.top - note.bottom : null }, scroll: { documentWidth: document.documentElement.scrollWidth, appHeight: document.querySelector('#app').clientHeight, appScrollHeight: document.querySelector('#app').scrollHeight }, tokens: Object.fromEntries(['--accent', '--page', '--muted', '--day', '--day-ink', '--night', '--night-ink'].map(p => [p, getComputedStyle(document.documentElement).getPropertyValue(p).trim()])), brandDot: getComputedStyle(document.querySelector('.wordmark .brand-dot')).backgroundColor, favicon: document.querySelector('[rel=icon]').href };
  }
  const home = await page.evaluate(inspectScreen);
  fs.writeFileSync(path.join(folder, 'home-evidence.json'), JSON.stringify({ source, errors, marginRules, ...home }, null, 2));
  // Actual text foreground composited through opacity and ancestor backgrounds.
  async function contrastAudit(view) {
    return page.evaluate(view => {
      const canvas=document.createElement('canvas'); canvas.width=canvas.height=1;
      const colors=canvas.getContext('2d',{willReadFrequently:true});
      const parse = value => { colors.clearRect(0,0,1,1); colors.fillStyle=value; colors.fillRect(0,0,1,1); const n=colors.getImageData(0,0,1,1).data; return [n[0],n[1],n[2],n[3]/255]; };
      const over = (a,b) => { const alpha = a[3] + b[3]*(1-a[3]); return [...a.slice(0,3).map((v,i) => (v*a[3]+b[i]*b[3]*(1-a[3]))/(alpha||1)), alpha]; };
      const luminance = rgb => rgb.slice(0,3).map(v => { v/=255; return v<=.04045 ? v/12.92 : ((v+.055)/1.055)**2.4; }).reduce((s,v,i)=>s+v*[.2126,.7152,.0722][i],0);
      const failures = [], checked = [];
      const root = document.querySelector('#'+view+'-view');
      const walker = document.createTreeWalker(root, NodeFilter.SHOW_TEXT);
      for (let node; node=walker.nextNode();) {
        const text=node.textContent.trim(); if(!text) continue;
        const el=node.parentElement, style=getComputedStyle(el);
        const range=document.createRange(); range.selectNodeContents(node);
        const rect=range.getBoundingClientRect();
        if(!rect.width || !rect.height || style.visibility==='hidden') continue;
        const chain=[]; for(let p=el;p;p=p.parentElement) chain.unshift(p);
        let bg=[255,255,255,1]; let opacity=1;
        for(const p of chain) { const s=getComputedStyle(p); bg=over(parse(s.backgroundColor),bg); opacity*=Number(s.opacity); }
        const foreground=parse(style.color); foreground[3]*=opacity;
        const fg=over(foreground,bg); const l1=luminance(fg), l2=luminance(bg);
        const ratio=(Math.max(l1,l2)+.05)/(Math.min(l1,l2)+.05);
        const size=parseFloat(style.fontSize), weight=parseFloat(style.fontWeight);
        const threshold=size>=24 || (size>=18.6667 && weight>=700) ? 3 : 4.5;
        const row={ text, tag:el.tagName, class:el.className, ratio:Number(ratio.toFixed(3)), threshold, color:style.color, background:bg, opacity, size, weight, top:rect.top, bottom:rect.bottom };
        checked.push(row); if(ratio < threshold) failures.push(row);
      }
      return { view, checked, failures, method:'Solid CSS backgrounds and opacity; gradient and pseudo-element backgrounds need manual review.' };
    }, view);
  }
  const audits=[await contrastAudit('home')];
  for(const view of ['day','rhythm']) {
    await page.locator('[data-tab="'+view+'"]').click();
    await page.evaluate(() => document.fonts.ready);
    await page.screenshot({path:path.join(folder, view+'.png')});
    audits.push(await contrastAudit(view));
  }
  fs.writeFileSync(path.join(folder, 'contrast.json'), JSON.stringify(audits,null,2));
  console.log(JSON.stringify({source, errors, margins:marginRules.map(r=>({selector:r.selector,line:r.line,values:r.properties.map(p=>p.name+':'+p.value)})),computedMarginTop:home.computedMarginTop,gaps:home.gaps,contrast:audits.map(a=>({view:a.view,checked:a.checked.length,failures:a.failures.length}))}));
  await browser.close();
})().catch(e => { console.error(e); process.exitCode=1; });
