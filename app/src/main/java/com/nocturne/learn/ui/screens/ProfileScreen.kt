package com.nocturne.learn.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.nocturne.learn.data.AppSettings
import com.nocturne.learn.data.learning.LearningState
import com.nocturne.learn.data.update.UpdateState
import com.nocturne.learn.ui.components.Hairline
import com.nocturne.learn.ui.components.SettingRow
import com.nocturne.learn.ui.components.SubjectSwitcherSheet
import com.nocturne.learn.ui.components.TabHeader
import com.nocturne.learn.ui.design.GhostButton
import com.nocturne.learn.ui.design.GlassSurface
import com.nocturne.learn.ui.design.NocturneChip
import com.nocturne.learn.ui.design.NocturneTextField
import com.nocturne.learn.ui.design.RowDot
import com.nocturne.learn.ui.theme.NocturneColors
import com.nocturne.learn.ui.util.rememberAppVersion

@Composable
fun ProfileScreen(
    settings: AppSettings,
    learning: LearningState,
    subjects: List<LearningState>,
    activeSubjectId: String?,
    onOpenProviders: () -> Unit,
    onUpdatePlan: (String, Int) -> Unit,
    onSelectSubject: (String) -> Unit,
    onCreateSubject: (String, Int) -> Unit,
    onDeleteSubject: (String) -> Unit,
    updateState: UpdateState,
    onCheckForUpdates: () -> Unit,
) {
    var showPlanEditor by remember { mutableStateOf(false) }
    var showSubjects by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }
    val activeProvider = settings.activeProvider
    val appVersion = rememberAppVersion()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
    ) {
        Spacer(Modifier.height(18.dp))
        TabHeader(eyebrow = "PROFILE · 档案", title = "我的")
        Spacer(Modifier.height(8.dp))
        Text(
            text = "航线由你定义，模型由你选择。",
            style = MaterialTheme.typography.bodyMedium,
            color = NocturneColors.TextSecondary,
        )
        Spacer(Modifier.height(24.dp))

        if (subjects.isEmpty()) {
            GlassSurface {
                Text(
                    text = "还没有学习目标",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "建一条航线，夜航才知道该往哪里走。",
                    style = MaterialTheme.typography.bodySmall,
                    color = NocturneColors.TextSecondary,
                )
                Spacer(Modifier.height(14.dp))
                GhostButton(
                    text = "建立第一条航线",
                    onClick = { showSubjects = true },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        } else {
            GlassSurface(modifier = Modifier.clickable { showSubjects = true }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = learning.goal.ifBlank { "未设定航线" },
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = "切换",
                        style = MaterialTheme.typography.labelSmall,
                        color = NocturneColors.ChampagneLight,
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RowDot(NocturneColors.Champagne.copy(alpha = 0.85f))
                    Text(
                        text = "  每天 ${learning.dailyMinutes} 分钟 · ${learning.masteredCount()}/${learning.knowledgePoints.size} 个知识点",
                        style = MaterialTheme.typography.bodySmall,
                        color = NocturneColors.TextSecondary,
                    )
                }
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    GhostButton(
                        text = "切换目标",
                        onClick = { showSubjects = true },
                        modifier = Modifier.weight(1f),
                    )
                    GhostButton(
                        text = "调整航线",
                        onClick = { showPlanEditor = true },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        Spacer(Modifier.height(22.dp))
        Text(
            text = "服务",
            style = MaterialTheme.typography.labelSmall,
            color = NocturneColors.TextTertiary,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
        )
        GlassSurface(contentPadding = PaddingValues(horizontal = 18.dp, vertical = 4.dp)) {
            SettingRow(
                title = "模型服务",
                onClick = onOpenProviders,
                subtitle = activeProvider?.let { "${it.displayName} · ${it.model}" }
                    ?: "还没有接入模型",
            )
            Hairline()
            SettingRow(
                title = "检查更新",
                onClick = onCheckForUpdates,
                subtitle = updateSubtitle(updateState, appVersion),
                value = if (updateState is UpdateState.Available) "新版本" else null,
            )
            Hairline()
            SettingRow(
                title = "关于夜航",
                onClick = { showAbout = true },
                subtitle = "版本 $appVersion",
            )
        }

        Spacer(Modifier.height(22.dp))
        Text(
            text = "数据",
            style = MaterialTheme.typography.labelSmall,
            color = NocturneColors.TextTertiary,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
        )
        GlassSurface {
            Text(
                text = "学习数据只保存在这台设备",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "没有账号、没有云端。API Key 使用 Android Keystore 加密，卸载应用即随之销毁。",
                style = MaterialTheme.typography.bodySmall,
                color = NocturneColors.TextSecondary,
            )
        }
        Spacer(Modifier.height(110.dp))
    }

    if (showPlanEditor) {
        PlanEditorDialog(
            goal = learning.goal,
            minutes = learning.dailyMinutes,
            onDismiss = { showPlanEditor = false },
            onSave = { goal, minutes ->
                onUpdatePlan(goal, minutes)
                showPlanEditor = false
            },
        )
    }
    if (showSubjects) {
        SubjectSwitcherSheet(
            subjects = subjects,
            activeSubjectId = activeSubjectId,
            onDismiss = { showSubjects = false },
            onSelect = {
                onSelectSubject(it)
                showSubjects = false
            },
            onCreate = { goal, minutes ->
                onCreateSubject(goal, minutes)
                showSubjects = false
            },
            onDelete = { id -> onDeleteSubject(id) },
        )
    }
    if (showAbout) {
        AboutDialog(onDismiss = { showAbout = false })
    }
}

private fun updateSubtitle(state: UpdateState, currentVersion: String): String = when (state) {
    is UpdateState.Idle -> "当前版本 $currentVersion"
    is UpdateState.Checking -> "正在检查…"
    is UpdateState.Available -> "发现 v${state.info.version} · ${state.info.kind.label}，可手动更新"
    is UpdateState.Downloading -> "正在下载 v${state.info.version} · ${(state.progress * 100).toInt()}%"
    is UpdateState.Ready -> "v${state.info.version} 已下载，等待安装"
    is UpdateState.UpToDate -> "v${state.version} 已是最新"
    is UpdateState.Failed -> "检查失败 · ${state.message}"
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PlanEditorDialog(
    goal: String,
    minutes: Int,
    onDismiss: () -> Unit,
    onSave: (String, Int) -> Unit,
) {
    var editGoal by remember { mutableStateOf(goal) }
    var editMinutes by remember { mutableIntStateOf(minutes) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NocturneColors.Night900,
        titleContentColor = Color.White,
        textContentColor = NocturneColors.TextSecondary,
        shape = RoundedCornerShape(26.dp),
        title = {
            Text(
                text = "调整航线",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
            )
        },
        text = {
            Column {
                NocturneTextField(
                    value = editGoal,
                    onValueChange = { editGoal = it.take(80) },
                    label = "我想学",
                )
                Spacer(Modifier.height(14.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf(15, 30, 45, 60, 90).forEach { option ->
                        NocturneChip(
                            text = "$option 分钟",
                            selected = editMinutes == option,
                            onClick = { editMinutes = option },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (editGoal.isNotBlank()) {
                        onSave(editGoal.trim(), editMinutes)
                    }
                },
            ) {
                Text("保存", color = NocturneColors.ChampagneLight)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = NocturneColors.TextTertiary)
            }
        },
    )
}

@Composable
private fun AboutDialog(onDismiss: () -> Unit) {
    val appVersion = rememberAppVersion()
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NocturneColors.Night900,
        titleContentColor = Color.White,
        textContentColor = NocturneColors.TextSecondary,
        shape = RoundedCornerShape(26.dp),
        title = {
            Text(
                text = "夜航 · Nocturne",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
            )
        },
        text = {
            Column {
                Text(
                    text = "版本 $appVersion",
                    style = MaterialTheme.typography.bodySmall,
                    color = NocturneColors.TextTertiary,
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "一个把 AI 当导航器的学习空间。它先理解你想去哪儿，再决定今天走哪一步。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = NocturneColors.TextSecondary,
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "深墨夜景 × 无色玻璃 × 克制的香槟金。",
                    style = MaterialTheme.typography.bodySmall,
                    color = NocturneColors.TextTertiary,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("知道了", color = NocturneColors.ChampagneLight)
            }
        },
    )
}
