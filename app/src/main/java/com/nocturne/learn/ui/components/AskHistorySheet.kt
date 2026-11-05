package com.nocturne.learn.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nocturne.learn.data.ask.AskConversation
import com.nocturne.learn.ui.theme.NocturneColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** 追问历史：切换、删除单条、清空全部。所有数据只在本机。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AskHistorySheet(
    conversations: List<AskConversation>,
    activeId: String?,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
    onDelete: (String) -> Unit,
    onClearAll: () -> Unit,
) {
    var pendingDelete by remember { mutableStateOf<AskConversation?>(null) }
    var showClear by remember { mutableStateOf(false) }
    val visible = conversations.filter { it.messages.isNotEmpty() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = NocturneColors.Night900,
        contentColor = Color.White,
        scrimColor = Color.Black.copy(alpha = 0.55f),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 14.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.18f)),
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 22.dp)
                .padding(bottom = 22.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "追问历史",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "清空全部",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (visible.isEmpty()) {
                        NocturneColors.TextTertiary.copy(alpha = 0.35f)
                    } else {
                        NocturneColors.TextTertiary
                    },
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable(enabled = visible.isNotEmpty()) {
                            showClear = true
                        }
                        .padding(horizontal = 10.dp, vertical = 7.dp),
                )
            }
            Spacer(Modifier.height(5.dp))
            Text(
                text = "追问独立于学习航线，所有对话只保存在本机。",
                style = MaterialTheme.typography.bodySmall,
                color = NocturneColors.TextTertiary,
            )
            Spacer(Modifier.height(12.dp))

            if (visible.isEmpty()) {
                Text(
                    text = "还没有追问历史。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = NocturneColors.TextSecondary,
                    modifier = Modifier.padding(vertical = 20.dp),
                )
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 420.dp)) {
                    items(visible, key = { it.id }) { conversation ->
                        HistoryRow(
                            conversation = conversation,
                            active = conversation.id == activeId,
                            onClick = { onSelect(conversation.id) },
                            onDelete = { pendingDelete = conversation },
                        )
                        Hairline()
                    }
                }
            }
        }
    }

    pendingDelete?.let { conversation ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            containerColor = NocturneColors.Night900,
            titleContentColor = Color.White,
            textContentColor = NocturneColors.TextSecondary,
            shape = RoundedCornerShape(26.dp),
            title = {
                Text(
                    text = "删除这段追问？",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                )
            },
            text = {
                Text(
                    text = "「${conversation.displayTitle}」会从本机删除，图片和对话都无法恢复。",
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingDelete = null
                        onDelete(conversation.id)
                    },
                ) {
                    Text("删除", color = NocturneColors.ChampagneLight)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text("取消", color = NocturneColors.TextTertiary)
                }
            },
        )
    }

    if (showClear) {
        AlertDialog(
            onDismissRequest = { showClear = false },
            containerColor = NocturneColors.Night900,
            titleContentColor = Color.White,
            textContentColor = NocturneColors.TextSecondary,
            shape = RoundedCornerShape(26.dp),
            title = {
                Text(
                    text = "清空全部追问？",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                )
            },
            text = {
                Text(
                    text = "所有追问、图片都会从本机删除，无法恢复。学习航线和进度不受影响。",
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClear = false
                        onClearAll()
                        onDismiss()
                    },
                ) {
                    Text("清空", color = NocturneColors.ChampagneLight)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClear = false }) {
                    Text("取消", color = NocturneColors.TextTertiary)
                }
            },
        )
    }
}

@Composable
private fun HistoryRow(
    conversation: AskConversation,
    active: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = conversation.displayTitle,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (active) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "当前",
                        style = MaterialTheme.typography.labelSmall,
                        color = NocturneColors.ChampagneLight,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(NocturneColors.Champagne.copy(alpha = 0.12f))
                            .padding(horizontal = 7.dp, vertical = 2.dp),
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = "${conversation.messages.count { it.fromUser }} 个问题 · ${timeLabel(conversation.updatedAt)}",
                style = MaterialTheme.typography.labelSmall,
                color = NocturneColors.TextTertiary,
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = "删除",
            style = MaterialTheme.typography.labelSmall,
            color = NocturneColors.TextTertiary,
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .clickable(onClick = onDelete)
                .padding(horizontal = 10.dp, vertical = 8.dp),
        )
    }
}

private fun timeLabel(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    return when {
        diff < 60_000L -> "刚刚"
        diff < 3_600_000L -> "${diff / 60_000L} 分钟前"
        diff < 86_400_000L -> "${diff / 3_600_000L} 小时前"
        else -> SimpleDateFormat("M 月 d 日", Locale.getDefault()).format(Date(timestamp))
    }
}
