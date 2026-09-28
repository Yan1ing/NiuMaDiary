package com.niumadiary.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.niumadiary.app.ui.model.MoodLevel
import com.niumadiary.app.ui.model.TagUi

@Composable
fun EntryEditorDialog(
    title: String,
    availableTags: List<TagUi>,
    initialText: String = "",
    initialMood: MoodLevel? = null,
    initialTagIds: Set<String> = emptySet(),
    onDismiss: () -> Unit,
    onSave: (text: String, mood: MoodLevel?, tagIds: Set<String>) -> Unit,
    onDelete: (() -> Unit)? = null,
) {
    var text by remember(initialText) { mutableStateOf(initialText) }
    var mood by remember(initialMood) { mutableStateOf(initialMood) }
    var tagIds by remember(initialTagIds) { mutableStateOf(initialTagIds) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text("这一刻的精神状态")
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(MoodLevel.entries, key = MoodLevel::name) { option ->
                        MoodChoice(
                            mood = option,
                            selected = mood == option,
                            onClick = { mood = if (mood == option) null else option },
                        )
                    }
                }
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it.take(10_000) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("想吐槽点什么") },
                    minLines = 3,
                    maxLines = 8,
                )
                if (availableTags.isNotEmpty()) {
                    Text("标签")
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(availableTags, key = TagUi::id) { tag ->
                            FilterChip(
                                selected = tag.id in tagIds,
                                onClick = {
                                    tagIds = if (tag.id in tagIds) tagIds - tag.id else tagIds + tag.id
                                },
                                label = { Text("${tag.emoji}${tag.label}") },
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = text.isNotBlank() || mood != null,
                onClick = {
                    onSave(text.trim(), mood, tagIds)
                    onDismiss()
                },
            ) { Text("保存") }
        },
        dismissButton = {
            if (onDelete == null) {
                TextButton(onClick = onDismiss) { Text("取消") }
            } else {
                TextButton(
                    onClick = {
                        onDelete()
                        onDismiss()
                    },
                ) { Text("删除") }
            }
        },
    )
}

@Composable
fun SummaryEditorDialog(
    initialText: String,
    initialMood: MoodLevel?,
    onDismiss: () -> Unit,
    onSave: (text: String, mood: MoodLevel?) -> Unit,
) {
    var text by remember(initialText) { mutableStateOf(initialText) }
    var mood by remember(initialMood) { mutableStateOf(initialMood) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("今日收工报告") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text("给今天一个最终状态")
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(MoodLevel.entries, key = MoodLevel::name) { option ->
                        MoodChoice(
                            mood = option,
                            selected = mood == option,
                            onClick = { mood = if (mood == option) null else option },
                        )
                    }
                }
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it.take(20_000) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("今天最想留下的一句话") },
                    minLines = 4,
                    maxLines = 8,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(text.trim(), mood)
                    onDismiss()
                },
            ) { Text("归档") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}
