package app.nowus.android.data

import app.nowus.android.domain.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class AccountRepositoryTest {
    @Test fun staleCacheHidesPartnerDataButPreservesOwnDataAndTimestamp() = runTest {
        val cached = AppState(
            me = Profile("小舟", "beijing"),
            setupComplete = true,
            partner = Profile("小雨", "new-york"),
            partnerSchedule = Schedule(Rhythm(), Rhythm()),
            note = Note("我的留言", 1_700_000_000_000),
            partnerNote = Note("对方留言", 1_700_000_000_001),
            paired = true,
            lastSyncMillis = 1_700_000_000_100,
        )
        val api = FakeApi().apply { snapshotError = IOException("offline") }
        val repository = AccountRepository(api, AccountSession("token", "user-a", Long.MAX_VALUE), MemoryCache(cached), pollIntervalMillis = 60_000)

        val stale = repository.states.first()

        assertTrue(stale.syncStale)
        assertEquals(1_700_000_000_100, stale.lastSyncMillis)
        assertEquals("小舟", stale.me.name)
        assertEquals("我的留言", stale.note?.text)
        assertTrue(stale.paired)
        assertNull(stale.partner)
        assertNull(stale.partnerSchedule)
        assertNull(stale.partnerNote)
    }

    @Test fun failedServerSaveLeavesAccountSnapshotUnchanged() = runTest {
        val api = FakeApi().apply { failNoteSave = true }
        val cache = MemoryCache()
        val repository = AccountRepository(api, AccountSession("token", "user-a", Long.MAX_VALUE), cache, pollIntervalMillis = 60_000)
        repository.states.first()

        val failure = runCatching {
            repository.update { it.copy(note = Note("草稿保留", Instant.now().toEpochMilli())) }
        }.exceptionOrNull()

        assertNotNull(failure)
        assertNull(repository.states.first().note)
        assertNull(cache.value?.note)
    }

    @Test fun clientCannotEditPartnerOrServerOwnedPairState() = runTest {
        val repository = AccountRepository(FakeApi(), AccountSession("token", "user-a", Long.MAX_VALUE), MemoryCache(), pollIntervalMillis = 60_000)
        repository.states.first()

        val failure = runCatching {
            repository.update { it.copy(partner = Profile("第三人", "london")) }
        }.exceptionOrNull()

        assertTrue(failure is IllegalArgumentException)
        assertNull(repository.states.first().partner)
    }

    private class MemoryCache(var value: AppState? = null) : AccountSnapshotStore {
        override suspend fun load(userId: String) = value
        override suspend fun save(userId: String, state: AppState) { value = state }
        override suspend fun clear() { value = null }
    }

    private class FakeApi : AccountApi {
        var snapshotError: Exception? = null
        var failNoteSave = false
        private val state = ApiSnapshot(
            userId = "user-a",
            me = ApiUserData(Profile("小舟", "beijing"), setupComplete = true),
            paired = false,
            pairStatus = "not_paired",
            sharingEnabled = true,
            serverTime = "2026-09-30T00:00:00Z",
        )

        override suspend fun requestOtp(email: String) = 60
        override suspend fun verifyOtp(email: String, code: String) = AccountSession("token", "user-a", Long.MAX_VALUE)
        override suspend fun snapshot(token: String): ApiSnapshot { snapshotError?.let { throw it }; return state }
        override suspend fun saveProfile(token: String, profile: Profile) = Unit
        override suspend fun saveSchedule(token: String, schedule: Schedule) = Unit
        override suspend fun saveNote(token: String, text: String) { if (failNoteSave) throw IOException("offline") }
        override suspend fun deleteNote(token: String) = Unit
        override suspend fun setTemporary(token: String, available: Boolean, minutes: Int) = Unit
        override suspend fun resetTemporary(token: String) = Unit
        override suspend fun markSetupComplete(token: String) = Unit
        override suspend fun setSharing(token: String, enabled: Boolean) = Unit
        override suspend fun unpair(token: String) = Unit
        override suspend fun createInvitation(token: String) = Invite("23456789AB", 1_900_000_000_000)
        override suspend fun revokeInvitation(token: String) = Unit
        override suspend fun previewInvitation(code: String) = InvitePreview(code, Profile("邀请人", "beijing"), listOf("资料"), 1_900_000_000_000)
        override suspend fun acceptInvitation(token: String, code: String) = Unit
        override suspend fun logout(token: String) = Unit
    }
}
