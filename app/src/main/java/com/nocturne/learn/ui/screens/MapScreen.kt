package com.nocturne.learn.ui.screens

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.nocturne.learn.ui.components.Hairline
import com.nocturne.learn.ui.components.QuietNote
import com.nocturne.learn.ui.components.TabHeader
import com.nocturne.learn.ui.design.GhostButton
import com.nocturne.learn.ui.design.GlassSurface
import com.nocturne.learn.ui.design.RowDot
import com.nocturne.learn.ui.design.SectionLabel
import com.nocturne.learn.ui.theme.NocturneColors

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
fun MapScreen(onBackHome: () -> Unit) {
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
                ConstellationPreview(Modifier.fillMaxSize())
            }
            Spacer(Modifier.height(10.dp))
            SectionLabel("尚未点亮")
            Spacer(Modifier.height(8.dp))
            Text(
                text = "完成首次诊断后，AI 会把目标拆成知识点，并标出已经亮起的那几颗。",
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
private fun ConstellationPreview(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
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
