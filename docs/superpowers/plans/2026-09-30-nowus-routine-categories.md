# 作息类别与时间轴配色 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 给 NowUs 的可编辑作息增加持久化活动类别，并在编辑器与双方时间轴使用同一套类别颜色。

**Architecture:** `RoutineBlock` 增加默认值为 `OTHER` 的可序列化枚举，`TimeEngine` 把类别随活动段返回。模板通过稳定块 ID 赋类并安排起床准备；服务端验证可选类别，兼容未带类别的旧客户端。Compose 编辑页用菜单编辑类别，`Theme.kt` 集中提供类别色，时间轴按活动段类别渲染。

**Tech Stack:** Kotlin、kotlinx.serialization、Jetpack Compose Material 3、FastAPI、Python pytest、PostgreSQL JSONB。

## Global Constraints

- Android 使用项目现有 Kotlin + Jetpack Compose 技术栈。
- 既有无类别 JSON 解码为 `OTHER`，保留旧版单段 `Rhythm` 行为。
- 新类别随现有 `Schedule` JSON 同步，不新增数据库迁移或类别推断服务端数据。
- 未安排空档不上传，不将运动、家务或社交示例写入用户模板。
- 颜色与名称并行显示，颜色不单独承载状态含义。

---

### Task 1: 定义并验证作息类别模型

**Files:**
- Modify: `android/app/src/main/java/app/nowus/android/domain/Model.kt`
- Modify: `android/app/src/main/java/app/nowus/android/domain/TimeEngine.kt`
- Test: `android/app/src/test/java/app/nowus/android/domain/DomainTest.kt`

**Interfaces:**
- Produce `RoutineCategory` entries `SLEEP`, `PREPARATION`, `MEAL`, `COMMUTE`, `STUDY_WORK`, `REST`, `EXERCISE`, `LIFE_ADMIN`, `SOCIAL`, `OTHER`, `UNSCHEDULED`.
- `RoutineBlock.category` defaults to `OTHER`; `UNSCHEDULED` is display-only.
- `Activity.category` defaults to `OTHER` and receives block category from `TimeEngine.activityAt`.

- [x] Add tests for old block JSON decoding to `OTHER`, explicit category round-trip, block activity category propagation, and unscheduled category.
- [x] Run the focused Android unit tests and confirm they fail because the category API does not exist.
- [x] Add the enum and model properties with serialization defaults; propagate categories from `TimeEngine`.
- [x] Run `:app:testDebugUnitTest --tests app.nowus.android.domain.DomainTest` and confirm it passes.

### Task 2: Apply categories to editable student and office templates

**Files:**
- Modify: `android/app/src/main/java/app/nowus/android/domain/RoutineTemplates.kt`
- Modify: `android/app/src/test/java/app/nowus/android/domain/DomainTest.kt`

**Interfaces:**
- `RoutineTemplates.forCity(cityId, template)` returns blocks assigned by stable template ID, never label text.

- [x] Assert student and office templates assign sleep, preparation, meals, commute, work/study, rest, and leisure correctly.
- [x] Add weekday preparation between sleep and breakfast and rest-day preparation between sleep and breakfast without overlapping adjacent blocks.
- [x] Run the focused template tests and confirm they pass.

### Task 3: Validate categories in backend without breaking old clients

**Files:**
- Modify: `backend/app/rules.py`
- Test: `backend/tests/test_rules.py`

**Interfaces:**
- Optional block category is accepted when absent (legacy defaults to `OTHER` on Android).
- Known persisted category strings are accepted; `UNSCHEDULED`, unknown strings, and non-strings return `时段类别无效`.

- [x] Add tests for a valid category, a missing category, and an invalid or display-only category; confirm the invalid-category test fails first.
- [x] Add the allow-list and reject only provided invalid category values.
- [x] Run the focused pytest test and the Docker backend suite.

### Task 4: Edit categories and render colors in Compose

**Files:**
- Modify: `android/app/src/main/java/app/nowus/android/ui/Theme.kt`
- Modify: `android/app/src/main/java/app/nowus/android/ui/NowUsApp.kt`
- Modify: `android/app/src/main/java/app/nowus/android/ui/Timeline.kt`
- Test: `android/app/src/androidTest/java/app/nowus/android/AppFlowTest.kt`

**Interfaces:**
- `routineCategoryColor(category: RoutineCategory): Color` is the single color mapping used in the editor and timeline.
- Each block shows an accessible category menu with a color swatch and localized label.
- Timeline segments and their semantics expose the same category while retaining activity name and time.

- [x] Add a Compose test that changes a custom block to `EXERCISE`, saves, and verifies the persisted category; add palette assertions for distinct mapped colors.
- [x] Run the focused instrumentation test and confirm it fails because category selection is absent.
- [x] Implement the category menu and use the shared palette for timeline segments on both tracks.
- [x] Run the focused instrumentation test on the connected Android device.

### Task 5: Update docs, run complete verification, build and deliver

**Files:**
- Modify: `docs/superpowers/specs/2026-09-30-nowus-routine-templates.md`
- Modify: `android/README.md`

- [x] Document category colors, template defaults, optional activities, and old-data behavior.
- [x] Run all Android unit tests, the full backend Docker suite, and the physical-device template/category flow.
- [x] Build and install the debug APK with the existing LAN API URL; app opened on the connected device.
- [ ] Commit and push the review branch after final diff verification.
