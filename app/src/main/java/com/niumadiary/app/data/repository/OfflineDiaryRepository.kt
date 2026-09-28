package com.niumadiary.app.data.repository

import com.niumadiary.app.data.local.dao.DiaryDao
import com.niumadiary.app.data.mapper.toDomain
import com.niumadiary.app.data.mapper.toEntity
import com.niumadiary.app.domain.model.DailyRecordStats
import com.niumadiary.app.domain.model.DiaryRecord
import com.niumadiary.app.domain.repository.DiaryRepository
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class OfflineDiaryRepository @Inject constructor(
    private val diaryDao: DiaryDao,
) : DiaryRepository {
    override fun observeRecord(id: String): Flow<DiaryRecord?> =
        diaryDao.observeRecord(id).map { it?.toDomain() }

    override fun observeRecordsForDate(date: LocalDate): Flow<List<DiaryRecord>> =
        diaryDao.observeRecordsForDate(date.toEpochDay()).map { records ->
            records.map { it.toDomain() }
        }

    override fun observeRecordsBetween(
        startInclusive: Instant,
        endExclusive: Instant,
    ): Flow<List<DiaryRecord>> {
        require(startInclusive < endExclusive) { "startInclusive must be before endExclusive" }
        return diaryDao.observeRecordsBetween(
            startInclusiveEpochMillis = startInclusive.toEpochMilli(),
            endExclusiveEpochMillis = endExclusive.toEpochMilli(),
        ).map { records -> records.map { it.toDomain() } }
    }

    override fun observeDailyStats(
        startDateInclusive: LocalDate,
        endDateInclusive: LocalDate,
    ): Flow<List<DailyRecordStats>> {
        require(startDateInclusive <= endDateInclusive) {
            "startDateInclusive must not be after endDateInclusive"
        }
        return diaryDao.observeDailyStats(
            startEpochDay = startDateInclusive.toEpochDay(),
            endEpochDay = endDateInclusive.toEpochDay(),
        ).map { rows -> rows.map { it.toDomain() } }
    }

    override suspend fun upsert(record: DiaryRecord) {
        diaryDao.upsertRecord(record.toEntity(), record.tags.mapTo(mutableSetOf()) { it.id })
    }

    override suspend fun delete(id: String) {
        diaryDao.deleteRecord(id)
    }
}
