package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.Formatters
import com.example.ui.UploadAiHighlightSuggestion
import com.example.ui.UploadAiState
import com.example.ui.theme.AmberHighlight
import com.example.ui.theme.CyberMagenta
import com.example.ui.theme.EmeraldReady
import com.example.ui.theme.RoyalViolet

@Composable
fun UploadAiHighlightCard(
    transcriptInput: String,
    consentGranted: Boolean,
    aiState: UploadAiState,
    onTranscriptChange: (String) -> Unit,
    onImportTranscriptClick: () -> Unit,
    onConsentChange: (Boolean) -> Unit,
    onAnalyzeClick: () -> Unit,
    onApproveSuggestion: (UploadAiHighlightSuggestion) -> Unit,
    onDismissSuggestion: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    StudioSurfaceCard(
        borderColor = CyberMagenta.copy(alpha = 0.45f),
        modifier = modifier
            .fillMaxWidth()
            .testTag("upload_ai_highlight_card")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        tint = CyberMagenta,
                        modifier = Modifier.size(20.dp)
                    )
                    Column {
                        Text(
                            text = "AI Highlight Detection",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Analyze transcript & review suggested timestamps before exporting",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                StudioPillBadge(
                    text = "OPTIONAL",
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    useMonoFont = true
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Video Transcript (.txt / .srt)",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                TextButton(
                    onClick = onImportTranscriptClick,
                    modifier = Modifier.testTag("upload_ai_import_transcript_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.UploadFile,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Load File")
                }
            }

            OutlinedTextField(
                value = transcriptInput,
                onValueChange = onTranscriptChange,
                placeholder = {
                    Text("Paste your transcript with timestamps (e.g. 00:15 Wait until you see this…) to detect real highlights")
                },
                minLines = 3,
                maxLines = 5,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("upload_ai_transcript_input")
            )

            // Explicit privacy consent checkbox (private videos are never uploaded)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = consentGranted,
                    onCheckedChange = onConsentChange,
                    colors = CheckboxDefaults.colors(
                        checkedColor = RoyalViolet,
                        uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.testTag("upload_ai_consent_checkbox")
                )
                Text(
                    text = "I consent to send only this transcript text for AI analysis. My private video stays 100% local on-device.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
            }

            GradientPrimaryButton(
                text = if (aiState is UploadAiState.Loading) {
                    "Analyzing Transcript with AI…"
                } else {
                    "Detect AI Highlights"
                },
                icon = Icons.Rounded.AutoAwesome,
                enabled = aiState !is UploadAiState.Loading,
                onClick = onAnalyzeClick,
                testTag = "upload_ai_detect_button",
                modifier = Modifier.fillMaxWidth()
            )

            AnimatedVisibility(visible = aiState is UploadAiState.Loading) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = CyberMagenta
                    )
                    Text(
                        text = "Scanning transcript for real high-retention moments and timestamps…",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            if (aiState is UploadAiState.Error) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("upload_ai_error_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = aiState.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            if (aiState is UploadAiState.Success) {
                if (aiState.suggestions.isEmpty()) {
                    Text(
                        text = "All suggested highlights reviewed.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        text = "Review & Approve Suggested Clips (${aiState.suggestions.size})",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        aiState.suggestions.forEach { suggestion ->
                            AiSuggestionReviewItem(
                                suggestion = suggestion,
                                onApprove = { onApproveSuggestion(suggestion) },
                                onDismiss = { onDismissSuggestion(suggestion.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AiSuggestionReviewItem(
    suggestion: UploadAiHighlightSuggestion,
    onApprove: () -> Unit,
    onDismiss: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.72f),
        border = BorderStroke(
            width = 1.dp,
            color = if (suggestion.approved) EmeraldReady.copy(alpha = 0.65f) else RoyalViolet.copy(alpha = 0.35f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("upload_ai_suggestion_${suggestion.id}")
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = suggestion.title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                StudioPillBadge(
                    text = "${suggestion.viralScore}% Viral",
                    containerColor = AmberHighlight.copy(alpha = 0.18f),
                    contentColor = AmberHighlight,
                    useMonoFont = true
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                StudioPillBadge(
                    text = "${Formatters.formatDuration(suggestion.startMs)} - ${Formatters.formatDuration(suggestion.endMs)}",
                    useMonoFont = true
                )
                StudioPillBadge(
                    text = Formatters.formatDuration(suggestion.durationMs),
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    useMonoFont = true
                )
            }

            if (suggestion.reason.isNotBlank()) {
                Text(
                    text = suggestion.reason,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (suggestion.suggestedSubtitle.isNotBlank()) {
                Text(
                    text = "Caption: \"${suggestion.suggestedSubtitle}\"",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (suggestion.approved) {
                    StudioPillBadge(
                        text = "APPROVED • IN QUEUE",
                        containerColor = EmeraldReady.copy(alpha = 0.18f),
                        contentColor = EmeraldReady,
                        useMonoFont = true
                    )
                } else {
                    OutlinedButton(
                        onClick = onApprove,
                        border = BorderStroke(1.dp, EmeraldReady.copy(alpha = 0.7f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = EmeraldReady),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("upload_ai_approve_${suggestion.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Approve & Add", maxLines = 1)
                    }

                    OutlinedButton(
                        onClick = onDismiss,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier
                            .height(44.dp)
                            .testTag("upload_ai_dismiss_${suggestion.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Dismiss suggestion",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Dismiss")
                    }
                }
            }
        }
    }
}
