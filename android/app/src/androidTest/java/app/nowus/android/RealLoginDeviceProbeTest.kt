package app.nowus.android

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.nowus.android.data.EncryptedSessionStore
import app.nowus.android.data.NowUsApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assume.assumeTrue
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.net.URL
import java.net.URLEncoder
import java.net.InetSocketAddress
import java.net.Socket
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class RealLoginDeviceProbeTest {
    @Test fun randomMailpitOtpCanBeVerifiedAndStoredOnDevice() = runBlocking {
        assumeTrue("Requires local API and Mailpit with adb reverse on ports 8000 and 8025", portOpen(8000) && portOpen(8025))
        val api = NowUsApiClient("http://127.0.0.1:8000")
        val email = "device-probe-${UUID.randomUUID().toString().take(12)}@example.net"
        val store = EncryptedSessionStore(InstrumentationRegistry.getInstrumentation().targetContext)
        var accessToken: String? = null
        store.clear()
        try {
            api.requestOtp(email)
            val code = readOtpFromMailpit(email)
            val session = api.verifyOtp(email, code)
            accessToken = session.accessToken
            store.save(session)
            assertEquals(session, EncryptedSessionStore(InstrumentationRegistry.getInstrumentation().targetContext).current.value)
            assertEquals(session.userId, api.snapshot(session.accessToken).userId)
        } finally {
            accessToken?.let { runCatching { api.logout(it) } }
            store.clear()
        }
    }

    private fun portOpen(port: Int) = runCatching {
        Socket().use { it.connect(InetSocketAddress("127.0.0.1", port), 500) }
        true
    }.getOrDefault(false)

    private suspend fun readOtpFromMailpit(email: String): String = withContext(Dispatchers.IO) {
        val json = Json
        val deadline = System.currentTimeMillis() + 15_000
        while (System.currentTimeMillis() < deadline) {
            val query = URLEncoder.encode("to:$email", Charsets.UTF_8.name())
            val search = json.parseToJsonElement(URL("http://127.0.0.1:8025/api/v1/search?query=$query&limit=1").readText())
                .jsonObject["messages"]!!.jsonArray
            if (search.isNotEmpty()) {
                val id = search.first().jsonObject["ID"]!!.jsonPrimitive.content
                val messageId = URLEncoder.encode(id, Charsets.UTF_8.name())
                val body = json.parseToJsonElement(URL("http://127.0.0.1:8025/api/v1/message/$messageId").readText()).jsonObject
                val text = body["Text"]!!.jsonPrimitive.content
                Regex("(?<!\\d)(\\d{6})(?!\\d)").find(text)?.let { return@withContext it.groupValues[1] }
            }
            Thread.sleep(100)
        }
        error("OTP was not visible in local Mailpit")
    }
}
