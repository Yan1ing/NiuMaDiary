package com.niumadiary.app.core.time

import com.niumadiary.app.core.holiday.WorkdayOverride
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WeeklyWorkProgressCalculatorTest {
    private val zone = ZoneId.of("Asia/Shanghai")
    private val schedule = WorkSchedule(
        start = LocalTime.of(9, 0),
        end = LocalTime.of(18, 0),
        zoneId = zone,
    )

    @Test
    fun `Wednesday afternoon counts only elapsed time inside work windows`() {
        val result = WeeklyWorkProgressCalculator.calculate(
            calendar = WorkCalendar(schedule),
            now = at(2026, 8, 19, 13),
        )

        assertEquals(LocalDate.of(2026, 8, 17), result.weekStart)
        assertEquals(LocalDate.of(2026, 8, 23), result.weekEnd)
        assertEquals(5, result.plannedWorkdays)
        assertEquals(Duration.ofHours(45), result.total)
        assertEquals(Duration.ofHours(22), result.completed)
        assertEquals(Duration.ofHours(23), result.remaining)
        assertTrue(result.fraction in 0.488..0.490)
    }

    @Test
    fun `time before Monday shift does not count`() {
        val result = WeeklyWorkProgressCalculator.calculate(
            calendar = WorkCalendar(schedule),
            now = at(2026, 8, 17, 8),
        )

        assertEquals(Duration.ZERO, result.completed)
        assertEquals(Duration.ofHours(45), result.remaining)
    }

    @Test
    fun `an off-day override reduces weekly total and elapsed hours`() {
        val wednesday = LocalDate.of(2026, 8, 19)
        val result = WeeklyWorkProgressCalculator.calculate(
            calendar = WorkCalendar(
                schedule = schedule,
                overrides = mapOf(wednesday to WorkdayOverride.OFF),
            ),
            now = at(2026, 8, 20, 12),
        )

        assertEquals(4, result.plannedWorkdays)
        assertEquals(Duration.ofHours(36), result.total)
        assertEquals(Duration.ofHours(21), result.completed)
        assertEquals(Duration.ofHours(15), result.remaining)
    }

    @Test
    fun `weekend after all shifts reports complete`() {
        val result = WeeklyWorkProgressCalculator.calculate(
            calendar = WorkCalendar(schedule),
            now = at(2026, 8, 23, 12),
        )

        assertEquals(result.total, result.completed)
        assertEquals(Duration.ZERO, result.remaining)
        assertEquals(1.0, result.fraction, 0.0)
    }

    private fun at(year: Int, month: Int, day: Int, hour: Int): ZonedDateTime =
        ZonedDateTime.of(year, month, day, hour, 0, 0, 0, zone)
}
