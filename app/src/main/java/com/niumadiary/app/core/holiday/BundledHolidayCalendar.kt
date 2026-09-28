package com.niumadiary.app.core.holiday

import java.time.LocalDate

/**
 * Holiday data shipped inside the APK, sourced from the published 2026 China holiday schedule.
 *
 * Keeping this local means the home screen never depends on network access. The immediately
 * following year is represented as awaiting publication rather than as a misleading empty year.
 */
object BundledHolidayCalendar : HolidayCalendar {
    private const val LATEST_PUBLISHED_YEAR = 2026

    private val year2026: HolidayYear = HolidayYear.published(
        year = 2026,
        adjustments = buildList {
            addOffDays("元旦", 1, 1..3)
            addWorkDay(1, 4)

            addWorkDay(2, 14)
            addOffDays("春节", 2, 15..23)
            addWorkDay(2, 28)

            addOffDays("清明节", 4, 4..6)

            addOffDays("劳动节", 5, 1..5)
            addWorkDay(5, 9)

            addOffDays("端午节", 6, 19..21)

            addWorkDay(9, 20)
            addOffDays("中秋节", 9, 25..27)

            addOffDays("国庆节", 10, 1..7)
            addWorkDay(10, 10)
        },
    )

    override fun year(year: Int): HolidayYear? = when (year) {
        LATEST_PUBLISHED_YEAR -> year2026
        LATEST_PUBLISHED_YEAR + 1 -> HolidayYear.awaitingPublication(year)
        else -> null
    }

    private fun MutableList<OfficialDayAdjustment>.addOffDays(
        name: String,
        month: Int,
        days: IntRange,
    ) {
        days.forEach { day ->
            add(
                OfficialDayAdjustment(
                    date = LocalDate.of(LATEST_PUBLISHED_YEAR, month, day),
                    isOffDay = true,
                    name = name,
                ),
            )
        }
    }

    private fun MutableList<OfficialDayAdjustment>.addWorkDay(month: Int, day: Int) {
        add(
            OfficialDayAdjustment(
                date = LocalDate.of(LATEST_PUBLISHED_YEAR, month, day),
                isOffDay = false,
                name = "法定调休补班",
            ),
        )
    }
}
