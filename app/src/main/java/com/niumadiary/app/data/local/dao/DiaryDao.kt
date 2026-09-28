package com.niumadiary.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.niumadiary.app.data.local.entity.DailyRecordStatsRow
import com.niumadiary.app.data.local.entity.DiaryRecordEntity
import com.niumadiary.app.data.local.entity.DiaryRecordTagCrossRef
import com.niumadiary.app.data.local.entity.DiaryRecordWithTags
import kotlinx.coroutines.flow.Flow

@Dao
abstract class DiaryDao {
    @Transaction
    @Query("SELECT * FROM diary_records WHERE id = :id LIMIT 1")
    abstract fun observeRecord(id: String): Flow<DiaryRecordWithTags?>

    @Transaction
    @Query(
        """
        SELECT * FROM diary_records
        WHERE localDateEpochDay = :localDateEpochDay
        ORDER BY occurredAtEpochMillis DESC, createdAtEpochMillis DESC
        """,
    )
    abstract fun observeRecordsForDate(localDateEpochDay: Long): Flow<List<DiaryRecordWithTags>>

    @Transaction
    @Query(
        """
        SELECT * FROM diary_records
        WHERE occurredAtEpochMillis >= :startInclusiveEpochMillis
          AND occurredAtEpochMillis < :endExclusiveEpochMillis
        ORDER BY occurredAtEpochMillis DESC, createdAtEpochMillis DESC
        """,
    )
    abstract fun observeRecordsBetween(
        startInclusiveEpochMillis: Long,
        endExclusiveEpochMillis: Long,
    ): Flow<List<DiaryRecordWithTags>>

    @Query(
        """
        SELECT
            localDateEpochDay,
            COUNT(*) AS recordCount,
            AVG(moodScore) AS averageMood,
            AVG(energyScore) AS averageEnergy,
            AVG(stressScore) AS averageStress,
            AVG(boredomScore) AS averageBoredom
        FROM diary_records
        WHERE localDateEpochDay BETWEEN :startEpochDay AND :endEpochDay
        GROUP BY localDateEpochDay
        ORDER BY localDateEpochDay ASC
        """,
    )
    abstract fun observeDailyStats(
        startEpochDay: Long,
        endEpochDay: Long,
    ): Flow<List<DailyRecordStatsRow>>

    @Upsert
    protected abstract suspend fun upsertRecordEntity(record: DiaryRecordEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    protected abstract suspend fun insertTagRelations(relations: List<DiaryRecordTagCrossRef>)

    @Query("DELETE FROM diary_record_tag_cross_ref WHERE recordId = :recordId")
    protected abstract suspend fun deleteTagRelations(recordId: String)

    @Transaction
    open suspend fun upsertRecord(record: DiaryRecordEntity, tagIds: Set<String>) {
        upsertRecordEntity(record)
        deleteTagRelations(record.id)
        if (tagIds.isNotEmpty()) {
            insertTagRelations(tagIds.map { tagId -> DiaryRecordTagCrossRef(record.id, tagId) })
        }
    }

    @Query("DELETE FROM diary_records WHERE id = :id")
    abstract suspend fun deleteRecord(id: String): Int
}
