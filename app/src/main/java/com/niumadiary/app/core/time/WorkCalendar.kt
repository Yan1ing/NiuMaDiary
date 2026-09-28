package com.niumadiary.app.core.time

import com.niumadiary.app.core.holiday.HolidayCalendar
import com.niumadiary.app.core.holiday.HolidayDataStatus
import com.niumadiary.app.core.holiday.BundledHolidayCalendar
import com.niumadiary.app.core.holiday.WeekendPolicy
import com.niumadiary.app.core.holiday.WorkdayOverride
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

data class WorkSchedule(
    val start: LocalTime = LocalTime.of(9, 0),
    val end: LocalTime = LocalTime.of(18, 0),
    val zoneId: ZoneId = ZoneId.of(DEFAULT_ZONE_ID),
    val weekendPolicy: WeekendPolicy = WeekendPolicy.FIXED_WEEKENDS_OFF,
) {
    init {
        require(start < end) { "overnight or zero-length work schedules are not supported" }
    }

    companion object {
        const val DEFAULT_ZONE_ID: String = "Asia/Shanghai"
    }
}

enum class WorkdayDecisionSource {
    USER_OVERRIDE,
    FIXED_WEEKEND,
    OFFICIAL_ADJUSTMENT,
    WEEKEND_BASELINE,
    WEEKDAY_BASELINE,
}

enum class WorkdayConfidence {
    CONFIRMED,
    PROVISIONAL_HOLIDAY_DATA,
}

data class WorkdayDecision(
    val date: LocalDate,
    val isWorkday: Boolean,
    val source: WorkdayDecisionSource,
    val confidence: WorkdayConfidence,
    val holidayDataStatus: HolidayDataStatus,
    val holidayName: String? = null,
)

data class NextWorkday(
    val date: LocalDate,
    val decision: WorkdayDecision,
)

/**
 * Resolves workdays with this priority:
 * user override > chosen weekend policy > official annual data > weekday baseline.
 */
class WorkCalendar(
    val schedule: WorkSchedule = WorkSchedule(),
    private val holidayCalendar: HolidayCalendar = BundledHolidayCalendar,
    private val overrides: Map<LocalDate, WorkdayOverride> = emptyMap(),
) {
    fun decisionFor(date: LocalDate): WorkdayDecision {
        val holidayYear = holidayCalendar.year(date.year)
        val dataStatus = holidayYear?.status ?: HolidayDataStatus.UNAVAILABLE
        val officialAdjustment = holidayYear?.adjustmentOn(date)

        overrides[date]?.let { override ->
            return WorkdayDecision(
                date = date,
                isWorkday = override == WorkdayOverride.WORK,
                source = WorkdayDecisionSource.USER_OVERRIDE,
                confidence = WorkdayConfidence.CONFIRMED,
                holidayDataStatus = dataStatus,
                holidayName = officialAdjustment?.name,
            )
        }

        val isWeekend = date.dayOfWeek == DayOfWeek.SATURDAY ||
            date.dayOfWeek == DayOfWeek.SUNDAY

        if (isWeekend && schedule.weekendPolicy == WeekendPolicy.FIXED_WEEKENDS_OFF) {
            return WorkdayDecision(
                date = date,
                isWorkday = false,
                source = WorkdayDecisionSource.FIXED_WEEKEND,
                confidence = WorkdayConfidence.CONFIRMED,
                holidayDataStatus = dataStatus,
                holidayName = officialAdjustment?.name,
            )
        }

        officialAdjustment?.let { adjustment ->
            return WorkdayDecision(
                date = date,
                isWorkday = !adjustment.isOffDay,
                source = WorkdayDecisionSource.OFFICIAL_ADJUSTMENT,
                confidence = WorkdayConfidence.CONFIRMED,
                holidayDataStatus = dataStatus,
                holidayName = adjustment.name,
            )
        }

        val confidence = if (dataStatus == HolidayDataStatus.PUBLISHED) {
            WorkdayConfidence.CONFIRMED
        } else {
            WorkdayConfidence.PROVISIONAL_HOLIDAY_DATA
        }

        return WorkdayDecision(
            date = date,
            isWorkday = !isWeekend,
            source = if (isWeekend) {
                WorkdayDecisionSource.WEEKEND_BASELINE
            } else {
                WorkdayDecisionSource.WEEKDAY_BASELINE
            },
            confidence = confidence,
            holidayDataStatus = dataStatus,
        )
    }

    fun isWorkday(date: LocalDate): Boolean = decisionFor(date).isWorkday

    fun nextWorkday(
        onOrAfter: LocalDate,
        maxSearchDays: Int = DEFAULT_MAX_SEARCH_DAYS,
    ): NextWorkday? {
        require(maxSearchDays > 0) { "maxSearchDays must be positive" }

        repeat(maxSearchDays) { offset ->
            val date = onOrAfter.plusDays(offset.toLong())
            val decision = decisionFor(date)
            if (decision.isWorkday) return NextWorkday(date, decision)
        }
        return null
    }

    fun countWorkdays(fromInclusive: LocalDate, toInclusive: LocalDate): Int {
        if (fromInclusive > toInclusive) return 0
        val numberOfDays = ChronoUnit.DAYS.between(fromInclusive, toInclusive).toInt() + 1
        return (0 until numberOfDays).count { offset ->
            isWorkday(fromInclusive.plusDays(offset.toLong()))
        }
    }

    companion object {
        private const val DEFAULT_MAX_SEARCH_DAYS: Int = 366 * 3
    }
}
