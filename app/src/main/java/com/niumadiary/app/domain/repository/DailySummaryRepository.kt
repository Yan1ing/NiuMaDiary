package com.niumadiary.app.domain.repository

import com.niumadiary.app.domain.model.DailySummary
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

interface DailySummaryRepository {
    fun observe(date: LocalDate): Flow<DailySummary?>

    fun observeBetween(
        startDateInclusive: LocalDate,
        endDateInclusive: LocalDate,
    ): Flow<List<DailySummary>>

    suspend fun upsert(summary: DailySummary)

    suspend fun delete(date: LocalDate)
}
