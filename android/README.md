# NowUs Android

Kotlin + Jetpack Compose 原生 Android 本地体验版，最低 Android 8.0（API 26）。开发安装包使用 `app.nowus.android.debug`。

此阶段用于验证页面、时间与本地资料。邮箱验证码、服务端配对、对方真实资料同步尚未接入；应用内邀请仅用于同一台手机的配对演示，不会发送给伴侣。

## 构建

Android Studio 打开本目录，或使用命令行。需要 JDK 17 或 21、Android SDK platform 36 / build-tools 36.0.0、可访问 Google Maven 和 Maven Central。

在 `local.properties` 填写本机 SDK 目录（不要提交此文件）：

```properties
sdk.dir=C\:/Android
```

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:lintDebug
# 手机启用 USB 调试且 adb devices 显示 device 后：
.\gradlew.bat :app:connectedDebugAndroidTest
```

如果 Gradle 的设备测试运行器依赖暂时无法从 Google Maven 下载，可先生成测试 APK，再通过 AndroidJUnitRunner 直接运行同一套真机测试：

```powershell
.\gradlew.bat :app:assembleDebugAndroidTest
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell am instrument -w -r app.nowus.android.debug.test/androidx.test.runner.AndroidJUnitRunner
```

看到 `OK (9 tests)` 表示当前设备测试全部通过。

APK：`app/build/outputs/apk/debug/app-debug.apk`。安装：

```powershell
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n app.nowus.android.debug/app.nowus.android.MainActivity
```

固定依赖版本：Gradle 8.13、AGP 8.13.2、Kotlin 2.3.21、Compose BOM 2026.06.01。Wrapper 配置验证官方分发包 SHA-256。[Compose BOM 官方说明](https://developer.android.com/develop/ui/compose/bom)解释界面库版本管理方式。

## 数据与时间

- `domain/`：IANA 时区、工作日/休息日、连续活动、联系窗口与本地邀请规则，纯 Kotlin 单元测试。
- `data/`：DataStore 保存本机 JSON；原子更新、读取或保存失败可见。未启用云备份，卸载会清除本地资料。
- `AppViewModel`：单向状态与保存事件，真实时间在前台更新。
- `ui/`：森林绿主题、首次引导、此刻、双方时间轴、作息编辑。

通常作息是推测，联系意愿独立；未知不能推导为有空。临时联系状态在设定时刻开始并在到期时恢复。工作日按每个人所在地的星期计算；跨午夜区间按查询时刻当地日期所对应的模板解释。夏令时依据手机系统 IANA 数据库。

当前仅提供八个城市：北京、上海、纽约、伦敦、巴黎、东京、悉尼、加德满都；正式版本再扩充城市搜索。示例伴侣资料通过显式演示操作填入。固定时间演示与正常真实时间明确区分，重启回到真实时间。

后续阶段：邮箱验证码、真实唯一邀请、两账号配对和资料同步，再用两台手机验收。
