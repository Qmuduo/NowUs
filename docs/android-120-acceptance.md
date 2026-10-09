# NowUs 第 120 轮 Android 原生实现

日期：2026-10-03。用户确认采用第 120 轮后，实现于 `codex/nowus-android-120-completion-20261003`。第 120 轮为独立两两比较胜出的设计；该阶段没有数值评分。

## 已实现范围

- Compose 首页采用伙伴在左、本人在右的白色 / 深蓝时钟；姓名与城市位于各自半区。普通 360dp 宽度保留双栏，大字体或窄屏改为纵排。
- 冷灰背景、浅蓝共同联系卡、白色留言卡、10dp 内容卡片和单底部导航；启动主题、登录和资料流程同步蓝色视觉。
- 共同联系时间同时使用双方通常偏好及未到期的主动设置。双方日期按实际时区显示，跨午夜的结束时间显示完整日期；提示“尚未约定”，不推断实时在线状态。
- 伴侣留言可直接打开本人留言编辑器；保存失败保留草稿。本人可编辑 / 删除当前留言，不修改伴侣原留言。
- 临时联系意愿移至“我的节奏”，保留 30 / 60 / 180 分钟及恢复通常偏好的入口；到期按真实时间恢复。
- 分享撤回、离线权限无法确认或伴侣资料变更时，时间轴已打开的作息详情失效；旧异步计算结果不再显示。
- 本地固定演示改为 2026/9/29 北京 20:00 / 纽约 08:00；仅显式本地演示使用，真实账号不会填入示例伙伴。
- 沿用已有邮箱 OTP、邀请确认、作息模板与编辑、账号同步、分享管理和安全存储架构。

## 测试与构建

Android SDK `C:/Android`，API 36 模拟器 `phase15-api36`。沿用现有 AGP / Kotlin / Compose 版本，没有升级依赖。

```powershell
$env:ANDROID_HOME = 'C:\Android'
Set-Location android
.\gradlew.bat --init-script .\gradle-mirror.init.gradle `
  :app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest :app:lintDebug `
  --console=plain -PnowusApiBaseUrl=http://10.0.2.2:8000
.\gradlew.bat --init-script .\gradle-mirror.init.gradle `
  :app:connectedDebugAndroidTest --console=plain -PnowusApiBaseUrl=http://10.0.2.2:8000
```

| 验证 | 结果 |
| --- | --- |
| 单元测试 | 41 项通过，0 failure / error；领域 24 项，其中新增双方临时意愿回归 4 项 |
| API 36 instrumentation | 20 项通过，0 失败、0 跳过；包括真实 Mailpit OTP 登录、加密会话保存和服务器 snapshot 读取 |
| 本地后端 | 27 项测试通过；真实 A/B/C HTTP / PostgreSQL / SMTP 验收通过，覆盖配对、双向作息 / 留言、第三方访问拒绝、暂停 / 恢复及解除撤权 |
| 新增回复与大字体流程 | 伙伴留言 → 本人回复 → 保存 → 编辑；2 倍字体下两只时钟与回复可达 |
| 撤权回归 | 打开伴侣作息详情 → 分享暂停 / 隐去伙伴 → 详情立即关闭、伙伴内容消失 |
| TDD 红绿证据 | 4 项临时意愿测试先出现断言失败；回复入口和撤权详情分别先失败，修复后通过 |
| Lint | 0 errors，20 warnings；现有安全存储同步写入、依赖更新建议和 manifest 等警告未在本轮扩展处理 |
| 构建 | debug APK 与 instrumentation APK 构建成功 |
| 安装与启动 | APK v2 签名验证通过；无快照冷启动 MainActivity 正常进入登录页，无 App ANR；系统夜间设置下固定浅色主题仍可读 |
| USB 地址包 | 通过反向端口的真实登录探针在 API 36 模拟器通过；随后在用户 USB 手机（API 30 / Android 11）安装成功，MainActivity 启动成功并位于前台，API 8000 反向端口已配置；真机 OTP 登录及双手机同步尚未验证 |
| 设计归档 | 验证器确认 67 新归档文件、1168 旧历史文件和 15 采用的原型文件仍符合记录 |

既有 UI 测试作了稳定性修正：类别测试固定在工作日演示日期，时间选择器先滚动到目标字段再点击；带邀请码的登录页先滚动到发送 / 验证按钮并断言可见。冷启动复现证明旧测试点击的“验证并继续”位于屏幕外，不是账号服务失败。

## 安装包与截图

后续已配置同一 Wi-Fi 连接并安装 `NowUs-r120-wifi-debug.apk`；使用、真机 LAN 验证和重启方法见 [Wi-Fi 测试记录](android-wifi-test.md)。

交付目录：`artifacts/android-120/`（构建产物不纳入 Git）。

- `NowUs-r120-debug.apk`：模拟器地址 `http://10.0.2.2:8000`。
- `NowUs-r120-usb-debug.apk`：USB 手机调试地址 `http://127.0.0.1:8000`。
- 两包均为 Android 8.0 及以上、包名 `app.nowus.android.debug`、开发调试签名。安装同一包名会替换已有调试版，不会同时出现两个 App。
- `native-home.png`、`native-timeline.png`、`native-large-font.png`、`native-routine-template.png`：真实 Compose 渲染的验收截图。
- `native-login.png`：安装 APK 后启动真实 MainActivity 的登录画面。
- `instrumentation-final.log`、`usb-login-probe.log`、`build-info.json`：最终设备测试输出、两包校验值和构建基线。

首页截图中的姓名、留言和固定日期来自明确的测试夹具。正常首次启动仍从登录或本地资料设置开始，不会自动生成已配对伙伴。

模拟器安装：`adb install -r artifacts/android-120/NowUs-r120-debug.apk`。完整设备测试使用 API 36 模拟器；后续按用户请求在 USB 手机（API 30）完成安装与启动检查。

手机通过 USB 连接本机并开启 USB 调试后，安装 USB 包，再设置 API 反向端口：

```powershell
adb install -r artifacts/android-120/NowUs-r120-usb-debug.apk
adb reverse tcp:8000 tcp:8000
```

保持本机开发服务运行，验证码在本机 `http://127.0.0.1:8025` 的 Mailpit 测试邮箱查看，不会发送至外部邮箱。拔掉 USB 后，这个地址不能连接本机；独立运行需要按 `android/README.md` 为实际可访问的 HTTPS 服务重新构建。无需账号也可从登录页进入本地体验。

## 本地联网已验证，上线验收待完成

Docker engine 故障通过独立本地运行环境绕过：WSL PostgreSQL 16.15、Windows venv FastAPI 和 Mailpit 都仅绑定 loopback，使用专用开发 / 测试数据库。本地真实账号及双账号共享链路已通过。启动、停止、测试复验命令和初期环境失败证据见 [后端验收记录](android-120-backend-acceptance.md)。

本次完成可安装的 Android MVP 与本地真实联网验证。仍需 Docker PostgreSQL 18 基线复验、生产 HTTPS / SMTP 配置，以及双方实际手机、实际邮箱和跨境网络验收，才能判断可上线性。没有执行线上部署、发布或合并主分支。
