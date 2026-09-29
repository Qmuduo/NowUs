# NowUs Android 本地体验实现计划

已确认 Kotlin + Jetpack Compose，Android 优先。沿用已验证的森林绿、昼夜配色与三页布局。真实邮箱验证码及两账号同步属于下一阶段，当前所有配对必须明确标为演示。

## Global Constraints

- 工程位于 android/，包名 app.nowus.android；Gradle 8.13、AGP 8.13.2、Kotlin 2.3.21、Compose BOM 2026.06.01；minSdk 26、compile/targetSdk 36、Java target 17。
- 同一高度必须是同一 Instant；按 IANA 时区与本地日期选工作日或休息日，不能用固定 UTC 差。
- 通常作息、主动状态、联系意愿独立；未知不能推导为空闲。活动段各自连续。
- 正常使用真实时间，固定时间只在明确演示模式；本地保存失败需可见，不能显示虚假成功。
- 不接入假邮箱登录，不发送消息，不生成未填对方资料；不安装新全局开发软件。

### Task 1: 时间模型与本地状态规则

Files: android/app/src/main/java/app/nowus/android/domain/Model.kt、TimeEngine.kt、Rules.kt；相应 src/test/.../domain/*Test.kt。

- [ ] 先写并运行有意义的失败 JUnit 测试，再实现纯 Kotlin 逻辑。
- [ ] 序列化模型：Profile(name, cityId), Rhythm(sleepStart="23:00",sleepEnd="07:00",activity="上班",activityStart="09:00",activityEnd="18:00",contactKnown=true,contactStart="20:00",contactEnd="22:30"); Schedule(weekday,rest); TemporaryStatus(available,untilMillis); Note(text,updatedMillis); Invite(code,expiresMillis,revoked); AppState(me, schedule, setupComplete, partner nullable, partnerSchedule nullable, note nullable, invite nullable, temporary nullable)。城市集合包括北京、上海、纽约、伦敦、巴黎、东京、悉尼、加德满都，对应真实 IANA zones。
- [ ] API TimeEngine.dayBounds(date,zone): Window(start,end), TimeEngine.activityAt(profile,schedule?,temporary?,instant): Activity(label,source), contactAt(...): Boolean?, commonWindows(start,end,me,meSchedule,temporary,partner,partnerSchedule): List<Window>, segments(start,end,profile,schedule?): List<Segment(start,end,activity)>。使用半开区间、分钟精度联系窗口；当前秒数不偏移真实分钟边界。
- [ ] 跨午夜睡眠/联系、DST 23/25 小时、本地日期跨日、非整小时偏移、临时状态到期、未知无窗口、活动独立连续必须有测试。模板上班/上课区间不得跨日，不能与睡眠重叠；联系时段不与睡眠重叠，活动可以和联系重叠。拒绝空/超长昵称、非法城市、相同起止时间、非法时间。按 Unicode codepoints 限制昵称 20 / 留言 120。
- [ ] Rules.validateProfile、validateRhythm、saveNote、createInvite、revokeInvite、acceptInvite。邀请 24 小时有效，唯一随机代码由调用方传入；错误/过期/撤销/已配对拒绝且保留状态；接受后 partnerSchedule=null。示例作息只经显式操作填入。
- [ ] Gradle unit tests green，写 task-1-report.md，提交仅本任务文件。

### Task 2: 原生界面与持久化

Files: android/app/.../data/LocalRepository.kt、MainActivity.kt、AppViewModel.kt、ui/Theme.kt、ui/NowUsApp.kt、ui/Timeline.kt；src/androidTest/.../LocalRepositoryTest.kt、AppFlowTest.kt。

- [ ] DataStore Preferences 保存 JSON，加载/保存错误有可见反馈；ViewModel 提供状态和事件，UI 不直接读写存储。
- [ ] 三个页签「此刻／我们的一天／我的节奏」，采用 Compose Material3 基础组件与自定义森林绿主题，窗口安全区、滚动、键盘与系统返回。
- [ ] 首次填写昵称/城市→工作日休息日节奏→演示邀请/接收→成功，允许先独自使用。演示邀请可更新/撤销，输入错误反馈；对方未知保持未知，显式填入示例作息。
- [ ] 首页双时钟/双日期/准确时差/状态来源，当前或未来七天窗口含双日期时间，自己一条留言可编辑删除，临时可联系/忙碌 30/60/180 分钟与恢复，资料/城市可编辑。
- [ ] 真实时间随前台刷新；固定演示开关明确可退出、重启不保持。邀请使用真实时间避免演示造成失效误判。
- [ ] 时间轴同一瞬间对齐，默认附近/全天，日期切换，活动分别连续、未知来源明确，当前指示与共同窗口，短块可点看详情，大字体不隐藏必要信息。
- [ ] 先写可失败的持久化/Compose 流程测试，验证首次使用、未知→示例、留言重建恢复、临时状态，以及保存错误行为。
- [ ] 单测、assembleDebug、connectedDebugAndroidTest 与 lintDebug 成功。写任务报告并提交。

### Task 3: 审查与交付

Files: android/README.md、docs/superpowers/plans/2026-09-29-nowus-android-verification.md、README.md、已确认产品规格。

- [ ] 独立 review 时间规则与整个 diff，修复重要问题后重新运行相关检查。
- [ ] APK 与截图放 artifacts/（忽略构建产物），记录版本/测试证据/真实功能边界，提供安装与构建说明。
- [ ] 现有 Node 原型测试保持通过；新源码提交推送，主仓库可以查看。交付 APK 链接与下一步真实账号同步。
