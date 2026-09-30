package app.nowus.android.data

import app.nowus.android.data.StateRepository
import app.nowus.android.domain.AppState
import app.nowus.android.domain.Invite
import app.nowus.android.domain.InvitePreview
import app.nowus.android.domain.Note
import app.nowus.android.domain.Profile
import app.nowus.android.domain.Schedule
import app.nowus.android.domain.TemporaryStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.isActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant


class AccountRepository(
    private val api: AccountApi,
    private val session: AccountSession,
    private val cache: AccountSnapshotStore,
    private val onSessionExpired: suspend () -> Unit = {},
    private val pollIntervalMillis: Long = 30_000,
) : StateRepository {
    private val mutex = Mutex()
    private val current = MutableStateFlow<AppState?>(null)
    private var activeInviteCode: String? = null

    override val states: Flow<AppState> = channelFlow {
        if (current.value == null) cache.load(session.userId)?.let {
            activeInviteCode = it.invite?.takeIf { !it.revoked }?.code?.takeIf(String::isNotBlank)
            current.value = it.copy(
                partner = null,
                partnerSchedule = null,
                partnerNote = null,
                partnerTemporary = null,
                syncStale = true,
            )
        }
        val updates = launch {
            current.filterNotNull().distinctUntilChanged().collect { send(it) }
        }
        while (currentCoroutineContext().isActive) {
            try {
                refresh()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                val cached = current.value
                if (cached == null) throw error
                current.value = cached.copy(
                    partner = null,
                    partnerSchedule = null,
                    partnerNote = null,
                    partnerTemporary = null,
                    syncStale = true,
                )
            }
            delay(pollIntervalMillis)
        }
        updates.cancel()
    }

    override suspend fun update(transform: (AppState) -> AppState): Unit = mutex.withLock {
        val before = current.value ?: loadFresh()
        val next = transform(before)
        require(next.partner == before.partner && next.partnerSchedule == before.partnerSchedule &&
            next.partnerNote == before.partnerNote && next.partnerTemporary == before.partnerTemporary &&
            next.paired == before.paired && next.sharingEnabled == before.sharingEnabled &&
            next.sharingPaused == before.sharingPaused && next.invite == before.invite
        ) { "共享资料只能通过明确的服务端操作修改" }
        if (next.me != before.me) api.saveProfile(session.accessToken, next.me)
        if (next.schedule != before.schedule && next.schedule != null) api.saveSchedule(session.accessToken, next.schedule)
        if (next.schedule != before.schedule && next.schedule == null) throw IllegalArgumentException("作息不能为空")
        if (next.note != before.note) {
            if (next.note == null) api.deleteNote(session.accessToken) else api.saveNote(session.accessToken, next.note.text)
        }
        if (next.temporary != before.temporary) {
            val temporary = next.temporary
            if (temporary == null) api.resetTemporary(session.accessToken)
            else {
                val minutes = ((temporary.untilMillis - temporary.fromMillis) / 60_000L).toInt()
                api.setTemporary(session.accessToken, temporary.available, minutes)
            }
        }
        if (!before.setupComplete && next.setupComplete) api.markSetupComplete(session.accessToken)
        refresh()
        Unit
    }

    override suspend fun createInvitation(code: String, nowMillis: Long): Invite = mutex.withLock {
        val created = api.createInvitation(session.accessToken)
        activeInviteCode = created.code
        refresh()
        created
    }

    override suspend fun revokeInvitation(): Unit = mutex.withLock {
        api.revokeInvitation(session.accessToken)
        activeInviteCode = null
        refresh()
        Unit
    }

    override suspend fun previewInvitation(code: String, nowMillis: Long): InvitePreview =
        api.previewInvitation(code)

    override suspend fun acceptInvitation(code: String, partner: Profile?, nowMillis: Long): Unit = mutex.withLock {
        api.acceptInvitation(session.accessToken, code)
        activeInviteCode = null
        refresh()
        Unit
    }

    override suspend fun setSharing(enabled: Boolean): Unit = mutex.withLock {
        api.setSharing(session.accessToken, enabled)
        refresh()
        Unit
    }

    override suspend fun unpair(): Unit = mutex.withLock {
        api.unpair(session.accessToken)
        activeInviteCode = null
        refresh()
        Unit
    }

    override suspend fun logout() {
        try {
            api.logout(session.accessToken)
        } catch (_: Exception) {
            // Local sign-out must remain available while offline.
        } finally {
            cache.clear()
        }
    }

    private suspend fun loadFresh(): AppState = refresh()

    private suspend fun refresh(): AppState {
        val response = try {
            api.snapshot(session.accessToken)
        } catch (error: ApiException) {
            if (error.statusCode == 401) {
                cache.clear()
                onSessionExpired()
                throw CancellationException("登录会话已失效", error)
            }
            throw error
        }
        val timestamp = Instant.parse(response.serverTime).toEpochMilli()
        val invite = response.invitation?.let {
            val expiry = Instant.parse(it.expiresAt).toEpochMilli()
            Invite(activeInviteCode.orEmpty(), expiry)
        }
        val next = AppState(
            me = response.me.profile ?: Profile("", ""),
            schedule = response.me.schedule,
            setupComplete = response.me.setupComplete,
            partner = response.partner?.profile,
            partnerSchedule = response.partner?.schedule,
            note = response.me.note?.let { Note(it.text, Instant.parse(it.updatedAt).toEpochMilli()) },
            invite = invite,
            temporary = response.me.temporary,
            partnerNote = response.partner?.note?.let { Note(it.text, Instant.parse(it.updatedAt).toEpochMilli()) },
            partnerTemporary = response.partner?.temporary,
            paired = response.paired,
            sharingEnabled = response.sharingEnabled,
            sharingPaused = response.pairStatus == "sharing_paused",
            syncStale = false,
            lastSyncMillis = timestamp,
        )
        current.value = next
        runCatching { cache.save(session.userId, next) }
        return next
    }
}
