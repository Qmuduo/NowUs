package app.nowus.android

import app.nowus.android.data.AccountSession
import app.nowus.android.data.ApiException
import app.nowus.android.data.AuthenticationApi
import app.nowus.android.data.SessionStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun tearDown() { Dispatchers.resetMain() }

    @Test fun invalidCodeKeepsEmailAndCodeAndShowsSpecificFeedback() = runTest(dispatcher) {
        val api = FakeAuthenticationApi().apply { verifyFailure = ApiException(400, "otp_invalid") }
        val sessions = MemorySessionStore()
        val vm = AuthViewModel(api, sessions)
        vm.setEmail("pair@example.net")
        vm.requestOtp(); runCurrent()
        vm.setCode("135790")
        vm.verifyOtp(); runCurrent()

        assertEquals("pair@example.net", vm.email.value)
        assertEquals("135790", vm.code.value)
        assertEquals("验证码错误，请再试一次", vm.error.value)
        assertNull(sessions.current.value)
    }

    @Test fun successfulOtpPersistsTheSessionAndStartsTheAccountFlow() = runTest(dispatcher) {
        val session = AccountSession("opaque-token", "user-1", 1_800_000_000_000L)
        val api = FakeAuthenticationApi().apply { verifiedSession = session }
        val sessions = MemorySessionStore()
        val vm = AuthViewModel(api, sessions)
        vm.setEmail("pair@example.net")
        vm.requestOtp(); runCurrent()
        vm.setCode("001234")
        vm.verifyOtp(); runCurrent()

        assertEquals(session, sessions.current.value)
        assertEquals(0, vm.cooldownSeconds.value)
        assertNull(vm.error.value)
    }

    @Test fun responseTimestampFailureExplainsThatTheConsumedCodeMustBeResent() = runTest(dispatcher) {
        val api = FakeAuthenticationApi().apply {
            verifyFailure = java.time.format.DateTimeParseException("invalid timestamp", "2026-10-30T07:36:18+00:00", 19)
        }
        val sessions = MemorySessionStore()
        val vm = AuthViewModel(api, sessions)
        vm.setEmail("pair@example.net")
        vm.requestOtp(); runCurrent()
        vm.setCode("135790")
        vm.verifyOtp(); runCurrent()

        assertEquals("登录信息的时间格式不兼容，请重新发送验证码后重试", vm.error.value)
        assertNull(sessions.current.value)
    }

    private class FakeAuthenticationApi : AuthenticationApi {
        var verifyFailure: Exception? = null
        var verifiedSession = AccountSession("token", "user", 1_800_000_000_000L)
        override suspend fun requestOtp(email: String): Int = 60
        override suspend fun verifyOtp(email: String, code: String): AccountSession {
            verifyFailure?.let { throw it }
            return verifiedSession
        }
    }

    private class MemorySessionStore : SessionStore {
        override val current = MutableStateFlow<AccountSession?>(null)
        override suspend fun save(session: AccountSession) { current.value = session }
        override suspend fun clear() { current.value = null }
    }
}
