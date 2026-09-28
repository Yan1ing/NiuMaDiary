package com.niumadiary.app.domain.model

/** Stable IDs let the first-run database seed remain idempotent across upgrades. */
object BuiltInTags {
    val all: List<DiaryTag> = listOf(
        DiaryTag("builtin-meeting", "开会", 0xFFFF8A80L, 0),
        DiaryTag("builtin-boss", "领导", 0xFFFFB74DL, 1),
        DiaryTag("builtin-coworker", "同事", 0xFF81D4FAL, 2),
        DiaryTag("builtin-requirement", "需求", 0xFFCE93D8L, 3),
        DiaryTag("builtin-slacking", "摸鱼", 0xFFA5D6A7L, 4),
        DiaryTag("builtin-overtime", "加班", 0xFFE57373L, 5),
        DiaryTag("builtin-commute", "通勤", 0xFF90CAF9L, 6),
        DiaryTag("builtin-bug", "Bug", 0xFFE57373L, 7),
        DiaryTag("builtin-report", "汇报", 0xFFB39DDBL, 8),
        DiaryTag("builtin-client", "客户", 0xFF80CBC4L, 9),
        DiaryTag("builtin-learning", "学习", 0xFFA5D6A7L, 10),
        DiaryTag("builtin-lunch", "午饭", 0xFFFFCC80L, 11),
        DiaryTag("builtin-coffee", "咖啡", 0xFFBCAAA4L, 12),
        DiaryTag("builtin-deadline", "截止日期", 0xFFEF9A9AL, 13),
        DiaryTag("builtin-production", "线上故障", 0xFFFF8A80L, 14),
        DiaryTag("builtin-praise", "被夸", 0xFFFFE082L, 15),
        DiaryTag("builtin-team-building", "团建", 0xFF80DEeAL, 16),
    )
}
