package com.niumadiary.app.feature.daydetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.niumadiary.app.core.holiday.BundledHolidayCalendar
import com.niumadiary.app.core.time.WorkCalendar
import com.niumadiary.app.core.time.WorkSchedule
import com.niumadiary.app.domain.model.DailySummary
import com.niumadiary.app.domain.model.DiaryRecord
import com.niumadiary.app.domain.model.RecordType
import com.niumadiary.app.domain.model.UserPreferences
import com.niumadiary.app.domain.repository.DailySummaryRepository
import com.niumadiary.app.domain.repository.DiaryRepository
import com.niumadiary.app.domain.repository.TagRepository
import com.niumadiary.app.domain.repository.UserPreferencesRepository
import com.niumadiary.app.domain.repository.WorkdayOverrideRepository
import com.niumadiary.app.domain.time.IdGenerator
import com.niumadiary.app.domain.usecase.SaveDailySummaryUseCase
import com.niumadiary.app.ui.model.DayKind
import com.niumadiary.app.ui.model.MoodLevel
import com.niumadiary.app.ui.model.TagUi
import com.niumadiary.app.ui.model.toUi
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class DayDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val diaryRepository: DiaryRepository,
    private val dailySummaryRepository: DailySummaryRepository,
    tagRepository: TagRepository,
    preferencesRepository: UserPreferencesRepository,
    workdayOverrideRepository: WorkdayOverrideRepository,
    private val saveDailySummary: SaveDailySummaryUseCase,
    private val idGenerator: IdGenerator,
    private val clock: Clock,
) : ViewModel() {
    val selectedDate: LocalDate = savedStateHandle.get<String>(DATE_ARGUMENT)
        ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        ?: LocalDate.now(UserPreferences().zoneId)

    private val preferences = preferencesRepository.preferences.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        UserPreferences(),
    )
    private val records = diaryRepository.observeRecordsForDate(selectedDate).stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList(),
    )
    private val summary = dailySummaryRepository.observe(selectedDate).stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        null,
    )
    private val tags = tagRepository.observeAll().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList(),
    )

    val availableTags: StateFlow<List<TagUi>> = tags.combine(preferences) { values, _ ->
        values.map { it.toUi() }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val uiState: StateFlow<DayDetailUiState> = combine(
        records,
        summary,
        preferences,
        workdayOverrideRepository.observeAll(),
    ) { dayRecords, daySummary, prefs, overrides ->
        val calendar = WorkCalendar(
            schedule = WorkSchedule(
                start = prefs.workStart,
                end = prefs.workEnd,
                zoneId = prefs.zoneId,
                weekendPolicy = prefs.weekendPolicy,
            ),
            holidayCalendar = BundledHolidayCalendar,
            overrides = overrides,
        )
        val decision = calendar.decisionFor(selectedDate)
        DayDetailUiState(
            dateKey = selectedDate.toString(),
            dateLabel = selectedDate.format(DATE_FORMATTER),
            weekdayLabel = selectedDate.format(WEEKDAY_FORMATTER),
            holidayLabel = decision.holidayName.orEmpty(),
            dayKind = if (decision.isWorkday) DayKind.WORKDAY else DayKind.REST_DAY,
            entries = dayRecords.sortedByDescending(DiaryRecord::occurredAt)
                .map { it.toUi(prefs.zoneId) },
            summary = daySummary.toUi(),
            canAddEntry = !selectedDate.isAfter(LocalDate.now(prefs.zoneId)),
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        DayDetailUiState(dateKey = selectedDate.toString()),
    )

    fun saveEntry(
        entryId: String?,
        content: String,
        mood: MoodLevel?,
        tagIds: Set<String>,
    ) {
        if (content.isBlank() && mood == null) return
        viewModelScope.launch {
            val now = clock.instant()
            val prefs = preferences.value
            val existing = entryId?.let { diaryRepository.observeRecord(it).first() }
            val occurredAt = existing?.occurredAt ?: run {
                val today = now.atZone(prefs.zoneId).toLocalDate()
                if (selectedDate == today) {
                    now
                } else {
                    selectedDate.atTime(now.atZone(prefs.zoneId).toLocalTime())
                        .atZone(prefs.zoneId)
                        .toInstant()
                }
            }
            diaryRepository.upsert(
                DiaryRecord(
                    id = existing?.id ?: idGenerator.newId(),
                    occurredAt = occurredAt,
                    localDate = selectedDate,
                    type = existing?.type
                        ?: if (content.isBlank()) RecordType.MOOD_CHECK_IN else RecordType.RANT,
                    content = content.trim(),
                    moodScore = mood?.value,
                    energyScore = existing?.energyScore,
                    stressScore = existing?.stressScore,
                    boredomScore = existing?.boredomScore,
                    tags = tags.value.filterTo(mutableSetOf()) { it.id in tagIds },
                    createdAt = existing?.createdAt ?: now,
                    updatedAt = now,
                ),
            )
        }
    }

    fun deleteEntry(entryId: String) {
        viewModelScope.launch { diaryRepository.delete(entryId) }
    }

    fun saveSummary(content: String, mood: MoodLevel?) {
        viewModelScope.launch {
            if (content.isBlank() && mood == null) {
                summary.value?.let { dailySummaryRepository.delete(selectedDate) }
                return@launch
            }
            saveDailySummary(
                date = selectedDate,
                content = content,
                finalMoodScore = mood?.value,
                existing = summary.value,
            )
        }
    }

    companion object {
        const val DATE_ARGUMENT = "date"
        private val DATE_FORMATTER =
            DateTimeFormatter.ofPattern("M月d日", Locale.SIMPLIFIED_CHINESE)
        private val WEEKDAY_FORMATTER =
            DateTimeFormatter.ofPattern("EEEE", Locale.SIMPLIFIED_CHINESE)
    }
}
