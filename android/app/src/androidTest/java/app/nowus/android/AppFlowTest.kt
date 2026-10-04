package app.nowus.android
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import app.nowus.android.data.StateRepository
import app.nowus.android.domain.*
import app.nowus.android.ui.*
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.test.platform.app.InstrumentationRegistry
import android.graphics.Bitmap
import java.io.File
import kotlinx.coroutines.flow.*
import org.junit.*

class AppFlowTest {
 @get:Rule val compose=createComposeRule()
 @Test fun revokedSharingClosesOpenPartnerActivity(){
  val repo=MemoryRepository(sampleState());val vm=AppViewModel(repo);vm.setDemo(true)
  compose.setContent{NowUsTheme{NowUsApp(vm,realAccount=true)}}
  compose.onNodeWithText("一天").performClick()
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
  compose.onNodeWithText(partnerNote.text).assertExists()
  compose.onNodeWithText("留话").performClick()
  compose.onNodeWithText("写一张便签").performScrollTo().performClick()
  compose.onNodeWithTag("noteDraft").performTextInput("好，周末一起去")
  compose.onNodeWithText("保存便签").performClick()
  compose.runOnIdle{
   Assert.assertEquals("好，周末一起去",repo.data.value.note?.text)
   Assert.assertEquals(partnerNote,repo.data.value.partnerNote)
  }
  compose.onNodeWithText("编辑便签").performScrollTo().performClick()
  compose.onNodeWithTag("noteDraft").assertTextContains("好，周末一起去")
 }
 @Test fun largeFontHomeKeepsBothClocksAndReplyReachable(){
  val repo=MemoryRepository(sampleState().copy(partnerNote=Note("想你了",System.currentTimeMillis())))
  val vm=AppViewModel(repo);vm.setDemo(true)
  compose.setContent{val density=LocalDensity.current;CompositionLocalProvider(LocalDensity provides Density(density.density,2f)){NowUsTheme{NowUsApp(vm,realAccount=true)}}}
  compose.onNodeWithTag("clock-partner").performScrollTo().assertIsDisplayed()
  compose.onNodeWithTag("clock-self").performScrollTo().assertIsDisplayed()
  compose.onNodeWithText("留话").performClick()
  compose.onNodeWithText("写一张便签").performScrollTo().performClick()
  compose.onNodeWithTag("noteDraft").assertIsDisplayed()
  compose.onNodeWithText("收起").performScrollTo().performClick()
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
  compose.onNodeWithText("3 小时").assertExists()
  capture("nowus-status.png")
  compose.onNodeWithText("1 小时").performClick()
  compose.onNodeWithText("保存状态").performClick()
  compose.runOnIdle{val status=repo.data.value.temporary!!;Assert.assertFalse(status.available);Assert.assertEquals(3600000L,status.untilMillis-status.fromMillis)}
  compose.onNodeWithText("暂不方便").performClick()
  compose.onNodeWithText("跟随通常偏好").performClick()
  compose.onNodeWithText("保存状态").performClick()
  compose.runOnIdle{Assert.assertNull(repo.data.value.temporary)}
 }
 @Test fun invitationRejectsWrongAndRevokedCodeThenAcceptsNewCode(){
  val repo=MemoryRepository(AppState(Profile("阿青","beijing"),defaultSchedule(),true));val vm=AppViewModel(repo)
  compose.setContent{NowUsTheme{NowUsApp(vm)}}
  compose.onNodeWithText("邀请伴侣").performClick()
  compose.waitUntil(5_000){compose.onAllNodesWithText("本机演示邀请",substring=true).fetchSemanticsNodes().isNotEmpty()}
  capture("nowus-local-invite.png")
  compose.onNodeWithText("创建演示邀请").performScrollTo().performClick()
  compose.waitUntil(5_000){repo.data.value.invite!=null}
  capture("nowus-local-invite-active.png")
  compose.onNodeWithTag("profileName").performScrollTo().performTextInput("小雨")
  compose.onNodeWithText("城市：请选择 ▾").performScrollTo().performClick()
  compose.onNodeWithText("纽约 · America/New_York").performClick()
  compose.onNodeWithText("输入本机演示邀请码").performScrollTo().performTextInput("WRONG")
  compose.onNodeWithText("接受演示邀请").performScrollTo().performClick()
  compose.onAllNodesWithText("邀请码错误").onFirst().assertExists()
  val revokedCode=compose.runOnIdle{repo.data.value.invite!!.code}
  compose.onNodeWithText("撤销演示邀请").performScrollTo().performClick()
  compose.onNodeWithText("输入本机演示邀请码").performScrollTo().performTextReplacement(revokedCode)
  compose.onNodeWithText("接受演示邀请").performScrollTo().performClick()
  compose.onAllNodesWithText("邀请已撤销").onFirst().assertExists()
  compose.onNodeWithText("更新演示邀请").performScrollTo().performClick()
  val code=compose.runOnIdle{repo.data.value.invite!!.code}
  compose.onNodeWithText("输入本机演示邀请码").performScrollTo().performTextReplacement(code)
  compose.onNodeWithText("接受演示邀请").performScrollTo().performClick()
  compose.onAllNodesWithText("作息待填写").onFirst().assertExists()
  compose.runOnIdle{Assert.assertEquals("小雨",repo.data.value.partner?.name);Assert.assertNull(repo.data.value.partnerSchedule)}
 }
 @Test fun captureHomeAndTimeline(){
  val reference=sampleState().copy(me=Profile("林默","beijing"),partner=Profile("阿远","new-york"),partnerNote=Note("我找到你说的那家唱片店了，地址发你，周末一起去？",java.time.Instant.parse("2026-09-29T11:45:00Z").toEpochMilli()),note=Note("晚上再记下路边那家店名，等下次见面时讲给你听。",java.time.Instant.parse("2026-09-29T12:00:00Z").toEpochMilli()))
  val repo=MemoryRepository(reference);val vm=AppViewModel(repo);vm.setDemo(true)
  compose.setContent{NowUsTheme{NowUsApp(vm)}}
  compose.waitUntil(10000){compose.onAllNodesWithText("10:00–10:30").fetchSemanticsNodes().isNotEmpty()}
  compose.onNodeWithText("22:00–22:30").assertExists()
  compose.onNodeWithText("下一段可能适合联系的时间").assertDoesNotExist()
  capture("nowus-home.png")
  compose.onNodeWithText("一天").performClick()
  compose.onNodeWithText("我的状态").assertIsDisplayed()
  compose.waitUntil(10000){compose.onAllNodesWithText("每一段的详情").fetchSemanticsNodes().isNotEmpty()}
  compose.onNodeWithText("细绿色标记对应各自的联系偏好；它不代表在线，也不表示双方已经约好。", substring=true).assertExists()
  capture("nowus-timeline.png")
  compose.onNodeWithText("留话").performClick()
  compose.onNodeWithText("留给彼此").assertIsDisplayed()
  capture("nowus-note.png")
  compose.onNodeWithText("晚上再记下路边那家店名，等下次见面时讲给你听。").performScrollTo().assertIsDisplayed()
  capture("nowus-own-note.png")
  compose.onNodeWithTag("note-open-editor").performScrollTo().performClick()
  compose.onNodeWithTag("noteDraft").assertIsDisplayed()
  capture("nowus-note-editor.png")
 }
 @Test fun captureProfileSettingsAndRhythm(){
  val vm=AppViewModel(MemoryRepository(sampleState().copy(paired=true)))
  compose.setContent{NowUsTheme{NowUsApp(vm)}}
  compose.onNodeWithContentDescription("我的资料与设置").performClick()
  compose.onNodeWithText("我的日常").assertIsDisplayed()
  capture("nowus-profile.png")
  compose.onNodeWithText("我的节奏").performScrollTo().performClick()
  compose.waitUntil(5_000){compose.onAllNodesWithText("安排好平常的一天，就不用每天填写。").fetchSemanticsNodes().isNotEmpty()}
  compose.onNodeWithText("安排好平常的一天，就不用每天填写。").assertIsDisplayed()
  capture("nowus-rhythm.png")
 }
 @Test fun sameDayNightAndMorningUseDesignGreeting(){
  val reference=sampleState().copy(me=Profile("小满","beijing"),partner=Profile("阿远","new-york"))
  val vm=AppViewModel(MemoryRepository(reference));vm.setDemo(true)
  compose.setContent{NowUsTheme{NowUsApp(vm)}}
  compose.onNodeWithText("你的夜晚，他的早晨。",useUnmergedTree=true).assertIsDisplayed()
 }
 @Test fun longNamesAndMaximumNoteRemainAvailable(){
  val partnerNote=Note("想你 🌙".repeat(30),System.currentTimeMillis())
  val repo=MemoryRepository(sampleState().copy(me=Profile("我".repeat(20),"kathmandu"),partner=Profile("伴".repeat(20),"new-york"),partnerNote=partnerNote))
  val vm=AppViewModel(repo)
  compose.setContent{NowUsTheme{NowUsApp(vm)}}
  compose.onNodeWithTag("clock-self").assertIsDisplayed()
  compose.onNodeWithTag("clock-partner").assertIsDisplayed()
  Assert.assertTrue(compose.onAllNodesWithText("我".repeat(20), substring=true, useUnmergedTree=true).fetchSemanticsNodes().isNotEmpty())
  Assert.assertTrue(compose.onAllNodesWithText("伴".repeat(20), substring=true, useUnmergedTree=true).fetchSemanticsNodes().isNotEmpty())
  compose.onNodeWithText("加德满都").assertExists()
  capture("nowus-long-profile.png")
  compose.onNodeWithText("留话").performClick()
  compose.onNodeWithContentDescription("给 ${repo.data.value.me.name}：${partnerNote.text}。${repo.data.value.partner!!.name}",substring=true).performScrollTo().assertExists()
  capture("nowus-long-note.png")
 }
 @Test fun largeFontsKeepProfileAndRhythmReachable(){
  val repo=MemoryRepository(sampleState());val vm=AppViewModel(repo)
  compose.setContent{val density=LocalDensity.current;CompositionLocalProvider(LocalDensity provides Density(density.density,2f)){NowUsTheme{NowUsApp(vm)}}}
  compose.onNodeWithContentDescription("我的资料与设置").performClick()
  compose.onNodeWithText("我的节奏").performScrollTo().performClick()
  compose.onNodeWithText("保存我的节奏").performScrollTo().assertIsDisplayed()
  compose.onNodeWithTag("back-to-profile").assertExists()
  compose.onNodeWithTag("back-to-profile").performScrollTo().performClick()
  compose.onNodeWithText("我的",useUnmergedTree=true).assertIsDisplayed()
  compose.onNodeWithText("昵称与城市").performScrollTo().performClick()
  compose.onNodeWithTag("profileName").assertTextContains("阿青")
  compose.onNodeWithText("关闭").performScrollTo().performClick()
  capture("nowus-large-font.png")
  compose.onNodeWithText("此刻").performClick()
  compose.onNodeWithText("我的状态").performClick()
  compose.onNodeWithText("暂时不方便").performClick()
  compose.onNodeWithText("3 小时").assertIsDisplayed()
  compose.onNodeWithContentDescription("返回").performClick()
  compose.onNodeWithText("一天").performClick()
  compose.onNodeWithText("后一天").assertIsDisplayed().performClick()
 }
 private fun capture(name:String){
  val instrumentation=InstrumentationRegistry.getInstrumentation()
  instrumentation.waitForIdleSync()
  Thread.sleep(1_500)
  val bitmap=instrumentation.uiAutomation.takeScreenshot()
  val directory=instrumentation.targetContext.externalCacheDir!!
  File(directory,name).outputStream().use{bitmap.compress(Bitmap.CompressFormat.PNG,100,it)}
 }
 private fun sampleState()=AppState(Profile("阿青","beijing"),defaultSchedule(),true,Profile("小雨","new-york"),Schedule(Rhythm(activity="上课",activityEnd="17:00",contactStart="10:00",contactEnd="10:30"),Rhythm(sleepEnd="08:00",activity="休息",activityStart="10:00",activityEnd="12:00",contactStart="10:00",contactEnd="22:00")))
 @Test fun firstUseCanFinishAlone(){
  val repo=MemoryRepository(AppState(Profile("","beijing")))
  val vm=AppViewModel(repo)
  compose.setContent {NowUsTheme {NowUsApp(vm)}}
  compose.onNodeWithTag("profileName").performTextInput("阿青")
  compose.onNodeWithText("城市：请选择 ▾").performScrollTo().performClick()
  compose.onNodeWithText("北京 · Asia/Shanghai").performClick()
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
  compose.onNodeWithContentDescription("我的资料与设置").performClick()
   compose.onNodeWithText("我的节奏").performScrollTo().performClick()
   compose.onNodeWithText("使用学生或上班族参考模板").performScrollTo().performClick()
   compose.onNodeWithText("上班族模板").performClick()
  compose.onNodeWithText("应用上班族模板？").assertIsDisplayed()
  capture("nowus-template-confirm.png")
  compose.onNodeWithText("应用模板").performClick()
  compose.onAllNodesWithText("上午上班").onFirst().performScrollTo().assertExists()
  capture("nowus-rhythm.png")
  capture("nowus-routine-template.png")
  compose.onAllNodesWithText("午餐").onFirst().performScrollTo().assertExists()
  compose.onNodeWithTag("rhythm-weekday-dinner-row").performScrollTo().performClick()
  compose.onNodeWithTag("rhythm-weekday-dinner-start").assertTextEquals("20:00")
  compose.onNodeWithText("关闭").performClick()
  compose.onNodeWithTag("rhythm-weekday-breakfast-row").performScrollTo().performClick()
  compose.onNodeWithTag("rhythm-weekday-breakfast-label").performTextReplacement("早饭")
  compose.onNodeWithText("保存时段").performClick()
  compose.onNodeWithText("＋ 添加时段").performScrollTo().performClick()
  compose.onNodeWithTag("rhythm-weekday-custom-1-label").performTextReplacement("健身")
  compose.onNodeWithText("保存时段").performClick()
  compose.onNodeWithText("保存我的节奏").performScrollTo().performClick()
  compose.waitUntil(5_000){repo.data.value.schedule != null || vm.error.value != null}
  compose.runOnIdle{
    Assert.assertNull(vm.error.value)
    val saved=repo.data.value.schedule!!
    Assert.assertTrue(Rules.validateRhythm(saved.weekday).valid)
   Assert.assertEquals("上班族",saved.templateId)
   Assert.assertTrue(saved.weekday.blocks.any{it.label=="上午上班"})
   Assert.assertEquals("早饭",saved.weekday.blocks.single{it.id=="breakfast"}.label)
   Assert.assertEquals("健身",saved.weekday.blocks.single{it.id=="custom-1"}.label)
   Assert.assertEquals("20:00",saved.weekday.blocks.single{it.id=="dinner"}.start)
  }
 }
 @Test fun routineCategoryMenuSavesExerciseAndCategoriesHaveDistinctColors(){
  val persistedCategories=RoutineCategory.entries.filter { it!=RoutineCategory.UNSCHEDULED }
  Assert.assertEquals(persistedCategories.size,persistedCategories.map(::routineCategoryColor).distinct().size)

  val schedule=RoutineTemplates.forCity("beijing")
  val repo=MemoryRepository(AppState(Profile("阿青","beijing"),schedule,true));val vm=AppViewModel(repo);vm.setDemo(true)
  compose.setContent{NowUsTheme{NowUsApp(vm)}}
  compose.onNodeWithContentDescription("我的资料与设置").performClick()
  compose.onNodeWithText("我的节奏").performScrollTo().performClick()
  compose.onNodeWithTag("rhythm-weekday-breakfast-row").performScrollTo().performClick()
  compose.onNodeWithTag("rhythm-weekday-breakfast-category").performClick()
  compose.onNodeWithText("运动").performClick()
  compose.onNodeWithText("保存时段").performClick()
  compose.onNodeWithText("保存我的节奏").performScrollTo().performClick()
  compose.runOnIdle{
   Assert.assertEquals(RoutineCategory.EXERCISE,repo.data.value.schedule!!.weekday.blocks.single { it.id=="breakfast" }.category)
  }
  compose.waitUntil(5_000){vm.state.value?.schedule?.weekday?.blocks?.singleOrNull { it.id=="breakfast" }?.category==RoutineCategory.EXERCISE}
  compose.onNodeWithText("一天").performClick()
  compose.onNodeWithText("北京的全天").performClick()
  compose.waitUntil(10_000){compose.onAllNodesWithContentDescription("早餐 · 运动",substring=true).fetchSemanticsNodes().isNotEmpty()}
  compose.onAllNodesWithContentDescription("早餐 · 运动",substring=true).onFirst().assertExists()
 }
 @Test fun unknownPartnerRemainsUnknownAndOwnNoteCanBeSaved(){
  val repo=MemoryRepository(AppState(Profile("阿青","beijing"),Schedule(Rhythm(),Rhythm(sleepEnd="08:00",activity="休息",activityStart="10:00",activityEnd="12:00",contactStart="10:00",contactEnd="22:00")),true,Profile("小雨","new-york")))
  val vm=AppViewModel(repo)
  compose.setContent {NowUsTheme {NowUsApp(vm)}}
  compose.onAllNodesWithText("作息待填写").onFirst().assertExists()
  compose.onNodeWithText("填入对方示例作息").assertDoesNotExist()
  compose.onNodeWithText("留话").performClick()
  compose.onNodeWithText("写一张便签").performScrollTo().performClick()
  compose.onNodeWithTag("noteDraft").performTextInput("明天一起散步")
  compose.onNodeWithText("保存便签").performClick()
  compose.onNodeWithText("明天一起散步").assertExists()
  compose.runOnIdle {Assert.assertEquals("明天一起散步",repo.data.value.note?.text)}
 }
 @Test fun failedSaveKeepsNoteDraftOpen(){
  val repo=MemoryRepository(AppState(Profile("阿青","beijing"),setupComplete=true));repo.fail=true
  val vm=AppViewModel(repo)
  compose.setContent {NowUsTheme {NowUsApp(vm)}}
  compose.onNodeWithText("留话").performClick()
  compose.onNodeWithText("写一张便签").performScrollTo().performClick()
  compose.onNodeWithTag("noteDraft").performTextInput("保留草稿")
  compose.onNodeWithText("保存便签").performClick()
  compose.onNodeWithTag("noteDraft").assertTextContains("保留草稿")
  compose.onAllNodesWithText("保存失败，请重试").onFirst().assertExists()
 }
 @Test fun failedAccountSaveKeepsDraftAndDoesNotShowSuccess(){
  val repo=MemoryRepository(AppState(Profile("阿青","beijing"),setupComplete=true));repo.fail=true
  val vm=AppViewModel(repo)
  compose.setContent{NowUsTheme{NowUsApp(vm,realAccount=true)}}
  compose.onNodeWithText("留话").performClick()
  compose.onNodeWithText("写一张便签").performScrollTo().performClick()
  compose.onNodeWithTag("noteDraft").performTextInput("真实账号离线草稿")
  compose.onNodeWithText("保存便签").performClick()
  compose.onNodeWithTag("noteDraft").assertTextContains("真实账号离线草稿")
  compose.onAllNodesWithText("保存失败，请重试").onFirst().assertExists()
  compose.runOnIdle{Assert.assertNull(repo.data.value.note)}
 }
 private class MemoryRepository(initial:AppState):StateRepository{
  val data=MutableStateFlow(initial);var fail=false;var failAfterEmission=false
  override val states:Flow<AppState> get()=flow{if(failAfterEmission){emit(data.value);error("read failed after cached state")};emitAll(data)}
  override suspend fun update(transform:(AppState)->AppState){if(fail) error("save");data.value=transform(data.value)}
 }
}
