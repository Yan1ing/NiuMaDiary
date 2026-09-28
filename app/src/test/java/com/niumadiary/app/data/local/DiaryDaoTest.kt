package com.niumadiary.app.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.niumadiary.app.data.local.entity.DiaryRecordEntity
import com.niumadiary.app.data.local.entity.DiaryTagEntity
import java.io.IOException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DiaryDaoTest {
    private lateinit var database: NiuMaDiaryDatabase

    @Before
    fun createDatabase() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, NiuMaDiaryDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    @Throws(IOException::class)
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun `records keep exact order tags and daily aggregates`() = runTest {
        val tag = DiaryTagEntity(
            id = "tag-meeting",
            name = "开会",
            colorArgb = 0xFFFF8A80L,
            sortOrder = 0,
        )
        database.diaryTagDao().upsert(tag)

        val epochDay = java.time.LocalDate.of(2026, 8, 7).toEpochDay()
        val earlier = record(
            id = "earlier",
            occurredAt = 1_786_087_800_000L,
            epochDay = epochDay,
            mood = 2,
        )
        val later = record(
            id = "later",
            occurredAt = 1_786_091_400_000L,
            epochDay = epochDay,
            mood = 4,
        )

        database.diaryDao().upsertRecord(earlier, setOf(tag.id))
        database.diaryDao().upsertRecord(later, emptySet())

        val records = database.diaryDao().observeRecordsForDate(epochDay).first()
        assertEquals(listOf("later", "earlier"), records.map { it.record.id })
        assertEquals(listOf("开会"), records.last().tags.map { it.name })

        val stats = database.diaryDao().observeDailyStats(epochDay, epochDay).first().single()
        assertEquals(2, stats.recordCount)
        assertEquals(3.0, stats.averageMood ?: error("average expected"), 0.0)
        assertNull(stats.averageEnergy)
    }

    private fun record(
        id: String,
        occurredAt: Long,
        epochDay: Long,
        mood: Int,
    ): DiaryRecordEntity = DiaryRecordEntity(
        id = id,
        occurredAtEpochMillis = occurredAt,
        localDateEpochDay = epochDay,
        type = "RANT",
        content = "测试记录",
        moodScore = mood,
        energyScore = null,
        stressScore = null,
        boredomScore = null,
        createdAtEpochMillis = occurredAt,
        updatedAtEpochMillis = occurredAt,
    )
}
