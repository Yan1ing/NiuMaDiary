package com.niumadiary.app.core.time

import com.niumadiary.app.core.holiday.HolidayYear
import com.niumadiary.app.core.holiday.InMemoryHolidayCalendar
import com.niumadiary.app.core.holiday.OfficialDayAdjustment
import com.niumadiary.app.core.holiday.WeekendPolicy
import com.niumadiary.app.core.holiday.WorkdayOverride
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkCountdownCalculatorTest {
    private val zone: ZoneId = ZoneId.of("Asia/Shanghai")

    @Test
    fun `exactly 09 00 starts the work countdown`() {
        val calculator = calculator()

        val result = calculator.calculate(at(2026, 8, 7, 9, 0))

        assertEquals(WorkCountdownKind.UNTIL_OFF_WORK, result.kind)
        assertEquals(Duration.ofHours(9), result.remaining)
        assertEquals(LocalTime.of(18, 0), result.target.toLocalTime())
    }

    @Test
    fun `exactly 18 00 on Friday targets Monday morning`() {
        val calculator = calculator()

        val result = calculator.calculate(at(2026, 8, 7, 18, 0))

        assertEquals(WorkCountdownKind.UNTIL_NEXT_WORK, result.kind)
        assertEquals(LocalDate.of(2026, 8, 10), result.target.toLocalDate())
        assertEquals(Duration.ofHours(63), result.remaining)
    }

    @Test
    fun `countdown crosses a statutory long weekend`() {
        val calculator = calculator(
            adjustments = listOf(
                OfficialDayAdjustment(LocalDate.of(2026, 10, 1), isOffDay = true),
                OfficialDayAdjustment(LocalDate.of(2026, 10, 2), isOffDay = true),
            ),
        )

        val result = calculator.calculate(at(2026, 9, 30, 18, 0))

        assertEquals(LocalDate.of(2026, 10, 5), result.target.toLocalDate())
        assertEquals(Duration.ofHours(111), result.remaining)
    }

    @Test
    fun `fixed weekend mode ignores official Saturday workday`() {
        val saturday = LocalDate.of(2026, 8, 8)
        val calculator = calculator(
            weekendPolicy = WeekendPolicy.FIXED_WEEKENDS_OFF,
            adjustments = listOf(OfficialDayAdjustment(saturday, isOffDay = false)),
        )

        val result = calculator.calculate(at(2026, 8, 7, 18, 0))

        assertEquals(LocalDate.of(2026, 8, 10), result.target.toLocalDate())
    }

    @Test
    fun `official adjustment mode can target Saturday morning`() {
        val saturday = LocalDate.of(2026, 8, 8)
        val calculator = calculator(
            weekendPolicy = WeekendPolicy.FOLLOW_OFFICIAL_ADJUSTMENTS,
            adjustments = listOf(OfficialDayAdjustment(saturday, isOffDay = false)),
        )

        val result = calculator.calculate(at(2026, 8, 7, 18, 0))

        assertEquals(saturday, result.target.toLocalDate())
        assertEquals(Duration.ofHours(15), result.remaining)
    }

    @Test
    fun `explicit weekend work override wins in fixed weekend mode`() {
        val saturday = LocalDate.of(2026, 8, 8)
        val calculator = calculator(overrides = mapOf(saturday to WorkdayOverride.WORK))

        val result = calculator.calculate(at(2026, 8, 7, 18, 0))

        assertEquals(saturday, result.target.toLocalDate())
        assertFalse(result.isProvisional)
    }

    @Test
    fun `missing next year data produces a provisional target instead of assuming no holidays`() {
        val workCalendar = WorkCalendar(
            schedule = WorkSchedule(zoneId = zone),
            holidayCalendar = InMemoryHolidayCalendar(
                listOf(
                    HolidayYear.published(2026),
                    HolidayYear.awaitingPublication(2027),
                ),
            ),
        )
        val result = WorkCountdownCalculator(workCalendar)
            .calculate(at(2026, 12, 31, 18, 0))

        assertEquals(LocalDate.of(2027, 1, 1), result.target.toLocalDate())
        assertTrue(result.isProvisional)
    }

    @Test
    fun `injected clock drives parameterless calculation`() {
        val now = at(2026, 8, 7, 8, 0)
        val workCalendar = workCalendar()
        val calculator = WorkCountdownCalculator(
            workCalendar = workCalendar,
            clock = Clock.fixed(now, zone),
        )

        val result = calculator.calculate()

        assertEquals(WorkCountdownKind.UNTIL_WORK, result.kind)
        assertEquals(Duration.ofHours(1), result.remaining)
    }

    private fun calculator(
        weekendPolicy: WeekendPolicy = WeekendPolicy.FIXED_WEEKENDS_OFF,
        adjustments: List<OfficialDayAdjustment> = emptyList(),
        overrides: Map<LocalDate, WorkdayOverride> = emptyMap(),
    ): WorkCountdownCalculator = WorkCountdownCalculator(
        workCalendar(
            weekendPolicy = weekendPolicy,
            adjustments = adjustments,
            overrides = overrides,
        ),
    )

    private fun workCalendar(
        weekendPolicy: WeekendPolicy = WeekendPolicy.FIXED_WEEKENDS_OFF,
        adjustments: List<OfficialDayAdjustment> = emptyList(),
        overrides: Map<LocalDate, WorkdayOverride> = emptyMap(),
    ): WorkCalendar = WorkCalendar(
        schedule = WorkSchedule(zoneId = zone, weekendPolicy = weekendPolicy),
        holidayCalendar = InMemoryHolidayCalendar(
            listOf(HolidayYear.published(2026, adjustments)),
        ),
        overrides = overrides,
    )

    private fun at(
        year: Int,
        month: Int,
        day: Int,
        hour: Int,
        minute: Int,
    ): Instant = ZonedDateTime.of(year, month, day, hour, minute, 0, 0, zone).toInstant()
}
