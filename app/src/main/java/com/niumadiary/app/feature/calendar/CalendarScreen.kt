package com.niumadiary.app.feature.calendar

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.niumadiary.app.R
import com.niumadiary.app.ui.components.EmptyState
import com.niumadiary.app.ui.components.GlassCard
import com.niumadiary.app.ui.components.PageHeader
import com.niumadiary.app.ui.components.SunsetBackdrop
import com.niumadiary.app.ui.components.SunsetBackdropStyle
import com.niumadiary.app.ui.components.accentColor
import com.niumadiary.app.ui.model.DayKind
import com.niumadiary.app.ui.model.MoodLevel
import com.niumadiary.app.ui.theme.BorderSoft
import com.niumadiary.app.ui.theme.InkMuted
import com.niumadiary.app.ui.theme.Paper
import com.niumadiary.app.ui.theme.SunsetCoral
import com.niumadiary.app.ui.theme.SunsetInk
import com.niumadiary.app.ui.theme.SunsetLamp

data class CalendarDayUi(
    val dateKey: String,
    val dayNumber: String,
    val isInCurrentMonth: Boolean,
    val isToday: Boolean = false,
    val isSelected: Boolean = false,
    val dayKind: DayKind = DayKind.WORKDAY,
    val mood: MoodLevel? = null,
    val recordCount: Int = 0,
    val holidayLabel: String = "",
)

data class CalendarUiState(
    val monthLabel: String = "",
    val displayedYear: Int = 0,
    val displayedMonth: Int = 0,
    val isCurrentMonth: Boolean = true,
    val weekdayLabels: List<String> = listOf("一", "二", "三", "四", "五", "六", "日"),
    val days: List<CalendarDayUi> = emptyList(),
    val recordCountThisMonth: Int = 0,
    val recordedDayCountThisMonth: Int = 0,
    val plannedWorkdayCountThisMonth: Int = 0,
    val isLoading: Boolean = false,
)

@Composable
fun CalendarScreen(
    state: CalendarUiState,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onCurrentMonth: () -> Unit,
    onMonthSelected: (year: Int, month: Int) -> Unit,
    onDayClick: (dateKey: String) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
) {
    var showYearMonthPicker by rememberSaveable { mutableStateOf(false) }

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
                top = 14.dp,
                end = 16.dp,
                bottom = contentPadding.calculateBottomPadding() + 28.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item(key = "header") {
                PageHeader(
                    title = stringResource(R.string.calendar_title),
                    subtitle = stringResource(R.string.calendar_subtitle),
                )
            }

            item(key = "calendar") {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp)) {
                        MonthSwitcher(
                            monthLabel = state.monthLabel,
                            isCurrentMonth = state.isCurrentMonth,
                            onPreviousMonth = onPreviousMonth,
                            onNextMonth = onNextMonth,
                            onCurrentMonth = onCurrentMonth,
                            onMonthLabelClick = { showYearMonthPicker = true },
                        )
                        Spacer(Modifier.height(10.dp))
                        WeekdayHeader(labels = state.weekdayLabels)
                        Spacer(Modifier.height(6.dp))
                        if (state.days.isEmpty()) {
                            EmptyState(
                                emoji = if (state.isLoading) "⏳" else "📅",
                                title = if (state.isLoading) "正在整理日历" else "这个月还没有准备好",
                                body = if (state.isLoading) "马上就好。" else "稍后再来看看。",
                            )
                        } else {
                            CalendarGrid(
                                days = state.days,
                                onDayClick = onDayClick,
                            )
                        }
                    }
                }
            }

            item(key = "legend") {
                CalendarLegend()
            }

            item(key = "monthOverview") {
                MonthOverview(state = state)
            }
        }
    }

    if (showYearMonthPicker) {
        YearMonthPickerDialog(
            initialYear = state.displayedYear,
            initialMonth = state.displayedMonth,
            onDismiss = { showYearMonthPicker = false },
            onConfirm = { year, month ->
                onMonthSelected(year, month)
                showYearMonthPicker = false
            },
        )
    }
}

