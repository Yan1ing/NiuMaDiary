package com.niumadiary.app.domain.model

import java.time.Instant
import java.time.LocalDate

enum class RecordType {
    RANT,
    MOOD_CHECK_IN,
}

/**
 * One timestamped moment in the diary. Mood has eight expressive choices while the other
 * optional state scores retain their original 1..5 range.
 */
data class DiaryRecord(
    val id: String,
    val occurredAt: Instant,
    val localDate: LocalDate,
    val type: RecordType,
    val content: String = "",
    val moodScore: Int? = null,
    val energyScore: Int? = null,
    val stressScore: Int? = null,
    val boredomScore: Int? = null,
    val tags: Set<DiaryTag> = emptySet(),
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    init {
        require(id.isNotBlank()) { "id cannot be blank" }
        require(content.length <= MAX_CONTENT_LENGTH) { "content is too long" }
        validateScore("moodScore", moodScore, MAX_MOOD_SCORE)
        validateScore("energyScore", energyScore, MAX_SCORE)
        validateScore("stressScore", stressScore, MAX_SCORE)
        validateScore("boredomScore", boredomScore, MAX_SCORE)
        require(
            content.isNotBlank() ||
                listOf(moodScore, energyScore, stressScore, boredomScore).any { it != null },
        ) { "a record needs text or at least one state score" }
        require(!updatedAt.isBefore(createdAt)) { "updatedAt cannot be before createdAt" }
    }

    companion object {
        const val MIN_SCORE: Int = 1
        const val MAX_SCORE: Int = 5
        const val MAX_MOOD_SCORE: Int = 8
        const val MAX_CONTENT_LENGTH: Int = 10_000

        private fun validateScore(name: String, score: Int?, maximum: Int) {
            require(score == null || score in MIN_SCORE..maximum) {
                "$name must be between $MIN_SCORE and $maximum"
            }
        }
    }
}
