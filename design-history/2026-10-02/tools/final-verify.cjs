// Final verification of the restored prototype (round 88) against the fixed usage path.
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
  report.noHorizontalOverflow = await page.evaluate(() => document.documentElement.scrollWidth <= 375);

  // 1. partner's and own local time + status are readable
  report.timeReadings = await page.evaluate(() => [...document.querySelectorAll('.time-person')].map(el => ({
    who: el.querySelector('.person-label').innerText.replace(/\n/g, ' '),
    time: el.querySelector('.local-time').innerText,
    status: (el.querySelector('.partner-state-current, .time-weather') || el).innerText.replace(/\n/g, ' '),
  })));

  // 2. the shared window is stated once, with the estimate disclaimer kept apart from it
  report.window = await page.evaluate(() => ({
    label: document.querySelector('.common-label').innerText,
    headline: document.querySelector('.common-title').innerText.replace(/\n/g, ' '),
    ranges: [...document.querySelectorAll('.local-window')].map(el => el.innerText.replace(/\n/g, ' ')),
    source: document.querySelector('.common-footer, .foot-note') ? document.querySelector('.common-footer, .foot-note').innerText.replace(/\n/g, ' ') : null,
  }));

  // 3. reply entry: touch target size and gap above the tab bar
  report.reply = await page.evaluate(() => {
    const reply = document.querySelector('.note-compose').getBoundingClientRect();
    const tabbar = document.querySelector('.tabbar').getBoundingClientRect();
    return {width: Math.round(reply.width), height: Math.round(reply.height), gapToTabbar: Math.round(tabbar.top - reply.bottom)};
  });
  report.note = await page.evaluate(() => ({
    author: document.querySelector('.note-author').innerText,
    time: document.querySelector('.note-preview-head time').innerText,
    text: document.querySelector('.note-preview-text').innerText.replace(/\n/g, ' '),
    replyHint: document.querySelector('.note-compose-placeholder').innerText,
  }));

  // 4. reply opens a composer
  await page.locator('.note-compose').click();
  await page.waitForTimeout(250);
  report.replySheet = await page.locator('#sheet').innerText();
  report.replyHasTextarea = await page.locator('#sheet textarea').count() > 0;
  await page.locator('#close-sheet').click();
  await page.waitForTimeout(200);

  // 5. window detail opens from the shared-window area
  await page.locator('[data-action="window-detail"]').click();
  await page.waitForTimeout(250);
  report.windowSheet = await page.locator('#sheet').innerText();
  await page.locator('#close-sheet').click();
  await page.waitForTimeout(200);

  // 6. other tabs still render
  for (const tab of ['day', 'rhythm']) {
    await page.locator(`[data-tab="${tab}"]`).click();
    await page.waitForTimeout(350);
    const view = `#${tab}-view`;
    report[tab] = {
      visible: await page.locator(view).isVisible(),
      heading: (await page.locator(`${view} h1, ${view} h2`).first().innerText().catch(() => '')).slice(0, 40),
      height: await page.evaluate(sel => document.querySelector(sel).scrollHeight, view),
      overflowX: await page.evaluate(() => document.documentElement.scrollWidth > 375),
    };
  }
  await page.locator('[data-tab="home"]').click();
  await page.waitForTimeout(250);

  report.errors = errors;
  fs.writeFileSync(path.resolve(__dirname, 'final-verification.json'), JSON.stringify(report, null, 2));
  console.log(JSON.stringify(report, null, 2));
  await browser.close();
})();
