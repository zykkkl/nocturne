package com.nocturne.learn.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import com.nocturne.learn.data.learning.KnowledgePoint
import com.nocturne.learn.data.learning.LearningState
import com.nocturne.learn.ui.components.BackHeader
import com.nocturne.learn.ui.components.EmptyState
import com.nocturne.learn.ui.components.Hairline
import com.nocturne.learn.ui.components.QuietNote
import com.nocturne.learn.ui.design.GlassSurface
import com.nocturne.learn.ui.design.RowDot
import com.nocturne.learn.ui.theme.NocturneColors

private data class ChapterGroup(
    val title: String,
    val points: List<KnowledgePoint>,
)

@Composable
fun OutlineScreen(
    learning: LearningState,
    onBack: () -> Unit,
) {
    val groups = remember(learning.knowledgePoints) {
        learning.knowledgePoints
            .groupBy { it.chapter.trim().ifBlank { "未分章" } }
            .map { (chapter, points) -> ChapterGroup(chapter, points) }
            .sortedWith(
                compareBy(
                    { if (it.title == "未分章") 1 else 0 },
                    { it.points.minOfOrNull(KnowledgePoint::order) ?: 0 },
                    { it.title },
                ),
            )
    }
    val mastered = learning.masteredCount()
    var expanded by remember(groups) {
        mutableStateOf(groups.firstOrNull()?.title?.let(::setOf) ?: emptySet())
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 24.dp),
        contentPadding = PaddingValues(bottom = 110.dp),
    ) {
        item {
            Spacer(Modifier.height(18.dp))
            BackHeader(title = "教材目录", onBack = onBack)
            Spacer(Modifier.height(18.dp))
        }
        item {
            GlassSurface {
                Text(
                    text = learning.goal.ifBlank { "当前航线" },
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "${groups.size} 章 · ${learning.knowledgePoints.size} 个知识点 · 已掌握 $mastered",
                    style = MaterialTheme.typography.bodySmall,
                    color = NocturneColors.TextSecondary,
                )
                Spacer(Modifier.height(14.dp))
                ProgressBar(
                    progress = if (learning.knowledgePoints.isEmpty()) {
                        0f
                    } else {
                        mastered.toFloat() / learning.knowledgePoints.size
                    },
                )
            }
            Spacer(Modifier.height(18.dp))
        }

        if (groups.isEmpty()) {
            item {
                EmptyState(
                    title = "还没有知识点",
                    body = "导入教材或开始一次学习后，章节目录会在这里长出来。",
                    actionText = "返回",
                    onAction = onBack,
                )
            }
        } else {
            items(groups, key = { it.title }) { group ->
                ChapterCard(
                    group = group,
                    expanded = group.title in expanded,
                    onToggle = {
                        expanded = if (group.title in expanded) {
                            expanded - group.title
                        } else {
                            expanded + group.title
                        }
                    },
                )
                Spacer(Modifier.height(12.dp))
            }
        }

        if (groups.isNotEmpty()) {
            item {
                QuietNote("掌握度由练习与复习更新；目录只展示教材结构，不会改动进度。")
            }
        }
    }
}

@Composable
private fun ChapterCard(
    group: ChapterGroup,
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    GlassSurface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .clickable(onClick = onToggle),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RowDot(
                        color = if (group.points.any { it.mastery >= 0.8f }) {
                            NocturneColors.Champagne
                        } else {
                            NocturneColors.Moon
                        },
                        diameter = 5.dp,
                    )
                    Text(
                        text = "  ${group.title}",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "${group.points.size} 个知识点 · 已掌握 ${group.points.count { it.mastery >= 0.8f }}",
                    style = MaterialTheme.typography.labelSmall,
                    color = NocturneColors.TextTertiary,
                )
            }
            Text(
                text = if (expanded) "收起" else "展开",
                style = MaterialTheme.typography.labelSmall,
                color = NocturneColors.ChampagneLight,
            )
        }

        if (expanded) {
            Spacer(Modifier.height(12.dp))
            Hairline()
            Spacer(Modifier.height(4.dp))
            group.points.forEach { point ->
                SectionPointRow(point)
            }
        }
    }
}

@Composable
private fun SectionPointRow(point: KnowledgePoint) {
    Column(Modifier.padding(vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RowDot(
                color = when {
                    point.mastery >= 0.8f -> NocturneColors.Champagne
                    point.mastery > 0f -> NocturneColors.Moon
                    else -> Color.White.copy(alpha = 0.32f)
                },
                diameter = 4.dp,
            )
            Text(
                text = "  ${point.title}",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.92f),
                modifier = Modifier.weight(1f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "${(point.mastery * 100).toInt()}%",
                style = MaterialTheme.typography.labelSmall,
                color = NocturneColors.TextTertiary,
            )
        }
        if (point.note.isNotBlank()) {
            Spacer(Modifier.height(3.dp))
            Text(
                text = point.note,
                style = MaterialTheme.typography.labelSmall,
                color = NocturneColors.TextSecondary.copy(alpha = 0.85f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 12.dp),
            )
        }
        if (point.sourceRef.isNotBlank() && point.sourceRef != point.chapter) {
            Spacer(Modifier.height(3.dp))
            Text(
                text = "来源 · ${point.sourceRef}",
                style = MaterialTheme.typography.labelSmall,
                color = NocturneColors.TextTertiary.copy(alpha = 0.7f),
                modifier = Modifier.padding(start = 12.dp),
            )
        }
    }
}

@Composable
private fun ProgressBar(progress: Float) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(4.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(Color.White.copy(alpha = 0.08f)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .fillMaxHeight()
                .background(NocturneColors.Champagne.copy(alpha = 0.85f)),
        )
    }
}
