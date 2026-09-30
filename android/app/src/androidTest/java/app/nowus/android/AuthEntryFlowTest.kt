package app.nowus.android

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import app.nowus.android.data.AccountApi
import app.nowus.android.data.AccountSession
import app.nowus.android.data.AccountSnapshotStore
import app.nowus.android.data.ApiSnapshot
import app.nowus.android.data.ApiUserData
import app.nowus.android.data.SessionStore
import app.nowus.android.data.StateRepository
import app.nowus.android.domain.*
import app.nowus.android.ui.NowUsEntry
import app.nowus.android.ui.NowUsTheme
import app.nowus.android.ui.defaultSchedule
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class AuthEntryFlowTest {
    @get:Rule val compose = createComposeRule()

    @Test fun emailOtpCreatesAccountAndRestoresPendingInviteContextAfterLogin() {
        val api = FakeAccountApi()
        val inviteCode = "ABC2345DEF"
        val sessions = MemorySessionStore()
        val local = MemoryRepository(AppState(Profile("", "beijing")))
        val auth = AuthViewModel(api, sessions)
        val demo = AppViewModel(local)
        compose.setContent {
            NowUsTheme {
                NowUsEntry(auth, sessions, api, MemorySnapshotStore(), demo, local, inviteCode, {}, {})
            }
        }

        compose.onNodeWithText("已保存邀请码 $inviteCode。登录后可查看邀请人和共享范围。").assertExists()
        compose.onNodeWithTag("authEmail").performTextInput("person@example.net")
        compose.onNodeWithText("发送 6 位验证码").performClick()
        compose.onNodeWithTag("authOtpCode").assertIsDisplayed()
        compose.onNodeWithTag("authOtpCode").performTextInput("246810")
        compose.onNodeWithText("验证并继续").performClick()
        compose.waitUntil(10_000) { sessions.current.value?.userId == "user-a" }
        compose.onNodeWithText("真实账号 · 作息与留言仅和已配对伴侣共享").assertIsDisplayed()
        compose.waitUntil(10_000) { api.previewedCode == inviteCode }
        compose.onNodeWithText("邀请来自：小舟 · 北京").assertExists()
        compose.runOnIdle {
            assertEquals("person@example.net", api.requestedEmail)
            assertEquals("246810", api.verifiedCode)
            assertEquals("user-a", sessions.current.value?.userId)
        }
    }

    @Test fun savedSessionRestoresAccountWithoutRequestingAnotherOtp() {
        val api = FakeAccountApi()
        val sessions = MemorySessionStore(AccountSession("restored-token", "user-a", System.currentTimeMillis() + 86_400_000))
        val local = MemoryRepository(AppState(Profile("", "beijing")))
        val auth = AuthViewModel(api, sessions)
        val demo = AppViewModel(local)
        compose.setContent {
            NowUsTheme {
                NowUsEntry(auth, sessions, api, MemorySnapshotStore(), demo, local, null, {}, {})
            }
        }

        compose.onNodeWithText("真实账号 · 作息与留言仅和已配对伴侣共享").assertIsDisplayed()
        compose.onNodeWithText("发送 6 位验证码").assertDoesNotExist()
    }

    @Test fun newAccountGetsEditableScheduleSuggestionsThatUploadOnlyWhenSaved() {
        val api = FakeAccountApi(freshAccount = true)
        val sessions = MemorySessionStore()
        val local = MemoryRepository(AppState(Profile("", "beijing")))
        val auth = AuthViewModel(api, sessions)
        val demo = AppViewModel(local)
        compose.setContent {
            NowUsTheme {
                NowUsEntry(auth, sessions, api, MemorySnapshotStore(), demo, local, null, {}, {})
            }
        }

        compose.onNodeWithTag("authEmail").performTextInput("new-person@example.net")
        compose.onNodeWithText("发送 6 位验证码").performClick()
        compose.onNodeWithTag("authOtpCode").performTextInput("246810")
        compose.onNodeWithText("验证并继续").performClick()
        compose.waitUntil(10_000) { sessions.current.value?.userId == "user-a" }

        compose.onNodeWithText("城市：请选择 ▾").assertExists()
        compose.onNodeWithTag("profileName").performTextInput("海外伴侣")
        compose.onNodeWithText("城市：请选择 ▾").performClick()
        compose.onNodeWithText("纽约 · America/New_York").performClick()
        compose.onNodeWithText("下一步：我的节奏").performScrollTo().performClick()

        compose.onNodeWithText("23:00").assertExists()
        compose.onNodeWithTag("rhythm-weekday-sleep-start").assertExists()
        compose.onNodeWithText("休息日").performClick()
        compose.onNodeWithText("08:00").assertExists()
        compose.onNodeWithTag("rhythm-rest-sleep-end").assertExists()
        compose.onNodeWithText("工作日").performClick()
        compose.onNodeWithTag("rhythm-weekday-sleep-start").performClick()
        compose.onNodeWithText("设置睡眠开始").assertExists()
        compose.onNodeWithTag("time-wheel-hour").performScrollToIndex(0)
        compose.onNodeWithTag("time-wheel-minute").performScrollToIndex(5)
        compose.onNodeWithText("00:05").assertExists()
        compose.onNodeWithText("确定").performClick()
        compose.onNodeWithText("00:05").assertExists()
        compose.runOnIdle { assertEquals(0, api.savedScheduleCount) }

        compose.onNodeWithText("保存节奏并继续").performScrollTo().performClick()
        compose.waitUntil(10_000) { api.savedScheduleCount == 1 }
        compose.runOnIdle {
            assertEquals(1, api.savedScheduleCount)
            org.junit.Assert.assertEquals("00:05", api.savedSchedule!!.weekday.sleepStart)
            org.junit.Assert.assertTrue(Rules.validateRhythm(api.savedSchedule!!.weekday).valid)
            org.junit.Assert.assertTrue(Rules.validateRhythm(api.savedSchedule!!.rest).valid)
        }
    }

    private class MemorySessionStore(initial: AccountSession? = null) : SessionStore {
        override val current = MutableStateFlow(initial)
        override suspend fun save(session: AccountSession) { current.value = session }
        override suspend fun clear() { current.value = null }
    }

    private class MemorySnapshotStore : AccountSnapshotStore {
        private val snapshots = mutableMapOf<String, AppState>()
        override suspend fun load(userId: String) = snapshots[userId]
        override suspend fun save(userId: String, state: AppState) { snapshots[userId] = state }
        override suspend fun clear() { snapshots.clear() }
    }

    private class MemoryRepository(initial: AppState) : StateRepository {
        private val data = MutableStateFlow(initial)
        override val states: Flow<AppState> = data
        override suspend fun update(transform: (AppState) -> AppState) { data.value = transform(data.value) }
    }

    private class FakeAccountApi(private val freshAccount: Boolean = false) : AccountApi {
        var requestedEmail: String? = null
        var verifiedCode: String? = null
        var previewedCode: String? = null
        var savedScheduleCount = 0
        var savedSchedule: Schedule? = null
        private var profile: Profile? = if (freshAccount) null else Profile("小舟", "beijing")
        private var schedule: Schedule? = if (freshAccount) null else defaultSchedule()
        private var setupComplete = !freshAccount
        override suspend fun requestOtp(email: String): Int { requestedEmail = email; return 60 }
        override suspend fun verifyOtp(email: String, code: String): AccountSession {
            requestedEmail = email
            verifiedCode = code
            return AccountSession("session-token", "user-a", System.currentTimeMillis() + 86_400_000)
        }
        override suspend fun snapshot(token: String) = ApiSnapshot(
            userId = "user-a",
            me = ApiUserData(profile, schedule, setupComplete = setupComplete),
            paired = false,
            pairStatus = "not_paired",
            sharingEnabled = true,
            serverTime = Instant.now().toString(),
        )
        override suspend fun saveProfile(token: String, profile: Profile) { this.profile = profile }
        override suspend fun saveSchedule(token: String, schedule: Schedule) { savedScheduleCount += 1; savedSchedule = schedule; this.schedule = schedule }
        override suspend fun saveNote(token: String, text: String) = Unit
        override suspend fun deleteNote(token: String) = Unit
        override suspend fun setTemporary(token: String, available: Boolean, minutes: Int) = Unit
        override suspend fun resetTemporary(token: String) = Unit
        override suspend fun markSetupComplete(token: String) { setupComplete = true }
        override suspend fun setSharing(token: String, enabled: Boolean) = Unit
        override suspend fun unpair(token: String) = Unit
        override suspend fun createInvitation(token: String) = Invite("23456789AB", System.currentTimeMillis() + 86_400_000)
        override suspend fun revokeInvitation(token: String) = Unit
        override suspend fun previewInvitation(code: String): InvitePreview {
            previewedCode = code
            return InvitePreview(code, Profile("小舟", "beijing"), listOf("个人资料"), System.currentTimeMillis() + 86_400_000)
        }
        override suspend fun acceptInvitation(token: String, code: String) = Unit
        override suspend fun logout(token: String) = Unit
    }
}
