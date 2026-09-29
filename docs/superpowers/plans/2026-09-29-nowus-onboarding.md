# NowUs Onboarding Prototype Implementation Plan

> Execute inline in the current authorized prototype iteration. Track steps with checkboxes.

**Goal:** Deliver a clickable first-use/pairing flow and the real MVP acceptance checklist.

**Architecture:** Add a pure pairing-model.mjs for profile/invite state and an onboarding.mjs for local persistence and guided screens. Integrate its saved context into existing time, rhythm and note views; reuse rhythm form and preserve the separate original demo.

**Tech Stack:** Static HTML/CSS/ES modules, Node built-in tests, local browser verification.

## Constraints

- No real login, invitation transmission or server synchronization.
- Do not invent partner data while waiting; paired partner cities may drive clocks before schedules are filled.
- All profile values rendered in HTML are escaped; cities come from a controlled IANA-zone table.
- Contact willingness stays separate from activities. Local records are validated before restoration.
- Preserve existing design tokens and interaction accessibility.

## Tasks

- [x] Write failing tests for nickname/city validation, invite acceptance, expiration, revocation, repeat acceptance and record restoration.
- [x] Implement pure transitions and run the complete test suite.
- [x] Build city/profile, rhythm, invitation/receiver and success screens with exit/back paths.
- [x] Integrate selected cities, pending/paired/ready states and separately scoped notes into the app.
- [x] Write the real MVP feature and acceptance checklist with unresolved platform/authentication decisions explicit.
- [x] Verify the browser flow and 320/375/414 widths, syntax and Finesse checks, update screenshots/README, commit and push.
