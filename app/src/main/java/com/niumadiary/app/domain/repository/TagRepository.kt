package com.niumadiary.app.domain.repository

import com.niumadiary.app.domain.model.DiaryTag
import kotlinx.coroutines.flow.Flow

interface TagRepository {
    fun observeAll(): Flow<List<DiaryTag>>

    suspend fun upsert(tag: DiaryTag)

    suspend fun delete(id: String)
}
