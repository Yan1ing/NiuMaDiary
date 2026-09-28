package com.niumadiary.app.domain.model

import java.time.Instant
import java.time.LocalDate

data class DailySummary(
    val date: LocalDate,
    val content: String,
    val finalMoodScore: Int? = null,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    init {
        require(content.length <= MAX_CONTENT_LENGTH) { "summary is too long" }
        require(content.isNotBlank() || finalMoodScore != null) {
            "a summary needs text or a final mood score"
        }
        require(finalMoodScore == null || finalMoodScore in DiaryRecord.MIN_SCORE..DiaryRecord.MAX_MOOD_SCORE) {
            "finalMoodScore must be between 1 and ${DiaryRecord.MAX_MOOD_SCORE}"
        }
        require(!updatedAt.isBefore(createdAt)) { "updatedAt cannot be before createdAt" }
    }

    companion object {
        const val MAX_CONTENT_LENGTH: Int = 20_000
    }
}
