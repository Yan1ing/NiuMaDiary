package com.niumadiary.app.feature.stats

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.niumadiary.app.ui.components.GlassCard
import com.niumadiary.app.ui.components.MoodAvatar
import com.niumadiary.app.ui.components.PageHeader
import com.niumadiary.app.ui.components.SectionLabel
import com.niumadiary.app.ui.components.SunsetBackdrop
import com.niumadiary.app.ui.components.SunsetBackdropStyle
import com.niumadiary.app.ui.components.accentColor
import com.niumadiary.app.ui.model.MoodLevel
import com.niumadiary.app.ui.model.TagUi
import com.niumadiary.app.ui.theme.SunsetCoral
import com.niumadiary.app.ui.theme.SunsetInk
import com.niumadiary.app.ui.theme.SunsetLamp

/** A selectable time range supplied by the screen owner. */
data class StatsRangeUi(
    val id: String,
    val label: String,
)

/** One mood bucket. [share] is expected in the inclusive 0f..1f range. */
data class MoodDistributionUi(
    val mood: MoodLevel,
    val count: Int,
    val share: Float,
)

/** One frequently used tag. [share] controls only the relative display bar. */
data class TagUsageUi(
    val tag: TagUi,
    val count: Int,
    val share: Float,
)

/**
 * Render-ready statistics. All values come from the caller; this screen does not own or load data.
 * Set [insufficientSampleMessage] when the current range does not contain enough records for a
 * trustworthy trend.
 */
data class StatsUiState(
    val periodLabel: String = "",
    val availableRanges: List<StatsRangeUi> = emptyList(),
    val selectedRangeId: String = "",
    val plannedWorkdayCount: Int = 0,
    val recordedWorkdayCount: Int = 0,
    val workDayNumber: Int = 0,
    val moodDistribution: List<MoodDistributionUi> = emptyList(),
    val commonTags: List<TagUsageUi> = emptyList(),
    val insufficientSampleMessage: String? = null,
)

@Composable
fun StatsScreen(
    state: StatsUiState,
    onRangeSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
) {
    SunsetBackdrop(
        style = SunsetBackdropStyle.STATS,
        modifier = modifier.fillMaxSize(),
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(
                    WindowInsets.safeDrawing.only(
                        WindowInsetsSides.Top + WindowInsetsSides.Horizontal,
                    ),
                ),
            contentPadding = PaddingValues(
                start = 16.dp,
                top = 14.dp,
                end = 16.dp,
                bottom = contentPadding.calculateBottomPadding() + 28.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item(key = "header") {
                PageHeader(
                    title = "牛马统计",
                    subtitle = state.periodLabel.ifBlank { "看看这段工作时光留下了什么" },
                )
            }

            if (state.availableRanges.isNotEmpty()) {
                item(key = "ranges") {
                    RangeSelector(
                        ranges = state.availableRanges,
                        selectedRangeId = state.selectedRangeId,
                        onRangeSelected = onRangeSelected,
                    )
                }
            }

            item(key = "headline_metrics") {
                HeadlineMetrics(state = state)
            }

            state.insufficientSampleMessage
                ?.takeIf(String::isNotBlank)
                ?.let { message ->
                    item(key = "sample_notice") {
                        SampleNotice(message = message)
                    }
                }

            item(key = "mood_title") {
                SectionLabel(
                    title = "精神状态分布",
                    trailing = state.moodDistribution.sumOf { it.count.coerceAtLeast(0) }
                        .takeIf { it > 0 }
                        ?.let { "$it 次记录" }
                        .orEmpty(),
                )
            }

            item(key = "mood_distribution") {
                MoodDistributionCard(distribution = state.moodDistribution)
            }

            item(key = "tag_title") {
                SectionLabel(
                    title = "常用标签",
                    trailing = if (state.commonTags.isEmpty()) "" else "TOP ${state.commonTags.size}",
                )
            }

            item(key = "common_tags") {
                CommonTagsCard(tags = state.commonTags)
            }
        }
    }
}

