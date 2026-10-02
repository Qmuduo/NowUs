// Capture the home screen in the "own contact time set to unknown" state.
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

  await page.locator('[data-tab="rhythm"]').click();
  await page.waitForTimeout(400);
  await page.locator('#rhythm-view select').nth(1).selectOption({index: 1});
  await page.waitForTimeout(200);
  await page.locator('#rhythm-view button:has-text("保存我的节奏")').click();
  await page.waitForTimeout(700);
  await page.locator('[data-tab="home"]').click();
  await page.waitForTimeout(500);
  await page.screenshot({path: path.join(outDir, slug + '.png')});

  const ev = await page.evaluate(() => {
    const block = document.querySelector('.connection-panel');
    return {
      blockText: block ? block.innerText.replace(/\n+/g, ' | ') : null,
      docWidth: document.documentElement.scrollWidth,
      appScrollHeight: document.querySelector('#app').scrollHeight,
    };
  });
  console.log(slug + ' | ' + JSON.stringify(ev) + ' | errors:' + errors.length);
  await browser.close();
})();
