# NowUs Android 起步方案

日期：2026-09-29。

已确认：Android 优先、iOS 后续，真实账号采用邮箱验证码；用户选择 Kotlin + Jetpack Compose。第一阶段的 Android 本地体验工程已完成并可安装试用。

## 框架取舍

| 路线 | 当前阶段的价值 | 后续成本 |
| --- | --- | --- |
| Kotlin + Jetpack Compose，Android 优先推荐 | Android 原生 UI 与系统行为；可在现有 Windows／Android SDK 环境本地构建和测试 | 现有 JavaScript 时间模型需迁移成 Kotlin 并复验；iOS 后续单独选择界面方案，不能默认整套 UI 直接共享。 |
| React Native + Expo | 部分 JavaScript 时间逻辑可验证后迁移；以后更容易在同一工程推进 iOS | 现有 HTML／CSS 仍需改写成移动组件；Android 仍要做键盘、返回、安装和时间计算的真机验证。 |

最终选择：Kotlin + Jetpack Compose。表格保留此前路线比较；第一阶段源代码位于 `android/`，iOS 不在当前交付范围。

## 环境观察

当前仓库已有 `android/` 工程；本机 JDK 21、Android SDK 和 ADB 可用于 Android 开发，ADB 目前识别一台实体手机。真实账号接口与登录 UI 已完成，Android 单元测试、debug APK 构建及 13 项 Compose 仪器测试均通过。Google Maven 在此环境的 TLS 请求曾失败，本轮通过仓库附带的可选 Tencent/Aliyun Gradle init script 完成本地构建；此镜像仅是当前开发网络的取回方式，不改变 Gradle 默认仓库。现有设备数不足以完成两台实体手机验收。

## 第一阶段建议交付

目标：能在 Android 手机安装、操作、重启恢复的本地体验版本，为正式账号接入验证原生界面与时间逻辑。

- 复现已试用的「此刻／我们的一天／我的节奏」三个主页面，沿用森林绿及昼夜配色。
- 迁移昵称／城市／自己的作息／演示邀请／配对成功流程；等待与未知不生成对方信息。
- 正常模式使用当前真实时间，页面可见时更新，返回前台重新计算。固定评审时间仅保留在明确标识的演示入口。
- 支持工作日与休息日模板、跨午夜睡眠、独立联系偏好、临时状态及到期恢复。
- 时间轴同一高度对应同一时刻，双方日期明确，活动各自连续；当前／未来七天共同窗口保持来源说明。
- 本地保存自己的资料、作息与一条最多 120 字的留言；演示配对与真实账号明确区分。
- Android 系统返回、键盘遮挡、状态栏／导航栏安全区、较大字体与重启恢复一起验证。
- 交付可安装的开发 APK、源代码与构建说明；真实邮件、账号与双方同步尚未接入时，不称为正式 MVP。

## 模块边界

- 时间与作息计算独立于 UI，可固定时刻测试跨日、夏令时、非整小时偏移及未知资料。
- UI 使用状态与用户事件，不直接读写持久化存储。
- 本地数据通过仓储保存，后续真实账号与配对服务有明确接口；演示身份不能进入真实共享数据。
- 提供加载／校验失败／保存失败状态，保留输入，不给失败操作显示成功。

## 真实账号实现阶段（2026-09-30）

本地开发方案已选为 FastAPI、PostgreSQL 和 Mailpit，Android 继续使用 Kotlin + Jetpack Compose。代码实现邮箱 OTP、会话恢复、24 小时邀请预览与原子接受、双向资料／作息／留言同步、分享暂停／恢复与解除配对、服务端访问控制和本人资料显式导入。细节见 [同步架构选择](2026-09-30-nowus-real-sync-architecture.md)。

本地 Docker + Mailpit 已通过 A/B/C API 验收，但这不能验证真实邮件投递或跨境网络。实体设备只有一台，Android 两设备验收未完成；部署地域、HTTPS 域名与真实 SMTP 仍待提供。iOS 开发在 Android 核心流程与网络验收稳定后另行安排。

## 依据

- [Android 官方 Compose 介绍](https://developer.android.com/develop/ui)：Compose 是面向 Android 原生 UI 的 Kotlin 工具包。
- [Android 架构建议](https://developer.android.com/topic/architecture/recommendations)：UI 与数据层、仓储及单向数据流的边界。
- [Expo EAS Build](https://docs.expo.dev/build/introduction/)：React Native／Expo 双端构建路线的候选资料。
