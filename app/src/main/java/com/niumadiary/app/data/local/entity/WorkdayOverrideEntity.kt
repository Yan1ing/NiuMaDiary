package com.niumadiary.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workday_overrides")
data class WorkdayOverrideEntity(
    @PrimaryKey val localDateEpochDay: Long,
    val isWorkday: Boolean,
    val updatedAtEpochMillis: Long,
)
