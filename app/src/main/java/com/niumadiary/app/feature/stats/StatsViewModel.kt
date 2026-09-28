package com.niumadiary.app.feature.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.niumadiary.app.core.holiday.BundledHolidayCalendar
import com.niumadiary.app.core.holiday.WorkdayOverride
import com.niumadiary.app.core.time.WorkCalendar
import com.niumadiary.app.core.time.WorkSchedule
import com.niumadiary.app.domain.model.DailySummary
import com.niumadiary.app.domain.model.DiaryRecord
import com.niumadiary.app.domain.model.UserPreferences
import com.niumadiary.app.domain.repository.DailySummaryRepository
import com.niumadiary.app.domain.repository.DiaryRepository
import com.niumadiary.app.domain.repository.UserPreferencesRepository
import com.niumadiary.app.domain.repository.WorkdayOverrideRepository
import com.niumadiary.app.ui.model.TagUi
import com.niumadiary.app.ui.model.moodLevelFromScore
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
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

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class StatsViewModel @Inject constructor(
    private val diaryRepository: DiaryRepository,
    private val dailySummaryRepository: DailySummaryRepository,
    userPreferencesRepository: UserPreferencesRepository,
    workdayOverrideRepository: WorkdayOverrideRepository,
    private val clock: Clock,
) : ViewModel() {
    private val selectedRange = MutableStateFlow(StatsRange.MONTH)

    val uiState: StateFlow<StatsUiState> = combine(
        selectedRange,
        userPreferencesRepository.preferences,
        workdayOverrideRepository.observeAll(),
    ) { range, preferences, overrides ->
        StatsQuery(range, preferences, overrides)
    }.flatMapLatest(::observeStats)
        .catch {
            emit(
                StatsUiState(
                    availableRanges = AVAILABLE_RANGES,
                    selectedRangeId = selectedRange.value.id,
                    insufficientSampleMessage = "统计暂时开小差了，稍后再来看看。",
                ),
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = StatsUiState(
                availableRanges = AVAILABLE_RANGES,
                selectedRangeId = StatsRange.MONTH.id,
            ),
        )

    fun onRangeSelected(rangeId: String) {
        StatsRange.fromId(rangeId)?.let { selectedRange.value = it }
    }

    private fun observeStats(query: StatsQuery): Flow<StatsUiState> {
        val zoneId = query.preferences.zoneId
        val today = clock.instant().atZone(zoneId).toLocalDate()
        val endExclusive = today.plusDays(1).atStartOfDay(zoneId).toInstant()
        return combine(
            diaryRepository.observeRecordsBetween(
                ALL_TIME_START.atStartOfDay(zoneId).toInstant(),
                endExclusive,
            ),
            dailySummaryRepository.observeBetween(ALL_TIME_START, today),
        ) { records, summaries ->
            buildStatsState(query, records, summaries, today)
        }
    }

    private companion object {
        val ALL_TIME_START: LocalDate = LocalDate.of(1970, 1, 1)
        val AVAILABLE_RANGES: List<StatsRangeUi> = StatsRange.entries.map {
            StatsRangeUi(id = it.id, label = it.label)
        }
    }
}

private enum class StatsRange(val id: String, val label: String) {
    MONTH("month", "本月"),
    ALL("all", "全部"),
    ;

    companion object {
        fun fromId(id: String): StatsRange? = entries.firstOrNull { it.id == id }
    }
}

private data class StatsQuery(
    val range: StatsRange,
    val preferences: UserPreferences,
    val overrides: Map<LocalDate, WorkdayOverride>,
)

private fun buildStatsState(
    query: StatsQuery,
    records: List<DiaryRecord>,
    summaries: List<DailySummary>,
    today: LocalDate,
): StatsUiState {
    val firstRecordDate = records.minOfOrNull(DiaryRecord::localDate)
    val inferredAllStart = query.preferences.internshipStartDate ?: firstRecordDate ?: today
    val fromInclusive = when (query.range) {
        StatsRange.MONTH -> YearMonth.from(today).atDay(1)
        StatsRange.ALL -> inferredAllStart.coerceAtMost(today)
    }
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
    val statsEntries = records.map { record ->
        StatsEntry(
            id = record.id,
            occurredAt = record.occurredAt,
            moodScore = record.moodScore,
            tags = record.tags.mapTo(linkedSetOf()) { it.name },
        )
    }
    val statsSummaries = summaries.map { summary ->
        StatsDaySummary(date = summary.date, moodScore = summary.finalMoodScore)
    }
    val overview = StatsCalculator.calculateOverview(
        entries = statsEntries,
        summaries = statsSummaries,
        employmentStart = query.preferences.internshipStartDate,
        fromInclusive = fromInclusive,
        toInclusive = today,
        zoneId = query.preferences.zoneId,
        workCalendar = workCalendar,
    )

    val totalMoodSamples = overview.moodDistribution.values.sum()
    val moodDistribution = overview.moodDistribution.mapNotNull { (score, count) ->
        val mood = moodLevelFromScore(score) ?: return@mapNotNull null
        if (count == 0 || totalMoodSamples == 0) return@mapNotNull null
        MoodDistributionUi(
            mood = mood,
            count = count,
            share = count.toFloat() / totalMoodSamples,
        )
    }

    val tagsByName = records.asSequence()
        .flatMap { it.tags.asSequence() }
        .associateBy { it.name }
    val highestTagCount = overview.topTags.maxOfOrNull(TagCount::count) ?: 0
    val commonTags = overview.topTags.map { usage ->
        val tag = tagsByName[usage.tag]
        TagUsageUi(
            tag = TagUi(
                id = tag?.id ?: usage.tag,
                label = tag?.name ?: usage.tag,
            ),
            count = usage.count,
            share = if (highestTagCount == 0) 0f else usage.count.toFloat() / highestTagCount,
        )
    }

    return StatsUiState(
        periodLabel = periodLabel(query.range, fromInclusive, today),
        availableRanges = StatsRange.entries.map { StatsRangeUi(it.id, it.label) },
        selectedRangeId = query.range.id,
        plannedWorkdayCount = overview.plannedWorkdayCount,
        recordedWorkdayCount = overview.recordedWorkdayCount,
        workDayNumber = overview.internshipDayNumber ?: 0,
        moodDistribution = moodDistribution,
        commonTags = commonTags,
        insufficientSampleMessage = sampleMessage(
            totalMoodSamples = totalMoodSamples,
            representativeMoods = overview.representativeMoods,
            workCalendar = workCalendar,
        ),
    )
}

private fun sampleMessage(
    totalMoodSamples: Int,
    representativeMoods: List<DailyRepresentativeMood>,
    workCalendar: WorkCalendar,
): String? {
    if (totalMoodSamples == 0) return "还没有精神状态样本，先随手记下一次吧。"
    val workdayMoodCount = representativeMoods.count { workCalendar.isWorkday(it.date) }
    if (workdayMoodCount < 3) {
        return "再记录 ${3 - workdayMoodCount} 个有状态的上班日，观察会更靠谱。"
    }
    if (StatsCalculator.toughestWeekdays(representativeMoods, workCalendar) == null) {
        return "样本还没有覆盖 3 个不同工作周，暂时不下星期趋势结论。"
    }
    return null
}

private fun periodLabel(range: StatsRange, from: LocalDate, to: LocalDate): String = when (range) {
    StatsRange.MONTH -> "${to.year}年${to.monthValue}月"
    StatsRange.ALL -> "${from.toChineseDate()}—${to.toChineseDate()}"
}

private fun LocalDate.toChineseDate(): String = "${year}年${monthValue}月${dayOfMonth}日"
