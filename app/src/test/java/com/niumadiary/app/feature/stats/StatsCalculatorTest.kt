package com.niumadiary.app.feature.stats

import com.niumadiary.app.core.holiday.HolidayYear
import com.niumadiary.app.core.holiday.InMemoryHolidayCalendar
import com.niumadiary.app.core.time.WorkCalendar
import com.niumadiary.app.core.time.WorkSchedule
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StatsCalculatorTest {
    private val zone = ZoneId.of("Asia/Shanghai")
    private val workCalendar = WorkCalendar(
        schedule = WorkSchedule(zoneId = zone),
        holidayCalendar = InMemoryHolidayCalendar(
            listOf(HolidayYear.published(2026)),
        ),
    )

    @Test
    fun `daily mood prefers summary then falls back to latest mood entry`() {
        val monday = LocalDate.of(2026, 8, 3)
        val tuesday = monday.plusDays(1)
        val wednesday = monday.plusDays(2)
        val entries = listOf(
            entry("m1", monday, 9, mood = 2),
            entry("m2", monday, 16, mood = 4),
            entry("t1", tuesday, 9, mood = 1),
            entry("t2", tuesday, 15, mood = 3),
            entry("w1", wednesday, 12, mood = null),
        )

        val moods = StatsCalculator.dailyRepresentativeMoods(
            entries = entries,
            summaries = listOf(StatsDaySummary(monday, moodScore = 1)),
            zoneId = zone,
        )

        assertEquals(2, moods.size)
        assertEquals(1, moods[0].moodScore)
        assertEquals(RepresentativeMoodSource.DAY_SUMMARY, moods[0].source)
        assertEquals(3, moods[1].moodScore)
        assertEquals(RepresentativeMoodSource.LATEST_ENTRY, moods[1].source)
    }

    @Test
    fun `overview uses inclusive definitions and distinct recorded workdays`() {
        val monday = LocalDate.of(2026, 8, 3)
        val sunday = LocalDate.of(2026, 8, 9)
        val entries = listOf(
            entry("1", monday, 9, mood = 2, tags = setOf("开会", "摸鱼")),
            entry("2", monday, 14, mood = 3, tags = setOf("开会")),
            entry("3", monday.plusDays(2), 10, mood = null, tags = setOf("改需求")),
            entry("4", monday.plusDays(5), 11, mood = 5, tags = setOf("周末")),
        )

        val overview = StatsCalculator.calculateOverview(
            entries = entries,
            summaries = emptyList(),
            employmentStart = monday,
            fromInclusive = monday,
            toInclusive = sunday,
            zoneId = zone,
            workCalendar = workCalendar,
        )

        assertEquals(7, overview.internshipDayNumber)
        assertEquals(5, overview.plannedWorkdayCount)
        assertEquals(2, overview.recordedWorkdayCount)
        assertEquals(4, overview.entryCount)
        assertEquals(TagCount("开会", 2), overview.topTags.first())
    }

    @Test
    fun `mood distribution includes check-ins and day summary moods`() {
        val day = LocalDate.of(2026, 8, 3)
        val distribution = StatsCalculator.moodDistribution(
            entries = listOf(
                entry("1", day, 9, mood = 1),
                entry("2", day, 10, mood = 1),
                entry("3", day, 11, mood = 5),
            ),
            summaries = listOf(StatsDaySummary(day, moodScore = 1)),
        )

        assertEquals(
            mapOf(1 to 3, 2 to 0, 3 to 0, 4 to 0, 5 to 1, 6 to 0, 7 to 0, 8 to 0),
            distribution,
        )
    }

    @Test
    fun `new expressive mood choices are included without changing old buckets`() {
        val day = LocalDate.of(2026, 8, 3)
        val distribution = StatsCalculator.moodDistribution(
            entries = listOf(
                entry("coffee", day, 9, mood = 6),
                entry("recovered", day, 12, mood = 7),
            ),
            summaries = listOf(StatsDaySummary(day, moodScore = 8)),
        )

        assertEquals(1, distribution[6])
        assertEquals(1, distribution[7])
        assertEquals(1, distribution[8])
        assertEquals(0, distribution[1])
    }

    @Test
    fun `toughest hour waits for enough samples then ranks by low mood share`() {
        val monday = LocalDate.of(2026, 8, 3)
        val entries = listOf(
            entry("1", monday, 9, mood = 1),
            entry("2", monday.plusDays(1), 9, mood = 2),
            entry("3", monday.plusDays(2), 9, mood = 5),
            entry("4", monday, 15, mood = 1),
            entry("5", monday.plusDays(1), 15, mood = 4),
            entry("6", monday, 10, mood = 3),
            entry("7", monday.plusDays(1), 10, mood = 4),
            entry("8", monday.plusDays(2), 10, mood = 5),
        )

        assertNull(
            StatsCalculator.toughestHour(
                entries = entries.take(7),
                zoneId = zone,
            ),
        )

        val insight = StatsCalculator.toughestHour(entries = entries, zoneId = zone)

        assertEquals(9, insight?.hour)
        assertEquals(2, insight?.lowMoodCount)
        assertTrue(checkNotNull(insight).lowMoodRatio > 0.66)
    }

    @Test
    fun `weekday insight unlocks only after three distinct weeks`() {
        val firstMonday = LocalDate.of(2026, 8, 3)
        val twoWeeks = listOf(0L, 7L).flatMap { offset ->
            listOf(
                DailyRepresentativeMood(firstMonday.plusDays(offset), 1, RepresentativeMoodSource.LATEST_ENTRY),
                DailyRepresentativeMood(firstMonday.plusDays(offset + 1), 4, RepresentativeMoodSource.LATEST_ENTRY),
            )
        }
        assertNull(StatsCalculator.toughestWeekdays(twoWeeks, workCalendar))

        val threeWeeks = listOf(0L, 7L, 14L).flatMapIndexed { index, offset ->
            listOf(
                DailyRepresentativeMood(
                    firstMonday.plusDays(offset),
                    if (index == 1) 2 else 1,
                    RepresentativeMoodSource.LATEST_ENTRY,
                ),
                DailyRepresentativeMood(
                    firstMonday.plusDays(offset + 1),
                    if (index == 0) 3 else 4,
                    RepresentativeMoodSource.LATEST_ENTRY,
                ),
            )
        }

        val insight = StatsCalculator.toughestWeekdays(threeWeeks, workCalendar)

        assertEquals(3, insight?.observedWeekCount)
        assertEquals(listOf(java.time.DayOfWeek.MONDAY), insight?.toughestDays?.map { it.dayOfWeek })
        assertEquals(1.0, insight?.toughestDays?.single()?.medianMood ?: -1.0, 0.0)
    }

    @Test
    fun `internship day falls back to first entry and handles future start`() {
        val today = LocalDate.of(2026, 8, 7)

        assertEquals(
            3,
            StatsCalculator.internshipDayNumber(
                employmentStart = null,
                firstEntryDate = LocalDate.of(2026, 8, 5),
                today = today,
            ),
        )
        assertEquals(
            0,
            StatsCalculator.internshipDayNumber(
                employmentStart = LocalDate.of(2026, 8, 8),
                firstEntryDate = null,
                today = today,
            ),
        )
        assertNull(
            StatsCalculator.internshipDayNumber(
                employmentStart = null,
                firstEntryDate = null,
                today = today,
            ),
        )
    }

    private fun entry(
        id: String,
        date: LocalDate,
        hour: Int,
        mood: Int?,
        tags: Set<String> = emptySet(),
    ): StatsEntry = StatsEntry(
        id = id,
        occurredAt = date.atTime(LocalTime.of(hour, 0)).atZone(zone).toInstant(),
        moodScore = mood,
        tags = tags,
    )
}
