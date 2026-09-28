package com.niumadiary.app.core.holiday

import com.niumadiary.app.core.time.WorkCalendar
import com.niumadiary.app.core.time.WorkSchedule
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BundledHolidayCalendarTest {
    @Test
    fun `bundled 2026 calendar contains the complete published adjustment set`() {
        val year = checkNotNull(BundledHolidayCalendar.year(2026))

        assertEquals(HolidayDataStatus.PUBLISHED, year.status)
        assertEquals(39, year.allAdjustments().size)
        val nationalDay = checkNotNull(year.adjustmentOn(LocalDate.of(2026, 10, 1)))
        val makeUpDay = checkNotNull(year.adjustmentOn(LocalDate.of(2026, 10, 10)))
        assertTrue(nationalDay.isOffDay)
        assertEquals("国庆节", nationalDay.name)
        assertFalse(makeUpDay.isOffDay)
        assertEquals("法定调休补班", makeUpDay.name)
    }

    @Test
    fun `fixed weekend ignores bundled Sunday make-up workday`() {
        val calendar = WorkCalendar(
            schedule = WorkSchedule(weekendPolicy = WeekendPolicy.FIXED_WEEKENDS_OFF),
            holidayCalendar = BundledHolidayCalendar,
        )

        val decision = calendar.decisionFor(LocalDate.of(2026, 1, 4))

        assertFalse(decision.isWorkday)
    }

    @Test
    fun `official adjustment mode observes bundled Sunday make-up workday`() {
        val calendar = WorkCalendar(
            schedule = WorkSchedule(weekendPolicy = WeekendPolicy.FOLLOW_OFFICIAL_ADJUSTMENTS),
            holidayCalendar = BundledHolidayCalendar,
        )

        val decision = calendar.decisionFor(LocalDate.of(2026, 1, 4))

        assertTrue(decision.isWorkday)
        assertEquals(com.niumadiary.app.core.time.WorkdayDecisionSource.OFFICIAL_ADJUSTMENT, decision.source)
    }

    @Test
    fun `next year is explicitly awaiting publication`() {
        val year = checkNotNull(BundledHolidayCalendar.year(2027))

        assertEquals(HolidayDataStatus.AWAITING_PUBLICATION, year.status)
        assertTrue(year.allAdjustments().isEmpty())
    }
}
