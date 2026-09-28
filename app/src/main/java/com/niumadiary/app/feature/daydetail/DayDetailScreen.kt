package com.niumadiary.app.feature.daydetail

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
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.niumadiary.app.R
import com.niumadiary.app.ui.components.DiaryEntryCard
import com.niumadiary.app.ui.components.EmptyState
import com.niumadiary.app.ui.components.GlassCard
import com.niumadiary.app.ui.components.MoodBadge
import com.niumadiary.app.ui.components.PageHeader
import com.niumadiary.app.ui.components.SectionLabel
import com.niumadiary.app.ui.components.SoftDivider
import com.niumadiary.app.ui.components.SunsetBackdrop
import com.niumadiary.app.ui.components.SunsetBackdropStyle
import com.niumadiary.app.ui.model.DayKind
import com.niumadiary.app.ui.model.DaySummaryUi
import com.niumadiary.app.ui.model.DiaryEntryUi
import com.niumadiary.app.ui.theme.SunsetCoral
import com.niumadiary.app.ui.theme.SunsetCream
import com.niumadiary.app.ui.theme.SunsetInk
import com.niumadiary.app.ui.theme.SunsetLamp

data class DayDetailUiState(
    val dateKey: String = "",
    val dateLabel: String = "",
    val weekdayLabel: String = "",
    val holidayLabel: String = "",
    val dayKind: DayKind = DayKind.WORKDAY,
    val entries: List<DiaryEntryUi> = emptyList(),
    val summary: DaySummaryUi = DaySummaryUi(),
    val canAddEntry: Boolean = true,
)

@Composable
fun DayDetailScreen(
    state: DayDetailUiState,
    onBack: () -> Unit,
    onAddEntry: () -> Unit,
    onEntryClick: (entryId: String) -> Unit,
    onSummaryClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
) {
    SunsetBackdrop(
        style = SunsetBackdropStyle.CALENDAR,
        modifier = modifier.fillMaxSize(),
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(
                    WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
                ),
            contentPadding = PaddingValues(
                start = 16.dp,
                top = 8.dp,
                end = 16.dp,
                bottom = contentPadding.calculateBottomPadding() + 28.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item(key = "back") {
                Surface(
                    onClick = onBack,
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
                ) {
                    Text(
                        text = "←  ${stringResource(R.string.day_detail_back)}",
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(horizontal = 13.dp, vertical = 9.dp),
                    )
                }
            }

            item(key = "header") {
                PageHeader(
                    title = state.dateLabel.ifBlank { stringResource(R.string.day_detail_title) },
                    subtitle = buildString {
                        append(state.weekdayLabel)
                        if (state.holidayLabel.isNotBlank()) {
                            if (isNotEmpty()) append(" · ")
                            append(state.holidayLabel)
                        }
                    },
                    action = {
                        Surface(
                            shape = CircleShape,
                            color = if (state.dayKind == DayKind.WORKDAY) {
                                SunsetCoral.copy(alpha = 0.18f)
                            } else {
                                SunsetLamp.copy(alpha = 0.24f)
                            },
                        ) {
                            Text(
                                text = if (state.dayKind == DayKind.WORKDAY) "上班日" else "休息日",
                                style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                            )
                        }
                    },
                )
            }

            item(key = "overview") {
                DayOverview(state = state)
            }

            item(key = "summary") {
                SummaryCard(
                    summary = state.summary,
                    onClick = onSummaryClick,
                )
            }

            item(key = "timelineTitle") {
                SectionLabel(
                    title = stringResource(R.string.day_detail_timeline),
                    trailing = if (state.entries.isEmpty()) "" else "${state.entries.size} 条记录",
                )
            }

            if (state.entries.isEmpty()) {
                item(key = "empty") {
                    EmptyState(
                        emoji = "🫧",
                        title = stringResource(R.string.day_detail_empty_title),
                        body = stringResource(R.string.day_detail_empty_body),
                    )
                }
            } else {
                items(items = state.entries, key = { it.id }) { entry ->
                    DiaryEntryCard(
                        entry = entry,
                        onClick = { onEntryClick(entry.id) },
                    )
                }
            }

            if (state.canAddEntry) {
                item(key = "add") {
                    Button(
                        onClick = onAddEntry,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(17.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SunsetInk,
                            contentColor = SunsetCream,
                        ),
                    ) {
                        Text(stringResource(R.string.day_detail_add))
                    }
                }
            }
        }
    }
}

@Composable
private fun DayOverview(state: DayDetailUiState) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(18.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = state.entries.size.toString(),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = SunsetInk,
                )
                Text(
                    text = "当天记录",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            state.summary.mood?.let {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "代表状态",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(5.dp))
                    MoodBadge(mood = it)
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(
    summary: DaySummaryUi,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = if (summary.isEmpty) MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
        else SunsetLamp.copy(alpha = 0.72f),
        contentColor = SunsetInk,
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(R.string.day_detail_summary),
                    style = MaterialTheme.typography.titleMedium,
                )
                summary.mood?.let { MoodBadge(mood = it) }
            }
            Spacer(Modifier.height(9.dp))
            if (summary.isEmpty) {
                Text(
                    text = "还没有总结。点这里给这一天收个尾。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SunsetInk.copy(alpha = 0.66f),
                )
            } else {
                if (summary.text.isNotBlank()) {
                    Text(text = summary.text, style = MaterialTheme.typography.bodyLarge)
                }
                if (summary.annoyance.isNotBlank() || summary.goodThing.isNotBlank()) {
                    Spacer(Modifier.height(12.dp))
                    SoftDivider()
                    Spacer(Modifier.height(12.dp))
                }
                if (summary.annoyance.isNotBlank()) {
                    SummaryLine(label = "最想吐槽", text = summary.annoyance)
                }
                if (summary.goodThing.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    SummaryLine(label = "还不错的事", text = summary.goodThing)
                }
            }
        }
    }
}

@Composable
private fun SummaryLine(label: String, text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = SunsetInk.copy(alpha = 0.66f),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
    }
}
