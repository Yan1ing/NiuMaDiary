package com.niumadiary.app.core.holiday

import java.time.LocalDate

/** Controls whether official weekend make-up workdays are observed. */
enum class WeekendPolicy {
    /** Saturday and Sunday remain off unless the user explicitly overrides that date. */
    FIXED_WEEKENDS_OFF,

    /** Official make-up workdays may turn a Saturday or Sunday into a workday. */
    FOLLOW_OFFICIAL_ADJUSTMENTS,
}

/** A highest-priority, user supplied decision for one date. */
enum class WorkdayOverride {
    WORK,
    OFF,
}

/**
 * A date explicitly listed by an official annual holiday calendar.
 *
 * [isOffDay] is false for an official make-up workday.
 */
data class OfficialDayAdjustment(
    val date: LocalDate,
    val isOffDay: Boolean,
    val name: String? = null,
)

/** Distinguishes a complete annual calendar from an empty, not-yet-published one. */
enum class HolidayDataStatus {
    PUBLISHED,
    AWAITING_PUBLICATION,
    UNAVAILABLE,
}

/** A versioned snapshot of the official holiday information for one calendar year. */
class HolidayYear private constructor(
    val year: Int,
    val status: HolidayDataStatus,
    adjustments: Collection<OfficialDayAdjustment>,
) {
    private val adjustmentsByDate: Map<LocalDate, OfficialDayAdjustment>

    init {
        require(year in 1..9999) { "year must be supported by LocalDate" }
        require(adjustments.all { it.date.year == year }) {
            "all holiday adjustments must belong to year $year"
        }
        require(adjustments.map { it.date }.distinct().size == adjustments.size) {
            "a holiday year cannot contain duplicate dates"
        }
        require(status == HolidayDataStatus.PUBLISHED || adjustments.isEmpty()) {
            "unpublished holiday data cannot contain adjustments"
        }
        adjustmentsByDate = adjustments.associateBy(OfficialDayAdjustment::date)
    }

    fun adjustmentOn(date: LocalDate): OfficialDayAdjustment? {
        if (date.year != year) return null
        return adjustmentsByDate[date]
    }

    fun allAdjustments(): List<OfficialDayAdjustment> =
        adjustmentsByDate.values.sortedBy(OfficialDayAdjustment::date)

    companion object {
        fun published(
            year: Int,
            adjustments: Collection<OfficialDayAdjustment> = emptyList(),
        ): HolidayYear = HolidayYear(year, HolidayDataStatus.PUBLISHED, adjustments)

        fun awaitingPublication(year: Int): HolidayYear =
            HolidayYear(year, HolidayDataStatus.AWAITING_PUBLICATION, emptyList())

        fun unavailable(year: Int): HolidayYear =
            HolidayYear(year, HolidayDataStatus.UNAVAILABLE, emptyList())
    }
}

/** Read-only holiday source; production can back this with APK-bundled JSON. */
fun interface HolidayCalendar {
    fun year(year: Int): HolidayYear?

    companion object {
        val EMPTY: HolidayCalendar = HolidayCalendar { null }
    }
}

/** Convenient immutable implementation for tests and bundled annual snapshots. */
class InMemoryHolidayCalendar(years: Collection<HolidayYear>) : HolidayCalendar {
    private val yearsByNumber: Map<Int, HolidayYear>

    init {
        require(years.map(HolidayYear::year).distinct().size == years.size) {
            "holiday calendar cannot contain duplicate years"
        }
        yearsByNumber = years.associateBy(HolidayYear::year)
    }

    override fun year(year: Int): HolidayYear? = yearsByNumber[year]
}
