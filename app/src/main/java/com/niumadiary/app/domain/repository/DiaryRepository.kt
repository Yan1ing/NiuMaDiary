package com.niumadiary.app.domain.repository

import com.niumadiary.app.domain.model.DailyRecordStats
import com.niumadiary.app.domain.model.DiaryRecord
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

interface DiaryRepository {
    fun observeRecord(id: String): Flow<DiaryRecord?>

    fun observeRecordsForDate(date: LocalDate): Flow<List<DiaryRecord>>

    fun observeRecordsBetween(
        startInclusive: Instant,
        endExclusive: Instant,
    ): Flow<List<DiaryRecord>>

    fun observeDailyStats(
        startDateInclusive: LocalDate,
        endDateInclusive: LocalDate,
    ): Flow<List<DailyRecordStats>>

    suspend fun upsert(record: DiaryRecord)

    suspend fun delete(id: String)
}
