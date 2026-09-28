package com.niumadiary.app.data.mapper

import com.niumadiary.app.data.local.entity.DailyRecordStatsRow
import com.niumadiary.app.data.local.entity.DailySummaryEntity
import com.niumadiary.app.data.local.entity.DiaryRecordEntity
import com.niumadiary.app.data.local.entity.DiaryRecordWithTags
import com.niumadiary.app.data.local.entity.DiaryTagEntity
import com.niumadiary.app.domain.model.DailyRecordStats
import com.niumadiary.app.domain.model.DailySummary
import com.niumadiary.app.domain.model.DiaryRecord
import com.niumadiary.app.domain.model.DiaryTag
import com.niumadiary.app.domain.model.RecordType
import java.time.Instant
import java.time.LocalDate

internal fun DiaryRecord.toEntity(): DiaryRecordEntity = DiaryRecordEntity(
    id = id,
    occurredAtEpochMillis = occurredAt.toEpochMilli(),
    localDateEpochDay = localDate.toEpochDay(),
    type = type.name,
    content = content,
    moodScore = moodScore,
    energyScore = energyScore,
    stressScore = stressScore,
    boredomScore = boredomScore,
    createdAtEpochMillis = createdAt.toEpochMilli(),
    updatedAtEpochMillis = updatedAt.toEpochMilli(),
)

internal fun DiaryRecordWithTags.toDomain(): DiaryRecord = DiaryRecord(
    id = record.id,
    occurredAt = Instant.ofEpochMilli(record.occurredAtEpochMillis),
    localDate = LocalDate.ofEpochDay(record.localDateEpochDay),
    type = RecordType.valueOf(record.type),
    content = record.content,
    moodScore = record.moodScore,
    energyScore = record.energyScore,
    stressScore = record.stressScore,
    boredomScore = record.boredomScore,
    tags = tags
        .sortedWith(compareBy<DiaryTagEntity> { it.sortOrder }.thenBy { it.name.lowercase() })
        .mapTo(linkedSetOf()) { it.toDomain() },
    createdAt = Instant.ofEpochMilli(record.createdAtEpochMillis),
    updatedAt = Instant.ofEpochMilli(record.updatedAtEpochMillis),
)

internal fun DiaryTag.toEntity(): DiaryTagEntity = DiaryTagEntity(
    id = id,
    name = name.trim(),
    colorArgb = colorArgb,
    sortOrder = sortOrder,
)

internal fun DiaryTagEntity.toDomain(): DiaryTag = DiaryTag(
    id = id,
    name = name,
    colorArgb = colorArgb,
    sortOrder = sortOrder,
)

internal fun DailySummary.toEntity(): DailySummaryEntity = DailySummaryEntity(
    localDateEpochDay = date.toEpochDay(),
    content = content,
    finalMoodScore = finalMoodScore,
    createdAtEpochMillis = createdAt.toEpochMilli(),
    updatedAtEpochMillis = updatedAt.toEpochMilli(),
)

internal fun DailySummaryEntity.toDomain(): DailySummary = DailySummary(
    date = LocalDate.ofEpochDay(localDateEpochDay),
    content = content,
    finalMoodScore = finalMoodScore,
    createdAt = Instant.ofEpochMilli(createdAtEpochMillis),
    updatedAt = Instant.ofEpochMilli(updatedAtEpochMillis),
)

internal fun DailyRecordStatsRow.toDomain(): DailyRecordStats = DailyRecordStats(
    date = LocalDate.ofEpochDay(localDateEpochDay),
    recordCount = recordCount,
    averageMood = averageMood,
    averageEnergy = averageEnergy,
    averageStress = averageStress,
    averageBoredom = averageBoredom,
)
