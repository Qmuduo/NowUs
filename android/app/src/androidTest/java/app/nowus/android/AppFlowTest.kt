package app.nowus.android
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import app.nowus.android.data.StateRepository
import app.nowus.android.domain.*
import app.nowus.android.ui.*
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.test.platform.app.InstrumentationRegistry
import android.graphics.Bitmap
import java.io.File
import kotlinx.coroutines.flow.*
import org.junit.*

class AppFlowTest {
 @get:Rule val compose=createComposeRule()
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
  compose.onNodeWithText("设置临时联系意愿").performScrollTo().performClick()
  compose.onNodeWithText("忙碌").performClick()
  compose.onNodeWithText("180分").assertIsDisplayed()
  compose.onNodeWithText("60分").performClick()
  compose.onNodeWithText("保存临时意愿").performClick()
  compose.runOnIdle{val status=repo.data.value.temporary!!;Assert.assertFalse(status.available);Assert.assertEquals(3600000L,status.untilMillis-status.fromMillis)}
  compose.onNodeWithText("恢复通常联系偏好").performScrollTo().performClick()
  compose.runOnIdle{Assert.assertNull(repo.data.value.temporary)}
 }
 @Test fun invitationRejectsWrongAndRevokedCodeThenAcceptsNewCode(){
  val repo=MemoryRepository(AppState(Profile("阿青","beijing"),defaultSchedule(),true));val vm=AppViewModel(repo)
  compose.setContent{NowUsTheme{NowUsApp(vm)}}
  compose.onNodeWithText("创建演示邀请").performScrollTo().performClick()
  compose.onNodeWithTag("profileName").performScrollTo().performTextInput("小雨")
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
  compose.onNodeWithText("对方作息待补充").assertExists()
  compose.runOnIdle{Assert.assertEquals("小雨",repo.data.value.partner?.name);Assert.assertNull(repo.data.value.partnerSchedule)}
 }
 @Test fun captureHomeAndTimeline(){
  val repo=MemoryRepository(sampleState());val vm=AppViewModel(repo);vm.setDemo(true)
  compose.setContent{NowUsTheme{NowUsApp(vm)}}
  compose.waitUntil(10000){compose.onAllNodesWithText("此刻可以一起").fetchSemanticsNodes().isNotEmpty()}
  capture("nowus-home.png")
  compose.onNodeWithText("我们的一天").performClick()
  compose.waitUntil(10000){compose.onAllNodesWithText("每一段的详情").fetchSemanticsNodes().isNotEmpty()}
  compose.onNodeWithText("固定演示时间 · 2026/9/29 · 不代表此刻").assertExists()
  capture("nowus-timeline.png")
 }
 @Test fun largeFontsKeepProfileAndRhythmReachable(){
  val repo=MemoryRepository(sampleState());val vm=AppViewModel(repo)
  compose.setContent{val density=LocalDensity.current;CompositionLocalProvider(LocalDensity provides Density(density.density,2f)){NowUsTheme{NowUsApp(vm)}}}
  compose.onNodeWithText("我的节奏").performClick()
  compose.onNodeWithText("保存我的节奏").performScrollTo().assertIsDisplayed()
  compose.onNodeWithText("编辑昵称与城市").performScrollTo().performClick()
  compose.onNodeWithTag("profileName").assertTextContains("阿青")
  compose.onNodeWithText("关闭").performScrollTo().performClick()
  capture("nowus-large-font.png")
  compose.onNodeWithText("此刻").performClick()
  compose.onNodeWithText("设置临时联系意愿").performScrollTo().performClick()
  compose.onNodeWithText("180分").performScrollTo().assertIsDisplayed()
  compose.onNodeWithText("关闭").performScrollTo().performClick()
  compose.onNodeWithText("我们的一天").performClick()
  compose.onNodeWithText("后一天").assertIsDisplayed().performClick()
 }
 private fun capture(name:String){
  val bitmap=compose.onRoot().captureToImage().asAndroidBitmap()
  val directory=InstrumentationRegistry.getInstrumentation().targetContext.externalCacheDir!!
  File(directory,name).outputStream().use{bitmap.compress(Bitmap.CompressFormat.PNG,100,it)}
 }
 private fun sampleState()=AppState(Profile("阿青","beijing"),defaultSchedule(),true,Profile("小雨","new-york"),Schedule(Rhythm(activity="上课",activityEnd="17:00",contactStart="10:00",contactEnd="10:30"),Rhythm(sleepEnd="08:00",activity="休息",activityStart="10:00",activityEnd="12:00",contactStart="10:00",contactEnd="22:00")))
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
 @Test fun unknownPartnerRequiresExplicitSampleAndSavedNote(){
  val repo=MemoryRepository(AppState(Profile("阿青","beijing"),Schedule(Rhythm(),Rhythm(sleepEnd="08:00",activity="休息",activityStart="10:00",activityEnd="12:00",contactStart="10:00",contactEnd="22:00")),true,Profile("小雨","new-york")))
  val vm=AppViewModel(repo)
  compose.setContent {NowUsTheme {NowUsApp(vm)}}
  compose.onNodeWithText("对方作息待补充").assertExists()
  compose.onNodeWithText("填入对方示例作息").performScrollTo().performClick()
  compose.onNodeWithText("对方作息待补充").assertDoesNotExist()
  compose.onNodeWithText("写留言").performScrollTo().performClick()
  compose.onNodeWithTag("noteDraft").performTextInput("明天一起散步")
  compose.onNodeWithText("保存留言").performClick()
  compose.onNodeWithText("明天一起散步").assertExists()
  compose.runOnIdle {Assert.assertEquals("明天一起散步",repo.data.value.note?.text)}
 }
 @Test fun failedSaveKeepsNoteDraftOpen(){
  val repo=MemoryRepository(AppState(Profile("阿青","beijing"),setupComplete=true));repo.fail=true
  val vm=AppViewModel(repo)
  compose.setContent {NowUsTheme {NowUsApp(vm)}}
  compose.onNodeWithText("写留言").performScrollTo().performClick()
  compose.onNodeWithTag("noteDraft").performTextInput("保留草稿")
  compose.onNodeWithText("保存留言").performClick()
  compose.onNodeWithTag("noteDraft").assertTextContains("保留草稿")
  compose.onAllNodesWithText("保存失败，请重试").onFirst().assertExists()
 }
 @Test fun failedAccountSaveKeepsDraftAndDoesNotShowSuccess(){
  val repo=MemoryRepository(AppState(Profile("阿青","beijing"),setupComplete=true));repo.fail=true
  val vm=AppViewModel(repo)
  compose.setContent{NowUsTheme{NowUsApp(vm,realAccount=true)}}
  compose.onNodeWithText("写留言").performScrollTo().performClick()
  compose.onNodeWithTag("noteDraft").performTextInput("真实账号离线草稿")
  compose.onNodeWithText("保存留言").performClick()
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
