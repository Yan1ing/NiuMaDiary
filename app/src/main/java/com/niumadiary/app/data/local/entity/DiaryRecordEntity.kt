package com.niumadiary.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "diary_records",
    indices = [
        Index(value = ["occurredAtEpochMillis"]),
        Index(value = ["localDateEpochDay"]),
        Index(value = ["type"]),
    ],
)
data class DiaryRecordEntity(
    @PrimaryKey val id: String,
    val occurredAtEpochMillis: Long,
    val localDateEpochDay: Long,
    val type: String,
    val content: String,
    val moodScore: Int?,
    val energyScore: Int?,
    val stressScore: Int?,
    val boredomScore: Int?,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
)
