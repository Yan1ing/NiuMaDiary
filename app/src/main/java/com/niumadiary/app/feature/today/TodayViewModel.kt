package com.niumadiary.app.feature.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.niumadiary.app.core.holiday.BundledHolidayCalendar
import com.niumadiary.app.core.time.WorkCalendar
import com.niumadiary.app.core.time.WorkCountdown
import com.niumadiary.app.core.time.WorkCountdownCalculator
import com.niumadiary.app.core.time.WorkCountdownKind
import com.niumadiary.app.core.time.WorkSchedule
import com.niumadiary.app.core.time.WeeklyWorkProgress
import com.niumadiary.app.core.time.WeeklyWorkProgressCalculator
import com.niumadiary.app.domain.model.DailySummary
import com.niumadiary.app.domain.model.DiaryRecord
import com.niumadiary.app.domain.model.DiaryTag
import com.niumadiary.app.domain.model.RecordType
import com.niumadiary.app.domain.model.UserPreferences
import com.niumadiary.app.domain.repository.DailySummaryRepository
import com.niumadiary.app.domain.repository.DiaryRepository
import com.niumadiary.app.domain.repository.TagRepository
import com.niumadiary.app.domain.repository.UserPreferencesRepository
import com.niumadiary.app.domain.repository.WorkdayOverrideRepository
import com.niumadiary.app.domain.usecase.SaveDailySummaryUseCase
import com.niumadiary.app.domain.usecase.SaveDiaryRecordCommand
import com.niumadiary.app.domain.usecase.SaveDiaryRecordUseCase
import com.niumadiary.app.ui.model.MoodLevel
import com.niumadiary.app.ui.model.toUi
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.Duration
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class TodayViewModel @Inject constructor(
    private val diaryRepository: DiaryRepository,
    private val dailySummaryRepository: DailySummaryRepository,
    private val tagRepository: TagRepository,
    private val preferencesRepository: UserPreferencesRepository,
    private val workdayOverrideRepository: WorkdayOverrideRepository,
    private val saveDiaryRecord: SaveDiaryRecordUseCase,
    private val saveDailySummary: SaveDailySummaryUseCase,
    private val clock: Clock,
) : ViewModel() {
    private val draftText = MutableStateFlow("")
    private val selectedMood = MutableStateFlow<MoodLevel?>(null)
    private val selectedTagIds = MutableStateFlow<Set<String>>(emptySet())
    private val isSaving = MutableStateFlow(false)
    private val feedbackMessage = MutableStateFlow<String?>(null)

    private val preferences = preferencesRepository.preferences.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = UserPreferences(),
    )

    private val ticks = flow {
        while (true) {
            emit(clock.instant())
            delay(1_000)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = clock.instant(),
    )

    private val today = combine(preferences, ticks) { prefs, now ->
        now.atZone(prefs.zoneId).toLocalDate()
    }.distinctUntilChanged().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = LocalDate.now(UserPreferences().zoneId),
    )

    private val records = today.flatMapLatest(diaryRepository::observeRecordsForDate).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    private val summary = today.flatMapLatest(dailySummaryRepository::observe).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = null,
    )

    private val tags = tagRepository.observeAll().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    private val diaryData = combine(today, records, summary, tags) { date, entries, daySummary, allTags ->
        DiaryData(date, entries, daySummary, allTags)
    }

    private val workData = combine(
        preferences,
        workdayOverrideRepository.observeAll(),
        ticks,
    ) { prefs, overrides, now ->
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
        WorkData(
            preferences = prefs,
            countdown = WorkCountdownCalculator(calendar, clock).calculate(now),
            weeklyProgress = WeeklyWorkProgressCalculator.calculate(
                calendar = calendar,
                now = now.atZone(prefs.zoneId),
            ),
        )
    }

    private val baseState = combine(diaryData, workData) { diary, work -> BaseState(diary, work) }
    private val composerState = combine(
        draftText,
        selectedMood,
        selectedTagIds,
        isSaving,
        feedbackMessage,
    ) { draft, mood, tagIds, saving, feedback ->
        ComposerState(draft, mood, tagIds, saving, feedback)
    }

    val uiState: StateFlow<TodayUiState> = combine(baseState, composerState) { base, composer ->
        base.toUi(composer)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TodayUiState(),
    )

    fun onDraftChange(value: String) {
        draftText.value = value.take(MAX_DRAFT_LENGTH)
    }

    fun onMoodSelected(mood: MoodLevel) {
        selectedMood.value = mood
    }

    fun onTagToggle(tagId: String) {
        selectedTagIds.update { current ->
            if (tagId in current) current - tagId else current + tagId
        }
    }

    fun saveQuickRecord() {
        val text = draftText.value.trim()
        val mood = selectedMood.value
        if (text.isBlank() && mood == null || isSaving.value) return

        viewModelScope.launch {
            isSaving.value = true
            runCatching {
                saveDiaryRecord(
                    SaveDiaryRecordCommand(
                        type = if (text.isBlank()) RecordType.MOOD_CHECK_IN else RecordType.RANT,
                        content = text,
                        moodScore = mood?.value,
                        tags = tags.value.filterTo(mutableSetOf()) { it.id in selectedTagIds.value },
                    ),
                )
            }.onSuccess {
                draftText.value = ""
                selectedMood.value = null
                selectedTagIds.value = emptySet()
                showFeedback("记下了，今天又多一条生存证据。")
            }.onFailure {
                showFeedback("没存上，再点一次试试。")
            }
            isSaving.value = false
        }
    }

    fun saveSummary(content: String, mood: MoodLevel?) {
        viewModelScope.launch {
            val date = today.value
            if (content.isBlank() && mood == null) {
                summary.value?.let { dailySummaryRepository.delete(date) }
                return@launch
            }
            saveDailySummary(
                date = date,
                content = content,
                finalMoodScore = mood?.value,
                existing = summary.value,
            )
            showFeedback("今日收工报告已归档。")
        }
    }

    fun updateEntry(id: String, content: String, mood: MoodLevel?, tagIds: Set<String>) {
        viewModelScope.launch {
            val existing = diaryRepository.observeRecord(id).first() ?: return@launch
            if (content.isBlank() && mood == null) return@launch
            diaryRepository.upsert(
                existing.copy(
                    content = content.trim(),
                    moodScore = mood?.value,
                    tags = tags.value.filterTo(mutableSetOf()) { it.id in tagIds },
                    updatedAt = clock.instant(),
                ),
            )
        }
    }

    fun deleteEntry(id: String) {
        viewModelScope.launch { diaryRepository.delete(id) }
    }

    private fun showFeedback(message: String) {
        feedbackMessage.value = message
        viewModelScope.launch {
            delay(2_200)
            if (feedbackMessage.value == message) feedbackMessage.value = null
        }
    }

    private fun BaseState.toUi(composer: ComposerState): TodayUiState {
        val dateTime = work.countdown.now
        val isAfterWork = diary.date == dateTime.toLocalDate() &&
            dateTime.toLocalTime() >= work.preferences.workEnd
        return TodayUiState(
            dateLabel = diary.date.format(DATE_FORMATTER),
            greeting = greetingFor(dateTime.hour),
            countdownLabel = work.countdown.label(),
            countdownValue = work.countdown.remaining.formatCountdown(),
            countdownSupportingText = work.countdown.supportingText(),
            isWorkday = work.countdown.currentDateDecision.isWorkday,
            weeklyProgress = work.weeklyProgress.toUi(work.preferences),
            selectedMood = composer.mood,
            draftText = composer.draft,
            availableTags = diary.tags.map(DiaryTag::toUi),
            selectedTagIds = composer.tagIds,
            entries = diary.records.sortedByDescending(DiaryRecord::occurredAt)
                .map { it.toUi(work.preferences.zoneId) },
            summary = diary.summary.toUi(),
            showSummaryEmphasis = isAfterWork && work.countdown.currentDateDecision.isWorkday,
            isSaving = composer.saving,
            feedbackMessage = composer.feedback,
        )
    }

    private data class DiaryData(
        val date: LocalDate,
        val records: List<DiaryRecord>,
        val summary: DailySummary?,
        val tags: List<DiaryTag>,
    )

    private data class WorkData(
        val preferences: UserPreferences,
        val countdown: WorkCountdown,
        val weeklyProgress: WeeklyWorkProgress,
    )
    private data class BaseState(val diary: DiaryData, val work: WorkData)
    private data class ComposerState(
        val draft: String,
        val mood: MoodLevel?,
        val tagIds: Set<String>,
        val saving: Boolean,
        val feedback: String?,
    )

    private companion object {
        const val MAX_DRAFT_LENGTH = 10_000
        val DATE_FORMATTER: DateTimeFormatter =
            DateTimeFormatter.ofPattern("M月d日 EEEE", Locale.SIMPLIFIED_CHINESE)
    }
}

