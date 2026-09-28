package com.niumadiary.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "diary_tags",
    indices = [Index(value = ["name"], unique = true)],
)
data class DiaryTagEntity(
    @PrimaryKey val id: String,
    val name: String,
    val colorArgb: Long,
    val sortOrder: Int,
)
