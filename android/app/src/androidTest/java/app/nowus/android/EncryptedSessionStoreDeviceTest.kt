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
    @Test fun encryptedDraftsSurviveAccountSwitchAndStoreRecreation() = runBlocking {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val store=app.nowus.android.data.EncryptedAccountSnapshotStore(context)
        val a="draft-test-a";val b="draft-test-b"
        try {
            store.save(a,app.nowus.android.domain.AppState(app.nowus.android.domain.Profile("A","beijing"),noteDraft="private A"))
            store.save(b,app.nowus.android.domain.AppState(app.nowus.android.domain.Profile("B","beijing"),noteDraft="private B"))
            val reopened=app.nowus.android.data.EncryptedAccountSnapshotStore(context)
            assertEquals("private A",reopened.load(a)?.noteDraft)
            assertEquals("private B",reopened.load(b)?.noteDraft)
            assertEquals(null,reopened.load("draft-test-c"))
            val stored=context.getSharedPreferences("nowus_secure_snapshot",android.content.Context.MODE_PRIVATE).all.values.joinToString()
            org.junit.Assert.assertFalse(stored.contains("private A"))
            org.junit.Assert.assertFalse(stored.contains("private B"))
            store.clear()
            assertEquals(null,reopened.load(a))
            assertEquals("private A",reopened.loadDraft(a))
            assertEquals("private B",reopened.loadDraft(b))
        } finally {
            store.save(a,app.nowus.android.domain.AppState(app.nowus.android.domain.Profile("A","beijing")))
            store.save(b,app.nowus.android.domain.AppState(app.nowus.android.domain.Profile("B","beijing")))
            store.clear()
        }
    }

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
