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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nocturne.learn.SessionMessage
import com.nocturne.learn.SessionViewModel
import com.nocturne.learn.data.learning.LearningState
import com.nocturne.learn.ui.design.GlassSurface
import com.nocturne.learn.ui.design.JarvisOrb
import com.nocturne.learn.ui.design.RowDot
import com.nocturne.learn.ui.theme.NocturneColors
import kotlinx.coroutines.delay

@Composable
fun SessionScreen(
    viewModel: SessionViewModel,
    onBack: () -> Unit,
) {
    val ui by viewModel.ui.collectAsState()
    val learning by viewModel.learningState.collectAsState()
    var input by rememberSaveable { mutableStateOf("") }
    val listState = rememberLazyListState()
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    val phase = phaseOf(learning)

    LaunchedEffect(Unit) { viewModel.start() }

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
        SessionHeader(phase = phase, onBack = onBack)

        LazyColumn(
            modifier = Modifier.weight(1f),
            state = listState,
            contentPadding = PaddingValues(start = 22.dp, end = 22.dp, top = 14.dp, bottom = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (ui.messages.isEmpty() && !ui.thinking && ui.error == null) {
                item { OpeningNote() }
            }
            ui.messages.forEach { message ->
                item(key = message.id) { MessageBubble(message) }
            }
            if (ui.thinking) {
                item(key = "thinking") { ThinkingBubble() }
            }
            ui.error?.let { error ->
                item(key = "error") { ErrorCard(error) }
            }
        }

        InputBar(
            value = input,
            onValueChange = { input = it },
            enabled = !ui.thinking,
            onSend = {
                val text = input
                input = ""
                focusManager.clearFocus()
                viewModel.send(text)
            },
        )
    }
}

@Composable
private fun SessionHeader(phase: Phase, onBack: () -> Unit) {
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
                text = "SESSION · 贾维斯",
                style = MaterialTheme.typography.labelSmall,
                color = NocturneColors.TextTertiary,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = phase.title,
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
            )
            Text(
                text = phase.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = NocturneColors.TextSecondary,
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            RowDot(NocturneColors.Champagne, 5.dp)
            Spacer(Modifier.width(6.dp))
            Text(
                text = "在线",
                style = MaterialTheme.typography.labelSmall,
                color = NocturneColors.ChampagneLight,
            )
        }
    }
}

@Composable
private fun OpeningNote() {
    Text(
        text = "诊断不需要全对。答不上来，正好说明该从哪里出发。",
        style = MaterialTheme.typography.bodySmall,
        color = NocturneColors.TextTertiary,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
    )
}

@Composable
private fun MessageBubble(message: SessionMessage) {
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
                    text = "贾维斯",
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
            if (message.notes.isNotEmpty()) {
                Spacer(Modifier.height(9.dp))
                message.notes.forEach { note ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 3.dp),
                    ) {
                        RowDot(NocturneColors.Moon.copy(alpha = 0.8f), 4.dp)
                        Spacer(Modifier.width(7.dp))
                        Text(
                            text = note,
                            style = MaterialTheme.typography.labelSmall,
                            color = NocturneColors.TextTertiary,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ThinkingBubble() {
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
                    text = "正在推演下一步…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = NocturneColors.TextSecondary,
                )
            }
        }
    }
}

@Composable
private fun ErrorCard(message: String) {
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
    }
}

@Composable
private fun InputBar(
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean,
    onSend: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 18.dp, vertical = 10.dp),
    ) {
        GlassSurface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            contentPadding = PaddingValues(horizontal = 17.dp, vertical = 7.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    enabled = enabled,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 44.dp),
                    textStyle = TextStyle(
                        color = Color.White,
                        fontSize = 15.sp,
                        lineHeight = 22.sp,
                    ),
                    cursorBrush = SolidColor(NocturneColors.Champagne),
                    maxLines = 4,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(
                        onSend = { if (value.isNotBlank()) onSend() },
                    ),
                    decorationBox = { inner ->
                        Box(contentAlignment = Alignment.CenterStart) {
                            if (value.isBlank()) {
                                Text(
                                    text = "回答，或者问一个问题…",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = NocturneColors.TextTertiary,
                                )
                            }
                            inner()
                        }
                    },
                )
                Spacer(Modifier.width(10.dp))
                val canSend = value.isNotBlank() && enabled
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            if (canSend) NocturneColors.Champagne
                            else Color.White.copy(alpha = 0.06f),
                        )
                        .clickable(enabled = canSend, onClick = onSend),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "↑",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Medium,
                        ),
                        color = if (canSend) NocturneColors.Ink
                        else NocturneColors.TextTertiary,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

private data class Phase(val title: String, val subtitle: String)

private fun phaseOf(state: LearningState): Phase {
    val due = state.dueCount()
    return when {
        state.knowledgePoints.isEmpty() -> Phase(
            title = "诊断 · 先知道你会在哪",
            subtitle = "一次一个问题，答不出也没关系。",
        )

        due > 0 -> Phase(
            title = "复习 · $due 个知识点到期",
            subtitle = "先把旧账还掉，再学新的。",
        )

        else -> {
            val target = state.nextTarget()
            Phase(
                title = "学习 · ${target?.title ?: "下一颗星"}",
                subtitle = "一次只点亮一颗星。",
            )
        }
    }
}