@Composable
private fun MonthSwitcher(
    monthLabel: String,
    isCurrentMonth: Boolean,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onCurrentMonth: () -> Unit,
    onMonthLabelClick: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            IconButton(onClick = onPreviousMonth) {
                Text(
                    text = "‹",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.semantics { contentDescription = "上个月" },
                )
            }
            Surface(
                onClick = onMonthLabelClick,
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.62f),
                modifier = Modifier.semantics { contentDescription = "选择年份和月份" },
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = monthLabel,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(text = "⌄", style = MaterialTheme.typography.labelLarge)
                }
            }
            IconButton(onClick = onNextMonth) {
                Text(
                    text = "›",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.semantics { contentDescription = "下个月" },
                )
            }
        }
        if (!isCurrentMonth) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onCurrentMonth) {
                    Text(text = "回到本月")
                }
            }
        }
    }
}

@Composable
private fun WeekdayHeader(labels: List<String>) {
    Row(modifier = Modifier.fillMaxWidth()) {
        labels.take(7).forEachIndexed { index, label ->
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = if (index >= 5) SunsetCoral else MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun CalendarGrid(
    days: List<CalendarDayUi>,
    onDayClick: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        days.chunked(7).forEach { week ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                week.forEach { day ->
                    CalendarDayCell(
                        day = day,
                        onClick = { onDayClick(day.dateKey) },
                        modifier = Modifier.weight(1f),
                    )
                }
                repeat(7 - week.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun CalendarDayCell(
    day: CalendarDayUi,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val container = when {
        day.isSelected -> SunsetCoral.copy(alpha = 0.18f)
        day.isToday -> SunsetLamp.copy(alpha = 0.20f)
        day.mood != null -> day.mood.accentColor().copy(alpha = 0.11f)
        else -> Color.Transparent
    }
    val border = when {
        day.isSelected -> BorderStroke(2.dp, SunsetCoral)
        day.isToday -> BorderStroke(1.dp, SunsetLamp)
        else -> BorderStroke(1.dp, BorderSoft.copy(alpha = 0.55f))
    }
    val description = buildString {
        append(day.dateKey)
        append(if (day.dayKind == DayKind.WORKDAY) "，上班日" else "，休息日")
        day.mood?.let { append("，状态${it.label}") }
        if (day.recordCount > 0) append("，${day.recordCount}条记录")
    }

    Surface(
        onClick = onClick,
        enabled = day.isInCurrentMonth,
        modifier = modifier
            .height(66.dp)
            .semantics { contentDescription = description },
        shape = RoundedCornerShape(14.dp),
        color = container,
        border = border,
    ) {
        Column(
            modifier = Modifier.padding(vertical = 5.dp, horizontal = 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = day.dayNumber,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (day.isInCurrentMonth) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.28f),
                    fontWeight = if (day.isToday) FontWeight.Bold else FontWeight.Medium,
                )
                if (day.recordCount > 0) {
                    Spacer(Modifier.size(3.dp))
                    Surface(
                        modifier = Modifier.size(5.dp),
                        shape = CircleShape,
                        color = SunsetCoral,
                    ) {}
                }
            }
            Text(
                text = when {
                    !day.isInCurrentMonth -> ""
                    day.mood != null -> day.mood.emoji
                    day.dayKind == DayKind.REST_DAY -> "休"
                    else -> "·"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = if (day.dayKind == DayKind.REST_DAY) SunsetCoral else InkMuted,
                maxLines = 1,
            )
            Text(
                text = day.holidayLabel,
                style = MaterialTheme.typography.labelMedium,
                color = SunsetCoral,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun CalendarLegend() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LegendItem(marker = "●", label = stringResource(R.string.calendar_has_note), color = SunsetCoral)
        LegendItem(marker = "休", label = stringResource(R.string.calendar_rest_day), color = SunsetCoral)
        LegendItem(marker = "·", label = stringResource(R.string.calendar_workday), color = InkMuted)
    }
}

@Composable
private fun LegendItem(marker: String, label: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Text(text = marker, style = MaterialTheme.typography.labelMedium, color = color)
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.72f),
        )
    }
}

@Composable
private fun MonthOverview(state: CalendarUiState) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(text = "本月小结", style = MaterialTheme.typography.titleMedium)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OverviewMetric(
                    value = state.recordCountThisMonth.toString(),
                    label = "说了几句",
                    modifier = Modifier.weight(1f),
                )
                OverviewMetric(
                    value = state.recordedDayCountThisMonth.toString(),
                    label = "有记录的天",
                    modifier = Modifier.weight(1f),
                )
                OverviewMetric(
                    value = state.plannedWorkdayCountThisMonth.toString(),
                    label = "计划上班日",
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun OverviewMetric(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.74f),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = SunsetInk,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = SunsetInk.copy(alpha = 0.64f),
                textAlign = TextAlign.Center,
            )
        }
    }
}
