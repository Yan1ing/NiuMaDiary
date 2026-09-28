package com.niumadiary.app.data.local

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.niumadiary.app.data.di.DatabaseModule
import com.niumadiary.app.data.local.entity.DiaryRecordEntity
import com.niumadiary.app.domain.model.BuiltInTags
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DatabaseUpgradeCompatibilityTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private var database: NiuMaDiaryDatabase? = null

    @After
    fun cleanUp() {
        database?.close()
        context.deleteDatabase(NiuMaDiaryDatabase.DATABASE_NAME)
    }

    @Test
    fun `reopening an existing version one database keeps records and adds missing built-in tags`() =
        runTest {
            context.deleteDatabase(NiuMaDiaryDatabase.DATABASE_NAME)
            database = DatabaseModule.provideDatabase(context)
            val firstOpen = checkNotNull(database)
            val newTagId = "builtin-team-building"
            assertEquals(BuiltInTags.all.size, firstOpen.diaryTagDao().observeAll().first().size)

            firstOpen.diaryTagDao().delete(newTagId)
            val epochDay = LocalDate.of(2026, 8, 23).toEpochDay()
            firstOpen.diaryDao().upsertRecord(
                record = DiaryRecordEntity(
                    id = "existing-record",
                    occurredAtEpochMillis = 1_777_000_000_000L,
                    localDateEpochDay = epochDay,
                    type = "RANT",
                    content = "升级前的数据",
                    moodScore = 5,
                    energyScore = null,
                    stressScore = null,
                    boredomScore = null,
                    createdAtEpochMillis = 1_777_000_000_000L,
                    updatedAtEpochMillis = 1_777_000_000_000L,
                ),
                tagIds = emptySet(),
            )
            firstOpen.close()

            database = DatabaseModule.provideDatabase(context)
            val reopened = checkNotNull(database)
            val tagIds = reopened.diaryTagDao().observeAll().first().map { it.id }
            val records = reopened.diaryDao().observeRecordsForDate(epochDay).first()

            assertTrue(newTagId in tagIds)
            assertEquals("升级前的数据", records.single().record.content)
            assertEquals(5, records.single().record.moodScore)
        }
}
