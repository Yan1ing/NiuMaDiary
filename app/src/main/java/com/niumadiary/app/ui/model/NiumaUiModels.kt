package com.niumadiary.app.ui.model

import androidx.annotation.DrawableRes
import com.niumadiary.app.R

enum class MoodLevel(
    val value: Int,
    val label: String,
    val emoji: String,
    val description: String,
    @DrawableRes val imageRes: Int,
) {
    BROKEN(1, "彻底破防", "😭", "今天已经不想做人了", R.drawable.mood_broken),
    NUMB(2, "灵魂出窍", "😵‍💫", "脑子离线，只剩工位", R.drawable.mood_numb),
    OKAY(3, "勉强在线", "😶", "能干活，但请别催我", R.drawable.mood_okay),
    FINE(4, "状态不错", "🙂", "今天还能有说有笑", R.drawable.mood_fine),
    CHARGED(5, "元气满格", "😼", "效率和心情都在线", R.drawable.mood_charged),
    CAFFEINATED(6, "咖啡续命", "☕", "眼睛醒了，灵魂还在路上", R.drawable.mood_caffeinated),
    RECOVERED(7, "摸鱼回血", "🐟", "短暂离开工作，快乐回来了", R.drawable.mood_recovered),
    LEGENDARY(8, "今日封神", "✨", "手感火热，谁也拦不住", R.drawable.mood_legendary),
}

data class TagUi(
    val id: String,
    val label: String,
    val emoji: String = "",
)

data class DiaryEntryUi(
    val id: String,
    val timeLabel: String,
    val text: String = "",
    val mood: MoodLevel? = null,
    val tags: List<TagUi> = emptyList(),
    val isEdited: Boolean = false,
)

data class DaySummaryUi(
    val mood: MoodLevel? = null,
    val text: String = "",
    val annoyance: String = "",
    val goodThing: String = "",
) {
    val isEmpty: Boolean
        get() = mood == null && text.isBlank() && annoyance.isBlank() && goodThing.isBlank()
}

enum class DayKind {
    WORKDAY,
    REST_DAY,
}
