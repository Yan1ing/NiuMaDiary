package com.niumadiary.app.feature.stats

import androidx.lifecycle.viewModelScope
import com.niumadiary.app.core.holiday.WeekendPolicy
import com.niumadiary.app.core.holiday.WorkdayOverride
import com.niumadiary.app.domain.model.AppTheme
import com.niumadiary.app.domain.model.DailyRecordStats
import com.niumadiary.app.domain.model.DailySummary
import com.niumadiary.app.domain.model.DiaryRecord
import com.niumadiary.app.domain.model.DiaryTag
import com.niumadiary.app.domain.model.RecordType
import com.niumadiary.app.domain.model.UserPreferences
import com.niumadiary.app.domain.repository.DailySummaryRepository
import com.niumadiary.app.domain.repository.DiaryRepository
import com.niumadiary.app.domain.repository.UserPreferencesRepository
import com.niumadiary.app.domain.repository.WorkdayOverrideRepository
import com.niumadiary.app.ui.model.MoodLevel
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description

@OptIn(ExperimentalCoroutinesApi::class)
class StatsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val zone = ZoneId.of("Asia/Shanghai")
    private val meeting = DiaryTag("meeting", "开会", 0xFFF48778)
    private val commute = DiaryTag("commute", "通勤", 0xFF9293A6)
    private val fishing = DiaryTag("fishing", "摸鱼", 0xFFD8A5A7)

    @Test
    fun `month range aggregates only current month and shows a conservative sample notice`() =
        runTest(mainDispatcherRule.dispatcher) {
        val records = sampleRecords()
        val viewModel = viewModel(records = records)

        val state = viewModel.uiState.first { it.periodLabel == "2026年8月" }

        assertEquals("month", state.selectedRangeId)
        assertEquals(5, state.plannedWorkdayCount)
        assertEquals(2, state.recordedWorkdayCount)
        assertEquals(38, state.workDayNumber)
        assertEquals(4, state.moodDistribution.sumOf { it.count })
        assertEquals(MoodLevel.NUMB, state.moodDistribution.maxBy { it.count }.mood)
        assertEquals("开会", state.commonTags.first().tag.label)
        assertEquals(2, state.commonTags.first().count)
        assertEquals("再记录 1 个有状态的上班日，观察会更靠谱。", state.insufficientSampleMessage)
        viewModel.viewModelScope.cancel()
    }

    @Test
    fun `all range includes earlier records and preserves range choices`() =
        runTest(mainDispatcherRule.dispatcher) {
        val viewModel = viewModel(records = sampleRecords())
        viewModel.uiState.first { it.periodLabel == "2026年8月" }

        viewModel.onRangeSelected("all")
        val state = viewModel.uiState.first { it.selectedRangeId == "all" && it.periodLabel.isNotBlank() }

        assertEquals(listOf("month", "all"), state.availableRanges.map { it.id })
        assertEquals("2026年7月1日—2026年8月7日", state.periodLabel)
        assertEquals(28, state.plannedWorkdayCount)
        assertEquals(3, state.recordedWorkdayCount)
        assertEquals(5, state.moodDistribution.sumOf { it.count })
        viewModel.viewModelScope.cancel()
    }

    @Test
    fun `invalid range id leaves current range unchanged`() =
        runTest(mainDispatcherRule.dispatcher) {
        val viewModel = viewModel(records = sampleRecords())
        viewModel.uiState.first { it.periodLabel == "2026年8月" }

        viewModel.onRangeSelected("not-a-range")

        assertEquals("month", viewModel.uiState.value.selectedRangeId)
        viewModel.viewModelScope.cancel()
    }

    @Test
    fun `three distinct work weeks remove the insufficient sample notice`() =
        runTest(mainDispatcherRule.dispatcher) {
        val firstMonday = LocalDate.of(2026, 7, 20)
        val records = listOf(0L, 7L, 14L).mapIndexed { index, offset ->
            record(
                id = "week-$index",
                date = firstMonday.plusDays(offset),
                hour = 10,
                mood = index + 1,
                tags = setOf(meeting),
            )
        }
        val viewModel = viewModel(
            records = records,
            preferences = UserPreferences(internshipStartDate = firstMonday),
        )

        viewModel.onRangeSelected("all")
        val state = viewModel.uiState.first { it.selectedRangeId == "all" && it.periodLabel.isNotBlank() }

        assertNull(state.insufficientSampleMessage)
        viewModel.viewModelScope.cancel()
    }

    @Test
    fun `empty data explains that mood samples are missing`() =
        runTest(mainDispatcherRule.dispatcher) {
        val viewModel = viewModel(records = emptyList(), summaries = emptyList())

        val state = viewModel.uiState.first { it.periodLabel == "2026年8月" }

        assertTrue(state.moodDistribution.isEmpty())
        assertTrue(state.commonTags.isEmpty())
        assertEquals("还没有精神状态样本，先随手记下一次吧。", state.insufficientSampleMessage)
        viewModel.viewModelScope.cancel()
    }

    private fun viewModel(
        records: List<DiaryRecord>,
        preferences: UserPreferences = UserPreferences(
            internshipStartDate = LocalDate.of(2026, 7, 1),
        ),
        summaries: List<DailySummary> = listOf(
            summary(LocalDate.of(2026, 8, 3), mood = 2),
        ),
    ): StatsViewModel = StatsViewModel(
        diaryRepository = StatsFakeDiaryRepository(records),
        dailySummaryRepository = StatsFakeSummaryRepository(summaries),
        userPreferencesRepository = StatsFakePreferencesRepository(preferences),
        workdayOverrideRepository = StatsFakeOverrideRepository(),
        clock = fixedClock(LocalDate.of(2026, 8, 7), 12),
    )

    private fun sampleRecords(): List<DiaryRecord> = listOf(
        record("july", LocalDate.of(2026, 7, 31), 9, 5, setOf(commute)),
        record("aug-1", LocalDate.of(2026, 8, 3), 9, 1, setOf(meeting)),
        record("aug-2", LocalDate.of(2026, 8, 3), 16, 2, setOf(meeting)),
        record("aug-3", LocalDate.of(2026, 8, 4), 10, 4, setOf(fishing)),
    )

    private fun record(
        id: String,
        date: LocalDate,
        hour: Int,
        mood: Int,
        tags: Set<DiaryTag>,
    ): DiaryRecord {
        val instant = date.atTime(hour, 0).atZone(zone).toInstant()
        return DiaryRecord(
            id = id,
            occurredAt = instant,
            localDate = date,
            type = RecordType.MOOD_CHECK_IN,
            moodScore = mood,
            tags = tags,
            createdAt = instant,
            updatedAt = instant,
        )
    }

    private fun summary(date: LocalDate, mood: Int): DailySummary {
        val instant = date.atTime(18, 0).atZone(zone).toInstant()
        return DailySummary(
            date = date,
            content = "收工",
            finalMoodScore = mood,
            createdAt = instant,
            updatedAt = instant,
        )
    }

    private fun fixedClock(date: LocalDate, hour: Int): Clock = Clock.fixed(
        date.atTime(hour, 0).atZone(zone).toInstant(),
        ZoneOffset.UTC,
    )
}

