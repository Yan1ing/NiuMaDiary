package com.niumadiary.app

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.niumadiary.app.feature.calendar.CalendarScreen
import com.niumadiary.app.feature.calendar.CalendarViewModel
import com.niumadiary.app.feature.daydetail.DayDetailScreen
import com.niumadiary.app.feature.daydetail.DayDetailViewModel
import com.niumadiary.app.feature.settings.SettingsScreen
import com.niumadiary.app.feature.settings.SettingsViewModel
import com.niumadiary.app.feature.stats.StatsScreen
import com.niumadiary.app.feature.stats.StatsViewModel
import com.niumadiary.app.feature.today.TodayScreen
import com.niumadiary.app.feature.today.TodayViewModel
import com.niumadiary.app.ui.NiumaAppScaffold
import com.niumadiary.app.ui.NiumaDestination
import com.niumadiary.app.ui.components.EntryEditorDialog
import com.niumadiary.app.ui.components.SummaryEditorDialog
import com.niumadiary.app.ui.theme.NiumaDiaryTheme
import dagger.hilt.android.AndroidEntryPoint
import java.time.LocalTime

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.isNavigationBarContrastEnforced = false
        setContent {
            NiumaDiaryTheme {
                NiumaDiaryApp()
            }
        }
    }
}

@Composable
private fun NiumaDiaryApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val route = backStackEntry?.destination?.route
    val currentDestination = NiumaDestination.entries.firstOrNull { it.route == route }
        ?: NiumaDestination.CALENDAR

    NiumaAppScaffold(
        currentDestination = currentDestination,
        onDestinationSelected = { destination ->
            navController.navigate(destination.route) {
                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        },
    ) { contentPadding ->
        NavHost(
            navController = navController,
            startDestination = NiumaDestination.TODAY.route,
            modifier = Modifier.fillMaxSize(),
        ) {
            composable(NiumaDestination.TODAY.route) {
                val viewModel: TodayViewModel = hiltViewModel()
                val state by viewModel.uiState.collectAsStateWithLifecycle()
                var showSummaryEditor by rememberSaveable { mutableStateOf(false) }
                var editingEntryId by rememberSaveable { mutableStateOf<String?>(null) }

                TodayScreen(
                    state = state,
                    onDraftChange = viewModel::onDraftChange,
                    onMoodSelected = viewModel::onMoodSelected,
                    onTagToggle = viewModel::onTagToggle,
                    onSave = viewModel::saveQuickRecord,
                    onEntryClick = { editingEntryId = it },
                    onSummaryClick = { showSummaryEditor = true },
                    contentPadding = contentPadding,
                )

                if (showSummaryEditor) {
                    SummaryEditorDialog(
                        initialText = state.summary.text,
                        initialMood = state.summary.mood,
                        onDismiss = { showSummaryEditor = false },
                        onSave = viewModel::saveSummary,
                    )
                }

                val editingEntry = state.entries.firstOrNull { it.id == editingEntryId }
                if (editingEntryId != null && editingEntry != null) {
                    EntryEditorDialog(
                        title = "修改这条记录",
                        availableTags = state.availableTags,
                        initialText = editingEntry.text,
                        initialMood = editingEntry.mood,
                        initialTagIds = editingEntry.tags.mapTo(mutableSetOf()) { it.id },
                        onDismiss = { editingEntryId = null },
                        onSave = { text, mood, tagIds ->
                            viewModel.updateEntry(editingEntry.id, text, mood, tagIds)
                        },
                        onDelete = { viewModel.deleteEntry(editingEntry.id) },
                    )
                }
            }

            composable(NiumaDestination.CALENDAR.route) {
                val viewModel: CalendarViewModel = hiltViewModel()
                val state by viewModel.uiState.collectAsStateWithLifecycle()
                CalendarScreen(
                    state = state,
                    onPreviousMonth = viewModel::onPreviousMonth,
                    onNextMonth = viewModel::onNextMonth,
                    onCurrentMonth = viewModel::onCurrentMonth,
                    onMonthSelected = viewModel::onMonthSelected,
                    onDayClick = { date -> navController.navigate("day/$date") },
                    contentPadding = contentPadding,
                )
            }

            composable(NiumaDestination.STATS.route) {
                val viewModel: StatsViewModel = hiltViewModel()
                val state by viewModel.uiState.collectAsStateWithLifecycle()
                StatsScreen(
                    state = state,
                    onRangeSelected = viewModel::onRangeSelected,
                    contentPadding = contentPadding,
                )
            }

            composable(NiumaDestination.SETTINGS.route) {
                val context = LocalContext.current
                val viewModel: SettingsViewModel = hiltViewModel()
                val state by viewModel.uiState.collectAsStateWithLifecycle()
                val preferences by viewModel.preferences.collectAsStateWithLifecycle()

                SettingsScreen(
                    state = state,
                    onEmploymentDateClick = {
                        val initial = preferences.internshipStartDate
                            ?: java.time.LocalDate.now(preferences.zoneId)
                        DatePickerDialog(
                            context,
                            { _, year, month, day ->
                                viewModel.setEmploymentStartDate(
                                    java.time.LocalDate.of(year, month + 1, day),
                                )
                            },
                            initial.year,
                            initial.monthValue - 1,
                            initial.dayOfMonth,
                        ).show()
                    },
                    onWorkHoursClick = {
                        showWorkHoursPicker(
                            context = context,
                            start = preferences.workStart,
                            end = preferences.workEnd,
                            onSelected = viewModel::setWorkHours,
                        )
                    },
                    onTimeZoneClick = {
                        Toast.makeText(context, "当前按 Asia/Shanghai 计算", Toast.LENGTH_SHORT).show()
                    },
                    onFollowOfficialMakeupDaysChange = viewModel::setFollowOfficialMakeupDays,
                    onReminderEnabledChange = { enabled ->
                        if (enabled) {
                            Toast.makeText(context, "提醒功能将在后续版本接入", Toast.LENGTH_SHORT).show()
                        } else {
                            viewModel.setReminderEnabled(false)
                        }
                    },
                    onReminderTimeClick = {
                        Toast.makeText(context, "计划在下班后 18:10 提醒", Toast.LENGTH_SHORT).show()
                    },
                    onPrivacyLockChange = {
                        Toast.makeText(context, "隐私锁将在后续版本接入", Toast.LENGTH_SHORT).show()
                    },
                    onExportClick = {
                        Toast.makeText(context, "导出功能将在后续版本接入", Toast.LENGTH_SHORT).show()
                    },
                    onImportClick = {
                        Toast.makeText(context, "导入功能将在后续版本接入", Toast.LENGTH_SHORT).show()
                    },
                    onHolidayDataClick = {
                        Toast.makeText(context, "已内置 2026 年法定节假日安排", Toast.LENGTH_SHORT).show()
                    },
                    contentPadding = contentPadding,
                )
            }

            composable(
                route = DAY_DETAIL_ROUTE,
                arguments = listOf(navArgument(DayDetailViewModel.DATE_ARGUMENT) {
                    type = NavType.StringType
                }),
            ) {
                val viewModel: DayDetailViewModel = hiltViewModel()
                val state by viewModel.uiState.collectAsStateWithLifecycle()
                val availableTags by viewModel.availableTags.collectAsStateWithLifecycle()
                var showEntryEditor by rememberSaveable { mutableStateOf(false) }
                var editingEntryId by rememberSaveable { mutableStateOf<String?>(null) }
                var showSummaryEditor by rememberSaveable { mutableStateOf(false) }

                DayDetailScreen(
                    state = state,
                    onBack = { navController.popBackStack() },
                    onAddEntry = {
                        editingEntryId = null
                        showEntryEditor = true
                    },
                    onEntryClick = {
                        editingEntryId = it
                        showEntryEditor = true
                    },
                    onSummaryClick = { showSummaryEditor = true },
                    contentPadding = contentPadding,
                )

                if (showSummaryEditor) {
                    SummaryEditorDialog(
                        initialText = state.summary.text,
                        initialMood = state.summary.mood,
                        onDismiss = { showSummaryEditor = false },
                        onSave = viewModel::saveSummary,
                    )
                }

                if (showEntryEditor) {
                    val entry = state.entries.firstOrNull { it.id == editingEntryId }
                    EntryEditorDialog(
                        title = if (entry == null) "补记这一刻" else "修改这条记录",
                        availableTags = availableTags,
                        initialText = entry?.text.orEmpty(),
                        initialMood = entry?.mood,
                        initialTagIds = entry?.tags?.mapTo(mutableSetOf()) { it.id }.orEmpty(),
                        onDismiss = { showEntryEditor = false },
                        onSave = { text, mood, tagIds ->
                            viewModel.saveEntry(editingEntryId, text, mood, tagIds)
                        },
                        onDelete = entry?.let { value ->
                            { viewModel.deleteEntry(value.id) }
                        },
                    )
                }
            }
        }
    }
}

private fun showWorkHoursPicker(
    context: android.content.Context,
    start: LocalTime,
    end: LocalTime,
    onSelected: (LocalTime, LocalTime) -> Unit,
) {
    TimePickerDialog(
        context,
        { _, startHour, startMinute ->
            val chosenStart = LocalTime.of(startHour, startMinute)
            TimePickerDialog(
                context,
                { _, endHour, endMinute ->
                    val chosenEnd = LocalTime.of(endHour, endMinute)
                    if (chosenStart < chosenEnd) {
                        onSelected(chosenStart, chosenEnd)
                    } else {
                        Toast.makeText(context, "下班时间要晚于上班时间", Toast.LENGTH_SHORT).show()
                    }
                },
                end.hour,
                end.minute,
                true,
            ).show()
        },
        start.hour,
        start.minute,
        true,
    ).show()
}

private const val DAY_DETAIL_ROUTE = "day/{date}"
