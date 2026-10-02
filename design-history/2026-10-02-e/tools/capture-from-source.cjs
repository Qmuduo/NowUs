const {chromium} = require('C:/Users/MUDUO/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/playwright');
const path = require('node:path');
const [srcDir, outDir] = process.argv.slice(2);
(async () => {
  const browser = await chromium.launch({headless: true});
  const context = await browser.newContext({viewport: {width: 375, height: 812}, deviceScaleFactor: 1, locale: 'zh-CN', timezoneId: 'Asia/Shanghai'});
  const page = await context.newPage();
  const errors = [];
  page.on('pageerror', e => errors.push(String(e.message)));
  await page.route('**/*', r => r.continue({headers: {...r.request().headers(), 'cache-control': 'no-cache'}}));
  const url = 'http://127.0.0.1:8765/' + srcDir.split(path.sep).join('/') + '/?t=' + Date.now();
  await page.goto(url, {waitUntil: 'networkidle'});
  await page.reload({waitUntil: 'networkidle'});
  await page.evaluate(() => document.fonts.ready);
  await page.waitForTimeout(250);
  await page.screenshot({path: path.join(outDir, 'home.png')});
  console.log('captured from', srcDir, '| errors:', errors.length);
  await browser.close();
})();
