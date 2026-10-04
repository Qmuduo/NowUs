package app.nowus.android.domain

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ContactRailTest {
    private fun instant(value: String) = Instant.parse(value)
    private fun schedule(
        start: String = "10:00",
        end: String = "11:00",
        known: Boolean = true,
        sleepStart: String = "23:00",
        sleepEnd: String = "07:00",
    ) = Schedule(
        weekday = Rhythm(contactKnown = known, contactStart = start, contactEnd = end, sleepStart = sleepStart, sleepEnd = sleepEnd),
        rest = Rhythm(contactKnown = known, contactStart = start, contactEnd = end, sleepStart = sleepStart, sleepEnd = sleepEnd),
    )

    @Test
    fun contactRailsUseEachPersonsLocalClockAndRemainSeparate() {
        val start = instant("2026-10-03T00:00:00Z")
        val end = instant("2026-10-04T00:00:00Z")
        val beijing = TimeEngine.contactWindows(start, end, Profile("我", "beijing"), schedule())
        val newYork = TimeEngine.contactWindows(start, end, Profile("伴侣", "new-york"), schedule())

        assertEquals(listOf(Window(instant("2026-10-03T02:00:00Z"), instant("2026-10-03T03:00:00Z"))), beijing)
        assertEquals(listOf(Window(instant("2026-10-03T14:00:00Z"), instant("2026-10-03T15:00:00Z"))), newYork)
        assertTrue(beijing.single().end <= newYork.single().start)
    }

    @Test
    fun contactRailKeepsCrossMidnightIntervalContinuousAndExcludesSleep() {
        val profile = Profile("我", "beijing")
        val start = instant("2026-10-03T13:00:00Z")
        val end = instant("2026-10-03T19:00:00Z")

        val overnight = TimeEngine.contactWindows(
            start,
            end,
            profile,
            schedule("22:00", "02:00", sleepStart = "04:00", sleepEnd = "05:00"),
        )
        val sleepOverlap = TimeEngine.contactWindows(
            start,
            end,
            profile,
            schedule("22:00", "23:30", sleepStart = "22:30", sleepEnd = "07:00"),
        )

        assertEquals(listOf(Window(instant("2026-10-03T14:00:00Z"), instant("2026-10-03T18:00:00Z"))), overnight)
        assertEquals(listOf(Window(instant("2026-10-03T14:00:00Z"), instant("2026-10-03T14:30:00Z"))), sleepOverlap)
    }

    @Test
    fun unknownPreferencesProduceNoContactRail() {
        val start = instant("2026-10-03T00:00:00Z")
        val end = instant("2026-10-04T00:00:00Z")

        assertTrue(TimeEngine.contactWindows(start, end, Profile("伴侣", "new-york"), schedule(known = false)).isEmpty())
        assertTrue(TimeEngine.contactWindows(start, end, Profile("伴侣", "new-york"), null).isEmpty())
    }
}
