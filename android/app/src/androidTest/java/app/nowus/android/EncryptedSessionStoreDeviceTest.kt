package app.nowus.android

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.nowus.android.data.AccountSession
import app.nowus.android.data.EncryptedSessionStore
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EncryptedSessionStoreDeviceTest {
    @Test fun keystoreEncryptsPersistsAndRestoresSessionOnDevice() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val store = EncryptedSessionStore(context)
        store.clear()
        try {
            val expected = AccountSession("diagnostic-only-token", "diagnostic-user", System.currentTimeMillis() + 60_000)
            store.save(expected)
            assertEquals(expected, store.current.value)
            assertEquals(expected, EncryptedSessionStore(context).current.value)
        } finally {
            store.clear()
        }
    }
}
