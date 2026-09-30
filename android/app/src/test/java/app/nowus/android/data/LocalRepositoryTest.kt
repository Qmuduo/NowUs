package app.nowus.android.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import app.nowus.android.domain.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Test
import java.nio.file.Files

class LocalRepositoryTest {
    @Test fun profileAndNoteSurviveStoreRecreation() = runBlocking {
        val file = Files.createTempDirectory("nowus-test").resolve("state.preferences_pb").toFile()
        val job = SupervisorJob()
        val first = LocalRepository(PreferenceDataStoreFactory.create(scope=CoroutineScope(Dispatchers.IO + job), produceFile={file}))
        first.update { it.copy(me=Profile("阿木","beijing"), note=Note("醒来给我说一声 🌙",1234), setupComplete=true) }
        assertEquals("阿木",first.states.first().me.name)
        job.cancelAndJoin()
        val secondJob = SupervisorJob()
        try {
            val reopened = LocalRepository(PreferenceDataStoreFactory.create(scope=CoroutineScope(Dispatchers.IO + secondJob), produceFile={file}))
            assertEquals("醒来给我说一声 🌙",reopened.states.first().note?.text)
            assertTrue(reopened.states.first().setupComplete)
        } finally { secondJob.cancelAndJoin() }
    }

    @Test fun corruptStateFailsVisiblyAndDoesNotReplaceExistingBytes() = runBlocking {
        val file=Files.createTempDirectory("nowus-invalid").resolve("state.preferences_pb").toFile()
        val job=SupervisorJob()
        val store=PreferenceDataStoreFactory.create(scope=CoroutineScope(Dispatchers.IO+job),produceFile={file})
        val key=stringPreferencesKey("nowus_local_v1")
        store.edit { it[key]="broken json" }
        try {
            val repository=LocalRepository(store)
            assertThrows(Exception::class.java) { runBlocking { repository.states.first() } }
            assertThrows(Exception::class.java) { runBlocking { repository.update { it.copy(note=Note("hello",1)) } } }
            assertEquals("broken json",store.data.first()[key])
        } finally { job.cancelAndJoin() }
    }

}
