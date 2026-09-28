package com.niumadiary.app.core.time

import com.niumadiary.app.core.holiday.HolidayDataStatus
import com.niumadiary.app.core.holiday.HolidayYear
import com.niumadiary.app.core.holiday.InMemoryHolidayCalendar
import com.niumadiary.app.core.holiday.OfficialDayAdjustment
import com.niumadiary.app.core.holiday.WeekendPolicy
import com.niumadiary.app.core.holiday.WorkdayOverride
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkCalendarTest {
    @Test
    fun `fixed weekends ignore an official Saturday make-up workday`() {
        val saturday = LocalDate.of(2026, 1, 3)
        val calendar = calendar(
            weekendPolicy = WeekendPolicy.FIXED_WEEKENDS_OFF,
            adjustments = listOf(
                OfficialDayAdjustment(saturday, isOffDay = false, name = "make-up workday"),
            ),
        )

        val decision = calendar.decisionFor(saturday)

        assertFalse(decision.isWorkday)
        assertEquals(WorkdayDecisionSource.FIXED_WEEKEND, decision.source)
        assertEquals(WorkdayConfidence.CONFIRMED, decision.confidence)
    }

    @Test
    fun `official adjustment mode observes a Saturday make-up workday`() {
        val saturday = LocalDate.of(2026, 1, 3)
        val calendar = calendar(
            weekendPolicy = WeekendPolicy.FOLLOW_OFFICIAL_ADJUSTMENTS,
            adjustments = listOf(OfficialDayAdjustment(saturday, isOffDay = false)),
        )

        val decision = calendar.decisionFor(saturday)

        assertTrue(decision.isWorkday)
        assertEquals(WorkdayDecisionSource.OFFICIAL_ADJUSTMENT, decision.source)
    }

    @Test
    fun `user override has priority over fixed weekend and official holiday`() {
        val saturday = LocalDate.of(2026, 1, 3)
        val calendar = calendar(
            weekendPolicy = WeekendPolicy.FIXED_WEEKENDS_OFF,
            adjustments = listOf(OfficialDayAdjustment(saturday, isOffDay = true)),
            overrides = mapOf(saturday to WorkdayOverride.WORK),
        )

        val decision = calendar.decisionFor(saturday)

        assertTrue(decision.isWorkday)
        assertEquals(WorkdayDecisionSource.USER_OVERRIDE, decision.source)
    }

    @Test
    fun `official weekday holiday is off`() {
        val monday = LocalDate.of(2026, 1, 5)
        val calendar = calendar(
            adjustments = listOf(OfficialDayAdjustment(monday, isOffDay = true, name = "holiday")),
        )

        val decision = calendar.decisionFor(monday)

        assertFalse(decision.isWorkday)
        assertEquals("holiday", decision.holidayName)
        assertEquals(WorkdayDecisionSource.OFFICIAL_ADJUSTMENT, decision.source)
    }

    @Test
    fun `awaiting annual publication remains distinguishable from a published empty year`() {
        val monday = LocalDate.of(2027, 1, 4)
        val awaitingCalendar = WorkCalendar(
            holidayCalendar = InMemoryHolidayCalendar(
                listOf(HolidayYear.awaitingPublication(2027)),
            ),
        )
        val publishedCalendar = WorkCalendar(
            holidayCalendar = InMemoryHolidayCalendar(
                listOf(HolidayYear.published(2027)),
            ),
        )

        val provisional = awaitingCalendar.decisionFor(monday)
        val confirmed = publishedCalendar.decisionFor(monday)

        assertTrue(provisional.isWorkday)
        assertEquals(HolidayDataStatus.AWAITING_PUBLICATION, provisional.holidayDataStatus)
        assertEquals(WorkdayConfidence.PROVISIONAL_HOLIDAY_DATA, provisional.confidence)
        assertEquals(WorkdayConfidence.CONFIRMED, confirmed.confidence)
    }

    @Test
    fun `next workday crosses a weekday holiday and weekend`() {
        val thursday = LocalDate.of(2026, 10, 1)
        val friday = LocalDate.of(2026, 10, 2)
        val calendar = calendar(
            adjustments = listOf(
                OfficialDayAdjustment(thursday, isOffDay = true),
                OfficialDayAdjustment(friday, isOffDay = true),
            ),
        )

        val next = calendar.nextWorkday(thursday)

        assertEquals(LocalDate.of(2026, 10, 5), next?.date)
    }

    @Test
    fun `workday count is inclusive and returns zero for a reversed range`() {
        val calendar = calendar()

        assertEquals(
            5,
            calendar.countWorkdays(
                LocalDate.of(2026, 8, 3),
                LocalDate.of(2026, 8, 9),
            ),
        )
        assertEquals(
            0,
            calendar.countWorkdays(
                LocalDate.of(2026, 8, 9),
                LocalDate.of(2026, 8, 3),
            ),
        )
    }

    private fun calendar(
        weekendPolicy: WeekendPolicy = WeekendPolicy.FIXED_WEEKENDS_OFF,
        adjustments: List<OfficialDayAdjustment> = emptyList(),
        overrides: Map<LocalDate, WorkdayOverride> = emptyMap(),
    ): WorkCalendar = WorkCalendar(
        schedule = WorkSchedule(weekendPolicy = weekendPolicy),
        holidayCalendar = InMemoryHolidayCalendar(
            listOf(HolidayYear.published(2026, adjustments)),
        ),
        overrides = overrides,
    )
}
