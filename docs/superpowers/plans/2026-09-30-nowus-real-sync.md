# NowUs Real Account Sync Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Deliver a locally runnable Android + API flow for OTP login, two-user pairing, owned profile/rhythm/note sync, and server-enforced access revocation.

**Architecture:** Kotlin Compose talks to a FastAPI service over HTTP(S). PostgreSQL owns identities, OTPs, sessions, invitations, pair membership, and user-owned data. Mailpit is the development SMTP inbox; production SMTP and API origin are environment-configured. Existing Kotlin time calculations remain on device.

**Tech Stack:** Kotlin, Jetpack Compose, Android Keystore, Python 3.13, FastAPI, psycopg 3, PostgreSQL 18, Mailpit, Docker Compose, pytest, JUnit.

## Global Constraints

- Email + six-digit OTP; 10-minute expiry and 60-second resend interval.
- Invitations expire after 24 hours and one account belongs to at most one active pair.
- Partner values stay unknown until the owner submits them; demo data never enters real accounts automatically.
- Do not claim cross-border access or production email delivery without measurements and credentials.
- User text is plain text; per-user ownership and current pair membership are checked by the server.

---

### Task 1: API and PostgreSQL foundation

**Files:** `backend/app/*`, `backend/migrations/*`, `backend/tests/*`, `backend/Dockerfile`, `docker-compose.yml`, `backend/requirements.txt`

- [x] Add health, input validation, OTP, pairing, permission, and production SMTP configuration tests.
- [x] Add FastAPI, PostgreSQL connection handling, explicit versioned SQL migrations, Compose and health endpoint.
- [x] Run the backend test suite in a PostgreSQL container and verify service startup/migrations.

### Task 2: OTP, sessions, and permissions

**Files:** `backend/app/auth.py`, `backend/app/mail.py`, `backend/app/api.py`, migration SQL, `backend/tests/test_auth.py`

- [x] Test wrong, expired, one-use, resend cooldown, attempt limit, and hourly OTP rate limits.
- [x] Implement random OTP generation/digests, local Mailpit and environment-configured SMTP, hashed sessions, and bearer authentication.
- [x] Test account identity isolation and session revocation.

### Task 3: Pairing and shared-data API

**Files:** `backend/app/pairing.py`, `backend/app/data.py`, `backend/tests/test_pairing.py`, `backend/tests/test_permissions.py`

- [x] Test invite preview/scope, self-accept, expiration, revoke/rebuild, already-paired accounts, and concurrent B/C accepts.
- [x] Test two-user sync, owner-only writes, pause/resume, unpair, and account C denial.
- [x] Implement transactions, unique constraints and request-time access checks; run integration tests against PostgreSQL.

### Task 4: Android API and session

**Files:** `android/app/src/main/java/app/nowus/android/data/*`, `MainActivity.kt`, manifest/build config, Android unit tests.

- [x] Add client/repository tests for failed writes, stale snapshots, session restore, and partner-data privacy.
- [x] Implement typed API calls, Keystore-backed session/cache/invite storage, debug API URL configuration, and invite deep-link capture.
- [x] Run Android unit tests and assemble the debug APK.

### Task 5: Compose auth and real-account flows

**Files:** `android/app/src/main/java/app/nowus/android/ui/NowUsApp.kt`, new auth/pair/share composables, `AppViewModel.kt`, `androidTest/*`.

- [x] Add Compose tests for OTP login, restored session, and failed-save draft retention.
- [x] Wire profile/rhythm/note/temp updates, partner rendering, sharing pause/resume, unpair, sync timestamps and explicit local-import confirmation.
- [x] Keep local demo behind a separate entry; block demo-partner data from account repository updates.
- [x] Run Compose UI tests on the one connected Android device.

### Task 6: Documentation, end-to-end acceptance, and delivery

**Files:** `android/README.md`, specs touched for stale statements, backend config examples, architecture note.

- [x] Document Compose startup, migrations, Mailpit, Android API URL settings, tests and production SMTP variables.
- [x] Run A/B/C API acceptance using SMTP-to-Mailpit OTP delivery; verify two-way schedule/note sync, C denial, pause/resume and unpair revocation.
- [x] Run the final Android unit/build/device tests, backend PostgreSQL suite, and A/B/C API acceptance.
- [x] Inspect the full diff and secret boundary, complete read-only code review, commit, and push `codex/nowus-real-sync` for review.

### Follow-up outside this local implementation

- [ ] Deploy behind HTTPS in a selected region and measure reachability on real Mainland China and overseas networks.
- [ ] Configure production SMTP credentials/domain and verify delivery to trial users' mailbox providers.
- [ ] Complete acceptance on two physical Android phones. Current environment has one physical device; API A/B/C acceptance and Compose tests do not replace this release check.
