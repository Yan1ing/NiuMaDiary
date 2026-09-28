package com.niumadiary.app.data.repository

import com.niumadiary.app.core.holiday.WorkdayOverride
import com.niumadiary.app.data.local.dao.WorkdayOverrideDao
import com.niumadiary.app.data.local.entity.WorkdayOverrideEntity
import com.niumadiary.app.domain.repository.WorkdayOverrideRepository
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class RoomWorkdayOverrideRepository @Inject constructor(
    private val dao: WorkdayOverrideDao,
    private val clock: Clock,
) : WorkdayOverrideRepository {
    override fun observeAll(): Flow<Map<LocalDate, WorkdayOverride>> =
        dao.observeAll().map { rows ->
            rows.associate { row ->
                LocalDate.ofEpochDay(row.localDateEpochDay) to if (row.isWorkday) {
                    WorkdayOverride.WORK
                } else {
                    WorkdayOverride.OFF
                }
            }
        }

    override suspend fun set(date: LocalDate, override: WorkdayOverride) {
        dao.upsert(
            WorkdayOverrideEntity(
                localDateEpochDay = date.toEpochDay(),
                isWorkday = override == WorkdayOverride.WORK,
                updatedAtEpochMillis = clock.millis(),
            ),
        )
    }

    override suspend fun clear(date: LocalDate) {
        dao.delete(date.toEpochDay())
    }
}
