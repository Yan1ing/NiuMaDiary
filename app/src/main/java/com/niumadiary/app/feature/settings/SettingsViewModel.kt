package com.niumadiary.app.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.niumadiary.app.core.holiday.WeekendPolicy
import com.niumadiary.app.domain.model.UserPreferences
import com.niumadiary.app.domain.repository.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesRepository: UserPreferencesRepository,
) : ViewModel() {
    val preferences: StateFlow<UserPreferences> = preferencesRepository.preferences.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        UserPreferences(),
    )

    val uiState: StateFlow<SettingsUiState> = preferences.map { prefs ->
        SettingsUiState(
            employmentDateLabel = prefs.internshipStartDate?.format(DATE_FORMATTER).orEmpty(),
            workHoursLabel = "${prefs.workStart.format(TIME_FORMATTER)}–${prefs.workEnd.format(TIME_FORMATTER)}",
            timeZoneLabel = prefs.zoneId.id,
            followOfficialMakeupDays = prefs.weekendPolicy == WeekendPolicy.FOLLOW_OFFICIAL_ADJUSTMENTS,
            reminderEnabled = prefs.summaryReminderEnabled,
            reminderTimeLabel = "18:10",
            privacyLockEnabled = false,
            privacyLockAvailable = false,
            holidayDataStatus = "2026 年安排已离线更新",
            appVersionLabel = "0.1.2 · 本地优先版",
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        SettingsUiState(),
    )

    fun setEmploymentStartDate(date: LocalDate?) {
        viewModelScope.launch { preferencesRepository.setInternshipStartDate(date) }
    }

    fun setWorkHours(start: LocalTime, end: LocalTime) {
        if (start >= end) return
        viewModelScope.launch { preferencesRepository.setWorkSchedule(start, end) }
    }

    fun setZoneId(zoneId: ZoneId) {
        viewModelScope.launch { preferencesRepository.setZoneId(zoneId) }
    }

    fun setFollowOfficialMakeupDays(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setWeekendPolicy(
                if (enabled) {
                    WeekendPolicy.FOLLOW_OFFICIAL_ADJUSTMENTS
                } else {
                    WeekendPolicy.FIXED_WEEKENDS_OFF
                },
            )
        }
    }

    fun setReminderEnabled(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setSummaryReminderEnabled(enabled) }
    }

    private companion object {
        val DATE_FORMATTER: DateTimeFormatter =
            DateTimeFormatter.ofPattern("yyyy年M月d日", Locale.SIMPLIFIED_CHINESE)
        val TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    }
}
