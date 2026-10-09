# NowUs Android 第 120 轮实现与验收

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. 用户已确认采用胜出的设计并开始实现，无须重新请求设计批准。

**Goal:** 将已确认的第 120 轮落为可安装、可操作的 Android MVP，并验证现有真实账号链路。

**Architecture:** 沿用 Compose + AppViewModel + StateRepository 和已有 FastAPI 账号服务。首页提取独立展示组件，以真实 AppState 驱动；保留未知、同步失败、权限收回和草稿状态。共同窗口算法同时纳入双方临时意愿。

**Tech Stack:** Kotlin 2.3.21、现有 Compose / Material 3 依赖、AGP 8.13.2、Android API 26–36、FastAPI / PostgreSQL / Mailpit。

## Global Constraints

- 设计基线：`design-history/2026-10-03-a/round-120/final/home.png`；两两比较胜出，无数值评分。
- 保留既有设计快照、用户编辑和后端数据，不重写归档。
- 伙伴时钟在前；姓名城市位于各自白色 / 深蓝半区；浅蓝联系卡、白色留言卡、冷灰页面、单底部导航。
- 保持 48dp 触控区域、系统安全区域、可滚动表单及大字体适配。
- 不将通常作息描述为实时状态，不以示例资料补齐真实账号，不把联系窗口描述为双方约定。
- 沿用已接受 MVP 边界：不增加聊天、日历、定位、推送或小组件。
- 生产 HTTPS / SMTP 与跨境双设备验证需要实际基础设施；本次本地测试不得冒充生产验收。

## Task 1：共同时间正确性

**Files:** `domain/TimeEngine.kt`、`domain/DomainTest.kt`、`ui/Timeline.kt`（均在 android/app/src 对应源目录）。

- [x] 新增伴侣临时忙碌、未知偏好被主动可联系覆盖、毫秒起止和过期恢复回归测试，确认失败。
- [x] commonWindows 增加兼容的 partnerTemporary 参数，双方状态边界均进入分段。
- [x] 首页及时间轴传入双方临时意愿，运行全部领域测试。

## Task 2：原生首页与可操作入口

**Files:** 新增 `ui/HomeDashboard.kt`；修改 `ui/NowUsApp.kt`、`ui/Theme.kt`、`res/drawable/ic_nowus.xml`、`AppFlowTest.kt`。

- [x] 增加真实伙伴留言 → 回复 → 保存测试、未知偏好和大字体主页测试。
- [x] 首页保留三块主内容，系统状态仅在需要时展示；账户 / 本地体验说明和临时意愿管理放入我的节奏。
- [x] 留言编辑器共享；保存失败保留草稿，允许本人编辑删除，支持无伴侣起步。
- [x] 统一冷灰、蓝色、深蓝和 10dp 内容卡片；导航及图标配合设计；大字体时钟改纵排。
- [x] 更新固定演示为参考图的北京时间 20:00，只影响显式本地演示。

## Task 3：完整 MVP 验证与交付

**Files:** 新增验收记录 `docs/android-120-acceptance.md`；更新 `android/README.md`；交付 APK / 截图至被忽略的 `artifacts/android-120/`。

- [x] 构建 debug / instrumentation APK，运行单元测试和 API 36 模拟器全部 instrumentation 测试。
- [x] 原生首页、时间轴、表单与大字体截图检查；必要时修正并重验相关流程。
- [x] 运行已有后端测试、两账号 OTP / 配对 / 作息 / 留言 / 暂停 / 解除链路验收；记录环境阻碍。
- [x] 检查变更范围并做代码评审，验证设计快照未被更改。
- [x] 交付安装包、截图和明确的验收结果，标注线上依赖，不声称完成未验证部分。
