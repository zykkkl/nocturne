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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nocturne.learn.ui.components.EmptyState
import com.nocturne.learn.ui.components.Hairline
import com.nocturne.learn.ui.components.StatBlock
import com.nocturne.learn.ui.components.TabHeader
import com.nocturne.learn.ui.design.GlassSurface
import com.nocturne.learn.ui.design.JarvisOrb
import com.nocturne.learn.ui.design.SectionLabel
import com.nocturne.learn.ui.theme.NocturneColors

@Composable
fun ReviewScreen(onBackHome: () -> Unit) {
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
                JarvisOrb(diameter = 40.dp, active = false)
                Spacer(Modifier.height(0.dp))
                Column(Modifier.padding(start = 12.dp)) {
                    SectionLabel("记忆引擎")
                    Spacer(Modifier.height(5.dp))
                    Text(
                        text = "待启动",
                        style = MaterialTheme.typography.headlineSmall,
                        color = NocturneColors.TextSecondary,
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
                StatBlock(label = "待复习", value = "0", unit = "项", modifier = Modifier.weight(1f))
                StatBlock(label = "今日已复习", value = "0", unit = "项", modifier = Modifier.weight(1f))
                StatBlock(label = "连续复习", value = "0", unit = "天", modifier = Modifier.weight(1f))
            }
        }
        Spacer(Modifier.height(16.dp))
        EmptyState(
            title = "今天没有待复习的内容",
            body = "接入模型、完成第一次学习后，复习卡片会在这里出现。它们不会催你，只会在合适的时候出现。",
            actionText = "回到今日",
            onAction = onBackHome,
        )
        Spacer(Modifier.height(110.dp))
    }
}
