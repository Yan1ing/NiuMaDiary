package com.niumadiary.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.niumadiary.app.data.local.dao.DailySummaryDao
import com.niumadiary.app.data.local.dao.DiaryDao
import com.niumadiary.app.data.local.dao.DiaryTagDao
import com.niumadiary.app.data.local.dao.WorkdayOverrideDao
import com.niumadiary.app.data.local.entity.DailySummaryEntity
import com.niumadiary.app.data.local.entity.DiaryRecordEntity
import com.niumadiary.app.data.local.entity.DiaryRecordTagCrossRef
import com.niumadiary.app.data.local.entity.DiaryTagEntity
import com.niumadiary.app.data.local.entity.WorkdayOverrideEntity

@Database(
    entities = [
        DiaryRecordEntity::class,
        DiaryTagEntity::class,
        DiaryRecordTagCrossRef::class,
        DailySummaryEntity::class,
        WorkdayOverrideEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class NiuMaDiaryDatabase : RoomDatabase() {
    abstract fun diaryDao(): DiaryDao

    abstract fun diaryTagDao(): DiaryTagDao

    abstract fun dailySummaryDao(): DailySummaryDao

    abstract fun workdayOverrideDao(): WorkdayOverrideDao

    companion object {
        const val DATABASE_NAME: String = "niuma_diary.db"
    }
}