@Composable
private fun RangeSelector(
    ranges: List<StatsRangeUi>,
    selectedRangeId: String,
    onRangeSelected: (String) -> Unit,
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(items = ranges, key = { it.id }) { range ->
            FilterChip(
                selected = range.id == selectedRangeId,
                onClick = { onRangeSelected(range.id) },
                label = {
                    Text(
                        text = range.label,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
            )
        }
    }
}

@Composable
private fun HeadlineMetrics(state: StatsUiState) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            MetricCard(
                eyebrow = "计划上班",
                value = "${state.plannedWorkdayCount.coerceAtLeast(0)} 天",
                supporting = "这段时间的班表",
                accent = SunsetCoral,
                modifier = Modifier.weight(1f),
            )
            MetricCard(
                eyebrow = "已记录上班",
                value = "${state.recordedWorkdayCount.coerceAtLeast(0)} 天",
                supporting = coverageLabel(
                    recorded = state.recordedWorkdayCount,
                    planned = state.plannedWorkdayCount,
                ),
                accent = SunsetLamp,
                modifier = Modifier.weight(1f),
            )
        }

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    shape = CircleShape,
                    color = SunsetCoral.copy(alpha = 0.16f),
                ) {
                    Text(
                        text = "💼",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(11.dp),
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "工作日历",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = "第 ${state.workDayNumber.coerceAtLeast(0)} 天",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Text(
                    text = "继续存活",
                    style = MaterialTheme.typography.labelLarge,
                    color = SunsetCoral,
                )
            }
        }
    }
}

@Composable
private fun MetricCard(
    eyebrow: String,
    value: String,
    supporting: String,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    GlassCard(modifier = modifier.heightIn(min = 122.dp)) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Surface(
                modifier = Modifier.height(4.dp).fillMaxWidth(0.34f),
                shape = CircleShape,
                color = accent,
            ) {}
            Spacer(Modifier.height(2.dp))
            Text(
                text = eyebrow,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
            Text(
                text = supporting,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun SampleNotice(message: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = SunsetLamp.copy(alpha = 0.24f),
        contentColor = SunsetInk,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Text(text = "🧪", style = MaterialTheme.typography.titleLarge)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "样本还不够多",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = SunsetInk.copy(alpha = 0.72f),
                )
            }
        }
    }
}

@Composable
private fun MoodDistributionCard(distribution: List<MoodDistributionUi>) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        if (distribution.isEmpty()) {
            InlineEmptyState(
                emoji = "🌫️",
                title = "还没有状态样本",
                body = "随手记下一次心情，这里就会慢慢长出你的精神曲线。",
            )
        } else {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(15.dp),
            ) {
                distribution.forEach { item ->
                    MoodDistributionRow(item = item)
                }
            }
        }
    }
}

@Composable
private fun MoodDistributionRow(item: MoodDistributionUi) {
    val targetShare = item.share.coerceIn(0f, 1f)
    val animatedShare by animateFloatAsState(
        targetValue = targetShare,
        animationSpec = tween(durationMillis = 650),
        label = "${item.mood.name}Share",
    )

    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            MoodAvatar(mood = item.mood, size = 26.dp)
            Text(
                text = item.mood.label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "${(targetShare * 100f).toInt()}% · ${item.count.coerceAtLeast(0)} 次",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        LinearProgressIndicator(
            progress = animatedShare,
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(CircleShape),
            color = item.mood.accentColor(),
            trackColor = item.mood.accentColor().copy(alpha = 0.13f),
        )
    }
}

@Composable
private fun CommonTagsCard(tags: List<TagUsageUi>) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        if (tags.isEmpty()) {
            InlineEmptyState(
                emoji = "🏷️",
                title = "还没有常用标签",
                body = "多记几次开会、摸鱼或通勤，很快就能看出谁最常出现。",
            )
        } else {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                tags.forEachIndexed { index, item ->
                    TagUsageRow(rank = index + 1, item = item)
                }
            }
        }
    }
}

@Composable
private fun TagUsageRow(rank: Int, item: TagUsageUi) {
    val targetShare = item.share.coerceIn(0f, 1f)
    val animatedShare by animateFloatAsState(
        targetValue = targetShare,
        animationSpec = tween(durationMillis = 650),
        label = "${item.tag.id}Share",
    )

    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            Surface(
                shape = CircleShape,
                color = if (rank == 1) SunsetCoral.copy(alpha = 0.18f)
                else MaterialTheme.colorScheme.surfaceVariant,
            ) {
                Text(
                    text = rank.toString(),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                )
            }
            Text(
                text = "${item.tag.emoji}${item.tag.label}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "${item.count.coerceAtLeast(0)} 次",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        LinearProgressIndicator(
            progress = animatedShare,
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(CircleShape),
            color = SunsetCoral,
            trackColor = SunsetCoral.copy(alpha = 0.10f),
        )
    }
}

@Composable
private fun InlineEmptyState(
    emoji: String,
    title: String,
    body: String,
) {
    Row(
        modifier = Modifier.padding(18.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = emoji, style = MaterialTheme.typography.headlineSmall)
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(3.dp))
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun coverageLabel(recorded: Int, planned: Int): String {
    if (planned <= 0) return "等待生成班表"
    val percentage = (recorded.coerceAtLeast(0).toFloat() / planned * 100f)
        .coerceIn(0f, 100f)
        .toInt()
    return "记录覆盖 $percentage%"
}
