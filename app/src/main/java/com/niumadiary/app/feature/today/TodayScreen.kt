package com.niumadiary.app.feature.today

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.niumadiary.app.R
import com.niumadiary.app.ui.components.DiaryEntryCard
import com.niumadiary.app.ui.components.EmptyState
import com.niumadiary.app.ui.components.GlassCard
import com.niumadiary.app.ui.components.MoodChoice
import com.niumadiary.app.ui.components.SectionLabel
import com.niumadiary.app.ui.components.SunsetBackdrop
import com.niumadiary.app.ui.components.SunsetBackdropStyle
import com.niumadiary.app.ui.model.DaySummaryUi
import com.niumadiary.app.ui.model.DiaryEntryUi
import com.niumadiary.app.ui.model.MoodLevel
import com.niumadiary.app.ui.model.TagUi
import com.niumadiary.app.ui.theme.SunsetCoral
import com.niumadiary.app.ui.theme.SunsetCream
import com.niumadiary.app.ui.theme.SunsetInk
import com.niumadiary.app.ui.theme.SunsetLamp

data class TodayUiState(
    val dateLabel: String = "",
    val greeting: String = "",
    val countdownLabel: String = "",
    val countdownValue: String = "",
    val countdownSupportingText: String = "",
    val isWorkday: Boolean = true,
    val weeklyProgress: WeeklyWorkProgressUi = WeeklyWorkProgressUi(),
    val selectedMood: MoodLevel? = null,
    val draftText: String = "",
    val availableTags: List<TagUi> = emptyList(),
    val selectedTagIds: Set<String> = emptySet(),
    val entries: List<DiaryEntryUi> = emptyList(),
    val summary: DaySummaryUi = DaySummaryUi(),
    val showSummaryEmphasis: Boolean = false,
    val isSaving: Boolean = false,
    val feedbackMessage: String? = null,
) {
    val canSave: Boolean
        get() = !isSaving && (draftText.isNotBlank() || selectedMood != null)
}

data class WeeklyWorkProgressUi(
    val completedLabel: String = "0小时",
    val remainingLabel: String = "0小时",
    val totalLabel: String = "0小时",
    val percentageLabel: String = "0.0%",
    val scheduleLabel: String = "",
    val fraction: Float = 0f,
)

