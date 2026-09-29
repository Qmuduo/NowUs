package app.nowus.android.domain

import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.ZoneId

@Serializable data class Profile(val name: String, val cityId: String)
@Serializable data class Rhythm(val sleepStart: String="23:00", val sleepEnd: String="07:00", val activity: String="上班", val activityStart: String="09:00", val activityEnd: String="18:00", val contactKnown: Boolean=true, val contactStart: String="20:00", val contactEnd: String="22:30")
@Serializable data class Schedule(val weekday: Rhythm, val rest: Rhythm)
@Serializable data class TemporaryStatus(val available: Boolean, val untilMillis: Long)
@Serializable data class Note(val text: String, val updatedMillis: Long)
@Serializable data class Invite(val code: String, val expiresMillis: Long, val revoked: Boolean=false)
@Serializable data class AppState(val me: Profile, val schedule: Schedule?=null, val setupComplete: Boolean=false, val partner: Profile?=null, val partnerSchedule: Schedule?=null, val note: Note?=null, val invite: Invite?=null, val temporary: TemporaryStatus?=null)
data class City(val id: String,val name: String,val zoneId: String) { val zone: ZoneId get()=ZoneId.of(zoneId) }
object Cities {
 val all=listOf(City("beijing","北京","Asia/Shanghai"),City("shanghai","上海","Asia/Shanghai"),City("new-york","纽约","America/New_York"),City("london","伦敦","Europe/London"),City("paris","巴黎","Europe/Paris"),City("tokyo","东京","Asia/Tokyo"),City("sydney","悉尼","Australia/Sydney"),City("kathmandu","加德满都","Asia/Kathmandu"))
 fun byId(id: String): City?=all.find { it.id==id }
}
data class Window(val start: Instant,val end: Instant)
enum class ActivitySource { TEMPLATE,TEMPORARY,UNKNOWN }
data class Activity(val label: String,val source: ActivitySource)
data class Segment(val start: Instant,val end: Instant,val activity: Activity)
data class ValidationResult(val valid: Boolean,val errors: List<String> = emptyList())
data class RuleResult(val state: AppState,val error: String?=null)
