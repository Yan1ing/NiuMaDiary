package com.niumadiary.app.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.niumadiary.app.R
import com.niumadiary.app.ui.components.GlassCard
import com.niumadiary.app.ui.components.PageHeader
import com.niumadiary.app.ui.components.SoftDivider
import com.niumadiary.app.ui.components.SunsetBackdrop
import com.niumadiary.app.ui.components.SunsetBackdropStyle
import com.niumadiary.app.ui.theme.SunsetCoral
import com.niumadiary.app.ui.theme.SunsetInk
import com.niumadiary.app.ui.theme.SunsetLamp

data class SettingsUiState(
    val employmentDateLabel: String = "",
    val workHoursLabel: String = "",
    val timeZoneLabel: String = "",
    val followOfficialMakeupDays: Boolean = false,
    val reminderEnabled: Boolean = false,
    val reminderTimeLabel: String = "",
    val privacyLockEnabled: Boolean = false,
    val privacyLockAvailable: Boolean = true,
    val holidayDataStatus: String = "",
    val appVersionLabel: String = "",
    val isExporting: Boolean = false,
    val isImporting: Boolean = false,
)

@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onEmploymentDateClick: () -> Unit,
    onWorkHoursClick: () -> Unit,
    onTimeZoneClick: () -> Unit,
    onFollowOfficialMakeupDaysChange: (Boolean) -> Unit,
    onReminderEnabledChange: (Boolean) -> Unit,
    onReminderTimeClick: () -> Unit,
    onPrivacyLockChange: (Boolean) -> Unit,
    onExportClick: () -> Unit,
    onImportClick: () -> Unit,
    onHolidayDataClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
) {
    SunsetBackdrop(
        style = SunsetBackdropStyle.SETTINGS,
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
                    title = stringResource(R.string.settings_title),
                    subtitle = stringResource(R.string.settings_subtitle),
                )
            }

            item(key = "schedule") {
                SettingsSection(title = stringResource(R.string.settings_schedule)) {
                    SettingsRow(
                        emoji = "💼",
                        title = stringResource(R.string.settings_employment_date),
                        value = state.employmentDateLabel,
                        emptyValue = "未设置",
                        onClick = onEmploymentDateClick,
                    )
                    SoftDivider(Modifier.padding(start = 56.dp))
                    SettingsRow(
                        emoji = "🕘",
                        title = stringResource(R.string.settings_work_hours),
                        value = state.workHoursLabel,
                        emptyValue = "09:00–18:00",
                        onClick = onWorkHoursClick,
                    )
                    SoftDivider(Modifier.padding(start = 56.dp))
                    SettingsRow(
                        emoji = "🌏",
                        title = stringResource(R.string.settings_timezone),
                        value = state.timeZoneLabel,
                        emptyValue = "Asia/Shanghai",
                        onClick = onTimeZoneClick,
                    )
                    SoftDivider(Modifier.padding(start = 56.dp))
                    SettingsSwitchRow(
                        emoji = "📆",
                        title = stringResource(R.string.settings_makeup_days),
                        subtitle = stringResource(R.string.settings_makeup_days_hint),
                        checked = state.followOfficialMakeupDays,
                        onCheckedChange = onFollowOfficialMakeupDaysChange,
                    )
                }
            }

            item(key = "reminders") {
                SettingsSection(title = stringResource(R.string.settings_reminders_privacy)) {
                    SettingsSwitchRow(
                        emoji = "🔔",
                        title = stringResource(R.string.settings_summary_reminder),
                        subtitle = stringResource(R.string.settings_summary_reminder_hint),
                        checked = state.reminderEnabled,
                        onCheckedChange = onReminderEnabledChange,
                    )
                    if (state.reminderEnabled) {
                        SoftDivider(Modifier.padding(start = 56.dp))
                        SettingsRow(
                            emoji = "⏰",
                            title = stringResource(R.string.settings_reminder_time),
                            value = state.reminderTimeLabel,
                            emptyValue = "18:05",
                            onClick = onReminderTimeClick,
                        )
                    }
                    SoftDivider(Modifier.padding(start = 56.dp))
                    SettingsSwitchRow(
                        emoji = "🔐",
                        title = stringResource(R.string.settings_privacy_lock),
                        subtitle = if (state.privacyLockAvailable) {
                            stringResource(R.string.settings_privacy_lock_hint)
                        } else {
                            "这台设备暂不支持可用的身份验证"
                        },
                        checked = state.privacyLockEnabled,
                        enabled = state.privacyLockAvailable,
                        onCheckedChange = onPrivacyLockChange,
                    )
                }
            }

            item(key = "localNotice") {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = SunsetLamp.copy(alpha = 0.68f),
                    contentColor = SunsetInk,
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(11.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Text(text = "🧳")
                        Text(
                            text = stringResource(R.string.settings_local_notice),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            item(key = "data") {
                SettingsSection(title = stringResource(R.string.settings_data)) {
                    SettingsRow(
                        emoji = "📤",
                        title = if (state.isExporting) "正在导出…" else stringResource(R.string.settings_export),
                        subtitle = stringResource(R.string.settings_export_hint),
                        onClick = if (state.isExporting) null else onExportClick,
                    )
                    SoftDivider(Modifier.padding(start = 56.dp))
                    SettingsRow(
                        emoji = "📥",
                        title = if (state.isImporting) "正在导入…" else stringResource(R.string.settings_import),
                        subtitle = stringResource(R.string.settings_import_hint),
                        onClick = if (state.isImporting) null else onImportClick,
                    )
                    SoftDivider(Modifier.padding(start = 56.dp))
                    SettingsRow(
                        emoji = "🗓️",
                        title = stringResource(R.string.settings_holiday_data),
                        value = state.holidayDataStatus,
                        emptyValue = "查看状态",
                        onClick = onHolidayDataClick,
                    )
                }
            }

            item(key = "about") {
                SettingsSection(title = stringResource(R.string.settings_about)) {
                    SettingsRow(
                        emoji = "🐈",
                        title = stringResource(R.string.app_name),
                        subtitle = stringResource(R.string.settings_privacy_statement),
                        value = state.appVersionLabel,
                        emptyValue = "",
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(content = content)
        }
    }
}

@Composable
private fun SettingsRow(
    emoji: String,
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String = "",
    value: String = "",
    emptyValue: String = "",
    onClick: (() -> Unit)? = null,
) {
    val clickableModifier = if (onClick == null) modifier else modifier.clickable(onClick = onClick)
    Row(
        modifier = clickableModifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SettingEmoji(emoji)
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        val shownValue = value.ifBlank { emptyValue }
        if (shownValue.isNotBlank()) {
            Text(
                text = shownValue,
                style = MaterialTheme.typography.labelLarge,
                color = if (value.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant else SunsetCoral,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (onClick != null) {
            Text(
                text = "›",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SettingsSwitchRow(
    emoji: String,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SettingEmoji(emoji)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (enabled) 1f else 0.46f),
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (enabled) 1f else 0.46f),
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
        )
    }
}

@Composable
private fun SettingEmoji(emoji: String) {
    Surface(
        modifier = Modifier.size(42.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.78f),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Text(text = emoji)
        }
    }
}
