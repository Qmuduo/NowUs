// Walk the fixed usage path on the current prototype and report what a user would meet.
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
  report.readings = await page.evaluate(() => [...document.querySelectorAll('.track-row')].map(row => ({
    who: row.querySelector('.track-head').innerText.replace(/\n/g, ' '),
    now: row.querySelector('.now-time').innerText,
    window: row.querySelector('.window-time') ? row.querySelector('.window-time').innerText : null,
    state: row.querySelector('.track-state').innerText,
  })));

  // 2. the shared window is drawn on both tracks at the same horizontal position
  report.alignment = await page.evaluate(() => {
    const dots = [...document.querySelectorAll('.track-now')].map(e => +e.getBoundingClientRect().left.toFixed(1));
    const segs = [...document.querySelectorAll('.track-window')].map(e => {
      const r = e.getBoundingClientRect();
      return [+r.left.toFixed(1), +r.right.toFixed(1)];
    });
    const band = document.querySelector('.track-board').getBoundingClientRect();
    return {
      head: document.querySelector('.board-when').innerText,
      nowMarkers: dots,
      windowSegments: segs,
      sameInstantDrawn: dots.length === 2 && Math.abs(dots[0] - dots[1]) < 0.5 && segs.length === 2 && Math.abs(segs[0][0] - segs[1][0]) < 0.5,
      insideBoard: segs.every(s => s[0] >= band.left && s[1] <= band.right),
      legend: document.querySelector('.foot-key').innerText.replace(/\n/g, ' '),
      source: document.querySelector('.foot-note').innerText,
    };
  });

  // 3. reply entry: touch target size and gap above the tab bar
  report.reply = await page.evaluate(() => {
    const reply = document.querySelector('.reply-field').getBoundingClientRect();
    const tabbar = document.querySelector('.tabbar').getBoundingClientRect();
    return {width: reply.width, height: reply.height, gapToTabbar: Math.round(tabbar.top - reply.bottom)};
  });
  report.note = await page.evaluate(() => ({
    author: document.querySelector('.note-author').innerText,
    time: document.querySelector('.note-time').innerText,
    text: document.querySelector('.note-text').innerText,
    replyHint: document.querySelector('.reply-hint').innerText,
  }));

  // 4. reply opens a composer
  await page.locator('.reply-field').click();
  await page.waitForTimeout(250);
  report.replySheet = await page.locator('#sheet').innerText();
  report.replyHasTextarea = await page.locator('#sheet textarea').count() > 0;
  await page.locator('#close-sheet').click();
  await page.waitForTimeout(200);

  // 5. window detail opens from the board foot
  await page.locator('.foot-action').click();
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
  fs.writeFileSync(path.resolve(__dirname, 'selfcheck-verification.json'), JSON.stringify(report, null, 2));
  console.log(JSON.stringify(report, null, 2));
  await browser.close();
})();
