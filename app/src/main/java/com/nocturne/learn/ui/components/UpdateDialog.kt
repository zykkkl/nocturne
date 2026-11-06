package com.nocturne.learn.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.nocturne.learn.data.update.UpdateKind
import com.nocturne.learn.data.update.UpdateState
import com.nocturne.learn.ui.design.ChampagneButton
import com.nocturne.learn.ui.design.GhostButton
import com.nocturne.learn.ui.design.JarvisOrb
import com.nocturne.learn.ui.theme.NocturneColors

@Composable
fun UpdateDialog(
    visible: Boolean,
    state: UpdateState,
    onDownload: () -> Unit,
    onInstall: () -> Unit,
    onDismiss: () -> Unit,
    onRetry: () -> Unit,
) {
    if (!visible) return
    if (state is UpdateState.Idle) return
    if (state is UpdateState.Checking && !state.manual) return

    val info = when (state) {
        is UpdateState.Available -> state.info
        is UpdateState.Downloading -> state.info
        is UpdateState.Ready -> state.info
        else -> null
    }

    Dialog(
        onDismissRequest = {
            if (state !is UpdateState.Downloading) onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 26.dp)
                .shadow(24.dp, RoundedCornerShape(26.dp))
                .clip(RoundedCornerShape(26.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(NocturneColors.Night900, NocturneColors.Night950),
                    ),
                )
                .border(
                    1.dp,
                    Color.White.copy(alpha = 0.14f),
                    RoundedCornerShape(26.dp),
                )
                .padding(22.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                JarvisOrb(diameter = 38.dp, active = state !is UpdateState.UpToDate)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = titleFor(state),
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                    )
                    if (info != null) {
                        Text(
                            text = "v${info.version} · ${formatSize(info.sizeBytes)} · ${info.kind.label}",
                            style = MaterialTheme.typography.labelSmall,
                            color = NocturneColors.ChampagneLight,
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            Text(
                text = bodyFor(state),
                style = MaterialTheme.typography.bodySmall,
                color = NocturneColors.TextSecondary,
            )

            val notes = info?.notes.orEmpty()
            if (notes.isNotBlank() && state !is UpdateState.Downloading) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "本次更新",
                    style = MaterialTheme.typography.labelSmall,
                    color = NocturneColors.TextTertiary,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = NocturneColors.TextSecondary,
                    maxLines = 8,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .heightIn(max = 168.dp)
                        .verticalScroll(rememberScrollState()),
                )
            }

            if (state is UpdateState.Downloading) {
                Spacer(Modifier.height(18.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .background(Color.White.copy(alpha = 0.10f), RoundedCornerShape(2.dp)),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(state.progress.coerceIn(0f, 1f))
                            .height(4.dp)
                            .background(NocturneColors.Champagne, RoundedCornerShape(2.dp)),
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "${(state.progress * 100).toInt()}% · 下载完成后由系统安装",
                    style = MaterialTheme.typography.labelSmall,
                    color = NocturneColors.TextTertiary,
                )
            }

            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                when (state) {
                    is UpdateState.Available -> {
                        GhostButton(
                            text = "稍后",
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                        )
                        ChampagneButton(
                            text = "下载更新",
                            onClick = onDownload,
                            modifier = Modifier.weight(1.35f),
                        )
                    }

                    is UpdateState.Downloading -> {
                        GhostButton(
                            text = "后台继续",
                            onClick = onDismiss,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    is UpdateState.Ready -> {
                        GhostButton(
                            text = "稍后",
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                        )
                        ChampagneButton(
                            text = "安装",
                            onClick = onInstall,
                            modifier = Modifier.weight(1.35f),
                        )
                    }

                    is UpdateState.UpToDate -> {
                        ChampagneButton(
                            text = "好",
                            onClick = onDismiss,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    is UpdateState.Failed -> {
                        GhostButton(
                            text = "关闭",
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                        )
                        ChampagneButton(
                            text = "重试",
                            onClick = onRetry,
                            modifier = Modifier.weight(1.35f),
                        )
                    }

                    is UpdateState.Checking -> {
                        GhostButton(
                            text = "取消",
                            onClick = onDismiss,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    UpdateState.Idle -> Unit
                }
            }

            if (info != null) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Android 不允许应用静默覆盖安装。最后一步会在系统安装器中确认；你的学习数据与 API Key 不会被替换。",
                    style = MaterialTheme.typography.labelSmall,
                    color = NocturneColors.TextTertiary,
                )
            }
        }
    }
}

private fun titleFor(state: UpdateState): String = when (state) {
    is UpdateState.Checking -> "正在检查更新…"
    is UpdateState.Available -> when (state.info.kind) {
        UpdateKind.PATCH -> "发现小更新"
        UpdateKind.MINOR -> "发现功能更新"
        UpdateKind.MAJOR -> "发现大版本更新"
    }
    is UpdateState.Downloading -> "正在下载更新"
    is UpdateState.Ready -> "可以安装了"
    is UpdateState.UpToDate -> "已是最新版本"
    is UpdateState.Failed -> "更新没有完成"
    UpdateState.Idle -> ""
}

private fun bodyFor(state: UpdateState): String = when (state) {
    is UpdateState.Checking -> "正在向 GitHub 查询夜航的最新版本。"
    is UpdateState.Available ->
        "夜航 v${state.info.version} 已发布。更新不会清除航线、知识点或密钥；具体变更见下方更新说明。"
    is UpdateState.Downloading -> "正在从 GitHub Releases 下载签名安装包。"
    is UpdateState.Ready -> "安装包已就绪。点击「安装」后，在系统弹窗中确认即可覆盖升级。"
    is UpdateState.UpToDate -> "当前 v${state.version} 已是目前发布的最新版本。"
    is UpdateState.Failed -> state.message
    UpdateState.Idle -> ""
}

private fun formatSize(bytes: Long): String {
    if (bytes <= 0L) return "大小未知"
    val mb = bytes / (1024.0 * 1024.0)
    return if (mb >= 1.0) String.format("%.1f MB", mb) else "${bytes / 1024} KB"
}
