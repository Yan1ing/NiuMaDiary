package com.niumadiary.app.domain.model

import com.niumadiary.app.core.holiday.WeekendPolicy
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

enum class AppTheme {
    SYSTEM,
    LIGHT,
    DARK,
}

data class UserPreferences(
    val workStart: LocalTime = LocalTime.of(9, 0),
    val workEnd: LocalTime = LocalTime.of(18, 0),
    val zoneId: ZoneId = ZoneId.of(DEFAULT_ZONE_ID),
    val weekendPolicy: WeekendPolicy = WeekendPolicy.FIXED_WEEKENDS_OFF,
    val internshipStartDate: LocalDate? = null,
    val summaryReminderEnabled: Boolean = false,
    val hapticsEnabled: Boolean = true,
    val appTheme: AppTheme = AppTheme.SYSTEM,
) {
    init {
        require(workStart < workEnd) { "workStart must be before workEnd" }
    }

    companion object {
        const val DEFAULT_ZONE_ID: String = "Asia/Shanghai"
    }
}
