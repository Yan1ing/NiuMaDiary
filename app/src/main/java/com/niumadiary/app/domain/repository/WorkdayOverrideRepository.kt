package com.niumadiary.app.domain.repository

import com.niumadiary.app.core.holiday.WorkdayOverride
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

interface WorkdayOverrideRepository {
    fun observeAll(): Flow<Map<LocalDate, WorkdayOverride>>

    suspend fun set(date: LocalDate, override: WorkdayOverride)

    suspend fun clear(date: LocalDate)
}
