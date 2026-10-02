// Capture the day view of a given source under a given demo scenario.
const {chromium} = require('C:/Users/MUDUO/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/playwright');
const path = require('node:path');
const [srcDir, outDir, slug, scenarioIndex] = process.argv.slice(2);

(async () => {
  const browser = await chromium.launch({headless: true});
  const context = await browser.newContext({viewport: {width: 1200, height: 900}, deviceScaleFactor: 1, locale: 'zh-CN', timezoneId: 'Asia/Shanghai'});
  const page = await context.newPage();
  const errors = [];
  page.on('pageerror', e => errors.push(String(e.message)));
  await page.route('**/*', r => r.continue({headers: {...r.request().headers(), 'cache-control': 'no-cache'}}));
  await page.goto('http://127.0.0.1:8765/' + srcDir.split(path.sep).join('/') + '/?t=' + Date.now(), {waitUntil: 'networkidle'});
  await page.reload({waitUntil: 'networkidle'});
  await page.evaluate(() => document.fonts.ready);

  const buttons = await page.locator('#desktop-scenarios .scenario-button').all();
  const idx = Number(scenarioIndex) - 1;
  const label = (await buttons[idx].innerText()).replace(/\n/g, ' ').trim();
  await buttons[idx].click();
  await page.waitForTimeout(300);
  await page.setViewportSize({width: 375, height: 812});
  await page.waitForTimeout(300);
  await page.locator('[data-tab="day"]').click();
  await page.waitForTimeout(500);
  await page.screenshot({path: path.join(outDir, slug + '.png')});

  const ev = await page.evaluate(() => {
    const app = document.querySelector('#app');
    return {
      summary: document.querySelector('.window-summary').innerText.replace(/\n/g, ' '),
      summaryDisabled: document.querySelector('.window-summary').disabled,
      docWidth: document.documentElement.scrollWidth,
      appScrollHeight: app.scrollHeight,
    };
  });
  console.log(slug + ' | scenario=' + label + ' | ' + JSON.stringify(ev) + ' | errors:' + errors.length);
  await browser.close();
})();
