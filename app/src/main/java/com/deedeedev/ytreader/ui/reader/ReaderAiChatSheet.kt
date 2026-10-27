package com.deedeedev.ytreader.ui.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.deedeedev.ytreader.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ReaderAiChatSheet(
    visible: Boolean,
    subtitleTitle: String,
    messages: List<ReaderAiChatMessage>,
    isSending: Boolean,
    errorSummary: String?,
    errorDetails: String?,
    selectedFragment: String?,
    onDismiss: () -> Unit,
    onDismissError: () -> Unit,
    onClearChat: () -> Unit,
    onClearSelection: () -> Unit,
    onSend: (String) -> Unit,
    onCopyMessage: (String) -> Unit
) {
    if (!visible) return

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var draft by remember { mutableStateOf("") }
    var detailsExpanded by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val selectionPrompt = stringResourceSafe(R.string.reader_ai_chat_selection_prompt)
    val title = stringResourceSafe(R.string.reader_ai_chat_title)
    val placeholder = stringResourceSafe(R.string.reader_ai_chat_input_placeholder)
    val thinking = stringResourceSafe(R.string.reader_ai_chat_thinking)
    val copyLabel = stringResourceSafe(R.string.reader_ai_chat_copy_response)
    val clearLabel = stringResourceSafe(R.string.reader_ai_chat_clear)
    val detailsLabel = stringResourceSafe(R.string.reader_ai_chat_error_details)
    val selectionLabel = stringResourceSafe(R.string.reader_ai_chat_selected_fragment)
    val quickPrompts = listOf(
        stringResourceSafe(R.string.reader_ai_chat_quick_summary),
        stringResourceSafe(R.string.reader_ai_chat_quick_key_points),
        stringResourceSafe(R.string.reader_ai_chat_quick_explain)
    )
    val quickPromptMessages = listOf(
        stringResourceSafe(R.string.reader_ai_chat_quick_summary_prompt),
        stringResourceSafe(R.string.reader_ai_chat_quick_key_points_prompt),
        stringResourceSafe(R.string.reader_ai_chat_quick_explain_prompt)
    )
    val fragment = selectedFragment?.trim()?.takeIf { it.isNotEmpty() }
    val details = errorDetails?.takeIf { it.isNotBlank() }

    LaunchedEffect(fragment) {
        if (fragment != null) {
            draft = selectionPrompt
        }
    }

    LaunchedEffect(messages.size, isSending, errorSummary) {
        val target = messages.size + if (isSending || errorSummary != null) 1 else 0
        if (target > 0) {
            listState.animateScrollToItem(target - 1)
        }
    }

    LaunchedEffect(details) {
        detailsExpanded = false
    }

    fun submit() {
        val text = draft.trim()
        if (text.isEmpty() || isSending) return
        onSend(text)
        draft = ""
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = subtitleTitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(
                    onClick = {
                        draft = ""
                        onClearChat()
                    },
                    enabled = messages.isNotEmpty() || errorSummary != null
                ) {
                    Icon(
                        imageVector = Icons.Filled.DeleteSweep,
                        contentDescription = clearLabel
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResourceSafe(R.string.reader_close)
                    )
                }
            }

            if (fragment != null) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Row(
                        modifier = Modifier.padding(start = 12.dp, top = 8.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = selectionLabel,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = fragment,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                maxLines = 4,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                        IconButton(
                            onClick = {
                                if (draft == selectionPrompt) {
                                    draft = ""
                                }
                                onClearSelection()
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = stringResourceSafe(R.string.reader_ai_chat_clear_selection),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(quickPrompts.size) { index ->
                    AssistChip(
                        onClick = {
                            if (!isSending) {
                                onSend(quickPromptMessages[index])
                            }
                        },
                        enabled = !isSending,
                        label = { Text(quickPrompts[index]) }
                    )
                }
            }

            if (messages.isEmpty() && !isSending && errorSummary == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Filled.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                            modifier = Modifier.size(40.dp)
                        )
                        Text(
                            text = stringResourceSafe(R.string.reader_ai_chat_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(messages, key = { it.id }) { message ->
                        ReaderAiChatBubble(
                            message = message,
                            onCopy = { onCopyMessage(message.content) },
                            copyLabel = copyLabel
                        )
                    }
                    if (isSending) {
                        item(key = "typing") {
                            Row(
                                modifier = Modifier
                                    .background(
                                        MaterialTheme.colorScheme.surfaceVariant,
                                        RoundedCornerShape(12.dp)
                                    )
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = thinking,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    if (errorSummary != null) {
                        item(key = "error") {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.errorContainer
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = errorSummary,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onErrorContainer,
                                            modifier = Modifier.weight(1f)
                                        )
                                        IconButton(
                                            onClick = {
                                                detailsExpanded = !detailsExpanded
                                            },
                                            enabled = details != null
                                        ) {
                                            Icon(
                                                imageVector = if (detailsExpanded) {
                                                    Icons.Filled.ExpandLess
                                                } else {
                                                    Icons.Filled.ExpandMore
                                                },
                                                contentDescription = detailsLabel,
                                                tint = MaterialTheme.colorScheme.onErrorContainer
                                            )
                                        }
                                        IconButton(onClick = onDismissError) {
                                            Icon(
                                                imageVector = Icons.Filled.Close,
                                                contentDescription = stringResourceSafe(R.string.reader_dismiss),
                                                tint = MaterialTheme.colorScheme.onErrorContainer
                                            )
                                        }
                                    }
                                    if (detailsExpanded && details != null) {
                                        SelectionContainer {
                                            Text(
                                                text = details,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f),
                                                modifier = Modifier.padding(top = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text(placeholder) },
                    maxLines = 4,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { submit() })
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = { submit() },
                    enabled = draft.isNotBlank() && !isSending,
                    modifier = Modifier.padding(bottom = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Send,
                        contentDescription = stringResourceSafe(R.string.reader_ai_chat_send)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun ReaderAiChatBubble(
    message: ReaderAiChatMessage,
    copyLabel: String,
    onCopy: () -> Unit
) {
    val isUser = message.role == ReaderAiChatRole.USER
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.92f),
            shape = RoundedCornerShape(
                topStart = 14.dp,
                topEnd = 14.dp,
                bottomStart = if (isUser) 14.dp else 2.dp,
                bottomEnd = if (isUser) 2.dp else 14.dp
            ),
            color = if (isUser) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                if (!isUser) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResourceSafe(R.string.reader_ai_chat_assistant_label),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = onCopy,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ContentCopy,
                                contentDescription = copyLabel,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
                SelectionContainer {
                    Text(
                        text = message.content,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isUser) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun stringResourceSafe(resId: Int): String = androidx.compose.ui.res.stringResource(resId)
