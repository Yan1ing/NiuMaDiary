package com.niumadiary.app.feature.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.niumadiary.app.core.holiday.BundledHolidayCalendar
import com.niumadiary.app.core.time.WorkCalendar
import com.niumadiary.app.core.time.WorkSchedule
import com.niumadiary.app.domain.model.DailySummary
import com.niumadiary.app.domain.model.DiaryRecord
import com.niumadiary.app.domain.model.UserPreferences
import com.niumadiary.app.domain.repository.DailySummaryRepository
import com.niumadiary.app.domain.repository.DiaryRepository
import com.niumadiary.app.domain.repository.UserPreferencesRepository
import com.niumadiary.app.domain.repository.WorkdayOverrideRepository
import com.niumadiary.app.ui.model.DayKind
import com.niumadiary.app.ui.model.moodLevelFromScore
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val diaryRepository: DiaryRepository,
    private val dailySummaryRepository: DailySummaryRepository,
    userPreferencesRepository: UserPreferencesRepository,
    workdayOverrideRepository: WorkdayOverrideRepository,
    private val clock: Clock,
) : ViewModel() {
    private val initialMonth = YearMonth.from(clock.instant().atZone(WorkSchedule().zoneId))
    private val displayedMonth = MutableStateFlow(initialMonth)
    private var currentZoneId = WorkSchedule().zoneId

    val uiState: StateFlow<CalendarUiState> = combine(
        displayedMonth,
        userPreferencesRepository.preferences,
        workdayOverrideRepository.observeAll(),
    ) { month, preferences, overrides ->
        currentZoneId = preferences.zoneId
        CalendarQuery(month, preferences, overrides)
    }.flatMapLatest(::observeCalendar)
        .catch { error ->
            emit(
                CalendarUiState(
                    monthLabel = displayedMonth.value.toChineseMonthLabel(),
                    displayedYear = displayedMonth.value.year,
                    displayedMonth = displayedMonth.value.monthValue,
                    weekdayLabels = WEEKDAY_LABELS,
                    isLoading = false,
                ),
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = CalendarUiState(
                monthLabel = displayedMonth.value.toChineseMonthLabel(),
                displayedYear = displayedMonth.value.year,
                displayedMonth = displayedMonth.value.monthValue,
                weekdayLabels = WEEKDAY_LABELS,
                isLoading = true,
            ),
        )

    fun onPreviousMonth() {
        displayedMonth.update { it.minusMonths(1) }
    }

    fun onNextMonth() {
        displayedMonth.update { it.plusMonths(1) }
    }

    fun onCurrentMonth() {
        displayedMonth.value = YearMonth.from(clock.instant().atZone(currentZoneId))
    }

    fun onMonthSelected(year: Int, month: Int) {
        if (year !in 1900..2200 || month !in 1..12) return
        displayedMonth.value = YearMonth.of(year, month)
    }

    private fun observeCalendar(query: CalendarQuery): Flow<CalendarUiState> {
        val firstVisibleDate = query.month.firstVisibleDate()
        val lastVisibleDate = firstVisibleDate.plusDays(CALENDAR_CELL_COUNT - 1L)
        val zoneId = query.preferences.zoneId
        val startInstant = firstVisibleDate.atStartOfDay(zoneId).toInstant()
        val endExclusive = lastVisibleDate.plusDays(1).atStartOfDay(zoneId).toInstant()

        return combine(
            diaryRepository.observeRecordsBetween(startInstant, endExclusive),
            dailySummaryRepository.observeBetween(firstVisibleDate, lastVisibleDate),
        ) { records, summaries ->
            buildCalendarState(
                query = query,
                records = records,
                summaries = summaries,
                today = clock.instant().atZone(zoneId).toLocalDate(),
            )
        }
    }

    private companion object {
        const val CALENDAR_CELL_COUNT: Int = 42
        val WEEKDAY_LABELS: List<String> = listOf("一", "二", "三", "四", "五", "六", "日")
    }
}

private data class CalendarQuery(
    val month: YearMonth,
    val preferences: UserPreferences,
    val overrides: Map<LocalDate, com.niumadiary.app.core.holiday.WorkdayOverride>,
)

private fun buildCalendarState(
    query: CalendarQuery,
    records: List<DiaryRecord>,
    summaries: List<DailySummary>,
    today: LocalDate,
): CalendarUiState {
    val firstVisibleDate = query.month.firstVisibleDate()
    val recordsByDate = records.groupBy(DiaryRecord::localDate)
    val summariesByDate = summaries.associateBy(DailySummary::date)
    val workCalendar = WorkCalendar(
        schedule = WorkSchedule(
            start = query.preferences.workStart,
            end = query.preferences.workEnd,
            zoneId = query.preferences.zoneId,
            weekendPolicy = query.preferences.weekendPolicy,
        ),
        holidayCalendar = BundledHolidayCalendar,
        overrides = query.overrides,
    )

    val days = List(42) { index ->
        val date = firstVisibleDate.plusDays(index.toLong())
        val dayRecords = recordsByDate[date].orEmpty()
        val summaryMood = summariesByDate[date]?.finalMoodScore
        val latestEntryMood = dayRecords.asSequence()
            .filter { it.moodScore != null }
            .maxWithOrNull(compareBy<DiaryRecord> { it.occurredAt }.thenBy { it.id })
            ?.moodScore
        val decision = workCalendar.decisionFor(date)

        CalendarDayUi(
            dateKey = date.toString(),
            dayNumber = date.dayOfMonth.toString(),
            isInCurrentMonth = YearMonth.from(date) == query.month,
            isToday = date == today,
            dayKind = if (decision.isWorkday) DayKind.WORKDAY else DayKind.REST_DAY,
            mood = moodLevelFromScore(summaryMood ?: latestEntryMood),
            recordCount = dayRecords.size,
            holidayLabel = decision.holidayName.orEmpty(),
        )
    }

    val monthRecords = records.filter { YearMonth.from(it.localDate) == query.month }
    return CalendarUiState(
        monthLabel = query.month.toChineseMonthLabel(),
        displayedYear = query.month.year,
        displayedMonth = query.month.monthValue,
        isCurrentMonth = query.month == YearMonth.from(today),
        weekdayLabels = listOf("一", "二", "三", "四", "五", "六", "日"),
        days = days,
        recordCountThisMonth = monthRecords.size,
        recordedDayCountThisMonth = monthRecords.map(DiaryRecord::localDate).distinct().size,
        plannedWorkdayCountThisMonth = workCalendar.countWorkdays(
            query.month.atDay(1),
            query.month.atEndOfMonth(),
        ),
        isLoading = false,
    )
}

private fun YearMonth.firstVisibleDate(): LocalDate =
    atDay(1).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

private fun YearMonth.toChineseMonthLabel(): String = "${year}年${monthValue}月"
