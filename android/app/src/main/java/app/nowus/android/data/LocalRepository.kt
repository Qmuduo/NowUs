package app.nowus.android.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import app.nowus.android.domain.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.IOException

interface StateRepository {
    val states: Flow<AppState>
    suspend fun update(transform: (AppState) -> AppState)
}

class LocalRepository(private val store: DataStore<Preferences>) : StateRepository {
    private val key = stringPreferencesKey("nowus_local_v1")
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    override val states: Flow<AppState> = store.data.map { decode(it[key]) }

    override suspend fun update(transform: (AppState) -> AppState) {
        store.edit { preferences ->
            val next = transform(decode(preferences[key]))
            validate(next)
            preferences[key] = json.encodeToString(LocalRecord(data=next))
        }
    }

    private fun decode(raw: String?): AppState {
        if (raw == null) return AppState(Profile("", "beijing"))
        return try {
            val record = json.decodeFromString<LocalRecord>(raw)
            require(record.schema == 1) { "不支持的本地数据版本" }
            validate(record.data)
            record.data
        } catch (exception: Exception) {
            throw IOException("本地资料无法读取，原记录已保留", exception)
        }
    }

    private fun validate(state: AppState) {
        require(Cities.byId(state.me.cityId) != null) { "城市无效" }
        if (state.setupComplete || state.me.name.isNotEmpty()) {
            require(Rules.validateProfile(state.me).valid) { "本地昵称无效" }
        }
        state.partner?.let { require(Rules.validateProfile(it).valid) { "伴侣资料无效" } }
        require(state.partner != null || state.partnerSchedule == null) { "对方资料尚未填写" }
        listOfNotNull(state.schedule, state.partnerSchedule).forEach { schedule ->
            require(Rules.validateRhythm(schedule.weekday).valid && Rules.validateRhythm(schedule.rest).valid) { "作息无效" }
        }
        state.note?.let { require(it.text.codePointCount(0,it.text.length) <= 120) { "留言超出字数" } }
        state.temporary?.let { require(it.fromMillis <= it.untilMillis) { "临时状态起止无效" } }
    }
}

@Serializable
private data class LocalRecord(val schema: Int = 1, val data: AppState)
