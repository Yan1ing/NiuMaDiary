package com.niumadiary.app.feature.stats

import com.niumadiary.app.core.time.WorkCalendar
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.time.temporal.WeekFields

data class StatsEntry(
    val id: String,
    val occurredAt: Instant,
    val moodScore: Int? = null,
    val tags: Set<String> = emptySet(),
) {
    init {
        require(moodScore == null || moodScore in MOOD_RANGE) { "moodScore must be from 1 to 8" }
    }

    companion object {
        val MOOD_RANGE: IntRange = 1..8
    }
}

data class StatsDaySummary(
    val date: LocalDate,
    val moodScore: Int? = null,
) {
    init {
        require(moodScore == null || moodScore in StatsEntry.MOOD_RANGE) {
            "moodScore must be from 1 to 8"
        }
    }
}

enum class RepresentativeMoodSource {
    DAY_SUMMARY,
    LATEST_ENTRY,
}

data class DailyRepresentativeMood(
    val date: LocalDate,
    val moodScore: Int,
    val source: RepresentativeMoodSource,
)

data class TagCount(val tag: String, val count: Int)

data class HourlyMoodInsight(
    val hour: Int,
    val moodSampleCount: Int,
    val lowMoodCount: Int,
    val lowMoodRatio: Double,
)

data class WeekdayMoodMedian(
    val dayOfWeek: DayOfWeek,
    val medianMood: Double,
    val sampleCount: Int,
)

data class WeekdayMoodInsight(
    val toughestDays: List<WeekdayMoodMedian>,
    val allMedians: List<WeekdayMoodMedian>,
    val observedWeekCount: Int,
)

data class StatsOverview(
    val internshipDayNumber: Int?,
    val plannedWorkdayCount: Int,
    val recordedWorkdayCount: Int,
    val entryCount: Int,
    val representativeMoods: List<DailyRepresentativeMood>,
    val moodDistribution: Map<Int, Int>,
    val topTags: List<TagCount>,
)

/** Pure aggregation functions. Room/domain models should be mapped into these small inputs. */
object StatsCalculator {
    fun calculateOverview(
        entries: Collection<StatsEntry>,
        summaries: Collection<StatsDaySummary>,
        employmentStart: LocalDate?,
        fromInclusive: LocalDate,
        toInclusive: LocalDate,
        zoneId: ZoneId,
        workCalendar: WorkCalendar,
        tagLimit: Int = 5,
    ): StatsOverview {
        val entriesInRange = entries.filter {
            it.localDate(zoneId) in fromInclusive..toInclusive
        }
        val summariesInRange = summaries.filter { it.date in fromInclusive..toInclusive }
        val firstEntryDate = entries.minOfOrNull { it.localDate(zoneId) }

        return StatsOverview(
            internshipDayNumber = internshipDayNumber(
                employmentStart = employmentStart,
                firstEntryDate = firstEntryDate,
                today = toInclusive,
            ),
            plannedWorkdayCount = workCalendar.countWorkdays(fromInclusive, toInclusive),
            recordedWorkdayCount = recordedWorkdayCount(
                entries = entriesInRange,
                fromInclusive = fromInclusive,
                toInclusive = toInclusive,
                zoneId = zoneId,
                workCalendar = workCalendar,
            ),
            entryCount = entriesInRange.size,
            representativeMoods = dailyRepresentativeMoods(
                entriesInRange,
                summariesInRange,
                zoneId,
            ),
            moodDistribution = moodDistribution(entriesInRange, summariesInRange),
            topTags = topTags(entriesInRange, tagLimit),
        )
    }

    /** Natural calendar days, inclusive. Returns null until a start can be inferred. */
    fun internshipDayNumber(
        employmentStart: LocalDate?,
        firstEntryDate: LocalDate?,
        today: LocalDate,
    ): Int? {
        val start = employmentStart ?: firstEntryDate ?: return null
        if (start > today) return 0
        return ChronoUnit.DAYS.between(start, today).toInt() + 1
    }

    fun recordedWorkdayCount(
        entries: Collection<StatsEntry>,
        fromInclusive: LocalDate,
        toInclusive: LocalDate,
        zoneId: ZoneId,
        workCalendar: WorkCalendar,
    ): Int = entries.asSequence()
        .map { it.localDate(zoneId) }
        .filter { it in fromInclusive..toInclusive }
        .filter(workCalendar::isWorkday)
        .distinct()
        .count()

    /** Day summary mood wins; otherwise the latest mood-bearing entry of that day wins. */
    fun dailyRepresentativeMoods(
        entries: Collection<StatsEntry>,
        summaries: Collection<StatsDaySummary>,
        zoneId: ZoneId,
    ): List<DailyRepresentativeMood> {
        val latestEntryMoodByDate = entries.asSequence()
            .filter { it.moodScore != null }
            .groupBy { it.localDate(zoneId) }
            .mapValues { (_, dayEntries) ->
                dayEntries.maxWithOrNull(compareBy<StatsEntry> { it.occurredAt }.thenBy { it.id })
            }

        val summaryMoodByDate = summaries.asSequence()
            .filter { it.moodScore != null }
            .associateBy(StatsDaySummary::date)

        return (latestEntryMoodByDate.keys + summaryMoodByDate.keys)
            .sorted()
            .mapNotNull { date ->
                val summary = summaryMoodByDate[date]
                if (summary?.moodScore != null) {
                    DailyRepresentativeMood(
                        date = date,
                        moodScore = summary.moodScore,
                        source = RepresentativeMoodSource.DAY_SUMMARY,
                    )
                } else {
                    val entry = latestEntryMoodByDate[date] ?: return@mapNotNull null
                    DailyRepresentativeMood(
                        date = date,
                        moodScore = checkNotNull(entry.moodScore),
                        source = RepresentativeMoodSource.LATEST_ENTRY,
                    )
                }
            }
    }

