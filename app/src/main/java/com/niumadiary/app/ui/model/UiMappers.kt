package com.niumadiary.app.ui.model

import com.niumadiary.app.domain.model.DailySummary
import com.niumadiary.app.domain.model.DiaryRecord
import com.niumadiary.app.domain.model.DiaryTag
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

fun moodLevelFromScore(score: Int?): MoodLevel? =
    MoodLevel.entries.firstOrNull { it.value == score }

fun DiaryRecord.toUi(zoneId: ZoneId): DiaryEntryUi = DiaryEntryUi(
    id = id,
    timeLabel = occurredAt.atZone(zoneId).format(timeFormatter),
    text = content,
    mood = moodLevelFromScore(moodScore),
    tags = tags.sortedBy(DiaryTag::sortOrder).map(DiaryTag::toUi),
    isEdited = updatedAt != createdAt,
)

fun DailySummary?.toUi(): DaySummaryUi = this?.let { summary ->
    DaySummaryUi(
        mood = moodLevelFromScore(summary.finalMoodScore),
        text = summary.content,
    )
} ?: DaySummaryUi()

fun DiaryTag.toUi(): TagUi = TagUi(
    id = id,
    label = name,
    emoji = when {
        name.contains("咖啡") -> "☕"
        name.contains("午") || name.contains("饭") -> "🍱"
        name.contains("客户") -> "🤝"
        name.contains("汇报") -> "📊"
        name.contains("学习") || name.contains("培训") -> "📚"
        name.contains("截止") -> "⏳"
        name.contains("故障") || name.contains("线上") -> "🚨"
        name.contains("夸") -> "🌟"
        name.contains("团建") -> "🎉"
        name.contains("会") -> "🗣️"
        name.contains("需求") || name.contains("改") -> "🔁"
        name.contains("困") || name.contains("累") -> "🥱"
        name.contains("摸鱼") -> "🐟"
        name.contains("通勤") -> "🚇"
        name.contains("同事") -> "👥"
        name.contains("领导") -> "👔"
        name.contains("加班") -> "🌙"
        name.contains("代码") || name.contains("Bug", ignoreCase = true) -> "🐛"
        else -> "#"
    },
)
