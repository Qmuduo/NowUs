# NowUs Interactive Prototype Implementation Plan

> **For agentic workers:** Use inline execution for this small prototype. Steps use checkbox syntax for tracking.

**Goal:** Deliver a clickable mobile prototype of the approved home, aligned timeline and short-note experience.

**Architecture:** Keep the prototype independent of a production app. A small pure module owns demo schedules, timezone formatting and common windows; the page renders those results and manages local interactions.

**Tech Stack:** Static HTML, CSS and browser JavaScript; built-in Node test runner for time logic. No remote assets or runtime packages.

## Global Constraints

- Follow the approved product and prototype-direction documents.
- Distinguish routine inference from active status; unknown does not mean available.
- Same timeline position means same real instant; retain both local dates.
- Phone-first frame, accessible controls, reduced-motion support, no real account/message/notification integrations.
- Notes are plain text; replacing, editing and deleting only affect the local demo.

## Task 1: Verify the time model

**Files:** `prototype/model.test.mjs`, `prototype/model.mjs`.

- [x] Write expectations for Beijing/New York date conversion, the default shared window, no-overlap schedules, active busy override and cross-midnight sleep.
- [x] Run `node --test prototype/model.test.mjs` and confirm missing behavior fails.
- [x] Implement timezone parts, local-day bounds, routine lookup and shared-window intersection.
- [x] Re-run the tests and verify the expected minutes and local dates.

## Task 2: Build the approved screens

**Files:** `prototype/index.html`, `prototype/styles.css`, `prototype/app.mjs`.

- [x] Build the phone shell, status bar, safe areas and two-tab navigation.
- [x] Render the paired day/night time panel, source-labelled life stages, shared window and current notes.
- [x] Render the aligned day timeline from actual schedule boundaries, including midnight and shared-window markers.
- [x] Add accessible bottom dialogs for note editing, active status, activities and common windows.

## Task 3: Verify interaction and layout

- [x] Run syntax checks and the installed Finesse detector on built files.
- [x] Serve the repository on loopback to preview the prototype and its linked documents and check the browser preview.
- [x] Check notes including literal markup, status changes, scenario changes and navigation.
- [x] Check phone widths 320, 375 and 414, short height, desktop framing, keyboard focus and reduced motion where the available browser supports it.
- [x] Fix failures before delivery; record any verification limitation accurately.

## Task 4: Deliver review materials

**Files:** `README.md`, `.gitignore`, `.finesse/log.json` (local only).

- [x] Add prototype instructions and clarify mock data and local-only actions.
- [x] Record the Finesse CSS stamp and local build history after verification.
- [x] Open the preview in Codex and provide a direct entry point with the main things to try.
