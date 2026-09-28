package com.niumadiary.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.niumadiary.app.data.local.entity.WorkdayOverrideEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkdayOverrideDao {
    @Query("SELECT * FROM workday_overrides ORDER BY localDateEpochDay ASC")
    fun observeAll(): Flow<List<WorkdayOverrideEntity>>

    @Upsert
    suspend fun upsert(override: WorkdayOverrideEntity)

    @Query("DELETE FROM workday_overrides WHERE localDateEpochDay = :epochDay")
    suspend fun delete(epochDay: Long): Int
}
