package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.Formatters
import com.example.ui.StagedClipDraft
import com.example.ui.theme.CyberMagenta
import com.example.ui.theme.RoyalViolet

@Composable
fun StagedClipEditDialog(
    draft: StagedClipDraft,
    maxVideoDurationMs: Long,
    onDismiss: () -> Unit,
    onSave: (title: String, subtitle: String, startMs: Long, endMs: Long) -> Unit
) {
    val safeMax = maxVideoDurationMs.coerceAtLeast(1_000L)
    var title by remember(draft.localId) { mutableStateOf(draft.title) }
    var subtitle by remember(draft.localId) { mutableStateOf(draft.subtitleText) }
    var startMs by remember(draft.localId) {
        mutableLongStateOf(draft.startMs.coerceIn(0L, (safeMax - 500L).coerceAtLeast(0L)))
    }
    var endMs by remember(draft.localId) {
        mutableLongStateOf(draft.endMs.coerceIn((startMs + 500L).coerceAtMost(safeMax), safeMax))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(22.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        title = {
            Text(
                text = "Edit Clip & Subtitles",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Clip Title") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("staged_edit_title_input")
                )

                OutlinedTextField(
                    value = subtitle,
                    onValueChange = { subtitle = it },
                    label = { Text("Editable Subtitles") },
                    placeholder = { Text("Add caption overlay text for this Short…") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("staged_edit_subtitle_input")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StudioPillBadge(
                        text = "IN ${Formatters.formatDuration(startMs)}",
                        useMonoFont = true
                    )
                    StudioPillBadge(
                        text = "LEN ${Formatters.formatDuration((endMs - startMs).coerceAtLeast(0L))}",
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        useMonoFont = true
                    )
                    StudioPillBadge(
                        text = "OUT ${Formatters.formatDuration(endMs)}",
                        useMonoFont = true
                    )
                }

                RangeSlider(
                    value = startMs.toFloat()..endMs.toFloat(),
                    onValueChange = { range ->
                        startMs = range.start.toLong()
                        endMs = range.endInclusive.toLong()
                            .coerceAtLeast((startMs + 500L).coerceAtMost(safeMax))
                    },
                    valueRange = 0f..safeMax.toFloat(),
                    colors = SliderDefaults.colors(
                        thumbColor = CyberMagenta,
                        activeTrackColor = RoyalViolet,
                        inactiveTrackColor = MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { startMs = (startMs - 1_000L).coerceAtLeast(0L) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("IN -1s")
                    }
                    OutlinedButton(
                        onClick = {
                            startMs = (startMs + 1_000L).coerceAtMost((endMs - 1_000L).coerceAtLeast(0L))
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("IN +1s")
                    }
                    OutlinedButton(
                        onClick = {
                            endMs = (endMs - 1_000L).coerceAtLeast((startMs + 1_000L).coerceAtMost(safeMax))
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("OUT -1s")
                    }
                    OutlinedButton(
                        onClick = { endMs = (endMs + 1_000L).coerceAtMost(safeMax) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("OUT +1s")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(title, subtitle, startMs, endMs)
                    onDismiss()
                },
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .testTag("staged_edit_save_button")
            ) {
                Text("Save Changes", color = CyberMagenta)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
