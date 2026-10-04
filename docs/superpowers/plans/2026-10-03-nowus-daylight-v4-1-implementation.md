# NowUs Daylight v4.1 Android Implementation Plan

## Objective

Implement the current Android app natively in Kotlin and Jetpack Compose using `design-proposals/daylight/` v4.1 as its sole visual and interaction baseline. Preserve existing authenticated backend, local mode, account isolation, schedule engine, invitations, and note synchronization. All changes stay in the managed worktree containing a copy of the original checkout’s current tracked and untracked work.

## Global Constraints

- Use only current `design-proposals/daylight/` v4.1 for colors, paper surfaces, logo, icon paths, screen order, and interaction patterns. Do not revive earlier proposals, old icons, or prototype mock data.
- Keep displayed time, city, weekday, schedule, pairing, permissions, note data, and empty/loading/error states driven by real app state. Preserve authenticated/offline/local-experience behavior.
- Do not regress routine categories, templates, validation, overnight segments, invitations, permission scopes, pause-sharing, unpair, email OTP, or pending invite handling.
- Notes persist as a single current note. Drafts persist per account and per mode across navigation and normal restart; drafts never leak between accounts or authenticated/local modes. Enforce 120 Unicode code points, preserve drafts on failure, and clear only after successful save.
- Note deletion requires confirmation and undo. An undo must be conditional on the deleted revision and must never overwrite a later note. Surface a conflict when the server state has advanced.
- Do not introduce WebView-based production screens, fixed sample people/dates/codes, location permission, cloud font downloads, deployment, or new call/chat features.
- Supply v4.1 launcher adaptive/round/monochrome resources and native splash. Use platform fallbacks for CJK sans and serif; no runtime downloads.
- Do not modify the original checkout or erase historical design artifacts. Update README/acceptance records to identify v4.1 as current while retaining historical records.
- TDD: add focused regression tests, observe the expected failure, then implement. User explicitly requested verification, so run relevant tests/builds and device checks.

## Tasks

### Task 1: Persist drafts and make note deletion/undo revision-safe

Inspect Android local/account repositories, API and backend SQL/router conventions. Extend local state for per-account/per-mode drafts, preserving them through account refresh and encrypted cache without sending draft text to the service. Add note revisions to the synchronized model. Implement server-side conditional soft-delete and restore tied to the deleted revision, including migration and tests proving a stale undo cannot replace a newer note. Update ViewModel and repository tests for save/delete/restore, input failures, >120 Unicode code point rejection, and draft isolation. Keep delete as an explicit operation; do not implement undo as a generic upsert.

### Task 2: Add independently aligned schedule contact rails

Add domain coverage and a real interval calculation for each person’s contact preference, respecting local timezone, activity category, temporary status, unknown schedule, and cross-midnight boundaries. Render separate muted-green rails in the native day timeline while preserving the same time axis and existing schedule segments. Test person-specific rails and edge cases.

### Task 3: Migrate the Compose visual system and primary flows to v4.1

Implement v4.1 color/type/shape/elevation tokens and native components. Make 此刻 / 一天 / 留话 the primary native navigation. Rework the existing home, timeline, note, rhythm, profile/settings, pairing, and about destinations to match the v4.1 files. Keep the current ViewModels and working service actions. Use a persistent draft editor, confirmation before delete, and revision-safe undo with an accessible snackbar. Implement no-fake-data unpaired/loading/offline/unknown states. Ensure real system bars, keyboard, safe areas, back behavior, semantics and touch targets.

### Task 4: Migrate auth/local experience and v4.1 branding resources

Restyle existing email OTP, onboarding and local experience using the same v4.1 tokens while preserving each action. Derive Compose vectors and launcher icon layers from the provided v4.1 SVG/XML path assets. Add adaptive, round and monochrome icon declarations, Android 12+ splash and system bar colors. Verify no old app icon or prior visual system remains in the active app entry flows.

### Task 5: Run integrated verification and update delivery records

Run relevant Android unit, repository, ViewModel, Compose and backend tests, design schedule tests, and debug APK assembly. Install the APK on the available emulator and one physical SM-F936N without clearing personal-device data. Exercise core navigation, schedule editing, temporary contact state, note draft/save/delete/undo and stale conflict, invitation states, profile, back/keyboard/system bars and accessibility sizing. Capture and inspect native screenshots at available device sizes against v4.1; state clearly that there is one physical phone, not two. Update Android README and acceptance evidence with build variant, version, APK path, device/app version, screenshots, tests and limitations.

## Review checklist

- Kotlin tests cover note revisions and drafts, separate contact intervals, and retained schedule/invitation behavior. Backend tests demonstrate stale restore conflict.
- APK builds and launches on emulator and phone; personal-device app data is not cleared.
- Screenshots reviewed for 此刻 / 一天 / 留话, rhythm, profile and pairing, plus long text/font and keyboard/system-bar behavior where supported.
- Final record identifies exact APK and validation evidence and does not claim unsupported device coverage.

## Execution record (2026-10-04)

Tasks 1–4 were implemented in the protected worktree. Task 5's local verification and delivery record are complete: 62 Android unit tests and 27 connected Android tests passed; the large-font home/editor flow passed at 320/360/390/412dp; all 7 daylight schedule tests passed; the backend suite passed 33 tests and its local A/B/C HTTP + Mailpit acceptance passed. The debug APK was rebuilt, signature-verified, installed and launched on the API 36 AVD. See [the current acceptance report](../../acceptance/daylight-v4-1/README.md) for the APK hash, screenshot set and commands.

The final run had one API 36 emulator attached and no physical phone. Two-physical-device UI verification, production HTTPS/SMTP, deployment-region checks, and a manual TalkBack audit remain external acceptance items; none are reported as complete here.
