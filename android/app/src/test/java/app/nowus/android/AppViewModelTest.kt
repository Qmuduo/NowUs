package app.nowus.android
import app.nowus.android.data.StateRepository
import app.nowus.android.domain.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModelTest {
 private val dispatcher=StandardTestDispatcher()
 @Before fun setup(){ Dispatchers.setMain(dispatcher) }
 @After fun tearDown(){ Dispatchers.resetMain() }
 @Test fun failedSaveDoesNotSignalSuccessAndCanRetry()=runTest(dispatcher){
  val repo=FakeRepository(); val vm=AppViewModel(repo); runCurrent()
  repo.fail=true; var success=false
  vm.saveNote("draft") { success=true }; runCurrent()
  assertFalse(success); assertNull(vm.state.value?.note); assertNotNull(vm.error.value)
  repo.fail=false; vm.saveNote("draft") {success=true}; runCurrent()
  assertTrue(success); assertEquals("draft",vm.state.value?.note?.text)
 }
 @Test fun loadFailureIsVisibleAndRetryLoads()=runTest(dispatcher){
  val repo=FakeRepository(); repo.failLoad=true
  val vm=AppViewModel(repo); runCurrent(); assertNotNull(vm.error.value)
  repo.failLoad=false; vm.retry(); runCurrent(); assertNotNull(vm.state.value)
 }
 @Test fun temporaryUsesRealTimeEvenIfDemoClockIsActive()=runTest(dispatcher){
  val repo=FakeRepository();val vm=AppViewModel(repo);runCurrent()
  vm.setDemo(true);val before=java.time.Instant.now().toEpochMilli()
  vm.temporary(true,30);runCurrent()
  val temporary=repo.data.value.temporary!!
  assertTrue(temporary.fromMillis>=before);assertEquals(1800000L,temporary.untilMillis-temporary.fromMillis)
 }
 @Test fun wrongInviteRetainsStateAndExplicitPartnerRemainsUnknown()=runTest(dispatcher){
  val repo=FakeRepository();val vm=AppViewModel(repo);runCurrent()
  vm.createInvite();runCurrent();val invite=repo.data.value.invite!!
  vm.acceptInvite("wrong",Profile("小雨","new-york"));runCurrent()
  assertNull(repo.data.value.partner);assertEquals(invite,repo.data.value.invite);assertEquals("邀请码错误",vm.error.value)
  vm.acceptInvite(invite.code,Profile("小雨","new-york"));runCurrent()
  assertEquals("小雨",repo.data.value.partner?.name);assertNull(repo.data.value.partnerSchedule)
  vm.samplePartner();runCurrent();assertNotNull(repo.data.value.partnerSchedule)
 }
 @Test fun localImportWaitsForConfirmationAndCopiesOnlyOwnedFields()=runTest(dispatcher){
  val demo=FakeRepository(AppState(
   me=Profile("体验本人","beijing"),
   schedule=Schedule(Rhythm(activity="演示活动"),Rhythm(activity="休息")),
   note=Note("本人演示留言",100),
   partner=Profile("模拟伴侣","new-york"),
   invite=Invite("DEMO123456",10_000),
  ))
  val account=FakeRepository(AppState(
   me=Profile("真实本人","london"),
   partner=Profile("真实伴侣","paris"),
   paired=true,
   invite=Invite("REAL123456",20_000),
  ))
  val vm=AppViewModel(account,demo);runCurrent()
  vm.inspectLocalImport();runCurrent()
  assertNotNull(vm.localImport.value)
  assertEquals("真实本人",account.data.value.me.name)

  vm.importLocalData();runCurrent()
  assertEquals("体验本人",account.data.value.me.name)
  assertEquals("真实伴侣",account.data.value.partner?.name)
  assertEquals("REAL123456",account.data.value.invite?.code)
  assertEquals("本人演示留言",account.data.value.note?.text)
 }
 private class FakeRepository(initial:AppState=AppState(Profile("我","beijing"))):StateRepository{
  var fail=false; var failLoad=false
  val data=MutableStateFlow(initial)
  override val states:Flow<AppState> get()=flow {if(failLoad) error("load"); emitAll(data)}
  override suspend fun update(transform:(AppState)->AppState){if(fail) error("save");data.value=transform(data.value)}
 }
}
