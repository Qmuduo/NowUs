package app.nowus.android.domain

import org.junit.Assert.*
import org.junit.Test
import java.time.*

class DomainTest {
    private val me = Profile("我", "beijing")
    private val schedule = Schedule(Rhythm(), Rhythm(activity="休息", activityStart="09:00", activityEnd="18:00"))
    private fun instant(value: String) = Instant.parse(value)

    @Test fun dstDayBoundsHaveRealElapsedDuration() {
        assertEquals(23L, Duration.between(TimeEngine.dayBounds(LocalDate.parse("2026-03-08"), ZoneId.of("America/New_York")).start, TimeEngine.dayBounds(LocalDate.parse("2026-03-08"), ZoneId.of("America/New_York")).end).toHours())
        assertEquals(25L, Duration.between(TimeEngine.dayBounds(LocalDate.parse("2026-11-01"), ZoneId.of("America/New_York")).start, TimeEngine.dayBounds(LocalDate.parse("2026-11-01"), ZoneId.of("America/New_York")).end).toHours())
    }
    @Test fun midnightSleepAndContactAreHalfOpen() {
        val s = Schedule(Rhythm(contactStart="22:00", contactEnd="01:00",sleepStart="02:00",sleepEnd="08:00"), Rhythm())
        assertTrue(TimeEngine.contactAt(me,s,null,instant("2026-09-28T16:30:00Z"))!!)
        assertFalse(TimeEngine.contactAt(me,s,null,instant("2026-09-28T17:00:00Z"))!!)
        assertEquals("睡觉",TimeEngine.activityAt(me,s,null,instant("2026-09-28T19:00:00Z")).label)
    }
    @Test fun temporaryExpiresExactlyAndUnknownHasNoWindows() {
        val now=instant("2026-09-29T12:00:00Z")
        val temp=TemporaryStatus(false,now.toEpochMilli())
        assertTrue(TimeEngine.contactAt(me,schedule,temp,now)!!)
        assertNull(TimeEngine.contactAt(me,null,null,now))
        assertTrue(TimeEngine.commonWindows(now,now.plusSeconds(3600),me,null,null,me,schedule).isEmpty())
    }
    @Test fun boundariesDoNotInheritCurrentSeconds() {
        val start=instant("2026-09-29T11:59:37Z")
        val windows=TimeEngine.commonWindows(start,start.plusSeconds(7200),me,schedule,null,me,schedule)
        assertEquals(instant("2026-09-29T12:00:00Z"),windows.first().start)
    }
    @Test fun kathmanduUsesLocalDayAndQuarterHourOffset() {
        val p=Profile("伴侣","kathmandu")
        assertEquals("上班",TimeEngine.activityAt(p,schedule,null,instant("2026-09-29T03:15:00Z")).label)
        assertEquals(instant("2026-09-28T18:15:00Z"),TimeEngine.dayBounds(LocalDate.parse("2026-09-29"),ZoneId.of("Asia/Kathmandu")).start)
    }
    @Test fun activitySegmentsRemainContinuousWhenContactChanges() {
        val s=Schedule(Rhythm(contactStart="10:00",contactEnd="11:00"),Rhythm())
        val start=instant("2026-09-29T01:00:00Z")
        val segments=TimeEngine.segments(start,start.plusSeconds(9*3600),me,s)
        assertEquals(1,segments.size)
        assertEquals("上班",segments.single().activity.label)
    }
    @Test fun validatesCodepointsCityAndIntervals() {
        assertTrue(Rules.validateProfile(Profile("😀".repeat(20),"beijing")).valid)
        assertFalse(Rules.validateProfile(Profile("😀".repeat(21),"beijing")).valid)
        assertFalse(Rules.validateProfile(Profile(" ","invalid")).valid)
        assertFalse(Rules.validateRhythm(Rhythm(activityStart="22:00",activityEnd="02:00")).valid)
        assertFalse(Rules.validateRhythm(Rhythm(sleepStart="bad")).valid)
        assertFalse(Rules.validateRhythm(Rhythm(contactStart="23:00",contactEnd="23:00")).valid)
        assertFalse(Rules.validateRhythm(Rhythm(contactStart="23:30",contactEnd="00:30")).valid)
        assertTrue(Rules.validateRhythm(Rhythm(contactStart="10:00",contactEnd="11:00")).valid)
    }
    @Test fun unknownContactPreferenceIgnoresHiddenContactTimes() {
        assertFalse(Rules.validateRhythm(Rhythm(contactKnown=true,contactStart="bad")).valid)
        assertTrue(Rules.validateRhythm(Rhythm(contactKnown=false,contactStart="bad",contactEnd="")).valid)
    }
    @Test fun invitationAndNoteMutationsPreserveStateOnError() {
        val state=AppState(me,setupComplete=true)
        val created=Rules.createInvite(state,"unique",1000).state
        assertEquals(86401000L,created.invite!!.expiresMillis)
        assertEquals(created,Rules.acceptInvite(created,"wrong",Profile("你","tokyo"),2000).state)
        assertNotNull(Rules.acceptInvite(created,"unique",me,86401000).error)
        val revoked=Rules.revokeInvite(created).state
        assertNotNull(Rules.acceptInvite(revoked,"unique",me,2000).error)
        val paired=Rules.acceptInvite(created,"unique",Profile("你","tokyo"),2000).state
        assertNotNull(paired.partner)
        assertNull(paired.partnerSchedule)
        assertNotNull(Rules.acceptInvite(paired,"unique",me,2000).error)
        assertNotNull(Rules.saveNote(state,"😀".repeat(121),1000).error)
        assertEquals(120,Rules.saveNote(state,"😀".repeat(120),1000).state.note!!.text.codePointCount(0,240))
    }
    @Test fun overnightSleepCrossesRealLocalDates() {
        val moments=listOf("2026-09-29T14:59:00Z","2026-09-29T15:00:00Z","2026-09-29T22:59:00Z","2026-09-29T23:00:00Z")
        assertEquals(listOf("未安排","睡觉","睡觉","未安排"),moments.map {TimeEngine.activityAt(me,schedule,null,instant(it)).label})
    }
    @Test fun weekdaySelectionUsesEachProfilesLocalDate() {
        val distinct=Schedule(Rhythm(activity="工作",activityStart="01:00",activityEnd="22:00",sleepStart="22:30",sleepEnd="00:30"),Rhythm(activity="周末",activityStart="01:00",activityEnd="22:00",sleepStart="22:30",sleepEnd="00:30"))
        assertEquals("周末",TimeEngine.activityAt(me,distinct,null,instant("2026-10-02T17:00:00Z")).label)
        assertEquals("工作",TimeEngine.activityAt(Profile("你","new-york"),distinct,null,instant("2026-10-03T01:00:00Z")).label)
    }
    @Test fun dstContactWindowsPreserveSkippedAndRepeatedHours() {
        val ny=Profile("你","new-york")
        val r=Rhythm(sleepStart="04:00",sleepEnd="08:00",contactStart="01:00",contactEnd="03:00")
        val s=Schedule(r,r)
        assertEquals(listOf(Window(instant("2026-03-08T06:00:00Z"),instant("2026-03-08T07:00:00Z"))),TimeEngine.commonWindows(instant("2026-03-08T05:00:00Z"),instant("2026-03-08T09:00:00Z"),ny,s,null,ny,s))
        assertEquals(listOf(Window(instant("2026-11-01T05:00:00Z"),instant("2026-11-01T08:00:00Z"))),TimeEngine.commonWindows(instant("2026-11-01T04:00:00Z"),instant("2026-11-01T10:00:00Z"),ny,s,null,ny,s))
    }
    @Test fun temporaryOnlyAppliesInsideItsActualLifetime() {
        val from=instant("2026-09-29T02:00:15.125Z")
        val until=from.plusSeconds(90)
        val temp=TemporaryStatus(true,until.toEpochMilli(),from.toEpochMilli())
        assertFalse(TimeEngine.contactAt(me,schedule,temp,from.minusMillis(1))!!)
        assertTrue(TimeEngine.contactAt(me,schedule,temp,from)!!)
        assertFalse(TimeEngine.contactAt(me,schedule,temp,until)!!)
        val partnerRhythm=Rhythm(contactStart="09:00",contactEnd="18:00")
        assertEquals(listOf(Window(from,until)),TimeEngine.commonWindows(from.minusSeconds(30),until.plusSeconds(30),me,schedule,temp,me,Schedule(partnerRhythm,partnerRhythm)))
        assertEquals("上班",TimeEngine.activityAt(me,schedule,temp,from).label)
    }
}
