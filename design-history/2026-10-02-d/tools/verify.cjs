// Walk the fixed usage path on the current prototype and report what a user would meet.
// Selectors are tolerant so the same script can verify every round of this batch.
const {chromium} = require('C:/Users/MUDUO/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/playwright');
const fs = require('node:fs');
const path = require('node:path');

(async () => {
  const browser = await chromium.launch({headless: true});
  const context = await browser.newContext({viewport: {width: 375, height: 812}, deviceScaleFactor: 1, locale: 'zh-CN', timezoneId: 'Asia/Shanghai'});
  const page = await context.newPage();
  const errors = [];
  page.on('pageerror', e => errors.push(String(e.message)));
  page.on('console', m => { if (m.type() === 'error') errors.push('console: ' + m.text()); });
  await page.goto('http://127.0.0.1:8765/prototype/?verify=' + Date.now(), {waitUntil: 'networkidle'});

  const report = {};
  report.homeText = await page.locator('#home-view').innerText();
  report.metrics = await page.evaluate(() => {
    const app = document.querySelector('#app');
    const tabbar = document.querySelector('.tabbar').getBoundingClientRect();
    const reply = document.querySelector('.reply-field, .note-compose');
    const r = reply.getBoundingClientRect();
    const hint = document.querySelector('.reply-hint, .note-compose-placeholder');
    const note = document.querySelector('.note-text, .note-preview-text');
    const author = document.querySelector('.note-author');
    return {
      docWidth: document.documentElement.scrollWidth,
      appScrollHeight: app.scrollHeight,
      appHeight: app.clientHeight,
      noScroll: app.scrollHeight <= app.clientHeight,
      reply: {width: Math.round(r.width), height: Math.round(r.height), gapToTabbar: Math.round(tabbar.top - r.bottom)},
      replyHint: hint ? hint.innerText : null,
      noteText: note ? note.innerText : null,
      noteAuthor: author ? author.innerText : null,
    };
  });

  await page.locator('.reply-field, .note-compose').click();
  await page.waitForTimeout(250);
  report.replySheetTitle = await page.locator('#sheet-title').innerText();
  report.replyHasTextarea = await page.locator('#sheet textarea').count() > 0;
  await page.locator('#close-sheet').click();
  await page.waitForTimeout(200);

  const detail = page.locator('[data-action="window-detail"], .foot-action, .band-action');
  if (await detail.count()) {
    await detail.first().click();
    await page.waitForTimeout(250);
    report.windowSheetTitle = await page.locator('#sheet-title').innerText();
    await page.locator('#close-sheet').click();
    await page.waitForTimeout(200);
  }

  for (const tab of ['day', 'rhythm']) {
    await page.locator(`[data-tab="${tab}"]`).click();
    await page.waitForTimeout(350);
    const view = `#${tab}-view`;
    report[tab] = {
      visible: await page.locator(view).isVisible(),
      heading: (await page.locator(`${view} h1, ${view} h2`).first().innerText().catch(() => '')).slice(0, 30),
      overflowX: await page.evaluate(() => document.documentElement.scrollWidth > 375),
    };
  }
  await page.locator('[data-tab="home"]').click();
  await page.waitForTimeout(200);

  report.errors = errors;
  fs.writeFileSync(path.resolve(__dirname, 'selfcheck-verification.json'), JSON.stringify(report, null, 2));
  console.log(JSON.stringify(report, null, 2));
  await browser.close();
})();
