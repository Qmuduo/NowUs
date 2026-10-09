# NowUs 同一 Wi-Fi 测试

2026-10-03，按用户请求配置并安装。

## 当前可用配置

- 电脑 WLAN 地址：`192.168.3.46`，手机 wlan0 地址：`192.168.3.51`，同属 `192.168.3.0/24`。
- Wi-Fi 安装包：`artifacts/android-120/NowUs-r120-wifi-debug.apk`，API 固定为 `http://192.168.3.46:8000`。
- 已在用户手机（API 30 / Android 11）覆盖安装成功，保留原 App 数据，并启动 MainActivity 至前台。
- 新增 API 实例仅监听电脑 WLAN 地址 `192.168.3.46:8000`，使用已有 `nowus120_dev` 开发数据库。原 `127.0.0.1:8000` 服务保留，因此模拟器 / USB 包仍可用。
- Windows 防火墙规则 `NowUs-r120-WiFi-8000` 仅允许 WLAN 接口、目标 `192.168.3.46`、TCP 8000、来源 `192.168.3.0/24`；经管理员授权创建，没有修改网络类别。
- PostgreSQL 5432、Mailpit SMTP 1025 与 Mailpit UI 8025 仍仅监听 `127.0.0.1`。
- 手机的 API `adb reverse tcp:8000` 映射已移除，Wi-Fi 包无需 USB 端口映射。

## 使用

电脑保持开机，开发 API、PostgreSQL、Mailpit 保持运行；手机连接同一 Wi-Fi，即可拔掉 USB 使用。

邮箱验证码在电脑浏览器的 [Mailpit 测试邮箱](http://127.0.0.1:8025/) 查看，当前开发服务不会向外部邮箱发送邮件。可使用两个不同邮箱建立两个测试账号，再从 App 创建邀请并配对。

第二部 Android 8.0 及以上手机安装同一个 Wi-Fi APK 即可。连接 USB 安装时，在仓库根目录运行，并将 `<设备序列号>` 替换为 `adb devices` 列出的目标手机：

```powershell
adb -s <设备序列号> install -r artifacts/android-120/NowUs-r120-wifi-debug.apk
```

如果电脑的 WLAN 地址发生变化，需要重新启动相应地址的 API、调整这条防火墙规则，并按新地址构建 APK。当前包不能用于其他 Wi-Fi 或公网服务。

## 验证证据

| 验证 | 结果 |
| --- | --- |
| Wi-Fi debug 构建 | BUILD SUCCESSFUL；仅修改构建参数，没有修改 App / 后端业务代码 |
| APK v2 签名 | Verifies，1 signer |
| 手机安装与启动 | adb install Success；am start Status: ok；MainActivity 为 resumed activity |
| 手机直接 LAN HTTP | 移除 API USB 映射后，手机 curl `/health` 返回 `status=ok` |
| 手机真实验证码链路 | 手机经 LAN 请求 OTP、验证 OTP、读取 snapshot、DELETE session 退出均通过；验证码由本机 Mailpit 收取 |
| 已安装 App 联网 | 启动后，Wi-Fi API 日志持续收到该手机的 `GET /v1/snapshot` 并返回 200，原账号仍可读取服务器数据 |
| App 数据保护 | 验收使用独立随机 `wifi-probe-…@example.net` 账号；未读写 App 的账号会话、个人资料或作息 |
| 端口与规则复核 | API 限定 WLAN 地址 / TCP 8000 / 同网段；数据库及邮箱继续 loopback |

Wi-Fi APK SHA-256：`fe6534cf2bd5c1bb38f0698476af1c42a32149fd21a64c3d309e802d55f0010e`。
机器可读证据位于 `artifacts/android-120/wifi-build-info.json` 和 `backend-runtime/phone-wifi-probe-result.json`。

以上证明这部手机到开发服务的真实 Wi-Fi HTTP 账号链路；尚未验证第二部手机的端到端同步、真实外部邮箱投递或跨境公网连接。原 41 项单元 / 20 项模拟器设备 / 27 项后端验收详见 [第 120 轮验收](android-120-acceptance.md)。

## 重启与收回 Wi-Fi 接口

先按 [后端运行记录](android-120-backend-acceptance.md) 启动数据库、Mailpit 和原 API，并使用该文档 development 环境变量。确认 WLAN 仍为 `192.168.3.46`，再在同一个已配置环境变量的 PowerShell 中执行：

```powershell
$wifiRuntime = (Resolve-Path artifacts/android-120/backend-runtime).Path
Start-Process (Resolve-Path backend/.venv/Scripts/python.exe).Path `
  -ArgumentList '-m','uvicorn','backend.app.main:app','--host','192.168.3.46','--port','8000' `
  -WorkingDirectory (Get-Location).Path -WindowStyle Hidden `
  -RedirectStandardOutput "$wifiRuntime/api-wifi.stdout.log" `
  -RedirectStandardError "$wifiRuntime/api-wifi.stderr.log"
```

防火墙规则跨重启保留，无须重复创建。当前 Wi-Fi API 主进程启动时 PID 为 25072，launcher PID 为 21776；停止时先核对实际进程路径与上述参数，再结束本轮 Wi-Fi 实例，避免按旧 PID 操作其他进程。不要停止原 API、清空数据库或重置 WSL。

需要关闭 Wi-Fi 入口时，在管理员 PowerShell 中仅删除本轮规则：

```powershell
Remove-NetFirewallRule -Name 'NowUs-r120-WiFi-8000'
```

重新构建当前 Wi-Fi 包：

```powershell
$env:ANDROID_HOME='C:\Android'
Set-Location android
.\gradlew.bat --init-script .\gradle-mirror.init.gradle :app:assembleDebug `
  --console=plain -PnowusApiBaseUrl=http://192.168.3.46:8000
```
