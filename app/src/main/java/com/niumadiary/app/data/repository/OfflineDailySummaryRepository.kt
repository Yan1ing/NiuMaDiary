package com.niumadiary.app.data.repository

import com.niumadiary.app.data.local.dao.DailySummaryDao
import com.niumadiary.app.data.mapper.toDomain
import com.niumadiary.app.data.mapper.toEntity
import com.niumadiary.app.domain.model.DailySummary
import com.niumadiary.app.domain.repository.DailySummaryRepository
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class OfflineDailySummaryRepository @Inject constructor(
    private val dao: DailySummaryDao,
) : DailySummaryRepository {
    override fun observe(date: LocalDate): Flow<DailySummary?> =
        dao.observe(date.toEpochDay()).map { it?.toDomain() }

    override fun observeBetween(
        startDateInclusive: LocalDate,
        endDateInclusive: LocalDate,
    ): Flow<List<DailySummary>> {
        require(startDateInclusive <= endDateInclusive) {
            "startDateInclusive must not be after endDateInclusive"
        }
        return dao.observeBetween(
            startEpochDay = startDateInclusive.toEpochDay(),
            endEpochDay = endDateInclusive.toEpochDay(),
        ).map { summaries -> summaries.map { it.toDomain() } }
    }

    override suspend fun upsert(summary: DailySummary) {
        dao.upsert(summary.toEntity())
    }

    override suspend fun delete(date: LocalDate) {
        dao.delete(date.toEpochDay())
    }
}
