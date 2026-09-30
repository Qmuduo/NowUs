# NowUs Android

Android first release candidate: Kotlin + Jetpack Compose, minimum Android 8.0 (API 26). The app has two deliberately separate entry paths:

- **Email account** uses the FastAPI service for six-digit email OTP, pairing, and server-synced personal data.
- **Local experience** keeps demo people, demo invitations, and example schedules in a separate local DataStore. Nothing is uploaded from that path. Importing the signed-in user's own local profile, schedule, and note requires an explicit review and confirmation.

The local environment proves the feature flow only. It does not prove that a deployed service is reachable from Mainland China or a particular overseas network, or that production email reaches inboxes.

## Start the local server

From the repository root, copy the development template and start PostgreSQL, Mailpit, and the API:

```powershell
Copy-Item .env.example .env
docker compose up --build -d db mailpit api
docker compose ps
```

The API applies versioned SQL migrations in `backend/migrations/` on startup. The first launch applies `0001_initial.sql` and `0002_setup_complete.sql`. PostgreSQL data stays in the `nowus-postgres` Docker volume. Check `http://localhost:8000/health`; Mailpit's local inbox is `http://localhost:8025` and accepts SMTP on port 1025. The local-only values in `.env.example` are not deployment secrets.

Published development ports bind to `127.0.0.1` by default, including API, PostgreSQL, and Mailpit SMTP/inbox. To connect a physical Android phone on the same LAN, explicitly change `NOWUS_API_BIND_ADDRESS` in the private `.env` to `0.0.0.0`, restart Compose, and use the development computer's LAN address in `NOWUS_API_BASE_URL`. PostgreSQL and Mailpit remain loopback-only.

For live logs and shutdown:

```powershell
docker compose logs -f api
docker compose down
```

`docker compose down` keeps the database volume. To reset local accounts and mail data, use `docker compose down -v` (this deletes the local development database).

### API and A/B/C acceptance

Run the isolated backend tests in a disposable PostgreSQL test container:

```powershell
docker compose --profile test run --rm tests
```

With the API and Mailpit running, this script requests real random OTPs from the service, reads the messages from the local Mailpit API, and checks two-way schedule/note sharing, invitation preview/accept, C's denied access, pause/resume, and unpair revocation:

```powershell
python backend/scripts/acceptance.py
```

The test inbox is intentionally local. The server does not expose a fixed OTP or a bypass login.

### Real SMTP configuration

For a private deployment environment, set `NOWUS_ENV=production`, `NOWUS_DEV_MAILBOX=false`, a unique `NOWUS_APP_SECRET` of at least 32 characters, `NOWUS_DATABASE_URL`, and the `NOWUS_SMTP_HOST`, `NOWUS_SMTP_PORT_INTERNAL`, `NOWUS_SMTP_FROM`, `NOWUS_SMTP_USERNAME`, and `NOWUS_SMTP_PASSWORD` values in a secret store or private `.env`. Set `NOWUS_SMTP_STARTTLS=true` for a submission service that supports STARTTLS. Never commit `.env` or credentials. Production also needs an HTTPS origin and real network/mailbox checks; Compose is only the single-machine development setup.

OTP requests expire after 10 minutes, can be resent after 60 seconds, allow five requests per email per hour and 30 requests per source IP per hour, and permit at most five verification attempts. A request accepted by the API is not proof the message reached an inbox.

## Android setup and build

Use JDK 17 or 21, Android SDK platform 36 and build-tools 36.0.0. Android Studio can open `android/`, or run the wrapper commands below. Set `android/local.properties` to the installed SDK and do not commit it:

```properties
sdk.dir=C\:/Android
```

The debug emulator build defaults to `http://10.0.2.2:8000`, which reaches the host API from the Android emulator. For a physical phone on the same LAN, point the app at the development computer's LAN address; the debug variant alone permits local HTTP:

```powershell
$env:NOWUS_API_BASE_URL = "http://192.168.1.20:8000"
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug
```