private fun WeeklyWorkProgress.toUi(preferences: UserPreferences): WeeklyWorkProgressUi =
    WeeklyWorkProgressUi(
        completedLabel = completed.formatWorkDuration(),
        remainingLabel = remaining.formatWorkDuration(),
        totalLabel = total.formatWorkDuration(),
        percentageLabel = "%.1f%%".format(Locale.SIMPLIFIED_CHINESE, fraction * 100.0),
        scheduleLabel = "${plannedWorkdays} 个上班日 · 仅统计 ${preferences.workStart.format(TIME_FORMATTER)}–${preferences.workEnd.format(TIME_FORMATTER)}",
        fraction = fraction.toFloat(),
    )

private fun greetingFor(hour: Int): String = when (hour) {
    in 0..8 -> "早啊，今天先别被工作拿捏。"
    in 9..11 -> "开工了，想吐槽就趁热记。"
    in 12..13 -> "午休充会儿电。"
    in 14..17 -> "下午场，坚持就是胜利。"
    else -> "下班了，今天辛苦。"
}

private fun WorkCountdown.label(): String = when (kind) {
    WorkCountdownKind.UNTIL_WORK -> "距离上班"
    WorkCountdownKind.UNTIL_OFF_WORK -> "距离下班"
    WorkCountdownKind.UNTIL_NEXT_WORK -> "距离下次上班"
}

private fun WorkCountdown.supportingText(): String {
    val targetDate = target.toLocalDate()
    val targetLabel = targetDate.format(DateTimeFormatter.ofPattern("M月d日 EEEE", Locale.SIMPLIFIED_CHINESE))
    val holiday = targetDateDecision.holidayName?.let { " · $it" }.orEmpty()
    val provisional = if (isProvisional) " · 次年安排待官方发布" else ""
    return "$targetLabel ${target.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm"))}$holiday$provisional"
}

private fun Duration.formatCountdown(): String {
    val safeSeconds = seconds.coerceAtLeast(0)
    val days = safeSeconds / 86_400
    val hours = (safeSeconds % 86_400) / 3_600
    val minutes = (safeSeconds % 3_600) / 60
    val remainingSeconds = safeSeconds % 60
    return if (days > 0) {
        "%d天 %02d:%02d:%02d".format(days, hours, minutes, remainingSeconds)
    } else {
        "%02d:%02d:%02d".format(hours, minutes, remainingSeconds)
    }
}

private fun Duration.formatWorkDuration(): String {
    val totalMinutes = seconds.coerceAtLeast(0) / 60
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (minutes == 0L) "${hours}小时" else "${hours}小时${minutes}分"
}

private val TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
