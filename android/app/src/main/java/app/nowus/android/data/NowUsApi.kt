package app.nowus.android.data

import app.nowus.android.domain.Invite
import app.nowus.android.domain.InvitePreview
import app.nowus.android.domain.Profile
import app.nowus.android.domain.Schedule
import app.nowus.android.domain.TemporaryStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL
import java.time.Instant


@Serializable data class AccountSession(val accessToken: String, val userId: String, val expiresAtMillis: Long)

interface AuthenticationApi {
    suspend fun requestOtp(email: String): Int
    suspend fun verifyOtp(email: String, code: String): AccountSession
}

interface AccountApi : AuthenticationApi {
    suspend fun snapshot(token: String): ApiSnapshot
    suspend fun saveProfile(token: String, profile: Profile)
    suspend fun saveSchedule(token: String, schedule: Schedule)
    suspend fun saveNote(token: String, text: String)
    suspend fun deleteNote(token: String)
    suspend fun setTemporary(token: String, available: Boolean, minutes: Int)
    suspend fun resetTemporary(token: String)
    suspend fun markSetupComplete(token: String)
    suspend fun setSharing(token: String, enabled: Boolean)
    suspend fun unpair(token: String)
    suspend fun createInvitation(token: String): Invite
    suspend fun revokeInvitation(token: String)
    suspend fun previewInvitation(code: String): InvitePreview
    suspend fun acceptInvitation(token: String, code: String)
    suspend fun logout(token: String)
}

@Serializable data class ApiSnapshot(
    val userId: String,
    val me: ApiUserData,
    val paired: Boolean,
    val pairStatus: String,
    val sharingEnabled: Boolean,
    val partner: ApiUserData? = null,
    val invitation: ApiPendingInvitation? = null,
    val serverTime: String,
)

@Serializable data class ApiUserData(
    val profile: Profile? = null,
    val schedule: Schedule? = null,
    val note: ApiNote? = null,
    val temporary: TemporaryStatus? = null,
    val setupComplete: Boolean = false,
)

@Serializable data class ApiNote(val text: String, val updatedAt: String)
@Serializable data class ApiPendingInvitation(val expiresAt: String)
@Serializable data class ApiInviteCreated(
    val code: String = "",
    val expiresAt: String,
    val inviter: ApiInviteProfile,
    val scope: List<String>,
)
@Serializable data class ApiInviteProfile(val name: String, val cityId: String, val cityName: String)
@Serializable data class ApiOtpRequested(val retryAfterSeconds: Int)
@Serializable data class ApiSessionPayload(val accessToken: String, val userId: String, val expiresAt: String)
@Serializable data class ApiErrorEnvelope(val detail: kotlinx.serialization.json.JsonElement? = null)

class ApiException(
    val statusCode: Int,
    val errorCode: String,
    val retryAfterSeconds: Int? = null,
) : IOException(errorCode)

class NowUsApiClient(private val baseUrl: String) : AccountApi {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val origin = baseUrl.trim().trimEnd('/')

    override suspend fun requestOtp(email: String): Int {
        val response = post("/v1/auth/otp", null, OtpInput(email.trim().lowercase()))
        return json.decodeFromString<ApiOtpRequested>(response).retryAfterSeconds
    }

    override suspend fun verifyOtp(email: String, code: String): AccountSession {
        val response = post("/v1/auth/verify", null, VerifyInput(email.trim().lowercase(), code))
        val result = json.decodeFromString<ApiSessionPayload>(response)
        return AccountSession(result.accessToken, result.userId, Instant.parse(result.expiresAt).toEpochMilli())
    }

    override suspend fun snapshot(token: String): ApiSnapshot =
        json.decodeFromString(get("/v1/snapshot", token))

    override suspend fun saveProfile(token: String, profile: Profile) {
        put("/v1/me/profile", token, profile)
    }

    override suspend fun saveSchedule(token: String, schedule: Schedule) {
        put("/v1/me/rhythm", token, ScheduleInput(schedule.weekday, schedule.rest))
    }

    override suspend fun saveNote(token: String, text: String) {
        put("/v1/me/note", token, NoteInput(text))
    }

