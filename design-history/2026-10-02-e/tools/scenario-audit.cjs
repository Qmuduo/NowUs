// Render the adopted home screen across every demo scenario at 375x812 and record evidence.
const {chromium} = require('C:/Users/MUDUO/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/playwright');
const fs = require('node:fs');
const path = require('node:path');

const outDir = path.resolve(__dirname, '..', 'scenario-audit');
fs.mkdirSync(outDir, {recursive: true});

(async () => {
  const browser = await chromium.launch({headless: true});
  const context = await browser.newContext({viewport: {width: 1200, height: 900}, deviceScaleFactor: 1, locale: 'zh-CN', timezoneId: 'Asia/Shanghai'});
  const page = await context.newPage();
  const errors = [];
  page.on('pageerror', e => errors.push(String(e.message)));
  await page.goto('http://127.0.0.1:8765/prototype/?audit=' + Date.now(), {waitUntil: 'networkidle'});

  const buttons = await page.locator('#desktop-scenarios .scenario-button').all();
  const results = [];
  for (let i = 0; i < buttons.length; i++) {
    const label = (await buttons[i].innerText()).replace(/\n/g, ' ').trim();
    await buttons[i].click();
    await page.waitForTimeout(250);
    await page.setViewportSize({width: 375, height: 812});
    await page.waitForTimeout(350);
    const slug = 'scenario-' + (i + 1);
    await page.screenshot({path: path.join(outDir, slug + '.png')});
    const evidence = await page.evaluate(() => {
      const app = document.querySelector('#app');
      const rect = s => {
        const e = document.querySelector(s);
        if (!e) return null;
        const b = e.getBoundingClientRect();
        return {top: Math.round(b.top), bottom: Math.round(b.bottom), left: Math.round(b.left), right: Math.round(b.right)};
      };
      const clipped = [...document.querySelectorAll('#home-view *')]
        .filter(e => e.scrollWidth > e.clientWidth + 1 && getComputedStyle(e).overflow !== 'visible')
        .map(e => String(e.className) + ' :: ' + e.innerText.slice(0, 20));
      return {
        text: document.querySelector('#home-view').innerText,
        docWidth: document.documentElement.scrollWidth,
        appScrollHeight: app.scrollHeight,
        appHeight: app.clientHeight,
        noScroll: app.scrollHeight <= app.clientHeight,
        rects: {
          thesis: rect('.home-thesis'),
          pair: rect('.time-pair'),
          window: rect('.connection-panel'),
          note: rect('.note-section'),
          reply: rect('.note-compose'),
          tabbar: rect('.tabbar'),
        },
        clipped,
      };
    });
    results.push(Object.assign({label: label, slug: slug}, evidence));
    await page.setViewportSize({width: 1200, height: 900});
    await page.waitForTimeout(250);
  }
  fs.writeFileSync(path.join(outDir, 'audit.json'), JSON.stringify({results: results, errors: errors}, null, 2));
  for (const r of results) {
    const replyBottom = r.rects.reply ? r.rects.reply.bottom : 'n/a';
    const tabbarTop = r.rects.tabbar ? r.rects.tabbar.top : 'n/a';
    console.log(r.slug + ' | ' + r.label + ' | noScroll=' + r.noScroll + ' | overflowX=' + (r.docWidth > 375) + ' | replyBottom=' + replyBottom + ' tabbarTop=' + tabbarTop + ' | clipped=' + r.clipped.length);
  }
  console.log('errors:', errors.length);
  await browser.close();
})();
