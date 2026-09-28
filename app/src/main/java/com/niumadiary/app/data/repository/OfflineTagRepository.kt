package com.niumadiary.app.data.repository

import com.niumadiary.app.data.local.dao.DiaryTagDao
import com.niumadiary.app.data.mapper.toDomain
import com.niumadiary.app.data.mapper.toEntity
import com.niumadiary.app.domain.model.DiaryTag
import com.niumadiary.app.domain.repository.TagRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class OfflineTagRepository @Inject constructor(
    private val dao: DiaryTagDao,
) : TagRepository {
    override fun observeAll(): Flow<List<DiaryTag>> =
        dao.observeAll().map { tags -> tags.map { it.toDomain() } }

    override suspend fun upsert(tag: DiaryTag) {
        dao.upsert(tag.toEntity())
    }

    override suspend fun delete(id: String) {
        dao.delete(id)
    }
}
