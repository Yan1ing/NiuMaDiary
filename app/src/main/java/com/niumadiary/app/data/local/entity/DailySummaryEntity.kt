package com.niumadiary.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_summaries")
data class DailySummaryEntity(
    @PrimaryKey val localDateEpochDay: Long,
    val content: String,
    val finalMoodScore: Int?,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
)
