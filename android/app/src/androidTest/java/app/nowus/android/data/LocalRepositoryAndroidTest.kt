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
    @Test fun localDraftAndDeletedRevisionSurviveRecreationAndFailedSaveRetainsDraft() = runBlocking {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val file=File(context.cacheDir,"nowus-draft-${UUID.randomUUID()}.preferences_pb")
        val firstJob=SupervisorJob()
        val first=LocalRepository(PreferenceDataStoreFactory.create(scope=CoroutineScope(Dispatchers.IO+firstJob),produceFile={file}))
        first.saveNote("published",1)
        first.updateNoteDraft("unsent 🌙")
        val note=first.states.first().note!!
        first.deleteNote(note)
        firstJob.cancelAndJoin()
        val secondJob=SupervisorJob()
        try {
            val reopened=LocalRepository(PreferenceDataStoreFactory.create(scope=CoroutineScope(Dispatchers.IO+secondJob),produceFile={file}))
            assertEquals("unsent 🌙",reopened.states.first().noteDraft)
            assertNull(reopened.states.first().note)
            reopened.restoreNote(note)
            assertEquals("published",reopened.states.first().note?.text)
            assertEquals("unsent 🌙",reopened.states.first().noteDraft)
            assertNotNull(runCatching { reopened.saveNote("🌙".repeat(121),2) }.exceptionOrNull())
            assertEquals("🌙".repeat(121),reopened.states.first().noteDraft)
            assertEquals("published",reopened.states.first().note?.text)
            reopened.saveNote("🌙".repeat(120),3)
            assertNull(reopened.states.first().noteDraft)
            assertEquals(120,reopened.states.first().note!!.text.codePointCount(0,240))
        } finally { secondJob.cancelAndJoin();file.delete() }
    }

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
