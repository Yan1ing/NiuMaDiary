package com.niumadiary.app.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.niumadiary.app.core.holiday.WeekendPolicy
import com.niumadiary.app.domain.model.AppTheme
import com.niumadiary.app.domain.model.UserPreferences
import com.niumadiary.app.domain.repository.UserPreferencesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.userPreferencesDataStore by preferencesDataStore(name = "user_preferences")

@Singleton
class DataStoreUserPreferencesRepository @Inject constructor(
    @ApplicationContext context: Context,
) : UserPreferencesRepository {
    private val dataStore = context.userPreferencesDataStore

    override val preferences: Flow<UserPreferences> = dataStore.data
        .catch { error ->
            if (error is IOException) {
                emit(androidx.datastore.preferences.core.emptyPreferences())
            } else {
                throw error
            }
        }
        .map { it.toDomain() }

    override suspend fun setWorkSchedule(start: LocalTime, end: LocalTime) {
        require(start < end) { "start must be before end" }
        dataStore.edit { values ->
            values[Keys.WORK_START_MINUTES] = start.toMinutesOfDay()
            values[Keys.WORK_END_MINUTES] = end.toMinutesOfDay()
        }
    }

    override suspend fun setZoneId(zoneId: ZoneId) {
        dataStore.edit { it[Keys.ZONE_ID] = zoneId.id }
    }

    override suspend fun setWeekendPolicy(policy: WeekendPolicy) {
        dataStore.edit { it[Keys.WEEKEND_POLICY] = policy.name }
    }

    override suspend fun setInternshipStartDate(date: LocalDate?) {
        dataStore.edit { values ->
            if (date == null) {
                values.remove(Keys.INTERNSHIP_START_EPOCH_DAY)
            } else {
                values[Keys.INTERNSHIP_START_EPOCH_DAY] = date.toEpochDay()
            }
        }
    }

    override suspend fun setSummaryReminderEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.SUMMARY_REMINDER_ENABLED] = enabled }
    }

    override suspend fun setHapticsEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.HAPTICS_ENABLED] = enabled }
    }

    override suspend fun setAppTheme(theme: AppTheme) {
        dataStore.edit { it[Keys.APP_THEME] = theme.name }
    }

    private fun Preferences.toDomain(): UserPreferences {
        val defaults = UserPreferences()
        val storedStart = this[Keys.WORK_START_MINUTES]?.toLocalTimeOrNull() ?: defaults.workStart
        val storedEnd = this[Keys.WORK_END_MINUTES]?.toLocalTimeOrNull() ?: defaults.workEnd
        val validSchedule = storedStart < storedEnd

        return UserPreferences(
            workStart = if (validSchedule) storedStart else defaults.workStart,
            workEnd = if (validSchedule) storedEnd else defaults.workEnd,
            zoneId = this[Keys.ZONE_ID]
                ?.let { runCatching { ZoneId.of(it) }.getOrNull() }
                ?: defaults.zoneId,
            weekendPolicy = this[Keys.WEEKEND_POLICY]
                ?.let { runCatching { WeekendPolicy.valueOf(it) }.getOrNull() }
                ?: defaults.weekendPolicy,
            internshipStartDate = this[Keys.INTERNSHIP_START_EPOCH_DAY]
                ?.let { runCatching { LocalDate.ofEpochDay(it) }.getOrNull() },
            summaryReminderEnabled = this[Keys.SUMMARY_REMINDER_ENABLED]
                ?: defaults.summaryReminderEnabled,
            hapticsEnabled = this[Keys.HAPTICS_ENABLED] ?: defaults.hapticsEnabled,
            appTheme = this[Keys.APP_THEME]
                ?.let { runCatching { AppTheme.valueOf(it) }.getOrNull() }
                ?: defaults.appTheme,
        )
    }

    private fun LocalTime.toMinutesOfDay(): Int = hour * MINUTES_PER_HOUR + minute

    private fun Int.toLocalTimeOrNull(): LocalTime? =
        takeIf { it in 0 until MINUTES_PER_DAY }
            ?.let { LocalTime.of(it / MINUTES_PER_HOUR, it % MINUTES_PER_HOUR) }

    private object Keys {
        val WORK_START_MINUTES = intPreferencesKey("work_start_minutes")
        val WORK_END_MINUTES = intPreferencesKey("work_end_minutes")
        val ZONE_ID = stringPreferencesKey("zone_id")
        val WEEKEND_POLICY = stringPreferencesKey("weekend_policy")
        val INTERNSHIP_START_EPOCH_DAY = longPreferencesKey("internship_start_epoch_day")
        val SUMMARY_REMINDER_ENABLED = booleanPreferencesKey("summary_reminder_enabled")
        val HAPTICS_ENABLED = booleanPreferencesKey("haptics_enabled")
        val APP_THEME = stringPreferencesKey("app_theme")
    }

    private companion object {
        const val MINUTES_PER_HOUR: Int = 60
        const val MINUTES_PER_DAY: Int = 24 * MINUTES_PER_HOUR
    }
}
