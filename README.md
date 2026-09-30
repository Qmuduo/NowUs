# NowUs

面向跨时区情侣的日常节奏与陪伴 App。

帮助作息相对固定的学生和上班族情侣看懂彼此的一天，找到舒服的联系机会，并通过一条短留言表达关心。

## 当前阶段

已完成网页交互原型和 Android 本地体验版。本地开发后端已接入邮箱 OTP、两账号配对与资料同步；Android 构建和 13 项 Compose 仪器测试已在一台实体设备上通过。跨境网络、生产邮件与两台实体手机仍待外部资源和真实环境验证。Android 工程位于 [android/](android/README.md)。

- 首页优先「看见你的生活」，共同联系时间紧随其后。
- 区分通常作息、主动分享的当前状态和联系意愿。
- 以少量设置和简单的当天调整维持双方对照。
- 第一版采用一条短留言，不扩展成完整聊天系统。

## 产品设计

[产品设计文档](docs/superpowers/specs/2026-09-29-nowus-product-design.md)包含产品定位、MVP 范围、首页信息架构、时间轴规则、留言体验与验证方向。

## 体验原型

在仓库目录启动静态服务器：

```powershell
python -m http.server 8765 --bind 127.0.0.1
```

打开 [NowUs 原型](http://127.0.0.1:8765/prototype/)。页面使用 JavaScript 模块，需要通过 HTTP 打开。

- 「此刻」查看双方日期、时间、生活阶段与下一段共同时间。
- 「我们的一天」默认查看附近几小时，可展开全天；双方活动保持连续，点击活动查看独立的联系偏好。
- 「我的节奏」分别填写工作日／休息日的睡眠、上班或上课、愿意联系时间，保存后更新首页与时间轴。
- 「演示」切换日常、日期不同、今天没有共同窗口、临时忙碌和对方尚未设置五种场景。
- 「演示 → 体验首次使用与配对」走完昵称／城市、自己的节奏、演示邀请、接收与配对成功；也可[直接进入引导](http://127.0.0.1:8765/prototype/?flow=setup)。已配对时可点「重新体验首次使用」，新节奏保存前保留原记录。
- 等待邀请时不显示对方资料；接受后显示双方时钟，对方作息未填写则保持未知。模拟填写后可查看时间轴与共同窗口。
- 写一句留言，试试编辑、删除和撤销；调整自己的临时状态，观察共同窗口变化。

首页更早展示对方留言预览，联系窗口说明信息来源；今天没有后续交集时可查看未来七天的下一段窗口。资料不足时保持未知。

网页演示采用固定评审时间，伙伴作息使用示例数据。首次使用的昵称、城市、两套简化作息、当前留言和配对状态保存在当前浏览器，与原双人示例的作息／留言分开。网页演示邀请仍只在浏览器内模拟，不发送真实邀请或提醒。真实账号功能在 Android 工程中开发。

运行时间逻辑验证：

```powershell
node --test prototype/model.test.mjs prototype/pairing-model.test.mjs
```

[原型方向](docs/superpowers/specs/2026-09-29-nowus-prototype-direction.md) · [验证记录](docs/superpowers/plans/2026-09-29-nowus-prototype-review.md)

[正式 MVP 功能与验收清单](docs/superpowers/specs/2026-09-29-nowus-mvp-acceptance.md)明确真实配对、时间规则、分享控制、同步失败与首版取舍；[首次使用设计](docs/superpowers/specs/2026-09-29-nowus-onboarding-design.md)说明本轮流程。

原型已试用。Android 本地版本继续作为清晰隔离的演示入口；真实账号使用独立数据，不会自动上传演示人物、作息或邀请。

首批地区已确认「中国大陆一方 + 海外一方」。用户选择 **邮箱 + 6 位一次性验证码，Android 优先、iOS 后续；Android 使用 Kotlin + Jetpack Compose**。当前本地服务选用 FastAPI + PostgreSQL + Mailpit，方案取舍见[真实账号同步架构](docs/superpowers/specs/2026-09-30-nowus-real-sync-architecture.md)。

本地闭环覆盖 OTP、邀请预览与接受、双方作息／留言同步、C 账号权限拒绝、暂停分享和解除配对。它验证本地逻辑和 Mailpit 收件，不代表生产邮件或跨境可达。Compose 启动、数据库迁移、Android 构建与测试见 [Android 工程说明](android/README.md)。
