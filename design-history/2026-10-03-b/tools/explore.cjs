// Implementation-only alternatives, never sent to an independent reviewer.
const {chromium}=require('C:/Users/MUDUO/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/playwright');
const path=require('node:path'),fs=require('node:fs');
const root=path.resolve(__dirname,'..');
(async()=>{
  const browser=await chromium.launch({headless:true}),results=[];
  for(const variant of ['start','center']) {
    const context=await browser.newContext({viewport:{width:375,height:812},deviceScaleFactor:1,locale:'zh-CN',timezoneId:'Asia/Shanghai'});
    const page=await context.newPage();
    await page.goto('http://127.0.0.1:8765/prototype/',{waitUntil:'networkidle'});
    await page.evaluate(()=>document.fonts.ready);
    await page.addStyleTag({content:`#home-view .note-section{flex:1 0 auto;justify-content:${variant==='start'?'flex-start':'center'}}#home-view .note-compose{min-height:44px}`});
    await page.screenshot({path:path.join(root,'exploration',variant+'.png')});
    const geometry=await page.evaluate(()=>Object.fromEntries(['.time-pair','.connection-panel','.note-section','.note-preview','.note-compose','.tabbar'].map(selector=>[selector,document.querySelector(selector).getBoundingClientRect().toJSON()])));
    results.push({variant,geometry,reviewed:false});
    await context.close();
  }
  fs.writeFileSync(path.join(root,'exploration','geometry.json'),JSON.stringify(results,null,2));
  console.log(JSON.stringify(results.map(r=>({variant:r.variant,noteHeight:r.geometry['.note-section'].height,replyHeight:r.geometry['.note-compose'].height,noteToNav:r.geometry['.tabbar'].top-r.geometry['.note-section'].bottom}))));
  await browser.close();
})().catch(e=>{console.error(e);process.exitCode=1});