    override suspend fun deleteNote(token: String) {
        request("DELETE", "/v1/me/note", token)
    }

    override suspend fun setTemporary(token: String, available: Boolean, minutes: Int) {
        put("/v1/me/temporary", token, TemporaryInput(available, minutes))
    }

    override suspend fun resetTemporary(token: String) {
        request("DELETE", "/v1/me/temporary", token)
    }

    override suspend fun markSetupComplete(token: String) {
        put("/v1/me/setup", token, SetupInput(true))
    }

    override suspend fun setSharing(token: String, enabled: Boolean) {
        put("/v1/me/sharing", token, SharingInput(enabled))
    }

    override suspend fun unpair(token: String) {
        request("DELETE", "/v1/pairing", token)
    }

    override suspend fun createInvitation(token: String): Invite {
        val created = json.decodeFromString<ApiInviteCreated>(request("POST", "/v1/invites", token))
        return Invite(created.code, Instant.parse(created.expiresAt).toEpochMilli())
    }

    override suspend fun revokeInvitation(token: String) {
        request("DELETE", "/v1/me/invitation", token)
    }

    override suspend fun previewInvitation(code: String): InvitePreview {
        val encoded = URLEncoder.encode(code.trim(), Charsets.UTF_8.name())
        val created = json.decodeFromString<ApiInviteCreated>(get("/v1/invites/$encoded", null))
        return InvitePreview(
            code = code.trim(),
            inviter = Profile(created.inviter.name, created.inviter.cityId),
            scope = created.scope,
            expiresMillis = Instant.parse(created.expiresAt).toEpochMilli(),
        )
    }

    override suspend fun acceptInvitation(token: String, code: String) {
        post("/v1/invites/accept", token, InviteInput(code.trim()))
    }

    override suspend fun logout(token: String) {
        request("DELETE", "/v1/auth/session", token)
    }

    private suspend fun get(path: String, token: String?): String = request("GET", path, token)

    private suspend inline fun <reified T> post(path: String, token: String?, body: T): String =
        request("POST", path, token, json.encodeToString(body))

    private suspend inline fun <reified T> put(path: String, token: String, body: T): String =
        request("PUT", path, token, json.encodeToString(body))

    private suspend fun request(method: String, path: String, token: String?, body: String? = null): String =
        withContext(Dispatchers.IO) {
            if (origin.isBlank()) throw ApiException(0, "api_not_configured")
            val connection = (URL(origin + path).openConnection() as HttpURLConnection)
            try {
                connection.requestMethod = method
                connection.connectTimeout = 8_000
                connection.readTimeout = 10_000
                connection.setRequestProperty("Accept", "application/json")
                if (token != null) connection.setRequestProperty("Authorization", "Bearer $token")
                if (body != null) {
                    connection.doOutput = true
                    connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                    connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
                }
                val status = connection.responseCode
                val stream = if (status in 200..299) connection.inputStream else connection.errorStream
                val response = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
                if (status !in 200..299) throw decodeError(status, response)
                response
            } finally {
                connection.disconnect()
            }
        }

    private fun decodeError(status: Int, response: String): ApiException {
        return try {
            val detail = json.decodeFromString<ApiErrorEnvelope>(response).detail?.jsonObject
            ApiException(
                status,
                detail?.get("code")?.jsonPrimitive?.contentOrNull ?: "request_failed",
                detail?.get("retryAfterSeconds")?.jsonPrimitive?.contentOrNull?.toIntOrNull(),
            )
        } catch (_: Exception) {
            ApiException(status, "request_failed")
        }
    }

    @Serializable private data class OtpInput(val email: String)
    @Serializable private data class VerifyInput(val email: String, val code: String)
    @Serializable private data class ScheduleInput(val weekday: app.nowus.android.domain.Rhythm, val rest: app.nowus.android.domain.Rhythm)
    @Serializable private data class NoteInput(val text: String)
    @Serializable private data class TemporaryInput(val available: Boolean, val minutes: Int)
    @Serializable private data class SetupInput(val complete: Boolean)
    @Serializable private data class SharingInput(val enabled: Boolean)
    @Serializable private data class InviteInput(val code: String)
}
