package com.niumadiary.app.core.time

import java.time.Duration
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.temporal.TemporalAdjusters

data class WeeklyWorkProgress(
    val weekStart: LocalDate,
    val weekEnd: LocalDate,
    val plannedWorkdays: Int,
    val total: Duration,
    val completed: Duration,
) {
    val remaining: Duration
        get() = total.minus(completed).coerceAtLeast(Duration.ZERO)

    val fraction: Double
        get() = if (total.isZero) 0.0 else {
            completed.seconds.toDouble().div(total.seconds).coerceIn(0.0, 1.0)
        }
}

/** Counts only time inside each effective workday's configured start/end window. */
object WeeklyWorkProgressCalculator {
    fun calculate(
        calendar: WorkCalendar,
        now: ZonedDateTime,
    ): WeeklyWorkProgress {
        val zonedNow = now.withZoneSameInstant(calendar.schedule.zoneId)
        val today = zonedNow.toLocalDate()
        val weekStart = today.with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
        val weekEnd = weekStart.plusDays(6)
        val shiftDuration = Duration.between(calendar.schedule.start, calendar.schedule.end)
        val workdays = (0L..6L)
            .map(weekStart::plusDays)
            .filter(calendar::isWorkday)

        val completed = workdays.fold(Duration.ZERO) { accumulated, date ->
            accumulated.plus(
                when {
                    date < today -> shiftDuration
                    date > today -> Duration.ZERO
                    zonedNow.toLocalTime() <= calendar.schedule.start -> Duration.ZERO
                    zonedNow.toLocalTime() >= calendar.schedule.end -> shiftDuration
                    else -> Duration.between(calendar.schedule.start, zonedNow.toLocalTime())
                },
            )
        }

        return WeeklyWorkProgress(
            weekStart = weekStart,
            weekEnd = weekEnd,
            plannedWorkdays = workdays.size,
            total = shiftDuration.multipliedBy(workdays.size.toLong()),
            completed = completed,
        )
    }
}

private fun Duration.coerceAtLeast(minimum: Duration): Duration =
    if (this < minimum) minimum else this
