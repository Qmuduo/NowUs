package app.nowus.android.domain

import java.time.*
import java.time.temporal.ChronoUnit

object TimeEngine {
 fun dayBounds(date: LocalDate,zone: ZoneId)=Window(date.atStartOfDay(zone).toInstant(),date.plusDays(1).atStartOfDay(zone).toInstant())
 private fun rhythm(profile: Profile,schedule: Schedule?,instant: Instant): Rhythm? {
  val zone=Cities.byId(profile.cityId)?.zone ?: return null
  val day=instant.atZone(zone).dayOfWeek
  return if(day==DayOfWeek.SATURDAY || day==DayOfWeek.SUNDAY) schedule?.rest else schedule?.weekday
 }
 private fun minute(profile: Profile,instant: Instant): Int {
  val time=instant.atZone(Cities.byId(profile.cityId)!!.zone).toLocalTime()
  return time.hour*60+time.minute
 }
 internal fun parseMinute(value: String): Int? {
  if(!Regex("(?:[01][0-9]|2[0-3]):[0-5][0-9]").matches(value)) return null
  return value.substring(0,2).toInt()*60+value.substring(3).toInt()
 }
 internal fun contains(minute: Int,start: String,end: String): Boolean {
  val a=parseMinute(start) ?: return false
  val b=parseMinute(end) ?: return false
  return if(a<b) minute>=a && minute<b else a!=b && (minute>=a || minute<b)
 }
 fun activityAt(profile: Profile,schedule: Schedule?,temporary: TemporaryStatus?,instant: Instant): Activity {
  val r=rhythm(profile,schedule,instant) ?: return Activity("未安排",ActivitySource.UNKNOWN)
  val m=minute(profile,instant)
  if(r.blocks.isNotEmpty()) {
   val block=r.blocks.firstOrNull { contains(m,it.start,it.end) }
   return Activity(block?.label ?: "未安排",ActivitySource.TEMPLATE)
  }
  return Activity(when {contains(m,r.sleepStart,r.sleepEnd)->"睡觉"; contains(m,r.activityStart,r.activityEnd)->r.activity; else->"未安排"},ActivitySource.TEMPLATE)
 }
 fun contactAt(profile: Profile,schedule: Schedule?,temporary: TemporaryStatus?,instant: Instant): Boolean? {
  if(temporary!=null && instant.toEpochMilli()>=temporary.fromMillis && instant.toEpochMilli()<temporary.untilMillis) return temporary.available
  val r=rhythm(profile,schedule,instant) ?: return null
  if(!r.contactKnown) return null
  val m=minute(profile,instant)
  val sleep=r.blocks.firstOrNull { it.id=="sleep" }
  return !(sleep?.let { contains(m,it.start,it.end) } ?: contains(m,r.sleepStart,r.sleepEnd)) && contains(m,r.contactStart,r.contactEnd)
 }
 private fun boundaries(start: Instant,end: Instant,extras: List<Instant> = emptyList()): List<Instant> {
  require(start<=end)
  val result=mutableListOf(start)
  var point=start.truncatedTo(ChronoUnit.MINUTES).plusSeconds(60)
  while(point<end) { result.add(point); point=point.plusSeconds(60) }
  result.addAll(extras.filter { it>start && it<end })
  result.add(end)
  return result.distinct().sorted()
 }
 fun commonWindows(start: Instant,end: Instant,me: Profile,meSchedule: Schedule?,temporary: TemporaryStatus?,partner: Profile,partnerSchedule: Schedule?): List<Window> {
  val result=mutableListOf<Window>()
  boundaries(start,end,temporary?.let { listOf(Instant.ofEpochMilli(it.fromMillis), Instant.ofEpochMilli(it.untilMillis)) } ?: emptyList()).zipWithNext().forEach { (a,b)->
   if(a<b && contactAt(me,meSchedule,temporary,a)==true && contactAt(partner,partnerSchedule,null,a)==true) {
    if(result.lastOrNull()?.end==a) result[result.lastIndex]=result.last().copy(end=b) else result.add(Window(a,b))
   }
  }
  return result
 }
 fun segments(start: Instant,end: Instant,profile: Profile,schedule: Schedule?): List<Segment> {
  val result=mutableListOf<Segment>()
  boundaries(start,end).zipWithNext().forEach { (a,b)->
   if(a<b) { val activity=activityAt(profile,schedule,null,a)
    if(result.lastOrNull()?.let {it.end==a && it.activity==activity}==true) result[result.lastIndex]=result.last().copy(end=b) else result.add(Segment(a,b,activity))
   }
  }
  return result
 }
}

