package com.niumadiary.app.domain.usecase

import com.niumadiary.app.domain.model.DailySummary
import com.niumadiary.app.domain.repository.DailySummaryRepository
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

class SaveDailySummaryUseCase @Inject constructor(
    private val repository: DailySummaryRepository,
    private val clock: Clock,
) {
    suspend operator fun invoke(
        date: LocalDate,
        content: String,
        finalMoodScore: Int?,
        existing: DailySummary? = null,
    ): DailySummary {
        val now = clock.instant()
        val summary = DailySummary(
            date = date,
            content = content.trim(),
            finalMoodScore = finalMoodScore,
            createdAt = existing?.createdAt ?: now,
            updatedAt = now,
        )
        repository.upsert(summary)
        return summary
    }
}