private class StatsFakeDiaryRepository(
    records: List<DiaryRecord>,
) : DiaryRepository {
    private val data = MutableStateFlow(records)

    override fun observeRecord(id: String): Flow<DiaryRecord?> = data.map { records ->
        records.firstOrNull { it.id == id }
    }

    override fun observeRecordsForDate(date: LocalDate): Flow<List<DiaryRecord>> = data.map {
        records -> records.filter { it.localDate == date }
    }

    override fun observeRecordsBetween(
        startInclusive: Instant,
        endExclusive: Instant,
    ): Flow<List<DiaryRecord>> = data.map { records ->
        records.filter { it.occurredAt >= startInclusive && it.occurredAt < endExclusive }
    }

    override fun observeDailyStats(
        startDateInclusive: LocalDate,
        endDateInclusive: LocalDate,
    ): Flow<List<DailyRecordStats>> = flowOf(emptyList())

    override suspend fun upsert(record: DiaryRecord) {
        data.value = data.value.filterNot { it.id == record.id } + record
    }

    override suspend fun delete(id: String) {
        data.value = data.value.filterNot { it.id == id }
    }
}

private class StatsFakeSummaryRepository(
    summaries: List<DailySummary>,
) : DailySummaryRepository {
    private val data = MutableStateFlow(summaries)

    override fun observe(date: LocalDate): Flow<DailySummary?> = data.map { summaries ->
        summaries.firstOrNull { it.date == date }
    }

    override fun observeBetween(
        startDateInclusive: LocalDate,
        endDateInclusive: LocalDate,
    ): Flow<List<DailySummary>> = data.map { summaries ->
        summaries.filter { it.date in startDateInclusive..endDateInclusive }
    }

    override suspend fun upsert(summary: DailySummary) {
        data.value = data.value.filterNot { it.date == summary.date } + summary
    }

    override suspend fun delete(date: LocalDate) {
        data.value = data.value.filterNot { it.date == date }
    }
}

private class StatsFakePreferencesRepository(
    initial: UserPreferences,
) : UserPreferencesRepository {
    private val state = MutableStateFlow(initial)
    override val preferences: Flow<UserPreferences> = state

    override suspend fun setWorkSchedule(start: LocalTime, end: LocalTime) {
        state.value = state.value.copy(workStart = start, workEnd = end)
    }

    override suspend fun setZoneId(zoneId: ZoneId) {
        state.value = state.value.copy(zoneId = zoneId)
    }

    override suspend fun setWeekendPolicy(policy: WeekendPolicy) {
        state.value = state.value.copy(weekendPolicy = policy)
    }

    override suspend fun setInternshipStartDate(date: LocalDate?) {
        state.value = state.value.copy(internshipStartDate = date)
    }

    override suspend fun setSummaryReminderEnabled(enabled: Boolean) {
        state.value = state.value.copy(summaryReminderEnabled = enabled)
    }

    override suspend fun setHapticsEnabled(enabled: Boolean) {
        state.value = state.value.copy(hapticsEnabled = enabled)
    }

    override suspend fun setAppTheme(theme: AppTheme) {
        state.value = state.value.copy(appTheme = theme)
    }
}

private class StatsFakeOverrideRepository : WorkdayOverrideRepository {
    private val state = MutableStateFlow<Map<LocalDate, WorkdayOverride>>(emptyMap())

    override fun observeAll(): Flow<Map<LocalDate, WorkdayOverride>> = state

    override suspend fun set(date: LocalDate, override: WorkdayOverride) {
        state.value = state.value + (date to override)
    }

    override suspend fun clear(date: LocalDate) {
        state.value = state.value - date
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    val dispatcher: TestDispatcher = UnconfinedTestDispatcher(),
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(dispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}
