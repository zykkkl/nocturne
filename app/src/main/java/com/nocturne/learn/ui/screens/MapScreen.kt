package com.nocturne.learn.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nocturne.learn.data.learning.KnowledgePoint
import com.nocturne.learn.data.learning.LearningState
import com.nocturne.learn.ui.components.Hairline
import com.nocturne.learn.ui.components.QuietNote
import com.nocturne.learn.ui.components.TabHeader
import com.nocturne.learn.ui.design.GhostButton
import com.nocturne.learn.ui.design.GlassSurface
import com.nocturne.learn.ui.design.RowDot
import com.nocturne.learn.ui.design.SectionLabel
import com.nocturne.learn.ui.theme.NocturneColors
import kotlin.math.roundToInt

private val constellationPoints = listOf(
    0.10f to 0.68f,
    0.23f to 0.42f,
    0.34f to 0.74f,
    0.47f to 0.36f,
    0.56f to 0.60f,
    0.67f to 0.28f,
    0.76f to 0.66f,
    0.86f to 0.44f,
    0.92f to 0.72f,
)

@Composable
fun MapScreen(
    learning: LearningState,
    onBackHome: () -> Unit,
    onOpenOutline: () -> Unit,
) {
    val points = learning.knowledgePoints
    val hasChapters = points.any { it.chapter.isNotBlank() }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
    ) {
        Spacer(Modifier.height(18.dp))
        TabHeader(eyebrow = "STAR MAP · 星图", title = "知识星图")
        Spacer(Modifier.height(8.dp))
        Text(
            text = "会的知识点，会在这里连成星座。",
            style = MaterialTheme.typography.bodyMedium,
            color = NocturneColors.TextSecondary,
        )
        Spacer(Modifier.height(24.dp))
        GlassSurface {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp),
            ) {
                Constellation(points = points, modifier = Modifier.fillMaxSize())
            }
            Spacer(Modifier.height(14.dp))
            SectionLabel(if (points.isEmpty()) "尚未点亮" else "${points.size} 颗星在航线里")
            Spacer(Modifier.height(8.dp))
            Text(
                text = if (points.isEmpty()) {
                    "完成首次诊断后，AI 会把目标拆成知识点，并标出已经亮起的那几颗。"
                } else {
                    "每掌握一个知识点，它就再亮一点。答对、答得快、隔了更久仍记得，都会让它更亮。"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = NocturneColors.TextSecondary,
            )
            Spacer(Modifier.height(16.dp))
            GhostButton(
                text = "回到今日",
                onClick = onBackHome,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (hasChapters) {
            Spacer(Modifier.height(16.dp))
            GlassSurface(
                modifier = Modifier.clickable(onClick = onOpenOutline),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        SectionLabel("教材目录")
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "按章节看知识点与来源",
                            style = MaterialTheme.typography.bodyMedium,
                            color = NocturneColors.TextSecondary,
                        )
                    }
                    Text(
                        text = "打开 ›",
                        style = MaterialTheme.typography.labelSmall,
                        color = NocturneColors.ChampagneLight,
                    )
                }
            }
        }
        if (points.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            GlassSurface(contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 18.dp)) {
                points.forEachIndexed { index, point ->
                    KnowledgePointRow(point)
                    if (index != points.lastIndex) Hairline()
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        GlassSurface {
            SectionLabel("掌握度")
            Spacer(Modifier.height(10.dp))
            Text(
                text = "每个知识点都有 0~1 的掌握度。答对、答得快、隔了更久仍记得，它就会更亮。",
                style = MaterialTheme.typography.bodyMedium,
                color = NocturneColors.TextSecondary,
            )
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                MasteryLegend(color = Color.White.copy(alpha = 0.22f), label = "未点亮")
                MasteryLegend(color = NocturneColors.Moon.copy(alpha = 0.85f), label = "在学")
                MasteryLegend(color = NocturneColors.Champagne, label = "已掌握")
            }
        }
        Spacer(Modifier.height(16.dp))
        Hairline()
        Spacer(Modifier.height(14.dp))
        QuietNote("星图会随着复习与练习自动生长，不需要手动画线。")
        Spacer(Modifier.height(110.dp))
    }
}

@Composable
private fun KnowledgePointRow(point: KnowledgePoint) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RowDot(pointColor(point), 6.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = point.title,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (point.note.isNotBlank()) {
                Spacer(Modifier.height(3.dp))
                Text(
                    text = point.note,
                    style = MaterialTheme.typography.bodySmall,
                    color = NocturneColors.TextTertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Text(
            text = "${statusLabel(point)} · ${(point.mastery * 100).roundToInt()}%",
            style = MaterialTheme.typography.labelSmall,
            color = if (point.status == "mastered") NocturneColors.ChampagneLight
            else NocturneColors.TextSecondary,
        )
    }
}

@Composable
private fun Constellation(points: List<KnowledgePoint>, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        if (points.isEmpty()) {
            val offsets = constellationPoints.map { (fractionX, fractionY) ->
                Offset(fractionX * size.width, fractionY * size.height)
            }
            for (index in 0 until offsets.lastIndex) {
                drawLine(
                    color = Color.White.copy(alpha = 0.10f),
                    start = offsets[index],
                    end = offsets[index + 1],
                    strokeWidth = 1.dp.toPx(),
                )
            }
            offsets.forEachIndexed { index, offset ->
                if (index == 4) {
                    drawCircle(
                        color = NocturneColors.Champagne.copy(alpha = 0.16f),
                        radius = 9.dp.toPx(),
                        center = offset,
                    )
                    drawCircle(
                        color = NocturneColors.Champagne,
                        radius = 3.dp.toPx(),
                        center = offset,
                    )
                } else {
                    drawCircle(
                        color = Color.White.copy(alpha = 0.50f),
                        radius = 1.8.dp.toPx(),
                        center = offset,
                    )
                }
            }
            return@Canvas
        }

        val offsets = points.mapIndexed { index, _ ->
            val x = if (points.size == 1) 0.5f else 0.12f + 0.76f * index / (points.size - 1)
            val y = 0.52f + 0.18f * kotlin.math.sin(index * 1.7f)
            Offset(x * size.width, y * size.height)
        }
        for (index in 0 until offsets.lastIndex) {
            drawLine(
                color = Color.White.copy(alpha = 0.12f),
                start = offsets[index],
                end = offsets[index + 1],
                strokeWidth = 1.dp.toPx(),
            )
        }
        offsets.forEachIndexed { index, offset ->
            val point = points[index]
            val color = pointColor(point)
            if (point.status == "mastered" || point.mastery > 0.45f) {
                drawCircle(
                    color = color.copy(alpha = 0.18f),
                    radius = (8 + point.mastery * 8).dp.toPx(),
                    center = offset,
                )
            }
            drawCircle(
                color = color,
                radius = (2.2f + point.mastery * 2.6f).dp.toPx(),
                center = offset,
            )
        }
    }
}

@Composable
private fun MasteryLegend(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        RowDot(color, 6.dp)
        Text(
            text = "  $label",
            style = MaterialTheme.typography.bodySmall,
            color = NocturneColors.TextSecondary,
        )
    }
}

private fun pointColor(point: KnowledgePoint): Color = when {
    point.status == "mastered" -> NocturneColors.Champagne
    point.mastery > 0f -> NocturneColors.Moon.copy(alpha = 0.9f)
    else -> Color.White.copy(alpha = 0.32f)
}

private fun statusLabel(point: KnowledgePoint): String = when {
    point.status == "mastered" -> "已掌握"
    point.mastery > 0f -> "在学"
    else -> "未点亮"
}
