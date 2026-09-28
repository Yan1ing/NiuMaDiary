package com.niumadiary.app.feature.calendar

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
import com.niumadiary.app.ui.model.DayKind
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description

@OptIn(ExperimentalCoroutinesApi::class)
class CalendarViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val zone = ZoneId.of("Asia/Shanghai")

    @Test
    fun `month state contains 42 Monday-first cells and aggregates current month`() =
        runTest(mainDispatcherRule.dispatcher) {
        val monday = LocalDate.of(2026, 8, 3)
        val records = listOf(
            record("morning", monday, 9, mood = 2),
            record("afternoon", monday, 17, mood = 4),
            record("wednesday", monday.plusDays(2), 11, mood = null),
        )
        val viewModel = viewModel(
            clock = fixedClock(LocalDate.of(2026, 8, 7), 12),
            records = records,
            summaries = listOf(summary(monday, mood = 1)),
        )

        val state = viewModel.uiState.first { !it.isLoading && it.days.size == 42 }

        assertEquals("2026年8月", state.monthLabel)
        assertEquals("2026-07-27", state.days.first().dateKey)
        assertEquals("2026-09-06", state.days.last().dateKey)
        assertEquals(3, state.recordCountThisMonth)
        assertEquals(2, state.recordedDayCountThisMonth)
        assertEquals(21, state.plannedWorkdayCountThisMonth)
        assertEquals(1, state.days.count { it.isToday })

        val mondayCell = state.days.single { it.dateKey == monday.toString() }
        assertEquals(2, mondayCell.recordCount)
        assertEquals(MoodLevel.BROKEN, mondayCell.mood)
        viewModel.viewModelScope.cancel()
    }

    @Test
    fun `previous and next month update the observed month`() =
        runTest(mainDispatcherRule.dispatcher) {
        val viewModel = viewModel(clock = fixedClock(LocalDate.of(2026, 8, 7), 12))
        viewModel.uiState.first { !it.isLoading }

        viewModel.onPreviousMonth()
        val july = viewModel.uiState.first { it.monthLabel == "2026年7月" && !it.isLoading }
        assertEquals("2026-06-29", july.days.first().dateKey)

        viewModel.onNextMonth()
        val august = viewModel.uiState.first { it.monthLabel == "2026年8月" && !it.isLoading }
        assertEquals("2026-07-27", august.days.first().dateKey)
        viewModel.viewModelScope.cancel()
    }

    @Test
    fun `month picker jump and current month shortcut update state`() =
        runTest(mainDispatcherRule.dispatcher) {
        val viewModel = viewModel(clock = fixedClock(LocalDate.of(2026, 8, 7), 12))
        val initial = viewModel.uiState.first { !it.isLoading }
        assertEquals(2026, initial.displayedYear)
        assertEquals(8, initial.displayedMonth)
        assertTrue(initial.isCurrentMonth)

        viewModel.onMonthSelected(2031, 11)
        val selected = viewModel.uiState.first { it.monthLabel == "2031年11月" && !it.isLoading }
        assertEquals(2031, selected.displayedYear)
        assertEquals(11, selected.displayedMonth)
        assertFalse(selected.isCurrentMonth)

        viewModel.onCurrentMonth()
        val current = viewModel.uiState.first { it.monthLabel == "2026年8月" && !it.isLoading }
        assertTrue(current.isCurrentMonth)
        viewModel.viewModelScope.cancel()
    }

    @Test
    fun `fixed weekend keeps bundled Sunday make-up day off`() =
        runTest(mainDispatcherRule.dispatcher) {
        val preferences = FakePreferencesRepository(
            UserPreferences(weekendPolicy = WeekendPolicy.FIXED_WEEKENDS_OFF),
        )
        val viewModel = viewModel(
            clock = fixedClock(LocalDate.of(2026, 1, 4), 12),
            preferences = preferences,
        )

        val state = viewModel.uiState.first { !it.isLoading && it.days.isNotEmpty() }
        val januaryFourth = state.days.single { it.dateKey == "2026-01-04" }

        assertEquals(DayKind.REST_DAY, januaryFourth.dayKind)
        assertEquals("法定调休补班", januaryFourth.holidayLabel)
        viewModel.viewModelScope.cancel()
    }

    @Test
    fun `official adjustment preference turns bundled Sunday into a workday`() =
        runTest(mainDispatcherRule.dispatcher) {
        val viewModel = viewModel(
            clock = fixedClock(LocalDate.of(2026, 1, 4), 12),
            preferences = FakePreferencesRepository(
                UserPreferences(weekendPolicy = WeekendPolicy.FOLLOW_OFFICIAL_ADJUSTMENTS),
            ),
        )

        val state = viewModel.uiState.first { !it.isLoading && it.days.isNotEmpty() }
        val januaryFourth = state.days.single { it.dateKey == "2026-01-04" }

        assertEquals(DayKind.WORKDAY, januaryFourth.dayKind)
        viewModel.viewModelScope.cancel()
    }

    @Test
    fun `personal override has priority in calendar cells`() =
        runTest(mainDispatcherRule.dispatcher) {
        val saturday = LocalDate.of(2026, 8, 8)
        val viewModel = CalendarViewModel(
            diaryRepository = FakeDiaryRepository(),
            dailySummaryRepository = FakeDailySummaryRepository(),
            userPreferencesRepository = FakePreferencesRepository(UserPreferences()),
            workdayOverrideRepository = FakeOverrideRepository(
                mapOf(saturday to WorkdayOverride.WORK),
            ),
            clock = fixedClock(LocalDate.of(2026, 8, 7), 12),
        )

        val state = viewModel.uiState.first { !it.isLoading && it.days.isNotEmpty() }

        assertEquals(
            DayKind.WORKDAY,
            state.days.single { it.dateKey == saturday.toString() }.dayKind,
        )
        viewModel.viewModelScope.cancel()
    }

    private fun viewModel(
        clock: Clock,
        records: List<DiaryRecord> = emptyList(),
        summaries: List<DailySummary> = emptyList(),
        preferences: FakePreferencesRepository = FakePreferencesRepository(UserPreferences()),
    ): CalendarViewModel = CalendarViewModel(
        diaryRepository = FakeDiaryRepository(records),
        dailySummaryRepository = FakeDailySummaryRepository(summaries),
        userPreferencesRepository = preferences,
        workdayOverrideRepository = FakeOverrideRepository(),
        clock = clock,
    )

    private fun record(
        id: String,
        date: LocalDate,
        hour: Int,
        mood: Int?,
    ): DiaryRecord {
        val instant = date.atTime(hour, 0).atZone(zone).toInstant()
        return DiaryRecord(
            id = id,
            occurredAt = instant,
            localDate = date,
            type = RecordType.MOOD_CHECK_IN,
            content = if (mood == null) "随手吐槽" else "",
            moodScore = mood,
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

private class FakeDiaryRepository(
    records: List<DiaryRecord> = emptyList(),
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

private class FakeDailySummaryRepository(
    summaries: List<DailySummary> = emptyList(),
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

private class FakePreferencesRepository(
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

private class FakeOverrideRepository(
    overrides: Map<LocalDate, WorkdayOverride> = emptyMap(),
) : WorkdayOverrideRepository {
    private val state = MutableStateFlow(overrides)

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
