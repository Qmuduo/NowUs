// Audit the two secondary views (我们的一天 / 我的节奏) across every demo scenario,
// and report how many shared windows the data actually contains.
const {chromium} = require('C:/Users/MUDUO/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/playwright');
const fs = require('node:fs');
const path = require('node:path');

const outDir = path.resolve(__dirname, '..', 'view-audit');
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
    await page.waitForTimeout(300);

    const perView = {};
    for (const tab of ['day', 'rhythm']) {
      await page.locator('[data-tab="' + tab + '"]').click();
      await page.waitForTimeout(400);
      const view = '#' + tab + '-view';
      await page.screenshot({path: path.join(outDir, 'scenario-' + (i + 1) + '-' + tab + '.png')});
      perView[tab] = await page.evaluate(sel => {
        const app = document.querySelector('#app');
        const root = document.querySelector(sel);
        const clipped = [...root.querySelectorAll('*')]
          .filter(e => e.scrollWidth > e.clientWidth + 1 && getComputedStyle(e).overflow !== 'visible')
          .map(e => String(e.className).slice(0, 40) + ' :: ' + (e.innerText || '').slice(0, 20));
        return {
          text: root.innerText,
          docWidth: document.documentElement.scrollWidth,
          appScrollHeight: app.scrollHeight,
          appHeight: app.clientHeight,
          viewScrollHeight: root.scrollHeight,
          clipped: clipped,
          windowSummary: (() => { const w = document.querySelector('.window-summary'); return w ? w.innerText.replace(/\n/g, ' ') : null; })(),
        };
      }, view);
      await page.locator('[data-tab="home"]').click();
      await page.waitForTimeout(250);
    }
    results.push({label: label, scenario: i + 1, views: perView});
    await page.setViewportSize({width: 1200, height: 900});
    await page.waitForTimeout(200);
  }
  fs.writeFileSync(path.join(outDir, 'view-audit.json'), JSON.stringify({results: results, errors: errors}, null, 2));
  for (const r of results) {
    for (const tab of ['day', 'rhythm']) {
      const v = r.views[tab];
      console.log('scenario-' + r.scenario + ' ' + r.label + ' | ' + tab + ' | overflowX=' + (v.docWidth > 375) + ' | appScroll=' + v.appScrollHeight + ' | viewScroll=' + v.viewScrollHeight + ' | clipped=' + v.clipped.length + (v.windowSummary ? ' | windowSummary=' + v.windowSummary : ''));
    }
  }
  console.log('errors:', errors.length);
  await browser.close();
})();
