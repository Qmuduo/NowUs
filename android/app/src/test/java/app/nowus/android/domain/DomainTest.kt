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
}
