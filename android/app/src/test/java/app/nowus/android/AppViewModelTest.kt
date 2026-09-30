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
 private class FakeRepository:StateRepository{
  var fail=false; var failLoad=false
  val data=MutableStateFlow(AppState(Profile("我","beijing")))
  override val states:Flow<AppState> get()=flow {if(failLoad) error("load"); emitAll(data)}
  override suspend fun update(transform:(AppState)->AppState){if(fail) error("save");data.value=transform(data.value)}
 }
}
