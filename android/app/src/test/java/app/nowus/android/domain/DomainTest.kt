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
    @Test fun intervalOverlapUsesHalfOpenRangesAcrossMidnight() {
        assertTrue(TimeEngine.intervalsOverlap(23 * 60, 7 * 60, 6 * 60 + 30, 7 * 60 + 30))
        assertFalse(TimeEngine.intervalsOverlap(23 * 60, 7 * 60, 7 * 60, 7 * 60 + 30))
        assertFalse(TimeEngine.intervalsOverlap(7 * 60, 8 * 60, 8 * 60, 9 * 60))
        assertTrue(TimeEngine.intervalsOverlap(8 * 60, 10 * 60, 9 * 60, 11 * 60))
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
    @Test fun partnerBusyRemovesOnlyItsMillisecondLifetimeAndRestoresAtExpiry() {
        val start=instant("2026-09-29T12:00:00Z")
        val end=instant("2026-09-29T12:05:00Z")
        val from=instant("2026-09-29T12:01:15.125Z")
        val until=instant("2026-09-29T12:03:20.875Z")
        val busy=TemporaryStatus(false,until.toEpochMilli(),from.toEpochMilli())
        assertEquals(listOf(Window(start,from),Window(until,end)),
            TimeEngine.commonWindows(start,end,me,schedule,null,me,schedule,busy))
        assertFalse(TimeEngine.contactAt(me,schedule,busy,until.minusMillis(1))!!)
        assertTrue(TimeEngine.contactAt(me,schedule,busy,until)!!)
    }
    @Test fun partnerAvailableCreatesWindowsForMissingOrUnknownContactPreferences() {
        val start=instant("2026-09-29T12:00:00Z")
        val end=instant("2026-09-29T12:05:00Z")
        val from=instant("2026-09-29T12:00:10.001Z")
        val until=instant("2026-09-29T12:04:50.999Z")
        val available=TemporaryStatus(true,until.toEpochMilli(),from.toEpochMilli())
        val unknown=Schedule(Rhythm(contactKnown=false),Rhythm(contactKnown=false))
        for(partnerSchedule in listOf(null,unknown)) {
            assertEquals(listOf(Window(from,until)),
                TimeEngine.commonWindows(start,end,me,schedule,null,me,partnerSchedule,available))
            assertNull(TimeEngine.contactAt(me,partnerSchedule,available,from.minusMillis(1)))
            assertTrue(TimeEngine.contactAt(me,partnerSchedule,available,from)!!)
            assertTrue(TimeEngine.contactAt(me,partnerSchedule,available,until.minusMillis(1))!!)
            assertNull(TimeEngine.contactAt(me,partnerSchedule,available,until))
        }
    }
    @Test fun simultaneousAvailableOverridesIntersectBothMillisecondLifetimes() {
        val start=instant("2026-09-29T02:00:00Z")
        val end=instant("2026-09-29T02:05:00Z")
        val myFrom=instant("2026-09-29T02:00:10.125Z")
        val myUntil=instant("2026-09-29T02:03:20.875Z")
        val partnerFrom=instant("2026-09-29T02:01:15.625Z")
        val partnerUntil=instant("2026-09-29T02:04:40.375Z")
        val mine=TemporaryStatus(true,myUntil.toEpochMilli(),myFrom.toEpochMilli())
        val theirs=TemporaryStatus(true,partnerUntil.toEpochMilli(),partnerFrom.toEpochMilli())
        assertEquals(listOf(Window(partnerFrom,myUntil)),
            TimeEngine.commonWindows(start,end,me,null,mine,me,null,theirs))
        assertEquals(listOf(Window(partnerFrom,myUntil)),
            TimeEngine.commonWindows(start,end,me,null,theirs,me,null,mine))
    }
    @Test fun partnerBusyWinsWhileMyAvailableOverrideIsActive() {
        val start=instant("2026-09-29T02:00:00Z")
        val end=instant("2026-09-29T02:05:00Z")
        val myFrom=instant("2026-09-29T02:00:10.125Z")
        val myUntil=instant("2026-09-29T02:04:50.875Z")
        val partnerFrom=instant("2026-09-29T02:01:15.625Z")
        val partnerUntil=instant("2026-09-29T02:03:20.375Z")
        val mine=TemporaryStatus(true,myUntil.toEpochMilli(),myFrom.toEpochMilli())
        val theirs=TemporaryStatus(false,partnerUntil.toEpochMilli(),partnerFrom.toEpochMilli())
        val contact=Rhythm(contactStart="09:00",contactEnd="18:00")
        assertEquals(listOf(Window(myFrom,partnerFrom),Window(partnerUntil,myUntil)),
            TimeEngine.commonWindows(start,end,me,null,mine,me,Schedule(contact,contact),theirs))
    }
    @Test fun cityTemplatesProvideEditableStudentAndOfficeDayBlocks() {
        val student=RoutineTemplates.forCity("beijing",RoutineTemplate.STUDENT)
        val weekday=student.weekday.blocks.map { it.label }
        assertTrue(weekday.containsAll(listOf("睡觉","早餐","上学通勤","上午上课","午餐","午休","下午上课","晚餐")))
        assertEquals("学生",student.templateId)
        assertTrue(Rules.validateRhythm(student.weekday).valid)
        assertTrue(Rules.validateRhythm(student.rest).valid)

        val office=RoutineTemplates.forCity("paris",RoutineTemplate.OFFICE_WORKER)
        assertEquals("上班族",office.templateId)
        assertTrue(office.weekday.blocks.any { it.label=="上午上班" })
        assertTrue(office.weekday.blocks.any { it.label=="通勤" })
        assertTrue(Rules.validateRhythm(office.weekday).valid)
        assertEquals("20:00",office.weekday.blocks.single { it.id=="dinner" }.start)
    }
    @Test fun customRoutineBlocksDriveTimelineAndRejectOverlaps() {
        val blocks=listOf(
            RoutineBlock("sleep","睡觉","23:00","07:00"),
            RoutineBlock("breakfast","早餐","07:00","07:30"),
            RoutineBlock("class","上午上课","08:00","12:00")
        )
        val custom=Rhythm(blocks=blocks)
        val date=instant("2026-09-29T23:15:00Z")
        assertEquals("早餐",TimeEngine.activityAt(me,Schedule(custom,custom),null,date).label)
        assertEquals("上午上课",TimeEngine.activityAt(me,Schedule(custom,custom),null,date.plusSeconds(3600)).label)
        assertFalse(Rules.validateRhythm(custom.copy(blocks=blocks+RoutineBlock("lunch","午餐","11:30","12:30"))).valid)
        assertFalse(Rules.validateRhythm(custom.copy(blocks=blocks+RoutineBlock("late","晚间活动","22:00","23:30"))).valid)
    }
    @Test fun segmentedRoutineTreatsWakeTimeAsSleepBoundary() {
        val blocks=listOf(
            RoutineBlock("sleep","睡觉","23:00","07:00"),
            RoutineBlock("breakfast","早餐","07:00","08:00"),
        )
        val adjacentContact=Rhythm(blocks=blocks,contactKnown=true,contactStart="07:00",contactEnd="08:00")
        assertTrue(Rules.validateRhythm(adjacentContact).valid)

        val sleepContact=adjacentContact.copy(contactStart="06:30",contactEnd="07:30")
        assertTrue(Rules.validateRhythm(sleepContact).errors.contains("联系不可与睡眠重叠"))

        val overlappingBlock=adjacentContact.copy(blocks=blocks+RoutineBlock("early","出门准备","06:30","07:30"))
        assertTrue(Rules.validateRhythm(overlappingBlock).errors.contains("日常时段不能重叠"))
    }
    @Test fun routineCategoriesDecodeOldBlocksAndReachTimelineActivities() {
        val oldBlock=kotlinx.serialization.json.Json.decodeFromString<RoutineBlock>(
            """{"id":"walk","label":"散步","start":"10:00","end":"11:00"}"""
        )
        assertEquals(RoutineCategory.OTHER,oldBlock.category)

        val blocks=listOf(
            RoutineBlock("sleep","睡觉","23:00","07:00"),
            RoutineBlock("workout","运动","10:00","11:00",RoutineCategory.EXERCISE),
        )
        val json=kotlinx.serialization.json.Json { encodeDefaults=true }
        val roundTrip=json.decodeFromString<RoutineBlock>(json.encodeToString(blocks[1]))
        assertEquals(RoutineCategory.EXERCISE,roundTrip.category)
        val timeline=Schedule(Rhythm(blocks=blocks),Rhythm(blocks=blocks))
        assertEquals(RoutineCategory.EXERCISE,TimeEngine.activityAt(me,timeline,null,instant("2026-09-29T02:30:00Z")).category)
        assertEquals(RoutineCategory.UNSCHEDULED,TimeEngine.activityAt(me,timeline,null,instant("2026-09-29T01:00:00Z")).category)
    }
    @Test fun studentAndOfficeTemplatesAssignCategoriesAndPreparation() {
        val student=RoutineTemplates.forCity("beijing",RoutineTemplate.STUDENT)
        assertEquals(RoutineCategory.PREPARATION,student.weekday.blocks.single { it.id=="preparation" }.category)
        assertEquals(RoutineCategory.MEAL,student.weekday.blocks.single { it.id=="breakfast" }.category)
        assertEquals(RoutineCategory.STUDY_WORK,student.weekday.blocks.single { it.id=="morning-class" }.category)
        assertEquals(RoutineCategory.COMMUTE,student.weekday.blocks.single { it.id=="commute-morning" }.category)
        assertEquals("08:45",student.rest.blocks.single { it.id=="preparation" }.end)
        assertEquals(RoutineCategory.OTHER,student.rest.blocks.single { it.id=="afternoon-free" }.category)
        assertFalse((student.weekday.blocks+student.rest.blocks).any { it.category==RoutineCategory.SOCIAL || it.category==RoutineCategory.LIFE_ADMIN || it.category==RoutineCategory.EXERCISE })

        val office=RoutineTemplates.forCity("paris",RoutineTemplate.OFFICE_WORKER)
        assertEquals(RoutineCategory.STUDY_WORK,office.weekday.blocks.single { it.id=="morning-work" }.category)
        assertEquals(RoutineCategory.REST,office.weekday.blocks.single { it.id=="evening-free" }.category)
        assertEquals(RoutineCategory.OTHER,office.rest.blocks.single { it.id=="afternoon-free" }.category)
        assertTrue(Rules.validateRhythm(student.weekday).valid)
        assertTrue(Rules.validateRhythm(student.rest).valid)
        assertTrue(Rules.validateRhythm(office.weekday).valid)
    }
    @Test fun legacyRhythmJsonStillDecodesAndKeepsActivityBehavior() {
        val legacy=kotlinx.serialization.json.Json.decodeFromString<Schedule>("""{"weekday":{"sleepStart":"23:00","sleepEnd":"07:00","activity":"上课","activityStart":"09:00","activityEnd":"17:00","contactKnown":true,"contactStart":"20:00","contactEnd":"22:00"},"rest":{"sleepStart":"23:00","sleepEnd":"08:00","activity":"休息","activityStart":"10:00","activityEnd":"12:00","contactKnown":true,"contactStart":"20:00","contactEnd":"22:00"}}""")
        assertTrue(legacy.weekday.blocks.isEmpty())
        assertEquals("学生",legacy.templateId)
        assertEquals("上课",legacy.weekday.activity)
    }
}
