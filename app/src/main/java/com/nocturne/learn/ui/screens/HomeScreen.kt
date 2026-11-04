package com.nocturne.learn.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
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
import com.nocturne.learn.data.AppSettings
import com.nocturne.learn.data.ProviderConfig
import com.nocturne.learn.data.learning.LearningState
import com.nocturne.learn.ui.components.Hairline
import com.nocturne.learn.ui.components.StatBlock
import com.nocturne.learn.ui.components.SubjectSwitcherSheet
import com.nocturne.learn.ui.design.ChampagneButton
import com.nocturne.learn.ui.design.GhostButton
import com.nocturne.learn.ui.design.GlassSurface
import com.nocturne.learn.ui.design.JarvisOrb
import com.nocturne.learn.ui.design.ProgressRing
import com.nocturne.learn.ui.design.RowDot
import com.nocturne.learn.ui.design.SectionLabel
import com.nocturne.learn.ui.theme.NocturneColors
import com.nocturne.learn.ui.util.rememberAppVersion
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun HomeScreen(
    settings: AppSettings,
    learning: LearningState,
    subjects: List<LearningState>,
    activeSubjectId: String?,
    onConfigureModel: () -> Unit,
    onOpenMap: () -> Unit,
    onStartSession: () -> Unit,
    onAsk: () -> Unit,
    onSelectSubject: (String) -> Unit,
    onCreateSubject: (String, Int) -> Unit,
) {
    val provider = settings.activeProvider
    val appVersion = rememberAppVersion()
    var showSubjects by remember { mutableStateOf(false) }
    val dateText = remember {
        LocalDate.now().format(DateTimeFormatter.ofPattern("M 月 d 日 · EEEE", Locale.CHINA))
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 18.dp, bottom = 130.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Column {
                Text(
                    text = "NOCTURNE · 夜航",
                    style = MaterialTheme.typography.labelSmall,
                    color = NocturneColors.TextTertiary,
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = greetingForNow(),
                    style = MaterialTheme.typography.displaySmall,
                    color = Color.White,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = dateText,
                    style = MaterialTheme.typography.bodySmall,
                    color = NocturneColors.TextTertiary,
                )
            }
        }

        item {
            val shape = RoundedCornerShape(26.dp)
            GlassSurface(
                modifier = Modifier
                    .clip(shape)
                    .clickable { showSubjects = true },
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = "当前航线",
                            style = MaterialTheme.typography.labelSmall,
                            color = NocturneColors.TextTertiary,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = learning.goal.ifBlank { "还没有设定航线" },
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Text(
                        text = "切换 ›",
                        style = MaterialTheme.typography.labelSmall,
                        color = NocturneColors.ChampagneLight,
                    )
                }
            }
        }

        item {
            JarvisCard(
                provider = provider,
                learning = learning,
                onConfigureModel = onConfigureModel,
            )
        }

        item {
            TodayCard(
                dailyMinutes = learning.dailyMinutes,
                learning = learning,
                providerReady = provider != null,
                onStartSession = onStartSession,
                onConfigureModel = onConfigureModel,
            )
        }

        item { AskCard(onAsk = onAsk) }

        item { MapTeaser(learning = learning, onOpenMap = onOpenMap) }

        item {
            Text(
                text = "夜航 · $appVersion",
                style = MaterialTheme.typography.labelSmall,
                color = NocturneColors.TextTertiary,
                modifier = Modifier.padding(start = 4.dp, top = 6.dp),
            )
        }
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
        )
    }
}

@Composable
private fun AskCard(onAsk: () -> Unit) {
    val shape = RoundedCornerShape(26.dp)
    GlassSurface(
        modifier = Modifier
            .clip(shape)
            .clickable(onClick = onAsk),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                SectionLabel("追问")
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "有一道题卡住了？",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "苏格拉底式追问，一次一个问题，不直接给答案。",
                    style = MaterialTheme.typography.bodySmall,
                    color = NocturneColors.TextSecondary,
                )
            }
            Text(
                text = "›",
                style = MaterialTheme.typography.titleLarge,
                color = NocturneColors.TextTertiary,
            )
        }
    }
}

