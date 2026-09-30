# NowUs Android 本地体验验收记录

日期：2026-09-30。范围：Android Kotlin + Jetpack Compose 本地体验版；邮箱验证码、真实邀请与两台设备同步尚未实现。

## 已核验

- `:app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest :app:lintDebug` 构建成功；18 项 JVM 测试通过，Lint 无错误。
- 在 Android 11 真机上安装应用 APK 与测试 APK，使用 AndroidJUnitRunner 直接执行设备测试：`OK (9 tests)`，包含首次使用、时间轴、留言、本地数据更新与重启读取。
- 原有网页原型的 `node --test prototype/model.test.mjs prototype/pairing-model.test.mjs` 通过 16 项。
- 检视了首页、时间轴与大字体页面截图，检查双日期、同一瞬间对齐、时间窗口、当前线与可滚动输入。

本机运行 `:app:connectedDebugAndroidTest` 时，Gradle 设备测试运行器的 `android-test-plugin-host-coverage:31.13.2` 下载遇到 Google Maven TLS 问题，设备测试因此没有通过该 Gradle 命令启动。测试 APK 本身已正常构建，并通过 AndroidJUnitRunner 在真机上完成全部 9 项；复现命令见 [Android 工程说明](../../../android/README.md)。

## 交付物与边界

- Debug APK：`artifacts/NowUs-android-0.1.0-local-debug.apk`，仅用于安装试用；SHA-256 `D4A88E88A11C6896342D2A94B3CF4A43A7A977A964877EB6B6909B0EC2B337B8`。
- 截图：`artifacts/nowus-home.png`、`artifacts/nowus-timeline.png`、`artifacts/nowus-large-font.png`。构建产物与截图被 Git 忽略，源代码和文档在仓库中。
- 邀请是单机演示，短留言和作息仅保存在本机。固定日期只属于明确标出的演示模式，正常使用读取真实时间。
- 下一阶段实现邮箱验证码、真实双账号配对和跨设备同步，并在中国大陆与海外两台手机上进行端到端验证。
