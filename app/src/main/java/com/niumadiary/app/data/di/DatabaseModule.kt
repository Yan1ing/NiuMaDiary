package com.niumadiary.app.data.di

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import com.niumadiary.app.data.local.NiuMaDiaryDatabase
import com.niumadiary.app.data.local.dao.DailySummaryDao
import com.niumadiary.app.data.local.dao.DiaryDao
import com.niumadiary.app.data.local.dao.DiaryTagDao
import com.niumadiary.app.data.local.dao.WorkdayOverrideDao
import com.niumadiary.app.domain.model.BuiltInTags
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): NiuMaDiaryDatabase =
        Room.databaseBuilder(
            context,
            NiuMaDiaryDatabase::class.java,
            NiuMaDiaryDatabase.DATABASE_NAME,
        )
            .addCallback(DefaultTagsCallback)
            .build()

    @Provides
    fun provideDiaryDao(database: NiuMaDiaryDatabase): DiaryDao = database.diaryDao()

    @Provides
    fun provideDiaryTagDao(database: NiuMaDiaryDatabase): DiaryTagDao = database.diaryTagDao()

    @Provides
    fun provideDailySummaryDao(database: NiuMaDiaryDatabase): DailySummaryDao =
        database.dailySummaryDao()

    @Provides
    fun provideWorkdayOverrideDao(database: NiuMaDiaryDatabase): WorkdayOverrideDao =
        database.workdayOverrideDao()

    private object DefaultTagsCallback : androidx.room.RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            seedMissingTags(db)
        }

        override fun onOpen(db: SupportSQLiteDatabase) {
            super.onOpen(db)
            seedMissingTags(db)
        }

        private fun seedMissingTags(db: SupportSQLiteDatabase) {
            BuiltInTags.all.forEach { tag ->
                db.execSQL(
                    """
                    INSERT OR IGNORE INTO diary_tags(id, name, colorArgb, sortOrder)
                    VALUES (?, ?, ?, ?)
                    """.trimIndent(),
                    arrayOf<Any?>(tag.id, tag.name, tag.colorArgb, tag.sortOrder),
                )
            }
        }
    }
}
