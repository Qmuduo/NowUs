# 第 120 轮后端验收记录

日期：2026-10-03。**本地后端验收通过**：27 项测试通过，真实本地 HTTP / PostgreSQL / SMTP 验收脚本通过。未验证生产 HTTPS / SMTP、Docker PostgreSQL 18 或跨境双设备连接。

本记录的 loopback 范围对应最初账号验收。随后按用户授权新增 Wi-Fi API 实例及限定网段的防火墙规则，数据库 / Mailpit 仍为 loopback；当前配置与手机验证见 [同一 Wi-Fi 测试](android-wifi-test.md)。

## 最终验证结果

| 验证 | 结果 | 范围 |
| --- | --- | --- |
| `backend/.venv/Scripts/python.exe -m pytest backend/tests -q` | **27 passed in 16.66s** | 仅使用新建专用 `nowus120_test` 数据库；测试 fixture 清空的表位于该测试数据库。 |
| `backend/.venv/Scripts/python.exe backend/scripts/acceptance.py` | **PASS** | A/B/C 本地 SMTP OTP 登录、邀请预览 / 接受、双向作息 / 留言同步、第三方访问拒绝、暂停 / 恢复、解除配对撤权。 |
| `GET http://127.0.0.1:8000/health` | `{"status":"ok"}` | 已配置真实数据库的 FastAPI 服务启动成功。 |
| 监听地址 | 均为 `127.0.0.1` | API 8000、Mailpit UI 8025、SMTP 1025、PostgreSQL 5432；没有 LAN 监听。 |

本地服务保留运行，供 Android 模拟器通过 `http://10.0.2.2:8000` 验证账号流程。Android 端验证结果另见主验收记录。

## 实际开发运行环境

- Windows Python 的被忽略 `backend/.venv` 使用 `--system-site-packages` 创建；在 venv 内安装 `backend/requirements.txt`，FastAPI **0.140.12**、psycopg / binary **3.3.6**。未修改全局 pip 依赖。
- Ubuntu 24.04 WSL 新安装 PostgreSQL **16.15**。仓库 Docker 基线为 PostgreSQL 18，本次验证使用兼容 SQL 的 PostgreSQL 16，不能替代 PostgreSQL 18 容器复验。
- 新建本地开发专用 role `nowus120_local`、数据库 `nowus120_dev` 和 `nowus120_test`；未接触已有用户数据库。PostgreSQL 配置 `listen_addresses=localhost`，Linux 实际监听 `127.0.0.1:5432`。
- Mailpit **v1.31.1** 来自官方 release 的 `mailpit-windows-amd64.zip`，保存于被忽略 `artifacts/android-120/backend-runtime/`，SMTP sink 与 UI 都仅绑定 loopback；未配置转发或外部 SMTP。
- API 使用专用开发数据库、development 模式以及仓库公开示例的开发配置值，后台隐藏运行。运行日志位于上述 artifacts 目录。未读取 / 输出生产 secrets。
- WSL 退出到 idle 时初次 localhost 数据库连接曾超时；保持开发 WSL 会话后成功。额外隐藏 `wsl.exe ... tail -f /dev/null` 进程（启动时 PID 25988）维持开发会话，供后续 Android 验证。

## 已解决与保留的失败尝试

最初主机 Python 的完整测试结果为 14 passed / 13 errors，原因是缺少 psycopg；非数据库测试单独 14 passed in 0.12s。首次 acceptance 的 OTP 请求因 API 未运行被拒绝，未发送邮件。安装 venv 依赖并启动新数据库后完成上述最终验证。

Docker 仍不可用：desktop-linux 与 default context 的 engine 命名管道均不存在，不能查询镜像缓存。当前 backend 进程启动于今日 13:06:45–46，当前日志最后写入今日 13:11:50；错误记录 `2026-10-03T05:07:36Z`（本地 13:07:36）显示无法移除 `C:/Users/MUDUO/AppData/Local/Docker/run/dockerEthernetVfkit` socket，属于本次启动。

支持的 `docker desktop restart --timeout 45` 最终退出码 1，报告 `Failed to stop Docker Desktop`、`processes still running`、`context deadline exceeded`。随后 `docker desktop start --timeout 15` 返回 `Docker Desktop is already running`，engine 仍不可用。未删除 socket、重置 Docker / WSL、重启主机、强制停止进程或清理卷；通过独立本地服务完成验收。

## 后续复验

### 重启当前非 Docker 开发环境

以下命令在仓库根目录的独立 PowerShell 中执行。服务仍在运行时不要重复启动；先检查 `/health` 与占用端口。现有数据库保留，无需重建或清空。启动命令沿用已核对的实际进程参数；所有后台窗口隐藏。

