package com.niumadiary.app.feature.calendar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.time.Year

@Composable
fun YearMonthPickerDialog(
    initialYear: Int,
    initialMonth: Int,
    onDismiss: () -> Unit,
    onConfirm: (year: Int, month: Int) -> Unit,
) {
    val safeInitialYear = initialYear.takeIf { it > 0 } ?: Year.now().value
    val safeInitialMonth = initialMonth.coerceIn(1, 12)
    var selectedYear by rememberSaveable(initialYear) { mutableIntStateOf(safeInitialYear) }
    var selectedMonth by rememberSaveable(initialMonth) { mutableIntStateOf(safeInitialMonth) }
    var yearMenuExpanded by rememberSaveable { mutableStateOf(false) }
    val currentYear = Year.now().value
    val yearRange = (minOf(currentYear, safeInitialYear) - 60)..
        (maxOf(currentYear, safeInitialYear) + 40)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "快速跳转月份") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    IconButton(onClick = { selectedYear -= 1 }) {
                        Text(text = "‹", style = MaterialTheme.typography.headlineSmall)
                    }
                    Box {
                        Surface(
                            onClick = { yearMenuExpanded = true },
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                        ) {
                            Text(
                                text = "$selectedYear 年  ⌄",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                            )
                        }
                        DropdownMenu(
                            expanded = yearMenuExpanded,
                            onDismissRequest = { yearMenuExpanded = false },
                        ) {
                            yearRange.reversed().forEach { year ->
                                DropdownMenuItem(
                                    text = { Text(text = "$year 年") },
                                    onClick = {
                                        selectedYear = year
                                        yearMenuExpanded = false
                                    },
                                )
                            }
                        }
                    }
                    IconButton(onClick = { selectedYear += 1 }) {
                        Text(text = "›", style = MaterialTheme.typography.headlineSmall)
                    }
                }

                Spacer(Modifier.height(16.dp))
                (1..12).chunked(4).forEach { rowMonths ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        rowMonths.forEach { month ->
                            Surface(
                                onClick = { selectedMonth = month },
                                shape = RoundedCornerShape(13.dp),
                                color = if (selectedMonth == month) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                                },
                                modifier = Modifier.weight(1f),
                            ) {
                                Text(
                                    text = "$month 月",
                                    style = MaterialTheme.typography.labelLarge,
                                    textAlign = TextAlign.Center,
                                    fontWeight = if (selectedMonth == month) {
                                        FontWeight.Bold
                                    } else {
                                        FontWeight.Normal
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp),
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selectedYear, selectedMonth) }) {
                Text(text = "跳转")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "取消")
            }
        },
    )
}
