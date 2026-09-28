package com.niumadiary.app.core.time

import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZonedDateTime

enum class WorkCountdownKind {
    UNTIL_WORK,
    UNTIL_OFF_WORK,
    UNTIL_NEXT_WORK,
}

data class WorkCountdown(
    val kind: WorkCountdownKind,
    val now: ZonedDateTime,
    val target: ZonedDateTime,
    val remaining: Duration,
    val currentDateDecision: WorkdayDecision,
    val targetDateDecision: WorkdayDecision,
) {
    val isProvisional: Boolean =
        currentDateDecision.confidence == WorkdayConfidence.PROVISIONAL_HOLIDAY_DATA ||
            targetDateDecision.confidence == WorkdayConfidence.PROVISIONAL_HOLIDAY_DATA
}

/** Pure countdown state machine. Time is injected so every boundary can be deterministic in tests. */
class WorkCountdownCalculator(
    private val workCalendar: WorkCalendar,
    private val clock: Clock = Clock.system(workCalendar.schedule.zoneId),
) {
    fun calculate(): WorkCountdown = calculate(clock.instant())

    fun calculate(now: Instant): WorkCountdown {
        val schedule = workCalendar.schedule
        val localNow = now.atZone(schedule.zoneId)
        val today = localNow.toLocalDate()
        val time = localNow.toLocalTime()
        val todayDecision = workCalendar.decisionFor(today)

        if (todayDecision.isWorkday && time < schedule.start) {
            val target = today.atTime(schedule.start).atZone(schedule.zoneId)
            return snapshot(
                kind = WorkCountdownKind.UNTIL_WORK,
                now = localNow,
                target = target,
                currentDecision = todayDecision,
                targetDecision = todayDecision,
            )
        }

        if (todayDecision.isWorkday && time >= schedule.start && time < schedule.end) {
            val target = today.atTime(schedule.end).atZone(schedule.zoneId)
            return snapshot(
                kind = WorkCountdownKind.UNTIL_OFF_WORK,
                now = localNow,
                target = target,
                currentDecision = todayDecision,
                targetDecision = todayDecision,
            )
        }

        val next = workCalendar.nextWorkday(today.plusDays(1))
            ?: error("no workday found in the configured search horizon")
        val target = next.date.atTime(schedule.start).atZone(schedule.zoneId)
        return snapshot(
            kind = WorkCountdownKind.UNTIL_NEXT_WORK,
            now = localNow,
            target = target,
            currentDecision = todayDecision,
            targetDecision = next.decision,
        )
    }

    private fun snapshot(
        kind: WorkCountdownKind,
        now: ZonedDateTime,
        target: ZonedDateTime,
        currentDecision: WorkdayDecision,
        targetDecision: WorkdayDecision,
    ): WorkCountdown {
        val remaining = Duration.between(now, target)
        check(!remaining.isNegative) { "countdown target must not be in the past" }
        return WorkCountdown(
            kind = kind,
            now = now,
            target = target,
            remaining = remaining,
            currentDateDecision = currentDecision,
            targetDateDecision = targetDecision,
        )
    }
}