```powershell
$runtime = (Resolve-Path artifacts/android-120/backend-runtime).Path
$python = (Resolve-Path backend/.venv/Scripts/python.exe).Path
Start-Process wsl.exe -ArgumentList '-d','Ubuntu','-u','root','--','tail','-f','/dev/null' -WindowStyle Hidden
wsl -d Ubuntu -u root -- pg_ctlcluster 16 main start
$env:NOWUS_ENV='development'
$env:NOWUS_DEV_MAILBOX='true'
$env:NOWUS_DATABASE_URL='postgresql://nowus120_local:nowus120-local-only-20261003@127.0.0.1:5432/nowus120_dev'
$env:NOWUS_APP_SECRET='local-development-secret-change-this-before-deploy-32-chars'
$env:NOWUS_SMTP_HOST='127.0.0.1'
$env:NOWUS_SMTP_PORT_INTERNAL='1025'
$env:NOWUS_SMTP_FROM='login@nowus.local'
$env:NOWUS_SMTP_USERNAME=''
$env:NOWUS_SMTP_PASSWORD=''
$env:NOWUS_SMTP_STARTTLS='false'
Start-Process "$runtime/mailpit.exe" -ArgumentList '--listen','127.0.0.1:8025','--smtp','127.0.0.1:1025','--database',"$runtime/mailpit.db",'--disable-version-check','--smtp-disable-rdns' -WindowStyle Hidden -RedirectStandardOutput "$runtime/mailpit.stdout.log" -RedirectStandardError "$runtime/mailpit.stderr.log"
Start-Process $python -ArgumentList '-m','uvicorn','backend.app.main:app','--host','127.0.0.1','--port','8000' -WorkingDirectory (Get-Location).Path -WindowStyle Hidden -RedirectStandardOutput "$runtime/api.stdout.log" -RedirectStandardError "$runtime/api.stderr.log"
```

若 `pg_ctlcluster` 报告已经运行则继续；其他失败应检查后再启动 API。启动后检查 `Invoke-RestMethod http://127.0.0.1:8000/health`、`Invoke-RestMethod http://127.0.0.1:8025/readyz` 和监听端口；API 日志必须显示 startup complete。这些公开密码仅属于本次专用本地开发 role，不应用于生产。

### 安全重新运行全部 27 项测试

在另一独立 PowerShell 中执行，显式指向专用 **test** 数据库，避免把开发服务的连接串用于 pytest：

```powershell
$env:NOWUS_ENV='test'
$env:NOWUS_DEV_MAILBOX='false'
$env:NOWUS_DATABASE_URL='postgresql://nowus120_local:nowus120-local-only-20261003@127.0.0.1:5432/nowus120_test'
$env:NOWUS_APP_SECRET='test-only-secret-not-used-outside-the-test-container'
$env:NOWUS_SMTP_HOST='127.0.0.1'
$env:NOWUS_SMTP_FROM='test@nowus.invalid'
$env:NOWUS_SMTP_USERNAME=''
$env:NOWUS_SMTP_PASSWORD=''
$env:NOWUS_SMTP_STARTTLS='false'
backend/.venv/Scripts/python.exe -m pytest backend/tests -q
```

### 停止本次后台服务

本轮交付时 API PID **28388**、Mailpit PID **23832**、持久 WSL keepalive PID **25988**。先用 `Get-CimInstance Win32_Process` 核对 PID 的 `ExecutablePath` / `CommandLine` 与上方启动命令完全对应，再在任务管理器结束这三个确认过的进程；PID 可能被复用，不得按旧 PID 盲目停止。另有本轮临时 WSL shell 约 15:13 自行结束，因此结束持久 keepalive 后 WSL 不一定立刻 idle。

若需要停止新增的 PostgreSQL cluster，可执行 `wsl -d Ubuntu -u root -- pg_ctlcluster 16 main stop`。此命令不删除 role、数据库、邮件或卷；不得使用 `wsl --shutdown`、Docker reset 或目录清理代替停止。当前 Android 验证完成前保持服务运行。

当前 venv 全部测试必须显式配置 `NOWUS_ENV=test` 和指向 `nowus120_test` 的 `NOWUS_DATABASE_URL`，以及所需开发配置。fixture 会清空账号与 OTP 表，不得连接实际账号数据库。

本地 API / Mailpit 保持运行时，可重复执行：

```powershell
backend/.venv/Scripts/python.exe backend/scripts/acceptance.py
```

Docker engine 恢复后仍需按仓库 PostgreSQL 18 基线复验：

```powershell
docker compose up -d --build db mailpit api
docker compose --profile test run --build --rm tests
python backend/scripts/acceptance.py
```

验收脚本默认仅连接 localhost API 和 Mailpit，应继续使用本地邮件 sink；本次没有发送外部邮箱邮件。
