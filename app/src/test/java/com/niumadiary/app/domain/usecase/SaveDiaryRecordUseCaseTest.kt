package com.niumadiary.app.domain.usecase

import com.niumadiary.app.core.holiday.WeekendPolicy
import com.niumadiary.app.domain.model.AppTheme
import com.niumadiary.app.domain.model.DailyRecordStats
import com.niumadiary.app.domain.model.DiaryRecord
import com.niumadiary.app.domain.model.RecordType
import com.niumadiary.app.domain.model.UserPreferences
import com.niumadiary.app.domain.repository.DiaryRepository
import com.niumadiary.app.domain.repository.UserPreferencesRepository
import com.niumadiary.app.domain.time.IdGenerator
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class SaveDiaryRecordUseCaseTest {
    @Test
    fun `trims text derives Shanghai date and persists exact instant`() = runTest {
        val instant = Instant.parse("2026-08-07T17:30:00Z")
        val repository = RecordingDiaryRepository()
        val useCase = SaveDiaryRecordUseCase(
            repository = repository,
            preferencesRepository = FakePreferencesRepository(),
            clock = Clock.fixed(instant, ZoneId.of("UTC")),
            idGenerator = IdGenerator { "record-1" },
        )

        val result = useCase(
            SaveDiaryRecordCommand(
                type = RecordType.RANT,
                content = "  今天的会怎么还没结束  ",
                moodScore = 2,
                boredomScore = 5,
            ),
        )

        assertEquals("record-1", result.id)
        assertEquals(instant, result.occurredAt)
        assertEquals(LocalDate.of(2026, 8, 8), result.localDate)
        assertEquals("今天的会怎么还没结束", result.content)
        assertSame(result, repository.savedRecord)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `rejects a record without text or state`() = runTest {
        val useCase = SaveDiaryRecordUseCase(
            repository = RecordingDiaryRepository(),
            preferencesRepository = FakePreferencesRepository(),
            clock = Clock.systemUTC(),
            idGenerator = IdGenerator { "record-2" },
        )

        useCase(SaveDiaryRecordCommand(type = RecordType.RANT, content = "   "))
    }

    @Test
    fun `persists the new highest expressive mood without changing the schema`() = runTest {
        val repository = RecordingDiaryRepository()
        val useCase = SaveDiaryRecordUseCase(
            repository = repository,
            preferencesRepository = FakePreferencesRepository(),
            clock = Clock.fixed(Instant.parse("2026-08-23T10:00:00Z"), ZoneId.of("UTC")),
            idGenerator = IdGenerator { "legendary" },
        )

        val result = useCase(
            SaveDiaryRecordCommand(
                type = RecordType.MOOD_CHECK_IN,
                moodScore = DiaryRecord.MAX_MOOD_SCORE,
            ),
        )

        assertEquals(8, result.moodScore)
        assertEquals(8, repository.savedRecord?.moodScore)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `rejects a mood above the expanded range`() = runTest {
        val useCase = SaveDiaryRecordUseCase(
            repository = RecordingDiaryRepository(),
            preferencesRepository = FakePreferencesRepository(),
            clock = Clock.systemUTC(),
            idGenerator = IdGenerator { "too-high" },
        )

        useCase(
            SaveDiaryRecordCommand(
                type = RecordType.MOOD_CHECK_IN,
                moodScore = DiaryRecord.MAX_MOOD_SCORE + 1,
            ),
        )
    }
}

private class RecordingDiaryRepository : DiaryRepository {
    var savedRecord: DiaryRecord? = null

    override fun observeRecord(id: String): Flow<DiaryRecord?> = flowOf(null)

    override fun observeRecordsForDate(date: LocalDate): Flow<List<DiaryRecord>> = flowOf(emptyList())

    override fun observeRecordsBetween(
        startInclusive: Instant,
        endExclusive: Instant,
    ): Flow<List<DiaryRecord>> = flowOf(emptyList())

    override fun observeDailyStats(
        startDateInclusive: LocalDate,
        endDateInclusive: LocalDate,
    ): Flow<List<DailyRecordStats>> = flowOf(emptyList())

    override suspend fun upsert(record: DiaryRecord) {
        savedRecord = record
    }

    override suspend fun delete(id: String) = Unit
}

private class FakePreferencesRepository : UserPreferencesRepository {
    override val preferences = MutableStateFlow(UserPreferences())

    override suspend fun setWorkSchedule(start: LocalTime, end: LocalTime) = Unit

    override suspend fun setZoneId(zoneId: ZoneId) = Unit

    override suspend fun setWeekendPolicy(policy: WeekendPolicy) = Unit

    override suspend fun setInternshipStartDate(date: LocalDate?) = Unit

    override suspend fun setSummaryReminderEnabled(enabled: Boolean) = Unit

    override suspend fun setHapticsEnabled(enabled: Boolean) = Unit

    override suspend fun setAppTheme(theme: AppTheme) = Unit
}