@Composable
private fun JarvisCard(
    provider: ProviderConfig?,
    learning: LearningState,
    onConfigureModel: () -> Unit,
) {
    val due = learning.dueCount()
    val line = when {
        provider == null -> "夜航需要一个大模型来思考。填入你自己的 API Key，它就会成为你的私人导师。"
        learning.knowledgePoints.isEmpty() -> "我在这里。先做一次诊断，弄清你已经会什么，再决定从哪一颗星出发。"
        due > 0 -> "有 $due 个知识点到期了。先把它们还掉，我们再往深处走。"
        else -> {
            val target = learning.nextTarget()?.title.orEmpty()
            "今天可以继续 $target。我会一次只讲一个概念，然后看你亲手做一遍。"
        }
    }

    GlassSurface {
        Row(verticalAlignment = Alignment.CenterVertically) {
            JarvisOrb(diameter = 46.dp, active = provider != null)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                SectionLabel("贾维斯")
                Spacer(Modifier.height(6.dp))
                Text(
                    text = if (provider != null) "引擎已接入" else "等待接入模型",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                )
            }
            Text(
                text = if (provider != null) "在线" else "离线",
                style = MaterialTheme.typography.labelSmall,
                color = if (provider != null) NocturneColors.ChampagneLight else NocturneColors.TextTertiary,
            )
        }
        Spacer(Modifier.height(14.dp))
        Text(
            text = line,
            style = MaterialTheme.typography.bodyMedium,
            color = NocturneColors.TextSecondary,
        )
        Spacer(Modifier.height(16.dp))
        if (provider == null) {
            ChampagneButton(
                text = "接入模型",
                onClick = onConfigureModel,
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RowDot(NocturneColors.Champagne.copy(alpha = 0.8f), 5.dp)
                Text(
                    text = "  ${provider.displayName} · ${provider.model}",
                    style = MaterialTheme.typography.bodySmall,
                    color = NocturneColors.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(10.dp))
            GhostButton(
                text = "模型服务",
                onClick = onConfigureModel,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun TodayCard(
    dailyMinutes: Int,
    learning: LearningState,
    providerReady: Boolean,
    onStartSession: () -> Unit,
    onConfigureModel: () -> Unit,
) {
    val due = learning.dueCount()
    val total = learning.knowledgePoints.size
    val mastered = learning.masteredCount()
    val progress = if (total == 0) 0f else mastered.toFloat() / total
    val title = when {
        total == 0 -> "尚未启航"
        due > 0 -> "先还 $due 笔旧账"
        else -> learning.nextTarget()?.title ?: "继续学习"
    }
    val subtitle = when {
        total == 0 -> "计划每天 $dailyMinutes 分钟，首航从诊断开始。"
        due > 0 -> "到期复习优先，预计 $dailyMinutes 分钟内完成。"
        else -> "下一次学习，一次点亮一颗星。"
    }

    GlassSurface {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                SectionLabel("今日航线")
                Spacer(Modifier.height(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = NocturneColors.TextSecondary,
                )
            }
            ProgressRing(progress = progress, diameter = 78.dp, caption = "掌握")
        }
        Spacer(Modifier.height(16.dp))
        Hairline()
        Spacer(Modifier.height(14.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            StatBlock(label = "连续学习", value = "${learning.streak}", unit = "天", modifier = Modifier.weight(1f))
            StatBlock(label = "掌握知识点", value = "$mastered", unit = "个", modifier = Modifier.weight(1f))
            StatBlock(label = "待复习", value = "$due", unit = "项", modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(16.dp))
        if (providerReady) {
            ChampagneButton(
                text = if (total == 0) "开始诊断" else if (due > 0) "开始复习" else "继续学习",
                onClick = onStartSession,
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            GhostButton(
                text = "先接入模型",
                onClick = onConfigureModel,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun MapTeaser(
    learning: LearningState,
    onOpenMap: () -> Unit,
) {
    val shape = RoundedCornerShape(26.dp)
    val total = learning.knowledgePoints.size
    val mastered = learning.masteredCount()
    GlassSurface(
        modifier = Modifier
            .clip(shape)
            .clickable(onClick = onOpenMap),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                SectionLabel("知识星图")
                Spacer(Modifier.height(8.dp))
                Text(
                    text = if (total == 0) "还没有星图" else "$mastered / $total 颗星已经点亮",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = if (total == 0) "完成诊断后，AI 会把目标拆成知识点。" else "每掌握一个知识点，就多一颗星。",
                    style = MaterialTheme.typography.bodySmall,
                    color = NocturneColors.TextSecondary,
                )
            }
            Text(
                text = "›",
                style = MaterialTheme.typography.titleLarge,
                color = NocturneColors.TextTertiary,
            )
        }
    }
}

private fun greetingForNow(): String {
    val hour = LocalTime.now().hour
    return when {
        hour < 5 -> "夜深了。"
        hour < 11 -> "早上好。"
        hour < 14 -> "中午好。"
        hour < 18 -> "下午好。"
        hour < 23 -> "晚上好。"
        else -> "夜深了。"
    }
}
