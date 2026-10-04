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
import java.util.concurrent.atomic.AtomicLong


class AccountRepository(
    private val api: AccountApi,
    private val session: AccountSession,
    private val cache: AccountSnapshotStore,
    private val onSessionExpired: suspend () -> Unit = {},
    private val pollIntervalMillis: Long = 30_000,
) : StateRepository {
    private val mutex = Mutex()
    private val cacheMutex = Mutex()
    private val permissionRevision = AtomicLong(0)
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
        val before = current.value ?: cache.load(session.userId) ?: loadFresh()
        val next = transform(before)
        require(next.partner == before.partner && next.partnerSchedule == before.partnerSchedule &&
            next.partnerNote == before.partnerNote && next.partnerTemporary == before.partnerTemporary &&
            next.paired == before.paired && next.sharingEnabled == before.sharingEnabled &&
            next.sharingPaused == before.sharingPaused && next.invite == before.invite
        ) { "共享资料只能通过明确的服务端操作修改" }
        if (next.copy(noteDraft = before.noteDraft) == before) {
            current.value = next
            persistCurrent()
            return@withLock
        }
        if (next.note != before.note) {
            requireNotNull(next.note) { "请通过删除操作清空留言" }
            require(next.note.text.isNotBlank()) { "留言不能为空" }
            require(next.note.text.codePointCount(0, next.note.text.length) <= 120) { "留言最多 120 个字符" }
        }
        if (next.me != before.me) api.saveProfile(session.accessToken, next.me)
        if (next.schedule != before.schedule && next.schedule != null) api.saveSchedule(session.accessToken, next.schedule)
        if (next.schedule != before.schedule && next.schedule == null) throw IllegalArgumentException("作息不能为空")
        var savedNote: Note? = null
        if (next.note != before.note) {
            permissionRevision.incrementAndGet()
            try {
                savedNote = api.saveNote(session.accessToken, requireNotNull(next.note).text).toNote()
            } finally { permissionRevision.incrementAndGet() }
            val acknowledged = (current.value ?: before).copy(note = savedNote, noteDraft = null, deletedNote = null)
            current.value = acknowledged
            persistCurrent()
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
        if (savedNote != null) refreshAfterNoteAcknowledgement() else refresh()
        Unit
    }

    override suspend fun deleteNote(expectedNote: Note): Unit = mutex.withLock {
        val before = current.value ?: cache.load(session.userId) ?: loadFresh()
        require(before.note == expectedNote) { "留言已更新，请刷新后重试" }
        val revision = requireNotNull(expectedNote.revision) { "请先同步留言后再删除" }
        permissionRevision.incrementAndGet()
        try { api.deleteNote(session.accessToken, revision) }
        finally { permissionRevision.incrementAndGet() }
        val acknowledged = (current.value ?: before).copy(note = null, deletedNote = expectedNote)
        current.value = acknowledged
        persistCurrent()
        refreshAfterNoteAcknowledgement()
    }

    override suspend fun restoreNote(note: Note): Unit = mutex.withLock {
        val before = current.value ?: cache.load(session.userId) ?: loadFresh()
        require(before.note == null && before.deletedNote == note) { "留言已更新，无法撤销删除" }
        val revision = requireNotNull(note.revision) { "请先同步留言后再撤销删除" }
        permissionRevision.incrementAndGet()
        val restored = try { api.restoreNote(session.accessToken, revision).toNote() }
        finally { permissionRevision.incrementAndGet() }
        val acknowledged = (current.value ?: before).copy(note = restored, deletedNote = null)
        current.value = acknowledged
        persistCurrent()
        refreshAfterNoteAcknowledgement()
    }

    private suspend fun refreshAfterNoteAcknowledgement() {
        try { refresh() }
        catch (error: CancellationException) { throw error }
        catch (_: Exception) {
            current.value = current.value?.copy(
                syncStale = true,
                partner = null,
                partnerSchedule = null,
                partnerNote = null,
                partnerTemporary = null,
            )
            runCatching { persistCurrent() }
        }
    }

    private suspend fun persistCurrent() = cacheMutex.withLock {
        current.value?.let { cache.save(session.userId, it) }
        Unit
    }

    private fun ApiNote.toNote() = Note(text, parseApiTimestamp(updatedAt).toEpochMilli(), updatedAt)

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
        val before = current.value ?: loadFresh()
        permissionRevision.incrementAndGet()
        api.setSharing(session.accessToken, enabled)
        permissionRevision.incrementAndGet()
        if (!enabled) activeInviteCode = null
        val latest = current.value ?: before
        val acknowledged = latest.copy(
            partner = null,
            partnerSchedule = null,
            partnerNote = null,
            partnerTemporary = null,
            invite = if (enabled) latest.invite else null,
            sharingEnabled = enabled,
            sharingPaused = latest.paired && !enabled,
            syncStale = true,
        )
        current.value = acknowledged
        runCatching { persistCurrent() }
        refresh()
        Unit
    }

    override suspend fun unpair(): Unit = mutex.withLock {
        val before = current.value ?: loadFresh()
        permissionRevision.incrementAndGet()
        api.unpair(session.accessToken)
        permissionRevision.incrementAndGet()
        activeInviteCode = null
        val latest = current.value ?: before
        val acknowledged = latest.copy(
            partner = null,
            partnerSchedule = null,
            partnerNote = null,
            partnerTemporary = null,
            invite = null,
            paired = false,
            sharingPaused = false,
            syncStale = true,
        )
        current.value = acknowledged
        runCatching { persistCurrent() }
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
        val observedPermissionRevision = permissionRevision.get()
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
        if (observedPermissionRevision != permissionRevision.get()) {
            return current.value ?: throw IllegalStateException("Account refresh was superseded before initial state loaded")
        }
        require(response.userId == session.userId) { "账号同步资料不匹配" }
        val local = current.value ?: cache.load(session.userId)
        val localDraft = if (local != null) local.noteDraft else cache.loadDraft(session.userId)
        val timestamp = parseApiTimestamp(response.serverTime).toEpochMilli()
        val invite = response.invitation?.let {
            val expiry = parseApiTimestamp(it.expiresAt).toEpochMilli()
            Invite(activeInviteCode.orEmpty(), expiry)
        }
        val next = AppState(
            me = response.me.profile ?: Profile("", ""),
            schedule = response.me.schedule,
            setupComplete = response.me.setupComplete,
            partner = response.partner?.profile,
            partnerSchedule = response.partner?.schedule,
            note = response.me.note?.let { it.toNote() },
            invite = invite,
            temporary = response.me.temporary,
            partnerNote = response.partner?.note?.let { it.toNote() },
            partnerTemporary = response.partner?.temporary,
            paired = response.paired,
            sharingEnabled = response.sharingEnabled,
            sharingPaused = response.pairStatus == "sharing_paused",
            syncStale = false,
            lastSyncMillis = timestamp,
            noteDraft = localDraft,
            deletedNote = local?.deletedNote?.takeIf { response.me.note == null },
        )
        current.value = next
        runCatching { persistCurrent() }
        return next
    }
}
