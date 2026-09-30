package app.nowus.android.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.nowus.android.domain.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class LocalRepositoryAndroidTest {
    @Test fun concurrentUpdatesAndOverwriteSurviveStoreRecreation() = runBlocking {
        // Exercise the same file-backed factory as the production Android delegate.
        // File.renameTo can replace a file on Android; on Windows it cannot.
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = File(context.cacheDir, "nowus-test-${UUID.randomUUID()}.preferences_pb")
        val firstJob = SupervisorJob()
        val repository = LocalRepository(PreferenceDataStoreFactory.create(
            scope=CoroutineScope(Dispatchers.IO + firstJob), produceFile={file}
        ))
        try {
            coroutineScope {
                launch { repository.update { it.copy(note=Note("晚安 🌙",1)) } }
                launch { repository.update { it.copy(me=Profile("阿木","london")) } }
            }
            repository.update { it.copy(note=Note("醒来再聊",2), setupComplete=true) }
            assertEquals("阿木",repository.states.first().me.name)
            assertEquals("醒来再聊",repository.states.first().note?.text)
        } finally { firstJob.cancelAndJoin() }
        val secondJob=SupervisorJob()
        try {
            val reopened = LocalRepository(PreferenceDataStoreFactory.create(
                scope=CoroutineScope(Dispatchers.IO + secondJob), produceFile={file}
            ))
            val state=reopened.states.first()
            assertEquals(Profile("阿木","london"), state.me)
            assertEquals("醒来再聊", state.note?.text)
            assertTrue(state.setupComplete)
        } finally { secondJob.cancelAndJoin(); file.delete() }
    }
}