@Composable
fun TodayScreen(
    state: TodayUiState,
    onDraftChange: (String) -> Unit,
    onMoodSelected: (MoodLevel) -> Unit,
    onTagToggle: (String) -> Unit,
    onSave: () -> Unit,
    onEntryClick: (String) -> Unit,
    onSummaryClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
) {
    SunsetBackdrop(
        style = SunsetBackdropStyle.TODAY,
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
            item(key = "hero") {
                TodayHero(
                    dateLabel = state.dateLabel,
                    greeting = state.greeting,
                )
            }

            item(key = "countdown") {
                CountdownCard(state = state)
            }

            item(key = "weeklyProgress") {
                WeeklyWorkProgressCard(progress = state.weeklyProgress)
            }

            item(key = "composer") {
                QuickComposer(
                    state = state,
                    onDraftChange = onDraftChange,
                    onMoodSelected = onMoodSelected,
                    onTagToggle = onTagToggle,
                    onSave = onSave,
                )
            }

            item(key = "summary") {
                DaySummaryCard(
                    summary = state.summary,
                    emphasized = state.showSummaryEmphasis,
                    onClick = onSummaryClick,
                )
            }

            item(key = "timelineTitle") {
                SectionLabel(
                    title = stringResource(R.string.today_timeline),
                    trailing = if (state.entries.isEmpty()) "" else "${state.entries.size} 条",
                    modifier = Modifier.padding(top = 2.dp),
                )
            }

            if (state.entries.isEmpty()) {
                item(key = "empty") {
                    EmptyState(
                        emoji = "☁️",
                        title = stringResource(R.string.today_empty_title),
                        body = stringResource(R.string.today_empty_body),
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
        }
    }
}

@Composable
private fun WeeklyWorkProgressCard(progress: WeeklyWorkProgressUi) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(
                        text = "本周工时进度",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = "本周总量 ${progress.totalLabel}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Surface(
                    shape = CircleShape,
                    color = SunsetCoral.copy(alpha = 0.18f),
                ) {
                    Text(
                        text = progress.percentageLabel,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = SunsetInk,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    )
                }
            }

            LinearProgressIndicator(
                progress = { progress.fraction.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(CircleShape),
                color = SunsetCoral,
                trackColor = SunsetCoral.copy(alpha = 0.14f),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                WorkProgressValue(label = "已经上了", value = progress.completedLabel)
                WorkProgressValue(
                    label = "还剩",
                    value = progress.remainingLabel,
                    horizontalAlignment = Alignment.End,
                )
            }
            if (progress.scheduleLabel.isNotBlank()) {
                Text(
                    text = progress.scheduleLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun WorkProgressValue(
    label: String,
    value: String,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
) {
    Column(horizontalAlignment = horizontalAlignment) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun TodayHero(
    dateLabel: String,
    greeting: String,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = dateLabel,
            style = MaterialTheme.typography.labelLarge,
            color = SunsetCream.copy(alpha = 0.84f),
        )
        Spacer(Modifier.height(5.dp))
        Text(
            text = stringResource(R.string.today_title),
            style = MaterialTheme.typography.displaySmall,
            color = SunsetCream,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = greeting.ifBlank { stringResource(R.string.today_default_greeting) },
            style = MaterialTheme.typography.bodyLarge,
            color = SunsetCream.copy(alpha = 0.88f),
        )
    }
}

@Composable
private fun CountdownCard(state: TodayUiState) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        color = SunsetInk.copy(alpha = 0.78f),
        contentColor = SunsetCream,
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(15.dp),
        ) {
            Image(
                painter = painterResource(R.drawable.hajimi_cat),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape),
            )
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = state.countdownLabel,
                        style = MaterialTheme.typography.labelLarge,
                        color = SunsetCream.copy(alpha = 0.78f),
                    )
                    Surface(
                        shape = CircleShape,
                        color = if (state.isWorkday) SunsetCoral.copy(alpha = 0.28f)
                        else SunsetLamp.copy(alpha = 0.25f),
                    ) {
                        Text(
                            text = if (state.isWorkday) "上班日" else "休息日",
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = state.countdownValue,
                    style = MaterialTheme.typography.headlineSmall,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                )
                if (state.countdownSupportingText.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = state.countdownSupportingText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = SunsetCream.copy(alpha = 0.72f),
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickComposer(
    state: TodayUiState,
    onDraftChange: (String) -> Unit,
    onMoodSelected: (MoodLevel) -> Unit,
    onTagToggle: (String) -> Unit,
    onSave: () -> Unit,
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(vertical = 18.dp)) {
            Column(modifier = Modifier.padding(horizontal = 18.dp)) {
                Text(
                    text = stringResource(R.string.today_mood_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = stringResource(R.string.today_mood_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.height(12.dp))
            LazyRow(
                contentPadding = PaddingValues(horizontal = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(items = MoodLevel.entries, key = { it.value }) { mood ->
                    MoodChoice(
                        mood = mood,
                        selected = state.selectedMood == mood,
                        onClick = { onMoodSelected(mood) },
                    )
                }
            }

            Spacer(Modifier.height(15.dp))
            OutlinedTextField(
                value = state.draftText,
                onValueChange = onDraftChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp),
                placeholder = { Text(stringResource(R.string.today_note_hint)) },
                minLines = 3,
                maxLines = 7,
                shape = RoundedCornerShape(20.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SunsetCoral,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                ),
            )

            if (state.availableTags.isNotEmpty()) {
                Spacer(Modifier.height(13.dp))
                Text(
                    text = stringResource(R.string.today_tags),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(horizontal = 18.dp),
                )
                Spacer(Modifier.height(7.dp))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 18.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(items = state.availableTags, key = { it.id }) { tag ->
                        FilterChip(
                            selected = tag.id in state.selectedTagIds,
                            onClick = { onTagToggle(tag.id) },
                            label = { Text("${tag.emoji}${tag.label}") },
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onSave,
                enabled = state.canSave,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp)
                    .height(52.dp),
                shape = RoundedCornerShape(17.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SunsetInk,
                    contentColor = SunsetCream,
                ),
            ) {
                Text(
                    text = when {
                        state.isSaving -> stringResource(R.string.today_saving)
                        state.draftText.isBlank() && state.selectedMood != null -> {
                            stringResource(R.string.today_save_mood)
                        }
                        else -> stringResource(R.string.today_save_note)
                    },
                )
            }

            AnimatedVisibility(
                visible = !state.feedbackMessage.isNullOrBlank(),
                enter = fadeIn() + slideInVertically { it / 2 },
                exit = fadeOut(),
            ) {
                Text(
                    text = state.feedbackMessage.orEmpty(),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 18.dp, top = 12.dp, end = 18.dp),
                )
            }
        }
    }
}

@Composable
private fun DaySummaryCard(
    summary: DaySummaryUi,
    emphasized: Boolean,
    onClick: () -> Unit,
) {
    val containerColor = if (emphasized) SunsetLamp.copy(alpha = 0.90f)
    else MaterialTheme.colorScheme.surface.copy(alpha = 0.93f)
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = containerColor,
        contentColor = SunsetInk,
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            horizontalArrangement = Arrangement.spacedBy(13.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = if (summary.isEmpty) "🧾" else summary.mood?.emoji ?: "📝")
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.today_summary_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = if (summary.isEmpty) stringResource(R.string.today_summary_empty) else summary.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = SunsetInk.copy(alpha = 0.72f),
                    maxLines = 2,
                )
            }
            Text(
                text = stringResource(
                    if (summary.isEmpty) R.string.today_summary_add else R.string.today_summary_edit,
                ),
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}
