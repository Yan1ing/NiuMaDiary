package com.niumadiary.app.domain.usecase

import com.niumadiary.app.domain.model.DiaryRecord
import com.niumadiary.app.domain.model.DiaryTag
import com.niumadiary.app.domain.model.RecordType
import com.niumadiary.app.domain.repository.DiaryRepository
import com.niumadiary.app.domain.repository.UserPreferencesRepository
import com.niumadiary.app.domain.time.IdGenerator
import java.time.Clock
import kotlinx.coroutines.flow.first
import javax.inject.Inject

data class SaveDiaryRecordCommand(
    val existing: DiaryRecord? = null,
    val type: RecordType,
    val content: String = "",
    val moodScore: Int? = null,
    val energyScore: Int? = null,
    val stressScore: Int? = null,
    val boredomScore: Int? = null,
    val tags: Set<DiaryTag> = emptySet(),
)

class SaveDiaryRecordUseCase @Inject constructor(
    private val repository: DiaryRepository,
    private val preferencesRepository: UserPreferencesRepository,
    private val clock: Clock,
    private val idGenerator: IdGenerator,
) {
    suspend operator fun invoke(command: SaveDiaryRecordCommand): DiaryRecord {
        val now = clock.instant()
        val preferences = preferencesRepository.preferences.first()
        val occurredAt = command.existing?.occurredAt ?: now
        val record = DiaryRecord(
            id = command.existing?.id ?: idGenerator.newId(),
            occurredAt = occurredAt,
            localDate = command.existing?.localDate
                ?: occurredAt.atZone(preferences.zoneId).toLocalDate(),
            type = command.type,
            content = command.content.trim(),
            moodScore = command.moodScore,
            energyScore = command.energyScore,
            stressScore = command.stressScore,
            boredomScore = command.boredomScore,
            tags = command.tags,
            createdAt = command.existing?.createdAt ?: now,
            updatedAt = now,
        )
        repository.upsert(record)
        return record
    }
}
