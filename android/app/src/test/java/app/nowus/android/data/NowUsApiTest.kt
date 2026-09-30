package app.nowus.android.data

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

class NowUsApiTest {
    @Test fun parsesZuluAndExplicitUtcOffsets() {
        val expected = Instant.parse("2026-10-30T07:36:18.754991Z")

        assertEquals(expected, parseApiTimestamp("2026-10-30T07:36:18.754991Z"))
        assertEquals(expected, parseApiTimestamp("2026-10-30T07:36:18.754991+00:00"))
    }
}
