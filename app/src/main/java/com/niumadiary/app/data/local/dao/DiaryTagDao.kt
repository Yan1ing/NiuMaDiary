package com.niumadiary.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.niumadiary.app.data.local.entity.DiaryTagEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DiaryTagDao {
    @Query("SELECT * FROM diary_tags ORDER BY sortOrder ASC, name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<DiaryTagEntity>>

    @Upsert
    suspend fun upsert(tag: DiaryTagEntity)

    @Query("DELETE FROM diary_tags WHERE id = :id")
    suspend fun delete(id: String): Int
}
