# Task 2 — Native UI and ViewModel report

Scope: Kotlin Compose UI and application ViewModel. The controller owns DataStore, Gradle configuration, theme/format helpers, and their verification.

## Files and interface

- `android/app/src/main/java/app/nowus/android/MainActivity.kt`: edge-to-edge activity, DataStore wiring, ViewModel factory, real-time refresh inside `repeatOnLifecycle(STARTED)` only.
- `AppViewModel.kt`: repository collection/retry, nullable loading state, visible load/save/validation errors, successful-write callbacks, profile/schedule/note/invitation/temporary events. Invitation creation/acceptance and temporary persistence use real `Instant.now()`. Fixed demo clock remains in ViewModel memory; UI disables temporary editing while fixed time is active.
- `ui/NowUsApp.kt`: four-step onboarding, independent-use path, three tabs, compact paired clocks with font-scale fallback, real offset/date/source labels, explicit unknown-to-sample transition, seven-day windows off the main thread, one local Unicode-limited note, editable profile and weekday/rest templates, modal temporary contact choices.
- `ui/Timeline.kt`: continuous independent activity tracks positioned by a shared Instant coordinate, local-date navigation, nearby/full-day modes, local-hour ticks with both dates/times, current indicator, visible common-window center stripe, accessible complete segment details below and in dialogs.
- Unit tests `AppViewModelTest.kt`: failed write cannot invoke success or mutate data; retry succeeds; failed loading can retry; temporary timing uses real clock; wrong invite preserves state; accepted partner stays unknown until explicit sample.
- Instrumentation `AppFlowTest.kt`: first-use solo flow, explicit sample + saved note, failed note retains draft, temporary set/reset, invitation wrong/revoked/new acceptance, screenshots, fontScale 2 reachability.

`StateRepository.states: Flow<AppState>` and `suspend update(transform)` are the only persistence interface used by the UI layer. A failed write retains the open editor and its draft; dismissing a dialog also retains its draft for reopening.

## Test evidence

Controller observed meaningful RED before implementation: compiled stub ViewModel ran direct JUnit, two tests failed two assertions (`android/.gradle/vm-direct-red.log`). Additional regression and instrumentation tests followed. Gradle `testDebugUnitTest` XML confirms 4/4 ViewModel tests passing at 2026-09-29T14:50:31Z (`TEST-app.nowus.android.AppViewModelTest.xml`); this whole run had one unrelated repository test failure under controller ownership. The controller's `android/.gradle/final-build-checks.log` records `testDebugUnitTest`, `assembleDebug`, and `lintDebug` successful on the current UI source (80 actionable tasks, BUILD SUCCESSFUL). The controller's direct device run `android/.gradle/native-final-suite.log` reports `OK (9 tests)` after the review fix, covering eight Compose flows plus the Android DataStore test.

## Native captures

Instrumentation writes PNGs to the target app's external cache directory: `nowus-home.png`, `nowus-timeline.png`, and `nowus-large-font.png`. Screens use an injected in-memory repository and do not replace installed-app user data. The controller can pull these to ignored artifacts after execution.

## Boundaries

Everything is explicitly local. Invitation codes create no network messages, accepted profiles simulate a partner, and supplied example rhythms are only inserted through an explicit action. No real account, real partner state, or synchronization is represented. Future windows are computed from templates and bounded temporary intervals, not predictions of an unknown partner's availability.


## Review follow-up and visual QA

A post-emission repository read failure now exposes `readFailed` and a retry action in both the loaded shell and partial onboarding. The new `loadedReadFailureOffersRetry` native regression first failed as expected because `重试加载` was absent (`android/.gradle/loaded-read-red.log`); implementation followed. Tab contents now retain their own saveable scroll/editor state. Timeline hour and current horizontal rules sit exactly at the segment coordinate, with the current label displayed above the chart to avoid covering an hour/date tick. Dual local date/time labels remain in left and right gutters. At large fonts, temporary durations wrap and date navigation has its own row.

Native screenshots captured by instrumentation and pulled to `artifacts/nowus-home.png`, `artifacts/nowus-timeline.png`, and `artifacts/nowus-large-font.png` were visually inspected. The initial timeline capture showed the 22:00/current label collision, addressed in the source follow-up above. The initial connected suite had 8 passing tests and the expected loaded-read RED failure. Final connected suite passed 9/9. The PNGs currently under `artifacts/` were pulled from the earlier pre-review run; refresh them from target external cache after final suite before using them in the handoff.
