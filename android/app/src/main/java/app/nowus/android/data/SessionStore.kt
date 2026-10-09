package app.nowus.android.data

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import app.nowus.android.domain.AppState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec


interface SessionStore {
    val current: StateFlow<AccountSession?>
    suspend fun save(session: AccountSession)
    suspend fun clear()
}

interface AccountSnapshotStore {
    suspend fun load(userId: String): AppState?
    suspend fun save(userId: String, state: AppState)
    suspend fun loadDraft(userId: String): String? = load(userId)?.noteDraft
    suspend fun clear()
}

interface PendingInviteStore {
    val code: StateFlow<String?>
    suspend fun save(code: String)
    suspend fun clear()
}

class EncryptedPendingInviteStore(context: Context) : PendingInviteStore {
    private val preferences = context.applicationContext.getSharedPreferences("nowus_pending_invite", Context.MODE_PRIVATE)
    private val _code = MutableStateFlow(readCode())
    override val code: StateFlow<String?> = _code.asStateFlow()

    override suspend fun save(code: String) {
        val normalized = code.trim().uppercase().filter(Char::isLetterOrDigit).take(10)
        require(normalized.length == 10) { "邀请码无效" }
        val encrypted = SecureBlob.encrypt("nowus_invite_key_v1", normalized)
        check(withContext(Dispatchers.IO) { preferences.edit().putString("code", encrypted).commit() }) { "无法安全保存邀请" }
        _code.value = normalized
    }

    override suspend fun clear() {
        withContext(Dispatchers.IO) { preferences.edit().remove("code").commit() }
        _code.value = null
    }

    private fun readCode(): String? = try {
        preferences.getString("code", null)?.let { SecureBlob.decrypt("nowus_invite_key_v1", it) }
    } catch (_: Exception) {
        preferences.edit().remove("code").commit()
        null
    }
}

class EncryptedSessionStore(context: Context) : SessionStore {
    private val preferences = context.applicationContext.getSharedPreferences("nowus_secure_session", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val _current = MutableStateFlow(readSession())
    override val current: StateFlow<AccountSession?> = _current.asStateFlow()

    override suspend fun save(session: AccountSession) {
        val encrypted = SecureBlob.encrypt("nowus_session_key_v1", json.encodeToString(session))
        val saved = withContext(Dispatchers.IO) { preferences.edit().putString("session", encrypted).commit() }
        check(saved) { "无法安全保存登录会话" }
        _current.value = session
    }

    override suspend fun clear() {
        withContext(Dispatchers.IO) { preferences.edit().remove("session").commit() }
        _current.value = null
    }

    private fun readSession(): AccountSession? = try {
        val encoded = preferences.getString("session", null) ?: return null
        json.decodeFromString(SecureBlob.decrypt("nowus_session_key_v1", encoded))
    } catch (_: Exception) {
        preferences.edit().remove("session").commit()
        null
    }
}

class EncryptedAccountSnapshotStore(context: Context) : AccountSnapshotStore {
    private val preferences = context.applicationContext.getSharedPreferences("nowus_secure_snapshot", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    override suspend fun load(userId: String): AppState? = withContext(Dispatchers.IO) {
        val key = "snapshot:$userId"
        try {
            val encoded = preferences.getString(key, null) ?: preferences.getString("snapshot", null)
            val record = encoded?.let {
                json.decodeFromString<SnapshotRecord>(SecureBlob.decrypt("nowus_snapshot_key_v1", it))
            }
            val snapshot = record?.state?.takeIf { record.userId == userId }
            snapshot?.copy(noteDraft = loadDraft(userId) ?: snapshot.noteDraft)
        } catch (_: Exception) {
            preferences.edit().remove(key).commit()
            null
        }
    }

    override suspend fun loadDraft(userId: String): String? = withContext(Dispatchers.IO) {
        preferences.getString("draft:$userId", null)?.let { SecureBlob.decrypt("nowus_snapshot_key_v1", it) }
    }

    override suspend fun save(userId: String, state: AppState) = withContext(Dispatchers.IO) {
        val encrypted = SecureBlob.encrypt("nowus_snapshot_key_v1", json.encodeToString(SnapshotRecord(userId, state)))
        val editor = preferences.edit().putString("snapshot:$userId", encrypted)
        if (state.noteDraft == null) editor.remove("draft:$userId")
        else editor.putString("draft:$userId", SecureBlob.encrypt("nowus_snapshot_key_v1", state.noteDraft))
        check(editor.commit()) { "无法安全保存同步快照" }
    }

    override suspend fun clear() {
        withContext(Dispatchers.IO) {
            val editor = preferences.edit().remove("snapshot")
            preferences.all.keys.filter { it.startsWith("snapshot:") }.forEach { editor.remove(it) }
            check(editor.commit()) { "无法清除同步快照" }
        }
    }
}

/** Private device-only drafts are scoped to one local profile or authenticated account. */
class EncryptedNoteDraftStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences("nowus_encrypted_note_drafts", Context.MODE_PRIVATE)

    fun load(scope: String): String? = try {
        preferences.getString(preferenceKey(scope), null)?.let { SecureBlob.decrypt(KEY_ALIAS, it) }
    } catch (_: Exception) {
        preferences.edit().remove(preferenceKey(scope)).commit()
        null
    }

    fun save(scope: String, text: String) {
        preferences.edit().putString(preferenceKey(scope), SecureBlob.encrypt(KEY_ALIAS, text)).apply()
    }

    fun clear(scope: String) {
        preferences.edit().remove(preferenceKey(scope)).apply()
    }

    private fun preferenceKey(scope: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(scope.toByteArray(Charsets.UTF_8))
        return "draft_${Base64.encodeToString(digest, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)}"
    }

    private companion object {
        const val KEY_ALIAS = "nowus_note_draft_key_v1"
    }
}

@Serializable private data class SnapshotRecord(val userId: String, val state: AppState)

internal object SecureBlob {
    private const val TRANSFORMATION = "AES/GCM/NoPadding"

    fun encrypt(alias: String, value: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key(alias))
        val encrypted = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
        val iv = cipher.iv
        val bytes = ByteBuffer.allocate(4 + iv.size + encrypted.size).putInt(iv.size).put(iv).put(encrypted).array()
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    fun decrypt(alias: String, value: String): String {
        val bytes = ByteBuffer.wrap(Base64.decode(value, Base64.NO_WRAP))
        val iv = ByteArray(bytes.int).also(bytes::get)
        val encrypted = ByteArray(bytes.remaining()).also(bytes::get)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key(alias), GCMParameterSpec(128, iv))
        return cipher.doFinal(encrypted).toString(Charsets.UTF_8)
    }

    private fun key(alias: String): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(alias, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(
                alias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build(),
        )
        return generator.generateKey()
    }
}
