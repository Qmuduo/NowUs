// Re-render every archived candidate from its own source snapshot and check that the
// archived screenshot still matches what that source produces.
const {chromium} = require('C:/Users/MUDUO/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/playwright');
const fs = require('node:fs');
const path = require('node:path');
const crypto = require('node:crypto');

const root = path.resolve(__dirname, '..');
const repo = path.resolve(root, '..', '..');
const rounds = ['baseline-108', 'round-110'];

(async () => {
  const browser = await chromium.launch({headless: true});
  const out = {};
  for (const round of rounds) {
    const dir = path.join(root, round);
    const candidate = path.join(dir, 'source', 'prototype');
    const entry = fs.existsSync(path.join(candidate, 'index.html')) ? candidate : path.join(root, 'baseline-88', 'source', 'prototype');
    const url = 'http://127.0.0.1:8765/' + path.relative(repo, entry).split(path.sep).join('/') + '/?recheck=' + Date.now();
    const context = await browser.newContext({viewport: {width: 375, height: 812}, deviceScaleFactor: 1, locale: 'zh-CN', timezoneId: 'Asia/Shanghai'});
    const page = await context.newPage();
    const errors = [];
    page.on('pageerror', e => errors.push(String(e.message)));
    await page.route('**/*', r => r.continue({headers: {...r.request().headers(), 'cache-control': 'no-cache'}}));
    await page.goto(url, {waitUntil: 'networkidle'});
    await page.reload({waitUntil: 'networkidle'});
    await page.evaluate(() => document.fonts.ready);
    await page.waitForTimeout(250);
    const buffer = await page.screenshot();
    const liveHash = crypto.createHash('sha256').update(buffer).digest('hex');
    const archived = path.join(dir, 'home.png');
    const archivedHash = fs.existsSync(archived) ? crypto.createHash('sha256').update(fs.readFileSync(archived)).digest('hex') : null;
    const evidence = await page.evaluate(() => {
      const app = document.querySelector('#app');
      return {
        text: document.querySelector('#home-view').innerText,
        scroll: {width: document.documentElement.scrollWidth, appHeight: app.clientHeight, appScrollHeight: app.scrollHeight},
      };
    });
    out[round] = {
      source: path.relative(repo, entry).split(path.sep).join('/'),
      renderedScreenshotSha256: liveHash,
      archivedScreenshotSha256: archivedHash,
      archiveMatchesRender: liveHash === archivedHash,
      viewport: {width: 375, height: 812, dpr: 1, zoom: 1},
      scroll: evidence.scroll,
      noHorizontalOverflow: evidence.scroll.width <= 375,
      text: evidence.text,
      errors,
    };
    await context.close();
  }
  fs.writeFileSync(path.join(root, 'render-evidence.json'), JSON.stringify(out, null, 2));
  for (const [k, v] of Object.entries(out)) console.log(k, 'archiveMatchesRender=' + v.archiveMatchesRender, 'overflow=' + !v.noHorizontalOverflow, 'errors=' + v.errors.length);
  await browser.close();
})();
