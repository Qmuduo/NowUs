package app.nowus.android.data

import app.nowus.android.domain.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class AccountRepositoryTest {
    @Test fun refreshPreservesLocalDraftFromCache() = runTest {
        val cached=kotlinx.serialization.json.Json { ignoreUnknownKeys=true }.decodeFromString<AppState>("""{"me":{"name":"我","cityId":"beijing"},"noteDraft":"unsent account"}""")
        val cache=MemoryCache(cached)
        val repository=AccountRepository(FakeApi(),AccountSession("token","user-a",Long.MAX_VALUE),cache,pollIntervalMillis=60_000)
        repository.states.first { !it.syncStale }
        assertTrue(kotlinx.serialization.json.Json.encodeToString(cache.value!!).contains("unsent account"))
    }

    @Test fun draftIsLocalAndSurvivesOfflineRefreshAndRepositoryRecreation() = runTest {
        val api=FakeApi();val cache=MemoryCache()
        val session=AccountSession("token","user-a",Long.MAX_VALUE)
        val repository=AccountRepository(api,session,cache,pollIntervalMillis=60_000)
        repository.states.first()
        api.snapshotError=IOException("offline")
        repository.updateNoteDraft("private unsent")
        assertEquals("private unsent",cache.value?.noteDraft)
        assertNull(api.snapshotResult.me.note)
        val reopened=AccountRepository(api,session,cache,pollIntervalMillis=60_000)
        assertEquals("private unsent",reopened.states.first().noteDraft)
    }

    @Test fun failedSaveKeepsDraftAndSuccessfulRetryClearsIt() = runTest {
        val api=FakeApi();val cache=MemoryCache()
        val repository=AccountRepository(api,AccountSession("token","user-a",Long.MAX_VALUE),cache,pollIntervalMillis=60_000)
        repository.states.first()
        api.failNoteSave=true
        assertNotNull(runCatching { repository.saveNote("unsent",123) }.exceptionOrNull())
        assertEquals("unsent",cache.value?.noteDraft)
        assertNull(cache.value?.note)
        api.failNoteSave=false
        repository.saveNote("published",124)
        assertNull(cache.value?.noteDraft)
        assertEquals("published",cache.value?.note?.text)
        assertEquals("2026-09-30T00:00:00.000001Z",cache.value?.note?.revision)
    }

    @Test fun acknowledgedSaveClearsDraftEvenIfFollowUpSnapshotFails() = runTest {
        val api=FakeApi();val cache=MemoryCache()
        val repository=AccountRepository(api,AccountSession("token","user-a",Long.MAX_VALUE),cache,pollIntervalMillis=60_000)
        repository.states.first()
        api.snapshotError=IOException("offline after acknowledgement")
        repository.saveNote("published",124)
        assertNull(cache.value?.noteDraft)
        assertEquals("published",cache.value?.note?.text)
        assertTrue(repository.states.first().syncStale)
    }

    @Test fun deleteAndRestoreUseExactServerRevisionAndNeverSaveOldText() = runTest {
        val api=FakeApi().apply {
            snapshotResult=snapshotResult.copy(me=snapshotResult.me.copy(note=ApiNote("old","2026-09-30T00:00:00.123456+00:00")))
        }
        val cache=MemoryCache()
        val repository=AccountRepository(api,AccountSession("token","user-a",Long.MAX_VALUE),cache,pollIntervalMillis=60_000)
        val note=repository.states.first().note!!
        assertEquals("2026-09-30T00:00:00.123456+00:00",note.revision)
        repository.deleteNote(note)
        assertNull(cache.value?.note)
        assertEquals(note,cache.value?.deletedNote)
        assertEquals(note.revision,api.deletedRevision)
        api.snapshotResult=api.snapshotResult.copy(me=api.snapshotResult.me.copy(note=ApiNote("newer","2026-09-30T00:00:00.223456+00:00")))
        val conflict=runCatching { repository.restoreNote(note) }.exceptionOrNull()
        assertTrue(conflict is ApiException)
        assertEquals("newer",api.snapshotResult.me.note?.text)
        assertEquals(0,api.noteSaves)
    }

    @Test fun successfulRestoreUsesRestoredServerRevision() = runTest {
        val api=FakeApi().apply { snapshotResult=snapshotResult.copy(me=snapshotResult.me.copy(note=ApiNote("old","2026-09-30T00:00:00.123456+00:00"))) }
        val cache=MemoryCache()
        val repository=AccountRepository(api,AccountSession("token","user-a",Long.MAX_VALUE),cache,pollIntervalMillis=60_000)
        val note=repository.states.first().note!!
        repository.deleteNote(note)
        repository.restoreNote(note)
        assertEquals("old",cache.value?.note?.text)
        assertEquals("2026-09-30T00:00:00.123457+00:00",cache.value?.note?.revision)
        assertNull(cache.value?.deletedNote)
        assertEquals(0,api.noteSaves)
    }

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

    @Test fun pauseClearsPartnerImmediatelyWhenFollowUpRefreshFails() = runTest {
        val api = FakeApi().apply { snapshotResult = pairedSnapshot() }
        val repository = AccountRepository(api, AccountSession("token", "user-a", Long.MAX_VALUE), MemoryCache(), pollIntervalMillis = 60_000)
        val observed = mutableListOf<AppState>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { repository.states.collect { observed += it } }
        runCurrent()
        assertEquals("小雨", observed.last().partner?.name)

        api.snapshotError = IOException("offline after pause")
        val failure = runCatching { repository.setSharing(false) }.exceptionOrNull()

        assertNotNull(failure)
        assertEquals(listOf(false), api.sharingChanges)
        assertNull(observed.last().partner)
        assertNull(observed.last().partnerSchedule)
        assertTrue(observed.last().sharingPaused)
        assertFalse(observed.last().sharingEnabled)
        assertTrue(observed.last().syncStale)
    }

    @Test fun unpairClearsPartnerImmediatelyWhenFollowUpRefreshFails() = runTest {
        val api = FakeApi().apply { snapshotResult = pairedSnapshot() }
        val repository = AccountRepository(api, AccountSession("token", "user-a", Long.MAX_VALUE), MemoryCache(), pollIntervalMillis = 60_000)
        val observed = mutableListOf<AppState>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { repository.states.collect { observed += it } }
        runCurrent()
        assertEquals("小雨", observed.last().partner?.name)

        api.snapshotError = IOException("offline after unpair")
        val failure = runCatching { repository.unpair() }.exceptionOrNull()

        assertNotNull(failure)
        assertEquals(1, api.unpairCalls)
        assertNull(observed.last().partner)
        assertNull(observed.last().partnerSchedule)
        assertFalse(observed.last().paired)
        assertFalse(observed.last().sharingPaused)
        assertNull(observed.last().invite)
        assertTrue(observed.last().syncStale)
    }

    @Test fun snapshotStartedBeforePauseCannotRestorePartnerDataAfterward() = runTest {
        val api = FakeApi().apply { snapshotResult = pairedSnapshot() }
        val repository = AccountRepository(api, AccountSession("token", "user-a", Long.MAX_VALUE), MemoryCache(), pollIntervalMillis = 60_000)
        val observed = mutableListOf<AppState>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { repository.states.collect { observed += it } }
        runCurrent()
        assertEquals("小雨", observed.last().partner?.name)

        val delayedSnapshot = CompletableDeferred<ApiSnapshot>()
        api.nextSnapshot = delayedSnapshot
        advanceTimeBy(60_000)
        runCurrent()
        assertEquals(2, api.snapshotCalls)

        repository.setSharing(false)
        assertNull(observed.last().partner)
        delayedSnapshot.complete(pairedSnapshot())
        runCurrent()

        assertNull(observed.last().partner)
        assertNull(observed.last().partnerSchedule)
        assertFalse(observed.last().sharingEnabled)
        assertTrue(observed.last().sharingPaused)
    }

    @Test fun snapshotCacheWriteCannotEraseNewerDraft() = runTest {
        val api=FakeApi()
        val started=CompletableDeferred<Unit>();val finish=CompletableDeferred<Unit>()
        var cached: AppState?=null
        var blockNext=false
        val cache=object:AccountSnapshotStore {
            override suspend fun load(userId:String)=cached
            override suspend fun save(userId:String,state:AppState) {
                if(blockNext) { blockNext=false;started.complete(Unit);finish.await() }
                cached=state
            }
            override suspend fun clear() { cached=null }
        }
        val repository=AccountRepository(api,AccountSession("token","user-a",Long.MAX_VALUE),cache,pollIntervalMillis=60_000)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { repository.states.collect() }
        runCurrent()
        blockNext=true
        advanceTimeBy(60_000);runCurrent();started.await()
        val draft=launch { repository.updateNoteDraft("new local draft") }
        runCurrent();finish.complete(Unit);draft.join();runCurrent()
        assertEquals("new local draft",cached?.noteDraft)
    }

    @Test fun failedFollowUpSnapshotAfterNoteSaveHidesUnconfirmedPartner() = runTest {
        val api=FakeApi().apply { snapshotResult=pairedSnapshot() }
        val repository=AccountRepository(api,AccountSession("token","user-a",Long.MAX_VALUE),MemoryCache(),pollIntervalMillis=60_000)
        val observed=mutableListOf<AppState>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { repository.states.collect { observed+=it } }
        runCurrent()
        api.snapshotError=IOException("offline after save")
        repository.saveNote("published",123)
        runCurrent()
        assertNull(observed.last().partner)
        assertTrue(observed.last().syncStale)
    }

    @Test fun snapshotStartedBeforeDeleteCannotReintroduceDeletedNote() = runTest {
        val api=FakeApi().apply { snapshotResult=snapshotResult.copy(me=snapshotResult.me.copy(note=ApiNote("old","2026-09-30T00:00:00.123456+00:00"))) }
        val before=api.snapshotResult
        val cache=MemoryCache()
        val repository=AccountRepository(api,AccountSession("token","user-a",Long.MAX_VALUE),cache,pollIntervalMillis=60_000)
        val observed=mutableListOf<AppState>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { repository.states.collect { observed+=it } }
        runCurrent()
        val note=observed.last().note!!
        val pending=CompletableDeferred<ApiSnapshot>();api.nextSnapshot=pending
        advanceTimeBy(60_000);runCurrent()
        repository.deleteNote(note)
        pending.complete(before);runCurrent()
        assertNull(observed.last().note)
        assertEquals(note,observed.last().deletedNote)
    }

    @Test fun sharedCacheKeepsAccountDraftsSeparateAcrossRepositories() = runTest {
        val snapshots=mutableMapOf<String,AppState>()
        val cache=object:AccountSnapshotStore {
            override suspend fun load(userId:String)=snapshots[userId]
            override suspend fun save(userId:String,state:AppState) { snapshots[userId]=state }
            override suspend fun clear() { snapshots.clear() }
        }
        val a=AccountRepository(FakeApi(),AccountSession("a","user-a",Long.MAX_VALUE),cache,pollIntervalMillis=60_000)
        val bApi=FakeApi().apply { snapshotResult=snapshotResult.copy(userId="user-b") }
        val b=AccountRepository(bApi,AccountSession("b","user-b",Long.MAX_VALUE),cache,pollIntervalMillis=60_000)
        a.states.first();a.updateNoteDraft("private A")
        b.states.first();assertNull(snapshots["user-b"]?.noteDraft);b.updateNoteDraft("private B")
        val reopened=AccountRepository(FakeApi(),AccountSession("a","user-a",Long.MAX_VALUE),cache,pollIntervalMillis=60_000)
        assertEquals("private A",reopened.states.first().noteDraft)
        assertEquals("private B",snapshots["user-b"]?.noteDraft)
    }

    private fun pairedSnapshot() = ApiSnapshot(
        userId = "user-a",
        me = ApiUserData(Profile("小舟", "beijing"), setupComplete = true),
        paired = true,
        pairStatus = "shared",
        sharingEnabled = true,
        partner = ApiUserData(Profile("小雨", "new-york"), Schedule(Rhythm(), Rhythm()), setupComplete = true),
        serverTime = "2026-09-30T00:00:00Z",
    )

    private class MemoryCache(var value: AppState? = null) : AccountSnapshotStore {
        override suspend fun load(userId: String) = value
        override suspend fun save(userId: String, state: AppState) { value = state }
        override suspend fun clear() { value = null }
    }

    private class FakeApi : AccountApi {
        var snapshotError: Exception? = null
        var failNoteSave = false
        var deletedRevision: String? = null
        var deletedApiNote: ApiNote? = null
        var noteSaves = 0
        var sharingChanges = mutableListOf<Boolean>()
        var unpairCalls = 0
        var snapshotCalls = 0
        var nextSnapshot: CompletableDeferred<ApiSnapshot>? = null
        var snapshotResult = ApiSnapshot(
            userId = "user-a",
            me = ApiUserData(Profile("小舟", "beijing"), setupComplete = true),
            paired = false,
            pairStatus = "not_paired",
            sharingEnabled = true,
            serverTime = "2026-09-30T00:00:00Z",
        )

        override suspend fun requestOtp(email: String) = 60
        override suspend fun verifyOtp(email: String, code: String) = AccountSession("token", "user-a", Long.MAX_VALUE)
        override suspend fun snapshot(token: String): ApiSnapshot {
            snapshotCalls++
            snapshotError?.let { throw it }
            val pending = nextSnapshot.also { nextSnapshot = null }
            return pending?.await() ?: snapshotResult
        }
        override suspend fun saveProfile(token: String, profile: Profile) = Unit
        override suspend fun saveSchedule(token: String, schedule: Schedule) = Unit
        override suspend fun saveNote(token: String, text: String): ApiNote {
            if (failNoteSave) throw IOException("offline")
            noteSaves++
            val note=ApiNote(text,"2026-09-30T00:00:00.000001Z")
            snapshotResult=snapshotResult.copy(me=snapshotResult.me.copy(note=note))
            return note
        }
        override suspend fun deleteNote(token: String, expectedRevision: String) {
            val note=snapshotResult.me.note
            if (note?.updatedAt != expectedRevision) throw ApiException(409,"note_conflict")
            deletedRevision=expectedRevision;deletedApiNote=note
            snapshotResult=snapshotResult.copy(me=snapshotResult.me.copy(note=null))
        }
        override suspend fun restoreNote(token: String, expectedRevision: String): ApiNote {
            if (snapshotResult.me.note != null || deletedRevision != expectedRevision) throw ApiException(409,"note_conflict")
            val note=ApiNote(requireNotNull(deletedApiNote).text,"2026-09-30T00:00:00.123457+00:00")
            snapshotResult=snapshotResult.copy(me=snapshotResult.me.copy(note=note))
            deletedApiNote=null;deletedRevision=null
            return note
        }
        override suspend fun setTemporary(token: String, available: Boolean, minutes: Int) = Unit
        override suspend fun resetTemporary(token: String) = Unit
        override suspend fun markSetupComplete(token: String) = Unit
        override suspend fun setSharing(token: String, enabled: Boolean) {
            sharingChanges += enabled
            snapshotResult = snapshotResult.copy(
                sharingEnabled = enabled,
                pairStatus = if (enabled) "shared" else "sharing_paused",
                partner = if (enabled) snapshotResult.partner else null,
            )
        }
        override suspend fun unpair(token: String) {
            unpairCalls++
            snapshotResult = snapshotResult.copy(paired = false, pairStatus = "not_paired", partner = null)
        }
        override suspend fun createInvitation(token: String) = Invite("23456789AB", 1_900_000_000_000)
        override suspend fun revokeInvitation(token: String) = Unit
        override suspend fun previewInvitation(code: String) = InvitePreview(code, Profile("邀请人", "beijing"), listOf("资料"), 1_900_000_000_000)
        override suspend fun acceptInvitation(token: String, code: String) = Unit
        override suspend fun logout(token: String) = Unit
    }
}
