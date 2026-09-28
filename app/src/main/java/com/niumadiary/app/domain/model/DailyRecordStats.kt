package com.niumadiary.app.domain.model

import java.time.LocalDate

data class DailyRecordStats(
    val date: LocalDate,
    val recordCount: Int,
    val averageMood: Double?,
    val averageEnergy: Double?,
    val averageStress: Double?,
    val averageBoredom: Double?,
)
