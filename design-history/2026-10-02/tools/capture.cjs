// Capture the fixed review screenshot for one round of the 2026-10-02 batch.
// Usage: node capture.cjs <outDir> [label]
const {chromium} = require('C:/Users/MUDUO/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/playwright');
const fs = require('node:fs');
const path = require('node:path');

const folder = path.resolve(__dirname, '..', process.argv[2]);
const label = process.argv[3] || path.basename(folder);
fs.mkdirSync(folder, {recursive: true});

(async () => {
  const browser = await chromium.launch({headless: true});
  // Fresh browser context per capture: no cache, no storage carried over.
  const context = await browser.newContext({
    viewport: {width: 375, height: 812},
    deviceScaleFactor: 1,
    locale: 'zh-CN',
    timezoneId: 'Asia/Shanghai',
  });
  const page = await context.newPage();
  const errors = [];
  page.on('pageerror', e => errors.push(String(e.message)));
  page.on('console', m => { if (m.type() === 'error') errors.push('console: ' + m.text()); });
  await page.route('**/*', route => route.continue({
    headers: {...route.request().headers(), 'cache-control': 'no-cache', 'pragma': 'no-cache'},
  }));
  await page.goto('http://127.0.0.1:8765/prototype/?capture=' + label + '-' + Date.now(), {waitUntil: 'networkidle'});
  await page.reload({waitUntil: 'networkidle'});
  await page.evaluate(() => document.fonts.ready);
  await page.waitForTimeout(300);
  await page.screenshot({path: path.join(folder, 'home.png')});

  const evidence = await page.evaluate(() => {
    const rect = s => {
      const el = document.querySelector(s);
      return el ? el.getBoundingClientRect().toJSON() : null;
    };
    const app = document.querySelector('#app');
    return {
      viewport: {width: innerWidth, height: innerHeight, dpr: devicePixelRatio, zoom: visualViewport.scale},
      styles: [...document.styleSheets].map(s => s.href),
      text: document.querySelector('#home-view').innerText,
      scroll: {width: document.documentElement.scrollWidth, appHeight: app.clientHeight, appScrollHeight: app.scrollHeight, appScrollTop: app.scrollTop},
      rects: ['.app-header', '.home-thesis', '.time-pair', '.connection-panel', '.note-section', '.tabbar', '.note-compose', '.common-card'].map(s => ({selector: s, rect: rect(s)})),
      fonts: [...new Set([...document.querySelectorAll('#home-view *')].map(el => getComputedStyle(el).fontFamily))],
    };
  });
  evidence.label = label;
  evidence.errors = errors;
  fs.writeFileSync(path.join(folder, 'capture.json'), JSON.stringify(evidence, null, 2));
  console.log(JSON.stringify({label, errors, scroll: evidence.scroll}));
  await browser.close();
})();
