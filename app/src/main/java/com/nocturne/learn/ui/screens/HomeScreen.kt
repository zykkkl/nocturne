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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nocturne.learn.data.AppSettings
import com.nocturne.learn.data.ProviderConfig
import com.nocturne.learn.ui.components.Hairline
import com.nocturne.learn.ui.components.StatBlock
import com.nocturne.learn.ui.design.ChampagneButton
import com.nocturne.learn.ui.design.GhostButton
import com.nocturne.learn.ui.design.GlassSurface
import com.nocturne.learn.ui.design.JarvisOrb
import com.nocturne.learn.ui.design.ProgressRing
import com.nocturne.learn.ui.design.RowDot
import com.nocturne.learn.ui.design.SectionLabel
import com.nocturne.learn.ui.theme.NocturneColors
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun HomeScreen(
    settings: AppSettings,
    onConfigureModel: () -> Unit,
    onOpenMap: () -> Unit,
) {
    val provider = settings.activeProvider
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
            GlassSurface(contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp)) {
                Text(
                    text = "当前航线",
                    style = MaterialTheme.typography.labelSmall,
                    color = NocturneColors.TextTertiary,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = settings.goal.ifBlank { "还没有设定航线" },
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                )
            }
        }

        item { JarvisCard(provider = provider, onConfigureModel = onConfigureModel) }

        item { TodayCard(dailyMinutes = settings.dailyMinutes) }

        item { MapTeaser(onOpenMap = onOpenMap) }

        item {
            Text(
                text = "夜航 · 0.1.0",
                style = MaterialTheme.typography.labelSmall,
                color = NocturneColors.TextTertiary,
                modifier = Modifier.padding(start = 4.dp, top = 6.dp),
            )
        }
    }
}

@Composable
private fun JarvisCard(
    provider: ProviderConfig?,
    onConfigureModel: () -> Unit,
) {
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
            text = if (provider == null) {
                "夜航需要一个大模型来思考。填入你自己的 API Key，它就会成为你的私人导师。"
            } else {
                "我在这里。下一次学习，我会先弄清你已经会什么，再决定我们从哪一颗星出发。"
            },
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
private fun TodayCard(dailyMinutes: Int) {
    GlassSurface {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                SectionLabel("今日航线")
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "尚未启航",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "计划每天 $dailyMinutes 分钟，首航从诊断开始。",
                    style = MaterialTheme.typography.bodySmall,
                    color = NocturneColors.TextSecondary,
                )
            }
            ProgressRing(progress = 0f, diameter = 78.dp, caption = "今日")
        }
        Spacer(Modifier.height(16.dp))
        Hairline()
        Spacer(Modifier.height(14.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            StatBlock(label = "连续学习", value = "0", unit = "天", modifier = Modifier.weight(1f))
            StatBlock(label = "掌握知识点", value = "0", unit = "个", modifier = Modifier.weight(1f))
            StatBlock(label = "待复习", value = "0", unit = "项", modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun MapTeaser(onOpenMap: () -> Unit) {
    val shape = RoundedCornerShape(26.dp)
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
                    text = "0 个知识点在等你点亮",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "每掌握一个知识点，就多一颗星。",
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
