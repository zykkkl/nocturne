package com.nocturne.learn.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nocturne.learn.data.learning.LearningState
import com.nocturne.learn.ui.design.ChampagneButton
import com.nocturne.learn.ui.design.NocturneChip
import com.nocturne.learn.ui.design.NocturneTextField
import com.nocturne.learn.ui.design.RowDot
import com.nocturne.learn.ui.theme.NocturneColors

private val subjectExamples = listOf("初中数学", "英语口语", "Python 入门", "高中物理", "考研政治")
private val subjectPaces = listOf(15, 30, 45, 60, 90)

/** 从今日页或「我的」打开：切换航线，或新建一条。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubjectSwitcherSheet(
    subjects: List<LearningState>,
    activeSubjectId: String?,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
    onCreate: (String, Int) -> Unit,
) {
    var showCreate by remember { mutableStateOf(false) }

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
                .padding(bottom = 24.dp),
        ) {
            Text(
                text = "学习目标",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "每条航线有自己的知识点、复习节奏和坚持天数。",
                style = MaterialTheme.typography.bodySmall,
                color = NocturneColors.TextTertiary,
            )
            Spacer(Modifier.height(14.dp))

            if (subjects.isEmpty()) {
                Text(
                    text = "还没有学习目标。先建一条航线。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = NocturneColors.TextSecondary,
                    modifier = Modifier.padding(vertical = 18.dp),
                )
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 380.dp)) {
                    items(subjects, key = { it.id }) { subject ->
                        SubjectRow(
                            subject = subject,
                            active = subject.id == activeSubjectId,
                            onClick = {
                                onSelect(subject.id)
                                onDismiss()
                            },
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            ChampagneButton(
                text = "＋ 新的学习目标",
                onClick = { showCreate = true },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    if (showCreate) {
        NewSubjectDialog(
            onDismiss = { showCreate = false },
            onCreate = { goal, minutes ->
                showCreate = false
                onDismiss()
                onCreate(goal, minutes)
            },
        )
    }
}

@Composable
private fun SubjectRow(
    subject: LearningState,
    active: Boolean,
    onClick: () -> Unit,
) {
    val due = subject.dueCount()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RowDot(
            color = if (active) NocturneColors.Champagne else Color.White.copy(alpha = 0.34f),
            diameter = if (active) 7.dp else 5.dp,
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = subject.goal.ifBlank { "未命名航线" },
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
                    )
                }
            }
            Spacer(Modifier.height(3.dp))
            Text(
                text = "${subject.masteredCount()}/${subject.knowledgePoints.size} 颗星 · 每天 ${subject.dailyMinutes} 分钟" +
                    if (due > 0) " · $due 项待复习" else "",
                style = MaterialTheme.typography.bodySmall,
                color = NocturneColors.TextTertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NewSubjectDialog(
    onDismiss: () -> Unit,
    onCreate: (String, Int) -> Unit,
) {
    var goal by remember { mutableStateOf("") }
    var minutes by remember { mutableIntStateOf(30) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NocturneColors.Night900,
        titleContentColor = Color.White,
        textContentColor = NocturneColors.TextSecondary,
        shape = RoundedCornerShape(26.dp),
        title = {
            Text(
                text = "新的学习目标",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
            )
        },
        text = {
            Column {
                Text(
                    text = "它会是另一条独立的航线。",
                    style = MaterialTheme.typography.bodySmall,
                    color = NocturneColors.TextTertiary,
                )
                Spacer(Modifier.height(12.dp))
                NocturneTextField(
                    value = goal,
                    onValueChange = { goal = it.take(80) },
                    label = "我想学",
                    placeholder = "例如：把初中函数补起来",
                )
                Spacer(Modifier.height(12.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    subjectExamples.forEach { example ->
                        NocturneChip(
                            text = example,
                            selected = goal == example,
                            onClick = { goal = example },
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    subjectPaces.forEach { option ->
                        NocturneChip(
                            text = "$option 分钟",
                            selected = minutes == option,
                            onClick = { minutes = option },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (goal.isNotBlank()) onCreate(goal.trim(), minutes) },
            ) {
                Text("建立航线", color = NocturneColors.ChampagneLight)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = NocturneColors.TextTertiary)
            }
        },
    )
}
