package com.niumadiary.app.data.local.entity

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

data class DiaryRecordWithTags(
    @Embedded val record: DiaryRecordEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = DiaryRecordTagCrossRef::class,
            parentColumn = "recordId",
            entityColumn = "tagId",
        ),
    )
    val tags: List<DiaryTagEntity>,
)

data class DailyRecordStatsRow(
    val localDateEpochDay: Long,
    val recordCount: Int,
    val averageMood: Double?,
    val averageEnergy: Double?,
    val averageStress: Double?,
    val averageBoredom: Double?,
)
