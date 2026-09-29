# NowUs Prototype V2 Implementation Plan

> Execute inline in the current authorized prototype iteration. No delegation is needed. Track steps with checkboxes.

**Goal:** Improve the six approved review findings in the existing static prototype.

**Architecture:** Extend the pure time model with optional own rhythms, future-window lookup, independent activity segments and explicit unknown contact plans. Keep view rendering in app.mjs and local form behavior in rhythm.mjs.

**Tech Stack:** Static HTML/CSS/ES modules, Node built-in test runner, local browser preview.

## Constraints

- Preserve the approved light sage and paired day/night direction.
- Routine, willingness and actual availability remain distinct.
- User notes and rhythms stay local; unknown data never yields a window.
- Use proportional time positions; independent activity segments never repeat just because the other person changes.
- Sleep and contact ranges may wrap midnight; main work/class hours must be same-day and must not overlap sleep.

## Task 1: Extend time behavior

- [x] Add tests for continuous activities, unknown partner data, next-day windows and edited weekday/rest rhythm including midnight contact ranges.
- [x] Run the tests to observe missing behavior fail.
- [x] Implement optional rhythm lookup, validation, independent activity segments and next-window search.
- [x] Pass the complete model test suite.

## Task 2: Update the home and timeline

- [x] Compress the paired clocks and add a focusable partner-note preview with an expanded dialog.
- [x] Use source-qualified contact labels, explain contact during busy activities, and display next-day or missing-data states.
- [x] Render continuous independent columns with a nearby/full-day switch and sticky date/people/tools region.

## Task 3: Add my rhythm

- [x] Add the third navigation destination and a weekday/rest template selector.
- [x] Render sleep, day activity and contact fields with explicit unknown/off-day options.
- [x] Validate on changes and submit; persist only valid drafts; update both other pages on save.

## Task 4: Verify and deliver

- [x] Test model behavior and JavaScript syntax; run the Finesse detector and Git whitespace checks.
- [x] Verify browser scenarios, timeline continuity, form errors/save/reload, keyboard focus and widths 320/375/414.
- [x] Update README/review record, local Finesse log and screenshots, then commit and push the approved iteration.
