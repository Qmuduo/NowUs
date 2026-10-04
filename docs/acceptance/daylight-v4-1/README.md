# NowUs Daylight v4.1 Android acceptance

Date: 2026-10-04. The sole current visual and interaction baseline is [`design-proposals/daylight/`](../../../design-proposals/daylight/), v4.1. This record covers the native Android implementation in the protected worktree; the original checkout and its pre-existing work were left intact.

## Implementation

The app uses Kotlin and Jetpack Compose, not a WebView. 此刻, 一天, 留话, 我的节奏, the profile and sharing surfaces, invite/pairing, onboarding, email OTP and local experience now use the v4.1 palette, paper note, brand mark, tab icons and native layouts. Existing IANA time calculations, independent contact preferences, account sync and access controls remain in the domain/data layers. Note drafts are account/mode scoped; save/delete/undo use persistent revisions and preserve input on failure. Local demo data stays separate from account data.

The active Android implementation is in `android/app/src/main/java/app/nowus/android/`; note soft-delete/restore is implemented by `backend/migrations/0003_note_soft_delete.sql` and the existing FastAPI notes routes. New Android launcher/adaptive/round/monochrome resources are in `android/app/src/main/res/` and are sourced from the daylight brand vectors.

## Installable APK

The installable LAN-specific APK was delivered locally at `artifacts/NowUs-Daylight-v4.1-LAN.apk`; it is a machine-specific build artifact and is not part of the source commit. Rebuild it for another LAN with `-PnowusApiBaseUrl=http://<LAN_IP>:8000`.

- Variant: installable debug build; debug-key signed (APK Signature Scheme v2 verified).
- App ID: `app.nowus.android.debug`; version code `410`; version name `4.1.0-local`.
- `minSdk 26`, `targetSdk 36`; APK size 13,614,969 bytes.
- SHA-256: `1F65D68D65A95D3470008077C51D7D7432053EC441B3869C10E21B053B22FA8F`.
- The delivered APK uses the development computer's current LAN IPv4 address on port 8000. Keep the backend running and connect the phone to the same LAN.
- The backend health check returned `{"status":"ok"}`. The APK installed and launched on the Android 36 emulator, which reached the configured host on TCP port 8000. No physical phone was attached for a handset-to-server check.

Install the delivered APK with `adb install -r artifacts/NowUs-Daylight-v4.1-LAN.apk`, then launch `app.nowus.android.debug/app.nowus.android.MainActivity`.

## Verification performed

- `:app:testDebugUnitTest`: **62 passed, 0 failed**.
- `:app:connectedDebugAndroidTest`: **35 passed, 0 failed, 0 skipped** on one Android 36 emulator (`phase15-api36`, AVD). The suite includes native navigation, routine editing, temporary state, unknown partner state, note draft/save/delete/undo, account separation, pending invite handling, signed-in invite UI, save/read failure behavior and revocation. A real OTP device probe used the local development API and Mailpit.
- Large-font Compose flow (`fontScale=2.0`) passed at **320, 360, 390 and 412dp** logical widths on the 480dpi emulator. The same path checks both clock cards, note entry, input and closing the editor. A separate native UI case renders two 20-character names and a 120-code-point mixed Chinese/emoji note; the name displays with an ellipsis and the full note remains scrollable.
- `node --test design-proposals/daylight/schedule.test.cjs`: **7 passed**. These cover only the design's schedule example, not Android or backend acceptance.
- Backend test suite: **33 passed** against the local PostgreSQL test setup during this implementation. The current local HTTP + Mailpit `python backend/scripts/acceptance.py` run also passed A/B/C OTP login, invite preview/accept, two-way schedule/note sync, third-account denial, sharing pause/resume and unpair revocation.
- The final APK was installed and launched in the emulator. `aapt` reports the v4.1 adaptive icon resource; the actual Android activity shows the new wordmark and readable dark system-bar icons. The email field was focused with the real Android keyboard visible and the system Back key dismissed the keyboard without leaving the form.

## Screenshots

The page screenshots below were captured from native Compose instrumentation on the emulator. The auth and keyboard screenshots were captured from the installed APK's actual `MainActivity`. The screenshot set was manually reviewed alongside the current v4.1 design pages and brand assets; this was visual inspection, not a pixel-diff score. Compose-test host system-bar icons differ from the installed activity, so use the auth screenshots for system-bar evidence.

| Surface or state | Capture |
| --- | --- |
| 此刻, paired clocks and note entry | [nowus-home.png](screenshots/nowus-home.png) |
| 我的, profile and sharing settings | [nowus-profile-settings.png](screenshots/nowus-profile-settings.png) |
| 一天, shared time axis and separate contact indicators | [nowus-timeline.png](screenshots/nowus-timeline.png) |
| 留话, partner paper note | [nowus-note.png](screenshots/nowus-note.png) |
| Own note below the partner note | [nowus-own-note.png](screenshots/nowus-own-note.png) |
| Native note editor | [nowus-note-editor.png](screenshots/nowus-note-editor.png) |
| Long-name home and 120-code-point note | [nowus-long-profile.png](screenshots/nowus-long-profile.png), [nowus-long-note.png](screenshots/nowus-long-note.png) |
| Temporary contact preference sheet | [nowus-status.png](screenshots/nowus-status.png) |
| Rhythm editor and routine template confirmation | [nowus-rhythm.png](screenshots/nowus-rhythm.png), [nowus-template-confirm.png](screenshots/nowus-template-confirm.png) |
| Signed-in invitation ticket route (instrumented account API fixture) | [nowus-invite.png](screenshots/nowus-invite.png) |
| Local-only invitation entry | [nowus-local-invite.png](screenshots/nowus-local-invite.png) |
| Actual sign-in screen, keyboard and system Back | [nowus-auth-entry.png](screenshots/nowus-auth-entry.png), [nowus-auth-keyboard.png](screenshots/nowus-auth-keyboard.png), [nowus-auth-back.png](screenshots/nowus-auth-back.png) |
| Launcher app drawer and adaptive icon mask | [nowus-launcher.png](screenshots/nowus-launcher.png) |

## Limits

- Only one Android 36 emulator was attached for this final run. No physical phone or second Android device was attached, so this record does not claim a two-device Android UI acceptance. The A/B/C test exercised three independent accounts through the local HTTP backend.
- OTP and pairing were checked against the local development API and Mailpit, not a production domain or an external mailbox. Production HTTPS, SMTP deliverability, Mainland China/overseas connectivity and a production-signed release build remain unverified.
- The invitation-page screenshot uses a test account API fixture. The separate A/B/C API acceptance used the running local service and real development OTP messages.
- TalkBack was not manually audited. The UI has Compose semantics for primary navigation, profile/status, note actions, schedule details and the long-note accessibility description; a dedicated screen-reader and touch-target audit remains outstanding.
