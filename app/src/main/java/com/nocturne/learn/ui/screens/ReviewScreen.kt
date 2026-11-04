package com.nocturne.learn.ui.screens

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nocturne.learn.data.learning.LearningState
import com.nocturne.learn.data.learning.ReviewItem
import com.nocturne.learn.ui.components.EmptyState
import com.nocturne.learn.ui.components.Hairline
import com.nocturne.learn.ui.components.StatBlock
import com.nocturne.learn.ui.components.TabHeader
import com.nocturne.learn.ui.design.ChampagneButton
import com.nocturne.learn.ui.design.GlassSurface
import com.nocturne.learn.ui.design.JarvisOrb
import com.nocturne.learn.ui.design.RowDot
import com.nocturne.learn.ui.design.SectionLabel
import com.nocturne.learn.ui.theme.NocturneColors

@Composable
fun ReviewScreen(
    learning: LearningState,
    onStartSession: () -> Unit,
    onBackHome: () -> Unit,
) {
    val now = System.currentTimeMillis()
    val due = learning.dueReviews(now).sortedBy { it.dueAt }
    val mastered = learning.knowledgePoints.count { it.status == "mastered" }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
    ) {
        Spacer(Modifier.height(18.dp))
        TabHeader(eyebrow = "REVIEW · 记忆", title = "复习")
        Spacer(Modifier.height(8.dp))
        Text(
            text = "在快要忘记的时候，把你叫回来。",
            style = MaterialTheme.typography.bodyMedium,
            color = NocturneColors.TextSecondary,
        )
        Spacer(Modifier.height(24.dp))
        GlassSurface {
            Row(verticalAlignment = Alignment.CenterVertically) {
                JarvisOrb(diameter = 40.dp, active = due.isNotEmpty())
                Spacer(Modifier.width(12.dp))
                Column {
                    SectionLabel("记忆引擎")
                    Spacer(Modifier.height(5.dp))
                    Text(
                        text = if (due.isEmpty()) "今天已清空" else "待复习 ${due.size} 项",
                        style = MaterialTheme.typography.headlineSmall,
                        color = if (due.isEmpty()) NocturneColors.TextSecondary else Color.White,
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            Text(
                text = "夜航用 FSRS 间隔算法安排复习：先密后疏，每次只复习该复习的。答错的知识点会重新排进近期。",
                style = MaterialTheme.typography.bodyMedium,
                color = NocturneColors.TextSecondary,
            )
            Spacer(Modifier.height(16.dp))
            Hairline()
            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                StatBlock(label = "待复习", value = due.size.toString(), unit = "项", modifier = Modifier.weight(1f))
                StatBlock(label = "已掌握", value = mastered.toString(), unit = "个", modifier = Modifier.weight(1f))
                StatBlock(label = "连续学习", value = learning.streak.toString(), unit = "天", modifier = Modifier.weight(1f))
            }
            if (due.isNotEmpty()) {
                Spacer(Modifier.height(18.dp))
                ChampagneButton(
                    text = "开始复习",
                    onClick = onStartSession,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        if (due.isEmpty()) {
            EmptyState(
                title = "今天没有待复习的内容",
                body = if (learning.knowledgePoints.isEmpty()) {
                    "接入模型、完成第一次学习后，复习卡片会在这里出现。它们不会催你，只会在合适的时候出现。"
                } else {
                    "知识点会在快要忘记的时候回到这里。现在可以去学下一颗星，复习会自己找上门。"
                },
                actionText = "回到今日",
                onAction = onBackHome,
            )
        } else {
            GlassSurface(contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 18.dp)) {
                due.forEachIndexed { index, item ->
                    DueRow(item = item, state = learning, now = now)
                    if (index != due.lastIndex) Hairline()
                }
            }
        }
        Spacer(Modifier.height(110.dp))
    }
}

@Composable
private fun DueRow(item: ReviewItem, state: LearningState, now: Long) {
    val point = state.knowledgePointById(item.kpId)
    val timing = when {
        item.dueAt <= now -> "已到期"
        item.dueAt - now < 86_400_000L -> "今天"
        else -> "${(item.dueAt - now) / 86_400_000L} 天后"
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RowDot(NocturneColors.Champagne.copy(alpha = 0.85f), 6.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = point?.title ?: "未知知识点",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = "间隔 ${item.intervalDays} 天 · 已复习 ${item.reps} 次",
                style = MaterialTheme.typography.bodySmall,
                color = NocturneColors.TextTertiary,
            )
        }
        Text(
            text = timing,
            style = MaterialTheme.typography.labelSmall,
            color = NocturneColors.TextSecondary,
        )
    }
}