Replace the example LAN address with the address reachable from the phone. The release variant has no API URL unless `NOWUS_API_BASE_URL` or `-PnowusApiBaseUrl=...` is set; use HTTPS for a deployed server. Main/release manifest does not allow cleartext HTTP.

For a USB-connected physical phone using the host's loopback-only development services, build with `-PnowusApiBaseUrl=http://127.0.0.1:8000` and reverse the API port before starting the app:

```powershell
adb reverse tcp:8000 tcp:8000
```

The local email OTP is visible in the host browser at `http://127.0.0.1:8025`. The device-level API + Mailpit instrumentation probe also needs `adb reverse tcp:8025 tcp:8025`.

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest
.\gradlew.bat :app:connectedDebugAndroidTest
```

If the current network cannot complete TLS to `dl.google.com`, an optional Gradle init script uses Tencent/Aliyun mirrors (not the default dependency sources):

```powershell
.\gradlew.bat --init-script .\gradle-mirror.init.gradle :app:testDebugUnitTest :app:assembleDebug :app:connectedDebugAndroidTest
```

Install the debug APK with:

```powershell
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n app.nowus.android.debug/app.nowus.android.MainActivity
```

Invite links use `nowus://invite/{CODE}`. Android stores the pending code encrypted with an Android Keystore key so login or app restart does not lose the invitation context. The link is an invitation token, not a login credential; the app previews it after login and still requires explicit acceptance. A manual code entry is also available.

## Data and time model

- `domain/` keeps the existing Kotlin IANA timezone calculations, weekday/rest-day schedules, timeline, temporary contact preference, and common contact windows. New accounts start with a city-adjusted editable student template; an office-worker template is also available. Each routine block stores a category (sleep, preparation, meal, commute, study/work, rest, exercise, life admin, social/leisure, or other); the editor and timeline share one color palette and continue to display text labels. Unscheduled gaps remain unknown. Old blocks without a category decode as “other”. Exact clock times are starting suggestions, not claims about everyone in a city; details and evidence limits are in [the routine template note](../docs/superpowers/specs/2026-09-30-nowus-routine-templates.md).
- `backend/` owns accounts, OTPs, hashed sessions, invitations, pair membership, and each user's own profile, schedule, temporary preference, and current note.
- The server returns partner data only while both members are sharing and the current pair exists. Pause and unpair checks apply on each read. A/B synchronization refreshes while the app is open (30-second poll) and when a request succeeds; offline UI shows the last server timestamp.
- Unknown partner fields remain absent. There are no demo defaults sent to the server. The editor's suggested schedule is not uploaded until the user saves it.
- Android encrypts its session, pending invite, and last account snapshot with Android Keystore-backed AES-GCM. Account snapshots are tagged with the owning account ID.
- The development API and Mailpit are single-host tools without production availability, automated backups, or tested cross-border routing. iOS, HTTPS deployment, real SMTP deliverability, and two-physical-device acceptance remain outstanding.

## Verification completed for this branch

- `docker compose --profile test run --build --rm tests`: 27 backend tests passed against PostgreSQL, including optional routine-category compatibility and rejection cases.
- `python backend/scripts/acceptance.py`: A/B/C OTP and Mailpit flow passed, including two-way schedule/note visibility, C access denial, pause/resume, and unpair revocation.
- `:app:testDebugUnitTest`: 37 Android unit tests passed, including category serialization, template classification, and timeline propagation.
- `:app:connectedDebugAndroidTest` with `AppFlowTest`: all 11 Compose flow tests passed on the connected SM-F936N physical Android device (Android 11), including template apply/save and category edit/timeline rendering. A prior LAN API/Mailpit OTP probe is recorded in the preceding real-account implementation results.
- `:app:assembleDebug` builds the installable debug APK after the current changes. Only one physical Android device is connected; two-device acceptance remains outstanding.

Android builds were run with `--init-script .\gradle-mirror.init.gradle` because this development network failed TLS requests to Google Maven. This is a local dependency-fetch workaround; it is not a measurement of app connectivity from Mainland China or overseas. Two-physical-device acceptance remains outstanding.
