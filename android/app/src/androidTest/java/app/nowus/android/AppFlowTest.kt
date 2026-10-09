package app.nowus.android
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import app.nowus.android.data.StateRepository
import app.nowus.android.data.EncryptedNoteDraftStore
import app.nowus.android.domain.*
import app.nowus.android.ui.*
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import android.graphics.Bitmap
import java.io.File
import kotlinx.coroutines.flow.*
import org.junit.*

class AppFlowTest {
 @get:Rule val compose=createComposeRule()
 @Before fun clearDraftStore(){InstrumentationRegistry.getInstrumentation().targetContext.getSharedPreferences("nowus_encrypted_note_drafts",0).edit().clear().commit()}
 @After fun clearDraftStoreAfterTest(){InstrumentationRegistry.getInstrumentation().targetContext.getSharedPreferences("nowus_encrypted_note_drafts",0).edit().clear().commit()}
 @Test fun encryptedNoteDraftSurvivesStoreRecreationAndRemainsAccountScoped(){
  val context=InstrumentationRegistry.getInstrumentation().targetContext
  EncryptedNoteDraftStore(context).save("account:test-user-a","应用重启恢复的私密草稿")
  val preferences=context.getSharedPreferences("nowus_encrypted_note_drafts",0)
  Assert.assertFalse(preferences.all.values.single().toString().contains("应用重启恢复的私密草稿"))
  val restored=EncryptedNoteDraftStore(context)
  Assert.assertEquals("应用重启恢复的私密草稿",restored.load("account:test-user-a"))
  Assert.assertNull(restored.load("account:test-user-b"))
  restored.clear("account:test-user-a")
  Assert.assertNull(restored.load("account:test-user-a"))
 }
 private fun openSettings(){compose.onNodeWithContentDescription("打开我的设置").performScrollTo().performClick()}
 private fun openRhythm(){openSettings();compose.onNodeWithText("我的节奏").performScrollTo().performClick()}
 private fun openTimeline(){compose.onNodeWithText("一天").performClick()}
 private fun openNotes(){compose.onNodeWithText("留话").performClick()}
 @Test fun revokedSharingClosesOpenPartnerActivity(){
  val repo=MemoryRepository(sampleState());val vm=AppViewModel(repo);vm.setDemo(true)
  compose.setContent{NowUsTheme{NowUsApp(vm,realAccount=true)}}
  openTimeline()
  compose.waitUntil(10000){compose.onAllNodesWithContentDescription("小雨 上课",substring=true).fetchSemanticsNodes().isNotEmpty()}
  compose.onNodeWithContentDescription("小雨 上课",substring=true).performScrollTo().performClick()
  compose.onNodeWithText("知道了").assertExists()
  compose.runOnIdle{repo.data.value=repo.data.value.copy(partner=null,partnerSchedule=null,sharingPaused=true)}
  compose.onNodeWithText("知道了").assertDoesNotExist()
  compose.onAllNodesWithText("小雨",substring=true).assertCountEquals(0)
 }
 @Test fun partnerMessageReplySavesMyNoteWithoutChangingPartnerMessage(){
  val partnerNote=Note("周末一起去唱片店？",System.currentTimeMillis())
  val repo=MemoryRepository(sampleState().copy(partnerNote=partnerNote));val vm=AppViewModel(repo)
  compose.setContent{NowUsTheme{NowUsApp(vm,realAccount=true)}}
  openNotes()
  compose.onNodeWithText(partnerNote.text).assertExists()
  compose.onNodeWithText("写下第一张便签").performScrollTo().performClick()
  compose.onNodeWithTag("noteDraft").performTextInput("好，周末一起去")
  compose.onNodeWithText("贴上这张便签").performClick()
  compose.runOnIdle{
   Assert.assertEquals("好，周末一起去",repo.data.value.note?.text)
   Assert.assertEquals(partnerNote,repo.data.value.partnerNote)
  }
  compose.onNodeWithContentDescription("编辑我的便签").performScrollTo().performClick()
  compose.onNodeWithTag("noteDraft").assertTextContains("好，周末一起去")
 }
 @Test fun largeFontHomeKeepsBothClocksAndReplyReachable(){
  val repo=MemoryRepository(sampleState().copy(partnerNote=Note("想你了",System.currentTimeMillis())))
  val vm=AppViewModel(repo);vm.setDemo(true)
  compose.setContent{val density=LocalDensity.current;CompositionLocalProvider(LocalDensity provides Density(density.density,2f)){NowUsTheme{NowUsApp(vm,realAccount=true)}}}
  compose.onNodeWithTag("clock-partner").performScrollTo().assertIsDisplayed()
  compose.onNodeWithTag("clock-self").performScrollTo().assertIsDisplayed()
  openNotes()
  compose.onNodeWithText("写下第一张便签").performScrollTo().performClick()
  compose.onNodeWithTag("noteDraft").assertIsDisplayed()
  compose.onNodeWithContentDescription("关闭弹层").performClick()
 }
 @Test fun loadedReadFailureOffersRetry(){
  val repo=MemoryRepository(sampleState());repo.failAfterEmission=true;val vm=AppViewModel(repo)
  compose.setContent{NowUsTheme{NowUsApp(vm)}}
  compose.onAllNodesWithText("加载失败，请重试").onFirst().assertExists()
  repo.failAfterEmission=false
  compose.onNodeWithText("重试加载").performScrollTo().performClick()
  compose.onAllNodesWithText("加载失败，请重试").assertCountEquals(0)
 }
 @Test fun temporaryContactCanBeSetAndReset(){
  val repo=MemoryRepository(AppState(Profile("阿青","beijing"),defaultSchedule(),true));val vm=AppViewModel(repo)
  compose.setContent{NowUsTheme{NowUsApp(vm)}}
  compose.onNodeWithText("我的状态").performClick()
  compose.onNodeWithText("暂时不方便").performClick()
  compose.onNodeWithText("1 小时").performClick()
  compose.onNodeWithText("保存状态").performClick()
  compose.runOnIdle{val status=repo.data.value.temporary!!;Assert.assertFalse(status.available);Assert.assertEquals(3600000L,status.untilMillis-status.fromMillis)}
  compose.onNodeWithText("暂时不方便").performClick()
  compose.onNodeWithText("跟随通常偏好").performClick()
  compose.onNodeWithText("保存状态").performClick()
  compose.runOnIdle{Assert.assertNull(repo.data.value.temporary)}
 }
 @Test fun invitationRejectsWrongAndRevokedCodeThenAcceptsNewCode(){
  val repo=MemoryRepository(AppState(Profile("阿青","beijing"),defaultSchedule(),true));val vm=AppViewModel(repo)
  compose.setContent{NowUsTheme{NowUsApp(vm)}}
  compose.onNodeWithText("给彼此留一个位置").performScrollTo().performClick()
  compose.onNodeWithText("创建演示邀请").performScrollTo().performClick()
  compose.onNodeWithText("我收到了一份邀请").performScrollTo().performClick()
  compose.onNodeWithTag("profileName").performScrollTo().performTextInput("小雨")
  compose.onNodeWithText("输入本机演示邀请码").performScrollTo().performTextInput("WRONG")
  compose.onNodeWithText("接受演示邀请").performScrollTo().performClick()
  compose.onAllNodesWithText("邀请码错误").onFirst().assertExists()
  val revokedCode=compose.runOnIdle{repo.data.value.invite!!.code}
  compose.onNodeWithContentDescription("关闭弹层").performClick()
  compose.onNodeWithText("撤销邀请").performScrollTo().performClick()
  compose.onNodeWithText("我收到了一份邀请").performScrollTo().performClick()
  compose.onNodeWithText("输入本机演示邀请码").performScrollTo().performTextReplacement(revokedCode)
  compose.onNodeWithText("接受演示邀请").performScrollTo().performClick()
  compose.onAllNodesWithText("邀请已撤销").onFirst().assertExists()
  compose.onNodeWithContentDescription("关闭弹层").performClick()
  compose.onNodeWithText("创建演示邀请").performScrollTo().performClick()
  val code=compose.runOnIdle{repo.data.value.invite!!.code}
  compose.onNodeWithText("我收到了一份邀请").performScrollTo().performClick()
  compose.onNodeWithText("输入本机演示邀请码").performScrollTo().performTextReplacement(code)
  compose.onNodeWithText("接受演示邀请").performScrollTo().performClick()
  compose.waitUntil(10_000){repo.data.value.partner!=null||vm.error.value!=null}
  capture("debug-invite-accepted.png")
  Assert.assertEquals("accept should persist the partner; error=${vm.error.value}","小雨",repo.data.value.partner?.name)
  Assert.assertNull(repo.data.value.partnerSchedule)
  compose.onAllNodesWithText("作息待补充").onFirst().assertExists()
 }
 @Test fun captureHomeAndTimeline(){
  val repo=MemoryRepository(daylightReferenceState());val vm=AppViewModel(repo);vm.setDemo(true);vm.now.value=java.time.Instant.parse("2026-10-03T14:17:00Z")
  compose.setContent{NowUsTheme{NowUsApp(vm)}}
  compose.waitUntil(10000){compose.onAllNodesWithText("09:00 – 09:30").fetchSemanticsNodes().isNotEmpty()}
  compose.onNodeWithText("21:00 – 21:30").assertExists()
  capture("nowus-home.png")
  openTimeline()
  compose.waitUntil(10000){compose.onAllNodesWithContentDescription("按真实时长排列的双方时间轴").fetchSemanticsNodes().isNotEmpty()}
  capture("nowus-timeline.png")
 }
 @Test fun largeFontsKeepProfileAndRhythmReachable(){
  val repo=MemoryRepository(sampleState());val vm=AppViewModel(repo)
  compose.setContent{val density=LocalDensity.current;CompositionLocalProvider(LocalDensity provides Density(density.density,2f)){NowUsTheme{NowUsApp(vm)}}}
  openSettings()
  compose.onNodeWithText("昵称与城市").performScrollTo().performClick()
  compose.onNodeWithTag("profileName").assertTextContains("阿青")
  compose.onNodeWithText("关闭").performScrollTo().performClick()
  compose.onNodeWithText("我的节奏").performScrollTo().performClick()
  compose.onNodeWithText("保存我的节奏").performScrollTo().assertIsDisplayed()
  capture("nowus-large-font.png")
  compose.onNodeWithContentDescription("返回").performClick()
  openTimeline()
  compose.onNodeWithContentDescription("后一天").assertIsDisplayed().performClick()
 }
 private fun capture(name:String){
  compose.waitForIdle();Thread.sleep(250)
  val directory=InstrumentationRegistry.getInstrumentation().targetContext.externalCacheDir!!
  Assert.assertTrue(UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).takeScreenshot(File(directory,name)))
 }
 private fun sampleState()=AppState(Profile("阿青","beijing"),defaultSchedule(),true,Profile("小雨","new-york"),Schedule(Rhythm(activity="上课",activityEnd="17:00",contactStart="10:00",contactEnd="10:30"),Rhythm(sleepEnd="08:00",activity="休息",activityStart="10:00",activityEnd="12:00",contactStart="10:00",contactEnd="22:00")))
 private fun daylightReferenceState():AppState {
  val referenceNow=java.time.Instant.parse("2026-10-03T14:17:00Z")
  val me=Profile("小满","beijing")
  val partner=Profile("阿远","new-york")
  val meDay=Rhythm(
   sleepStart="23:30",sleepEnd="09:00",activity="自己的时间",activityStart="09:00",activityEnd="23:30",contactStart="21:00",contactEnd="23:00",
   blocks=listOf(
    RoutineBlock("sleep","睡眠","00:00","09:00",RoutineCategory.SLEEP),
    RoutineBlock("day","自己的时间","09:00","23:30",RoutineCategory.REST),
   ),
  )
  val partnerDay=Rhythm(
   sleepStart="23:00",sleepEnd="08:00",activity="读书",activityStart="09:00",activityEnd="11:30",contactStart="09:00",contactEnd="09:30",
   blocks=listOf(
    RoutineBlock("sleep","睡眠","23:00","08:00",RoutineCategory.SLEEP),
    RoutineBlock("breakfast","早餐","08:00","09:00",RoutineCategory.MEAL),
    RoutineBlock("reading","读书","09:00","11:30",RoutineCategory.STUDY_WORK),
    RoutineBlock("lunch","午餐","11:30","13:00",RoutineCategory.MEAL),
    RoutineBlock("personal","自己的时间","13:00","18:00",RoutineCategory.REST),
    RoutineBlock("dinner","晚餐","18:00","19:00",RoutineCategory.MEAL),
    RoutineBlock("personal-evening","个人时间","19:00","23:00",RoutineCategory.REST),
   ),
  )
  return AppState(
   me=me,schedule=Schedule(meDay,meDay,"daylight-v4.1"),setupComplete=true,
   partner=partner,partnerSchedule=Schedule(partnerDay,partnerDay,"daylight-v4.1"),
   note=Note("等你下课，想听听你今天的小事。",referenceNow.minusSeconds(600).toEpochMilli()),
   partnerNote=Note("今天的第一杯咖啡，\n替你也喝了一口。",referenceNow.minusSeconds(127*60).toEpochMilli()),
   paired=true,
  )
 }
 private fun daylightRhythmSchedule():Schedule{
  val weekday=Rhythm(
   sleepStart="23:30",sleepEnd="07:30",activity="工作",activityStart="09:00",activityEnd="18:00",contactStart="21:00",contactEnd="23:00",
   blocks=listOf(
    RoutineBlock("sleep","睡眠","23:30","07:30",RoutineCategory.SLEEP),
    RoutineBlock("work","工作","09:00","18:00",RoutineCategory.STUDY_WORK),
    RoutineBlock("evening","晚餐与自己的时间","18:00","23:30",RoutineCategory.REST),
   ),
  )
  return Schedule(weekday,weekday,"daylight-v4.1")
 }
 @Test fun firstUseCanFinishAlone(){
  val repo=MemoryRepository(AppState(Profile("","beijing")))
  val vm=AppViewModel(repo)
  compose.setContent {NowUsTheme {NowUsApp(vm)}}
  compose.onNodeWithTag("profileName").performTextInput("阿青")
  compose.onNodeWithText("下一步：我的节奏").performScrollTo().performClick()
  compose.onNodeWithText("保存节奏并继续").performScrollTo().performClick()
  compose.onNodeWithText("先独自使用").performScrollTo().performClick()
  compose.onNodeWithText("开始使用").performClick()
  compose.onNodeWithText("此刻").assertExists()
  compose.runOnIdle {Assert.assertTrue(repo.data.value.setupComplete);Assert.assertNull(repo.data.value.partner)}
 }
 @Test fun cityBasedRoutineTemplateCanBeChangedAndSaved(){
  val repo=MemoryRepository(AppState(Profile("阿青","paris"),setupComplete=true));val vm=AppViewModel(repo)
  compose.setContent{NowUsTheme{NowUsApp(vm)}}
  openRhythm()
  compose.onNodeWithText("应用参考作息模板").performScrollTo().performClick()
  compose.onNodeWithText("上班族模板").performClick()
  compose.onNodeWithText("应用模板").performClick()
  capture("nowus-routine-template.png")
  compose.onAllNodesWithText("上午上班").onFirst().performScrollTo().assertExists()
  compose.onAllNodesWithText("午餐").onFirst().performScrollTo().assertExists()
  compose.onNodeWithText("晚餐").performScrollTo().performClick()
  compose.onNodeWithTag("rhythm-edit-dinner-start").assertTextEquals("20:00")
  compose.onNodeWithText("完成").performClick()
  compose.onNodeWithText("早餐").performScrollTo().performClick()
  compose.onNodeWithTag("rhythm-edit-breakfast-label").performTextReplacement("早饭")
  compose.onNodeWithText("完成").performClick()
  compose.onNodeWithText("+ 添加时段").performScrollTo().performClick()
  compose.onNodeWithTag("rhythm-edit-custom-1-label").performTextReplacement("健身")
  compose.onNodeWithText("完成").performClick()
  compose.onNodeWithText("保存我的节奏").performScrollTo().performClick()
  compose.runOnIdle{
   val saved=repo.data.value.schedule!!
   Assert.assertEquals("上班族",saved.templateId)
   Assert.assertTrue(saved.weekday.blocks.any{it.label=="上午上班"})
   Assert.assertEquals("早饭",saved.weekday.blocks.single{it.id=="breakfast"}.label)
   Assert.assertEquals("健身",saved.weekday.blocks.single{it.id=="custom-1"}.label)
   Assert.assertEquals("20:00",saved.weekday.blocks.single{it.id=="dinner"}.start)
  }
 }
 @Test fun routineCategoryMenuSavesExerciseAndCategoriesHaveDistinctColors(){
  Assert.assertEquals(Night,routineCategoryColor(RoutineCategory.SLEEP))
  Assert.assertEquals(Daylight,routineCategoryColor(RoutineCategory.MEAL))
  Assert.assertEquals(Panel,routineCategoryColor(RoutineCategory.STUDY_WORK))

  val schedule=RoutineTemplates.forCity("beijing")
  val repo=MemoryRepository(AppState(Profile("阿青","beijing"),schedule,true));val vm=AppViewModel(repo);vm.setDemo(true)
  compose.setContent{NowUsTheme{NowUsApp(vm)}}
  openRhythm()
  compose.onNodeWithText("早餐").performScrollTo().performClick()
  compose.onNodeWithTag("rhythm-edit-breakfast-category").performClick()
  compose.onNodeWithText("运动").performClick()
  compose.onNodeWithText("完成").performClick()
  compose.onNodeWithText("保存我的节奏").performScrollTo().performClick()
  compose.runOnIdle{
   Assert.assertEquals(RoutineCategory.EXERCISE,repo.data.value.schedule!!.weekday.blocks.single { it.id=="breakfast" }.category)
  }
  compose.waitUntil(5_000){vm.state.value?.schedule?.weekday?.blocks?.singleOrNull { it.id=="breakfast" }?.category==RoutineCategory.EXERCISE}
  openTimeline()
  compose.onNodeWithText("我的当地全天").performClick()
  compose.waitUntil(10_000){compose.onAllNodesWithContentDescription("早餐 · 运动",substring=true).fetchSemanticsNodes().isNotEmpty()}
  compose.onAllNodesWithContentDescription("早餐 · 运动",substring=true).onFirst().assertExists()
 }
 @Test fun unknownPartnerRequiresExplicitSampleAndSavedNote(){
  val repo=MemoryRepository(AppState(Profile("阿青","beijing"),Schedule(Rhythm(),Rhythm(sleepEnd="08:00",activity="休息",activityStart="10:00",activityEnd="12:00",contactStart="10:00",contactEnd="22:00")),true,Profile("小雨","new-york")))
  val vm=AppViewModel(repo)
  compose.setContent {NowUsTheme {NowUsApp(vm)}}
  compose.onAllNodesWithText("作息待补充").onFirst().assertExists()
  openSettings()
  compose.onNodeWithText("填入对方示例作息").performScrollTo().performClick()
  compose.onNodeWithContentDescription("返回").performClick()
  compose.onAllNodesWithText("作息待补充").assertCountEquals(0)
  openNotes()
  compose.onNodeWithText("写下第一张便签").performScrollTo().performClick()
  compose.onNodeWithTag("noteDraft").performTextInput("明天一起散步")
  compose.onNodeWithText("贴上这张便签").performClick()
  compose.onNodeWithText("明天一起散步").assertExists()
  compose.runOnIdle {Assert.assertEquals("明天一起散步",repo.data.value.note?.text)}
 }
 @Test fun failedSaveKeepsNoteDraftOpen(){
  val repo=MemoryRepository(AppState(Profile("阿青","beijing"),setupComplete=true));repo.fail=true
  val vm=AppViewModel(repo)
  compose.setContent {NowUsTheme {NowUsApp(vm)}}
  openNotes()
  compose.onNodeWithText("写下第一张便签").performScrollTo().performClick()
  compose.onNodeWithTag("noteDraft").performTextInput("保留草稿")
  compose.onNodeWithText("贴上这张便签").performClick()
  compose.onNodeWithTag("noteDraft").assertTextContains("保留草稿")
  compose.onAllNodesWithText("保存失败，请重试").onFirst().assertExists()
 }
 @Test fun failedAccountSaveKeepsDraftAndDoesNotShowSuccess(){
  val repo=MemoryRepository(AppState(Profile("阿青","beijing"),setupComplete=true));repo.fail=true
  val vm=AppViewModel(repo)
  compose.setContent{NowUsTheme{NowUsApp(vm,realAccount=true)}}
  openNotes()
  compose.onNodeWithText("写下第一张便签").performScrollTo().performClick()
  compose.onNodeWithTag("noteDraft").performTextInput("真实账号离线草稿")
  compose.onNodeWithText("贴上这张便签").performClick()
  compose.onNodeWithTag("noteDraft").assertTextContains("真实账号离线草稿")
  compose.onAllNodesWithText("保存失败，请重试").onFirst().assertExists()
  compose.runOnIdle{Assert.assertNull(repo.data.value.note)}
 }
 @Test fun daylightTimelineSupportsNearbyFullDateNavigationAndDetails(){
  val repo=MemoryRepository(daylightReferenceState());val vm=AppViewModel(repo);vm.setDemo(true);vm.now.value=java.time.Instant.parse("2026-10-03T14:17:00Z")
  compose.setContent{NowUsTheme{NowUsApp(vm)}}
  openTimeline()
  compose.waitUntil(10_000){compose.onAllNodesWithContentDescription("我 自己的时间",substring=true).fetchSemanticsNodes().isNotEmpty()}
  compose.onNodeWithText("附近几小时").assertIsDisplayed()
  compose.onNodeWithText("我的当地全天").performClick()
  compose.waitUntil(10_000){compose.onAllNodesWithContentDescription("我 睡眠",substring=true).fetchSemanticsNodes().isNotEmpty()}
  compose.onNodeWithContentDescription("前一天").performClick()
  compose.waitUntil(10_000){compose.onAllNodesWithText("10/02").fetchSemanticsNodes().isNotEmpty()}
  compose.onNodeWithText("回到今天").assertIsDisplayed().performClick()
  compose.waitUntil(10_000){compose.onAllNodesWithText("10/03 · 今天").fetchSemanticsNodes().isNotEmpty()}
  compose.onNodeWithContentDescription("后一天").performClick()
  compose.waitUntil(10_000){compose.onAllNodesWithText("10/04").fetchSemanticsNodes().isNotEmpty()}
  capture("v41-day-next-date-android.png")
  compose.waitUntil(10_000){compose.onAllNodesWithContentDescription("我 睡眠",substring=true).fetchSemanticsNodes().isNotEmpty()}
  compose.onNodeWithText("回到今天").assertIsDisplayed()
  compose.onNodeWithContentDescription("我 睡眠",substring=true).performScrollTo().performClick()
  compose.onNodeWithText("活动安排不是实时行踪；有活动，也可以选择愿意联系。",substring=true).assertExists()
  compose.onNodeWithText("知道了").performClick()
 }
 @Test fun contactWindowActionOpensTheCorrectDayAndHour(){
  val repo=MemoryRepository(sampleState());val vm=AppViewModel(repo);vm.setDemo(true)
  compose.setContent{NowUsTheme{NowUsApp(vm)}}
  compose.waitUntil(10_000){compose.onAllNodesWithContentDescription("打开我的设置").fetchSemanticsNodes().isNotEmpty()}
  val expected=compose.runOnIdle{
   val now=vm.now.value
   val state=repo.data.value
   val window=TimeEngine.commonWindows(now,now.plusSeconds(7*86400),state.me,state.schedule,state.temporary,state.partner!!,state.partnerSchedule,state.partnerTemporary).first{it.end>now}
   window.start.atZone(state.me.zone())
  }
  compose.waitUntil(10_000){compose.onAllNodesWithText("查看这段时间",substring=true).fetchSemanticsNodes().isNotEmpty()}
  compose.onNodeWithText("查看这段时间",substring=true).performScrollTo().performClick()
  compose.onNodeWithText("放到一天里看看").performClick()
  val expectedDate="${expected.monthValue.toString().padStart(2,'0')}/${expected.dayOfMonth.toString().padStart(2,'0')}"
  compose.onAllNodesWithText(expectedDate,substring=true).onFirst().assertExists()
  compose.waitUntil(10_000){compose.onAllNodesWithContentDescription("按真实时长排列的双方时间轴").fetchSemanticsNodes().isNotEmpty()}
 }
 @Test fun noteDeleteConfirmationUndoAndDraftValidationWork(){
  val original=Note("保留给对方的一句话",System.currentTimeMillis())
  val repo=MemoryRepository(AppState(Profile("阿青","beijing"),defaultSchedule(),true,note=original));val vm=AppViewModel(repo)
  compose.setContent{NowUsTheme{NowUsApp(vm)}}
  openNotes()
  compose.onNodeWithText("删除我的便签").performScrollTo().performClick()
  compose.onNodeWithText("再想想").performClick()
  compose.runOnIdle{Assert.assertEquals(original,repo.data.value.note)}
  compose.onNodeWithText("删除我的便签").performScrollTo().performClick()
  compose.onNodeWithText("删除留言").performClick()
  compose.onNodeWithText("撤销").performClick()
  compose.runOnIdle{Assert.assertEquals(original,repo.data.value.note)}
  compose.waitUntil(5_000){!vm.saving.value}
  compose.onNodeWithContentDescription("编辑我的便签").performScrollTo().performClick()
  compose.onNodeWithTag("noteDraft").performTextReplacement("")
  compose.onNodeWithText("贴上这张便签").performClick()
  compose.onNodeWithText("留言不能为空").assertExists()
  compose.onNodeWithTag("noteDraft").performTextReplacement("留给你")
  compose.onNodeWithContentDescription("关闭弹层").performClick()
  compose.onNodeWithContentDescription("编辑我的便签").performScrollTo().performClick()
  compose.onNodeWithTag("noteDraft").assertTextContains("留给你")
  val tooLong=buildString{repeat(120){append('字')};append("😀")}
  compose.onNodeWithTag("noteDraft").performTextReplacement(tooLong)
  compose.onNodeWithText("121 / 120 字").assertExists()
  compose.onNodeWithText("贴上这张便签").performScrollTo().performClick()
  compose.waitUntil(5_000){vm.error.value=="留言最多 120 个字符"}
  compose.onNodeWithText("留言最多 120 个字符").assertExists()
  compose.runOnIdle{Assert.assertEquals(original,repo.data.value.note)}
 }
 @Test fun captureDaylightScreensForPageComparison(){
  val repo=MemoryRepository(daylightReferenceState());val vm=AppViewModel(repo);vm.setDemo(true);vm.now.value=java.time.Instant.parse("2026-10-03T14:17:00Z")
  compose.setContent{NowUsTheme{NowUsApp(vm)}}
  compose.waitUntil(10_000){compose.onAllNodesWithText("你的夜晚，他的早晨。",substring=true).fetchSemanticsNodes().isNotEmpty()}
  compose.waitUntil(10_000){compose.onAllNodesWithText("查看这段时间",substring=true).fetchSemanticsNodes().isNotEmpty()}
  capture("v41-home-android.png")
  openTimeline()
  compose.waitUntil(10_000){compose.onAllNodesWithContentDescription("我 自己的时间",substring=true).fetchSemanticsNodes().isNotEmpty()}
  capture("v41-day-nearby-android.png")
  compose.onNodeWithText("我的当地全天").performClick()
  compose.waitUntil(10_000){compose.onAllNodesWithText("睡眠").fetchSemanticsNodes().isNotEmpty()}
  capture("v41-day-full-android.png")
  openNotes()
  compose.waitUntil(10_000){compose.onAllNodesWithText("留给彼此").fetchSemanticsNodes().isNotEmpty()}
  capture("v41-note-android.png")
  compose.onNodeWithContentDescription("编辑我的便签").performScrollTo().performClick()
  compose.waitUntil(10_000){compose.onAllNodesWithText("贴上这张便签").fetchSemanticsNodes().isNotEmpty()}
  capture("v41-note-editor-android.png")
  compose.onNodeWithTag("noteDraft").performClick()
  compose.waitForIdle()
  capture("v41-note-editor-keyboard-android.png")
  compose.onNodeWithText("贴上这张便签").performScrollTo().assertIsDisplayed()
  compose.onNodeWithContentDescription("关闭弹层").performScrollTo().performClick()
  openSettings()
  compose.waitUntil(10_000){compose.onAllNodesWithText("应用图标与 Logo").fetchSemanticsNodes().isNotEmpty()}
  compose.onNodeWithText("我的").assertIsDisplayed()
  capture("v41-settings-android.png")
  compose.runOnIdle{repo.data.value=repo.data.value.copy(schedule=daylightRhythmSchedule())}
  compose.waitUntil(10_000){vm.state.value?.schedule?.weekday?.blocks?.size==3}
  compose.onNodeWithText("我的节奏").performScrollTo().performClick()
  compose.waitUntil(10_000){compose.onAllNodesWithText("睡眠 8 小时").fetchSemanticsNodes().isNotEmpty()}
  compose.onNodeWithText("通常的一天").assertIsDisplayed()
  compose.onNodeWithText("工作").assertExists()
  capture("v41-rhythm-android.png")
 }
 @Test fun captureInviteFlowAndBrandScreen(){
  val inviteNow=java.time.Instant.parse("2026-10-03T14:17:00Z")
  val repo=MemoryRepository(AppState(Profile("林默","beijing"),defaultSchedule(),true,invite=Invite("824619",inviteNow.plusSeconds(86400).toEpochMilli())));val vm=AppViewModel(repo);vm.setDemo(true);vm.now.value=inviteNow
  compose.setContent{NowUsTheme{NowUsApp(vm)}}
  compose.onNodeWithText("给彼此留一个位置").performScrollTo().performClick()
  compose.waitUntil(10_000){compose.onAllNodesWithText("从你的此刻，\n到你们的日常。",substring=true).fetchSemanticsNodes().isNotEmpty()}
  compose.waitUntil(10_000){compose.onAllNodesWithText("复制邀请代码").fetchSemanticsNodes().isNotEmpty()}
  compose.onNodeWithText("NU · 824 619").assertExists()
  compose.onNodeWithText("撤销邀请").assertExists()
  capture("v41-invite-android.png")
  compose.onNodeWithContentDescription("返回").performClick()
  compose.waitUntil(10_000){compose.onAllNodesWithText("应用图标与 Logo").fetchSemanticsNodes().isNotEmpty()}
  compose.onNodeWithText("应用图标与 Logo").performScrollTo().performClick()
  compose.waitUntil(10_000){compose.onAllNodesWithText("图标与 Logo").fetchSemanticsNodes().isNotEmpty()}
  capture("v41-brand-android.png")
 }
 @Test fun captureDaylightStatusActivityConfirmationAndInviteSheets(){
  val repo=MemoryRepository(daylightReferenceState());val vm=AppViewModel(repo)
  vm.setDemo(true)
  vm.now.value=java.time.Instant.parse("2026-10-03T14:22:00Z")
  compose.setContent{NowUsTheme{NowUsApp(vm)}}
  compose.onNodeWithText("我的状态").performClick()
  compose.waitUntil(10_000){compose.onAllNodesWithText("此刻，按你的节奏").fetchSemanticsNodes().isNotEmpty()}
  capture("v41-status-sheet-android.png")
  compose.onNodeWithContentDescription("关闭弹层").performClick()
  vm.now.value=java.time.Instant.parse("2026-10-03T14:17:00Z")
  openTimeline()
  compose.waitUntil(10_000){compose.onAllNodesWithContentDescription("按真实时长排列的双方时间轴").fetchSemanticsNodes().isNotEmpty()}
  compose.onNodeWithText("我的当地全天").performClick()
  compose.waitUntil(10_000){compose.onAllNodesWithContentDescription("我 睡眠",substring=true).fetchSemanticsNodes().isNotEmpty()}
  compose.onNodeWithContentDescription("我 睡眠",substring=true).performScrollTo().performClick()
  compose.waitUntil(10_000){compose.onAllNodesWithContentDescription("关闭弹层").fetchSemanticsNodes().isNotEmpty()}
  capture("v41-activity-detail-android.png")
  compose.onNodeWithContentDescription("关闭弹层").performClick()
  openSettings()
  compose.onNodeWithText("正在分享日常").performScrollTo().performClick()
  compose.waitUntil(10_000){compose.onAllNodesWithText("暂停分享日常？").fetchSemanticsNodes().isNotEmpty()}
  capture("v41-pause-confirm-android.png")
  compose.onNodeWithContentDescription("关闭弹层").performClick()
  compose.onNodeWithText("解除配对").performScrollTo().performClick()
  compose.waitUntil(10_000){compose.onAllNodesWithText("解除与阿远的配对？").fetchSemanticsNodes().isNotEmpty()}
  capture("v41-unpair-confirm-android.png")
  compose.onNodeWithContentDescription("关闭弹层").performClick()
  compose.onNodeWithText("邀请与配对流程").performScrollTo().performClick()
  compose.onNodeWithText("我收到了一份邀请").performScrollTo().performClick()
  compose.waitUntil(10_000){compose.onAllNodesWithText("接受伴侣邀请").fetchSemanticsNodes().isNotEmpty()}
  capture("v41-accept-invite-form-android.png")
 }
 @Test fun captureInviteAcceptancePreview(){
  val code="824619"
  val repo=MemoryRepository(AppState(Profile("林默","beijing"),defaultSchedule(),true))
  repo.previewValue=InvitePreview(
   code,
   Profile("小满","beijing"),
   listOf("分享城市、通常作息和联系偏好","分享你们各自的一条当前留言"),
   java.time.Instant.parse("2026-10-04T14:17:00Z").toEpochMilli(),
  )
  val vm=AppViewModel(repo)
  vm.now.value=java.time.Instant.parse("2026-10-03T14:17:00Z")
  compose.setContent{NowUsTheme{NowUsApp(vm,realAccount=true,pendingInviteCode=code)}}
  compose.waitUntil(10_000){compose.onAllNodesWithText("接受小满的邀请").fetchSemanticsNodes().isNotEmpty()}
  compose.onAllNodesWithText("分享城市、通常作息和联系偏好").onFirst().assertExists()
  compose.onNodeWithText("接受并配对").assertIsDisplayed()
  compose.onNodeWithText("暂时不接受").assertIsDisplayed()
  val titleTop=compose.onNodeWithText("接受小满的邀请").fetchSemanticsNode().boundsInRoot.top
  capture("v41-accept-invite-android.png")
  val shellOutput=InstrumentationRegistry.getInstrumentation().uiAutomation
   .executeShellCommand("screencap -p /sdcard/Download/v41-accept-invite-android.png")
  android.os.ParcelFileDescriptor.AutoCloseInputStream(shellOutput).use{it.readBytes()}
  Assert.assertTrue("acceptance sheet should match the reference's lower sheet position; titleTop=$titleTop",titleTop>=1600f)
 }
 private class MemoryRepository(initial:AppState):StateRepository{
  val data=MutableStateFlow(initial);var fail=false;var failAfterEmission=false
  var previewValue:InvitePreview?=null
  override val states:Flow<AppState> get()=flow{if(failAfterEmission){emit(data.value);error("read failed after cached state")};emitAll(data)}
  override suspend fun update(transform:(AppState)->AppState){if(fail) error("save");data.value=transform(data.value)}
  override suspend fun previewInvitation(code:String,nowMillis:Long):InvitePreview=previewValue?.takeIf{it.code==code}?:error("unknown test invitation")
 }
}
