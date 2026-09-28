package com.niumadiary.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.niumadiary.app.ui.model.DiaryEntryUi
import com.niumadiary.app.ui.model.MoodLevel
import com.niumadiary.app.ui.theme.BorderSoft
import com.niumadiary.app.ui.theme.InkMuted
import com.niumadiary.app.ui.theme.MoodBroken
import com.niumadiary.app.ui.theme.MoodCharged
import com.niumadiary.app.ui.theme.MoodCaffeinated
import com.niumadiary.app.ui.theme.MoodFine
import com.niumadiary.app.ui.theme.MoodLegendary
import com.niumadiary.app.ui.theme.MoodNumb
import com.niumadiary.app.ui.theme.MoodOkay
import com.niumadiary.app.ui.theme.MoodRecovered
import com.niumadiary.app.ui.theme.Paper
import com.niumadiary.app.ui.theme.SunsetCoral
import com.niumadiary.app.ui.theme.SunsetInk

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.93f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.56f)),
        shadowElevation = 1.dp,
        content = content,
    )
}

@Composable
fun PageHeader(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    foreground: Color = MaterialTheme.colorScheme.onBackground,
    action: (@Composable RowScope.() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = foreground,
            )
            if (subtitle.isNotBlank()) {
                Spacer(Modifier.height(5.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = foreground.copy(alpha = 0.76f),
                )
            }
        }
        action?.invoke(this)
    }
}

fun MoodLevel.accentColor(): Color = when (this) {
    MoodLevel.BROKEN -> MoodBroken
    MoodLevel.NUMB -> MoodNumb
    MoodLevel.OKAY -> MoodOkay
    MoodLevel.FINE -> MoodFine
    MoodLevel.CHARGED -> MoodCharged
    MoodLevel.CAFFEINATED -> MoodCaffeinated
    MoodLevel.RECOVERED -> MoodRecovered
    MoodLevel.LEGENDARY -> MoodLegendary
}

@Composable
fun MoodChoice(
    mood: MoodLevel,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val container by animateColorAsState(
        targetValue = if (selected) mood.accentColor().copy(alpha = 0.22f) else Paper.copy(alpha = 0.76f),
        label = "moodContainer",
    )
    val borderWidth by animateDpAsState(
        targetValue = if (selected) 2.dp else 1.dp,
        animationSpec = spring(stiffness = 700f),
        label = "moodBorder",
    )

    Surface(
        modifier = modifier
            .sizeIn(minWidth = 112.dp, minHeight = 116.dp)
            .scale(if (selected) 1.04f else 1f)
            .semantics {
                contentDescription = "精神状态：${mood.label}"
                role = Role.RadioButton
            }
            .clickable(role = Role.RadioButton, onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = container,
        border = BorderStroke(borderWidth, if (selected) mood.accentColor() else BorderSoft),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            MoodAvatar(mood = mood, size = 50.dp)
            Spacer(Modifier.height(6.dp))
            Text(
                text = mood.label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = SunsetInk,
                maxLines = 1,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = mood.description,
                style = MaterialTheme.typography.labelSmall,
                color = InkMuted,
                maxLines = 2,
            )
        }
    }
}

@Composable
fun MoodAvatar(
    mood: MoodLevel,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 24.dp,
) {
    Image(
        painter = painterResource(mood.imageRes),
        contentDescription = mood.label,
        contentScale = ContentScale.Crop,
        modifier = modifier
            .size(size)
            .clip(CircleShape),
    )
}

@Composable
fun MoodBadge(
    mood: MoodLevel,
    modifier: Modifier = Modifier,
    showLabel: Boolean = true,
) {
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = mood.accentColor().copy(alpha = 0.18f),
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = if (showLabel) 10.dp else 7.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            MoodAvatar(mood = mood, size = 22.dp)
            if (showLabel) {
                Text(text = mood.label, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
fun DiaryEntryCard(
    entry: DiaryEntryUi,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val clickModifier = if (onClick == null) modifier else modifier.clickable(onClick = onClick)
    GlassCard(modifier = clickModifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(13.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = entry.timeLabel,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(7.dp))
                Box(
                    modifier = Modifier.size(9.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Surface(
                        modifier = Modifier.size(7.dp),
                        shape = CircleShape,
                        color = entry.mood?.accentColor() ?: SunsetCoral,
                    ) {}
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    entry.mood?.let { MoodBadge(mood = it) }
                    entry.tags.take(2).forEach { tag ->
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                        ) {
                            Text(
                                text = "${tag.emoji}${tag.label}",
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            )
                        }
                    }
                }

                if (entry.text.isNotBlank()) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = entry.text,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 5,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (entry.isEdited) {
                    Spacer(Modifier.height(7.dp))
                    Text(
                        text = "已编辑",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyState(
    emoji: String,
    title: String,
    body: String,
    modifier: Modifier = Modifier,
) {
    GlassCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(text = emoji, style = MaterialTheme.typography.displaySmall)
            Spacer(Modifier.height(9.dp))
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(5.dp))
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun SectionLabel(
    title: String,
    modifier: Modifier = Modifier,
    trailing: String = "",
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = title, style = MaterialTheme.typography.titleMedium)
        if (trailing.isNotBlank()) {
            Text(
                text = trailing,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun SoftDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(modifier = modifier, color = BorderSoft)
}
