package com.nocturne.learn.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nocturne.learn.AskMessage
import com.nocturne.learn.AskViewModel
import com.nocturne.learn.ui.components.ChatInputBar
import com.nocturne.learn.ui.design.ChampagneButton
import com.nocturne.learn.ui.design.GlassSurface
import com.nocturne.learn.ui.design.JarvisOrb
import com.nocturne.learn.ui.design.RowDot
import com.nocturne.learn.ui.theme.NocturneColors
import kotlinx.coroutines.delay

private val askExamples = listOf(
    "这道题为什么第一步要移项？",
    "循环和递归到底差在哪？",
    "怎么判断一个函数是不是奇函数？",
)

@Composable
fun AskScreen(
    viewModel: AskViewModel,
    providerReady: Boolean,
    onBack: () -> Unit,
    onOpenProviders: () -> Unit,
) {
    val ui by viewModel.ui.collectAsState()
    var input by rememberSaveable { mutableStateOf("") }
    val listState = rememberLazyListState()
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current

    LaunchedEffect(ui.messages.size, ui.thinking) {
        delay(40)
        if (listState.layoutInfo.totalItemsCount > 0) {
            listState.animateScrollToItem(listState.layoutInfo.totalItemsCount - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .imePadding(),
    ) {
        AskHeader(
            onBack = onBack,
            onReset = {
                viewModel.reset()
                input = ""
                focusManager.clearFocus()
            },
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            state = listState,
            contentPadding = PaddingValues(start = 22.dp, end = 22.dp, top = 8.dp, bottom = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (ui.messages.isEmpty()) {
                item { AskIntro(onExample = { input = it }) }
            }
            ui.messages.forEach { message ->
                item(key = message.id) { AskBubble(message) }
            }
            if (ui.thinking) {
                item(key = "thinking") { AskThinking() }
            }
            ui.error?.let { error ->
                item(key = "error") {
                    AskError(
                        message = error,
                        showSetup = !providerReady,
                        onOpenProviders = onOpenProviders,
                    )
                }
            }
        }

        if (!providerReady) {
            ChampagneButton(
                text = "接入模型，才能开始追问",
                onClick = onOpenProviders,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 6.dp),
            )
        }
        ChatInputBar(
            value = input,
            onValueChange = { input = it },
            enabled = providerReady && !ui.thinking,
            onSend = {
                val text = input
                input = ""
                focusManager.clearFocus()
                viewModel.ask(text)
            },
            placeholder = "把卡住你的那道题说出来…",
        )
    }
}

@Composable
private fun AskHeader(onBack: () -> Unit, onReset: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.04f))
                .border(1.dp, Color.White.copy(alpha = 0.12f), CircleShape)
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "←",
                style = MaterialTheme.typography.titleMedium,
                color = NocturneColors.TextSecondary,
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = "ASK · 追问",
                style = MaterialTheme.typography.labelSmall,
                color = NocturneColors.TextTertiary,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = "把一个问题问透",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
            )
        }
        Text(
            text = "新对话",
            style = MaterialTheme.typography.labelSmall,
            color = NocturneColors.ChampagneLight,
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .clickable(onClick = onReset)
                .padding(horizontal = 10.dp, vertical = 7.dp),
        )
    }
}

@Composable
private fun AskIntro(onExample: (String) -> Unit) {
    GlassSurface {
        Row(verticalAlignment = Alignment.CenterVertically) {
            JarvisOrb(diameter = 42.dp, active = true)
            Spacer(Modifier.width(13.dp))
            Column {
                Text(
                    text = "追问向导",
                    style = MaterialTheme.typography.labelSmall,
                    color = NocturneColors.ChampagneLight.copy(alpha = 0.9f),
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "我不直接给答案",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        Text(
            text = "把卡住你的那道题、那个概念，或者那句没看懂的话发给我。我会一次问一个问题，陪你走到自己能说出答案的地方。",
            style = MaterialTheme.typography.bodyMedium,
            color = NocturneColors.TextSecondary,
        )
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            RowDot(NocturneColors.Moon.copy(alpha = 0.8f), 5.dp)
            Text(
                text = "  从下面挑一个开始，也可以直接输入",
                style = MaterialTheme.typography.bodySmall,
                color = NocturneColors.TextTertiary,
            )
        }
        Spacer(Modifier.height(10.dp))
        askExamples.forEach { example ->
            Text(
                text = example,
                style = MaterialTheme.typography.bodySmall,
                color = NocturneColors.TextSecondary,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.04f))
                    .clickable { onExample(example) }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            )
            Spacer(Modifier.height(7.dp))
        }
    }
}

@Composable
private fun AskBubble(message: AskMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.fromUser) Arrangement.End else Arrangement.Start,
    ) {
        GlassSurface(
            modifier = Modifier.widthIn(max = 330.dp),
            shape = RoundedCornerShape(24.dp),
            contentPadding = PaddingValues(horizontal = 17.dp, vertical = 13.dp),
        ) {
            if (!message.fromUser) {
                Text(
                    text = "追问向导",
                    style = MaterialTheme.typography.labelSmall,
                    color = NocturneColors.ChampagneLight.copy(alpha = 0.85f),
                )
                Spacer(Modifier.height(5.dp))
            }
            Text(
                text = message.text,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 15.sp,
                    lineHeight = 24.sp,
                ),
                color = if (message.fromUser) Color.White else NocturneColors.TextPrimary,
            )
        }
    }
}

@Composable
private fun AskThinking() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
    ) {
        GlassSurface(
            modifier = Modifier.widthIn(max = 330.dp),
            shape = RoundedCornerShape(24.dp),
            contentPadding = PaddingValues(horizontal = 17.dp, vertical = 14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                JarvisOrb(diameter = 22.dp, active = true)
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "正在想下一个问题…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = NocturneColors.TextSecondary,
                )
            }
        }
    }
}

@Composable
private fun AskError(message: String, showSetup: Boolean, onOpenProviders: () -> Unit) {
    GlassSurface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Text(
            text = "信号中断",
            style = MaterialTheme.typography.labelSmall,
            color = NocturneColors.ChampagneLight,
        )
        Spacer(Modifier.height(5.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = NocturneColors.TextSecondary,
        )
        if (showSetup) {
            Spacer(Modifier.height(10.dp))
            Text(
                text = "去接入模型 →",
                style = MaterialTheme.typography.labelSmall,
                color = NocturneColors.ChampagneLight,
                textAlign = TextAlign.Start,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .clickable(onClick = onOpenProviders)
                    .padding(vertical = 4.dp, horizontal = 2.dp),
            )
        }
    }
}
