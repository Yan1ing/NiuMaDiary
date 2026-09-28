package com.niumadiary.app.domain.repository

import com.niumadiary.app.core.holiday.WeekendPolicy
import com.niumadiary.app.domain.model.AppTheme
import com.niumadiary.app.domain.model.UserPreferences
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow

interface UserPreferencesRepository {
    val preferences: Flow<UserPreferences>

    suspend fun setWorkSchedule(start: LocalTime, end: LocalTime)

    suspend fun setZoneId(zoneId: ZoneId)

    suspend fun setWeekendPolicy(policy: WeekendPolicy)

    suspend fun setInternshipStartDate(date: LocalDate?)

    suspend fun setSummaryReminderEnabled(enabled: Boolean)

    suspend fun setHapticsEnabled(enabled: Boolean)

    suspend fun setAppTheme(theme: AppTheme)
}