    /** Counts all manually selected moods, including the optional end-of-day summary mood. */
    fun moodDistribution(
        entries: Collection<StatsEntry>,
        summaries: Collection<StatsDaySummary>,
    ): Map<Int, Int> {
        val counts = StatsEntry.MOOD_RANGE.associateWith { 0 }.toMutableMap()
        entries.mapNotNull(StatsEntry::moodScore).forEach { counts[it] = counts.getValue(it) + 1 }
        summaries.mapNotNull(StatsDaySummary::moodScore).forEach {
            counts[it] = counts.getValue(it) + 1
        }
        return counts.toSortedMap()
    }

    fun topTags(entries: Collection<StatsEntry>, limit: Int = 5): List<TagCount> {
        require(limit >= 0) { "limit cannot be negative" }
        return entries.asSequence()
            .flatMap { it.tags.asSequence() }
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .groupingBy { it }
            .eachCount()
            .map { (tag, count) -> TagCount(tag, count) }
            .sortedWith(compareByDescending<TagCount> { it.count }.thenBy { it.tag.lowercase() })
            .take(limit)
    }

    /**
     * Finds the hour with the highest share of low moods. Conservative sample thresholds avoid
     * presenting a one-off bad check-in as a conclusion.
     */
    fun toughestHour(
        entries: Collection<StatsEntry>,
        zoneId: ZoneId,
        lowMoodMaximum: Int = 2,
        minimumTotalMoodSamples: Int = 8,
        minimumSamplesPerHour: Int = 2,
    ): HourlyMoodInsight? {
        require(lowMoodMaximum in StatsEntry.MOOD_RANGE) { "invalid low mood threshold" }
        require(minimumTotalMoodSamples > 0) { "minimumTotalMoodSamples must be positive" }
        require(minimumSamplesPerHour > 0) { "minimumSamplesPerHour must be positive" }

        val moodEntries = entries.filter { it.moodScore != null }
        if (moodEntries.size < minimumTotalMoodSamples) return null

        return moodEntries.groupBy { it.occurredAt.atZone(zoneId).hour }
            .mapNotNull { (hour, samples) ->
                if (samples.size < minimumSamplesPerHour) return@mapNotNull null
                val lowCount = samples.count { checkNotNull(it.moodScore) <= lowMoodMaximum }
                if (lowCount == 0) return@mapNotNull null
                HourlyMoodInsight(
                    hour = hour,
                    moodSampleCount = samples.size,
                    lowMoodCount = lowCount,
                    lowMoodRatio = lowCount.toDouble() / samples.size,
                )
            }
            .sortedWith(
                compareByDescending<HourlyMoodInsight> { it.lowMoodRatio }
                    .thenByDescending { it.lowMoodCount }
                    .thenByDescending { it.moodSampleCount }
                    .thenBy { it.hour },
            )
            .firstOrNull()
    }

    /** Uses one representative mood per workday so frequently logging days cannot dominate. */
    fun toughestWeekdays(
        dailyMoods: Collection<DailyRepresentativeMood>,
        workCalendar: WorkCalendar,
        minimumObservedWeeks: Int = 3,
    ): WeekdayMoodInsight? {
        require(minimumObservedWeeks > 0) { "minimumObservedWeeks must be positive" }
        val workdayMoods = dailyMoods.filter { workCalendar.isWorkday(it.date) }
        val weekFields = WeekFields.ISO
        val observedWeeks = workdayMoods.map {
            it.date.get(weekFields.weekBasedYear()) to it.date.get(weekFields.weekOfWeekBasedYear())
        }.distinct().size
        if (observedWeeks < minimumObservedWeeks) return null

        val medians = workdayMoods.groupBy { it.date.dayOfWeek }
            .map { (day, moods) ->
                WeekdayMoodMedian(
                    dayOfWeek = day,
                    medianMood = median(moods.map(DailyRepresentativeMood::moodScore)),
                    sampleCount = moods.size,
                )
            }
            .sortedBy { it.dayOfWeek.value }
        if (medians.isEmpty()) return null
        val lowestMedian = medians.minOf(WeekdayMoodMedian::medianMood)

        return WeekdayMoodInsight(
            toughestDays = medians.filter { it.medianMood == lowestMedian },
            allMedians = medians,
            observedWeekCount = observedWeeks,
        )
    }

    private fun median(values: Collection<Int>): Double {
        require(values.isNotEmpty()) { "median needs at least one value" }
        val sorted = values.sorted()
        val middle = sorted.size / 2
        return if (sorted.size % 2 == 1) {
            sorted[middle].toDouble()
        } else {
            (sorted[middle - 1] + sorted[middle]) / 2.0
        }
    }

    private fun StatsEntry.localDate(zoneId: ZoneId): LocalDate =
        occurredAt.atZone(zoneId).toLocalDate()
}
