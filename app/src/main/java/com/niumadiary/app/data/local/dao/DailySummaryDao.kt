package com.niumadiary.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.niumadiary.app.data.local.entity.DailySummaryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DailySummaryDao {
    @Query("SELECT * FROM daily_summaries WHERE localDateEpochDay = :epochDay LIMIT 1")
    fun observe(epochDay: Long): Flow<DailySummaryEntity?>

    @Query(
        """
        SELECT * FROM daily_summaries
        WHERE localDateEpochDay BETWEEN :startEpochDay AND :endEpochDay
        ORDER BY localDateEpochDay ASC
        """,
    )
    fun observeBetween(startEpochDay: Long, endEpochDay: Long): Flow<List<DailySummaryEntity>>

    @Upsert
    suspend fun upsert(summary: DailySummaryEntity)

    @Query("DELETE FROM daily_summaries WHERE localDateEpochDay = :epochDay")
    suspend fun delete(epochDay: Long): Int
}
