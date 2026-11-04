package com.nocturne.learn.ui.screens

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nocturne.learn.ui.components.QuietNote
import com.nocturne.learn.ui.design.ChampagneButton
import com.nocturne.learn.ui.design.GhostButton
import com.nocturne.learn.ui.design.GlassSurface
import com.nocturne.learn.ui.design.NocturneChip
import com.nocturne.learn.ui.design.NocturneTextField
import com.nocturne.learn.ui.design.RowDot
import com.nocturne.learn.ui.design.SectionLabel
import com.nocturne.learn.ui.theme.NocturneColors

private val goalExamples = listOf("初中数学", "英语口语", "Python 入门", "高中物理", "考研政治")
private val paceOptions = listOf(15, 30, 45, 60, 90)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OnboardingScreen(
    initialGoal: String,
    initialMinutes: Int,
    onComplete: (String, Int) -> Unit,
) {
    var step by rememberSaveable { mutableIntStateOf(0) }
    var goal by rememberSaveable { mutableStateOf(initialGoal) }
    var minutes by rememberSaveable { mutableIntStateOf(initialMinutes) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .imePadding()
            .padding(horizontal = 26.dp),
    ) {
        Spacer(Modifier.height(20.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "夜航",
                style = MaterialTheme.typography.displaySmall,
                color = Color.White,
            )
            Spacer(Modifier.height(0.dp))
            Text(
                text = "   NOCTURNE",
                style = MaterialTheme.typography.labelSmall,
                color = NocturneColors.TextTertiary,
            )
        }
        Spacer(Modifier.height(8.dp))
        RowDot(NocturneColors.Champagne.copy(alpha = 0.8f), 4.dp)

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            Crossfade(
                targetState = step,
                animationSpec = tween(durationMillis = 460),
                label = "onboarding-step",
            ) { current ->
                if (current == 0) {
                    GoalStep(
                        goal = goal,
                        onGoalChange = { goal = it.take(80) },
                    )
                } else {
                    PaceStep(
                        goal = goal,
                        minutes = minutes,
                        onMinutesChange = { minutes = it },
                    )
                }
            }
        }

        Column(modifier = Modifier.padding(bottom = 16.dp)) {
            if (step == 0) {
                ChampagneButton(
                    text = "继续",
                    onClick = { step = 1 },
                    enabled = goal.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    GhostButton(
                        text = "返回",
                        onClick = { step = 0 },
                        modifier = Modifier.weight(1f),
                    )
                    ChampagneButton(
                        text = "启航",
                        onClick = { onComplete(goal.trim(), minutes) },
                        modifier = Modifier.weight(2f),
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            QuietNote(
                text = "AI 只做导航，方向盘在你手里。",
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GoalStep(
    goal: String,
    onGoalChange: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(top = 34.dp),
    ) {
        SectionLabel("首次启航")
        Spacer(Modifier.height(16.dp))
        Text(
            text = "你想学的，\n是什么？",
            style = MaterialTheme.typography.displayMedium,
            color = Color.White,
        )
        Spacer(Modifier.height(14.dp))
        Text(
            text = "不必是学科名。一个目标、一个念头，都可以成为航线。",
            style = MaterialTheme.typography.bodyMedium,
            color = NocturneColors.TextSecondary,
        )
        Spacer(Modifier.height(30.dp))
        NocturneTextField(
            value = goal,
            onValueChange = onGoalChange,
            label = "我想学",
            placeholder = "例如：把初中函数补起来",
        )
        Spacer(Modifier.height(16.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            goalExamples.forEach { example ->
                NocturneChip(
                    text = example,
                    selected = goal == example,
                    onClick = { onGoalChange(example) },
                )
            }
        }
        Spacer(Modifier.height(20.dp))
        Text(
            text = "可以直接输入任何你想学的东西，不限学科与年级。",
            style = MaterialTheme.typography.bodySmall,
            color = NocturneColors.TextTertiary,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PaceStep(
    goal: String,
    minutes: Int,
    onMinutesChange: (Int) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(top = 34.dp),
    ) {
        SectionLabel("每日投入")
        Spacer(Modifier.height(16.dp))
        Text(
            text = "每天留出\n多久？",
            style = MaterialTheme.typography.displayMedium,
            color = Color.White,
        )
        Spacer(Modifier.height(14.dp))
        Text(
            text = "时间会决定航线的密度。随时可以修改。",
            style = MaterialTheme.typography.bodyMedium,
            color = NocturneColors.TextSecondary,
        )
        Spacer(Modifier.height(26.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            paceOptions.forEach { option ->
                NocturneChip(
                    text = "$option 分钟",
                    selected = minutes == option,
                    onClick = { onMinutesChange(option) },
                )
            }
        }
        Spacer(Modifier.height(24.dp))
        GlassSurface {
            SectionLabel("你的第一段航线")
            Spacer(Modifier.height(10.dp))
            Text(
                text = goal.ifBlank { "一段由你定义的旅程" },
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
            )
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                RowDot(NocturneColors.Champagne.copy(alpha = 0.85f))
                Text(
                    text = "  每天 $minutes 分钟 · 由浅入深",
                    style = MaterialTheme.typography.bodySmall,
                    color = NocturneColors.TextSecondary,
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = "第一步会是诊断：夜航需要知道你现在的水平，再决定从哪颗星出发。",
            style = MaterialTheme.typography.bodySmall,
            color = NocturneColors.TextTertiary,
            textAlign = TextAlign.Start,
        )
    }
}
