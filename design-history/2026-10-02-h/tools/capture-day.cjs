const {chromium} = require('C:/Users/MUDUO/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/playwright');
const path = require('node:path');
const [srcDir, outDir, slug] = process.argv.slice(2);
(async () => {
  const browser = await chromium.launch({headless: true});
  const context = await browser.newContext({viewport: {width: 375, height: 812}, deviceScaleFactor: 1, locale: 'zh-CN', timezoneId: 'Asia/Shanghai'});
  const page = await context.newPage();
  const errors = [];
  page.on('pageerror', e => errors.push(String(e.message)));
  await page.route('**/*', r => r.continue({headers: {...r.request().headers(), 'cache-control': 'no-cache'}}));
  await page.goto('http://127.0.0.1:8765/' + srcDir.split(path.sep).join('/') + '/?t=' + Date.now(), {waitUntil: 'networkidle'});
  await page.reload({waitUntil: 'networkidle'});
  await page.evaluate(() => document.fonts.ready);
  await page.locator('[data-tab="day"]').click();
  await page.waitForTimeout(500);
  await page.screenshot({path: path.join(outDir, slug + '.png')});
  const ev = await page.evaluate(() => {
    const g = s => { const e = document.querySelector(s); if (!e) return null; const b = e.getBoundingClientRect(); return [Math.round(b.left), Math.round(b.right)]; };
    return {people: document.querySelector('.timeline-people').innerText.replace(/\n/g, ' | '), summary: document.querySelector('.window-summary').innerText.replace(/\n/g, ' '), me: g('.activity-column.me'), partner: g('.activity-column.partner'), docWidth: document.documentElement.scrollWidth};
  });
  console.log(slug + ' | ' + JSON.stringify(ev) + ' | errors:' + errors.length);
  await browser.close();
})();
