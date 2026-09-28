package com.niumadiary.app.data.di

import com.niumadiary.app.data.preferences.DataStoreUserPreferencesRepository
import com.niumadiary.app.data.repository.OfflineDailySummaryRepository
import com.niumadiary.app.data.repository.OfflineDiaryRepository
import com.niumadiary.app.data.repository.OfflineTagRepository
import com.niumadiary.app.data.repository.RoomWorkdayOverrideRepository
import com.niumadiary.app.domain.repository.DailySummaryRepository
import com.niumadiary.app.domain.repository.DiaryRepository
import com.niumadiary.app.domain.repository.TagRepository
import com.niumadiary.app.domain.repository.UserPreferencesRepository
import com.niumadiary.app.domain.repository.WorkdayOverrideRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    abstract fun bindDiaryRepository(implementation: OfflineDiaryRepository): DiaryRepository

    @Binds
    abstract fun bindDailySummaryRepository(
        implementation: OfflineDailySummaryRepository,
    ): DailySummaryRepository

    @Binds
    abstract fun bindTagRepository(implementation: OfflineTagRepository): TagRepository

    @Binds
    abstract fun bindWorkdayOverrideRepository(
        implementation: RoomWorkdayOverrideRepository,
    ): WorkdayOverrideRepository

    @Binds
    abstract fun bindUserPreferencesRepository(
        implementation: DataStoreUserPreferencesRepository,
    ): UserPreferencesRepository
}
