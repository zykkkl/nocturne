package com.nocturne.learn.ui.screens

import android.app.Activity
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.nocturne.learn.AskViewModel
import com.nocturne.learn.data.AskImageStore
import com.nocturne.learn.data.ask.AskMessage
import com.nocturne.learn.ui.components.SourceLinks
import com.nocturne.learn.ui.components.AskHistorySheet
import com.nocturne.learn.ui.components.ChatInputBar
import com.nocturne.learn.ui.design.ChampagneButton
import com.nocturne.learn.ui.design.GlassSurface
import com.nocturne.learn.ui.design.JarvisOrb
import com.nocturne.learn.ui.design.RowDot
import com.nocturne.learn.ui.theme.NocturneColors
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
    val archive by viewModel.archive.collectAsState()
    var input by rememberSaveable { mutableStateOf("") }
    var pendingImage by rememberSaveable { mutableStateOf<String?>(null) }
    var showAttach by remember { mutableStateOf(false) }
    var showHistory by remember { mutableStateOf(false) }
    var captureFile by remember { mutableStateOf<File?>(null) }
    var captureUri by remember { mutableStateOf<Uri?>(null) }
    var attachError by remember { mutableStateOf<String?>(null) }
    val listState = rememberLazyListState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    fun useResult(uri: Uri, cleanup: File?) {
        scope.launch {
            val path = withContext(Dispatchers.IO) { AskImageStore.prepare(context, uri) }
            cleanup?.delete()
            if (path != null) {
                AskImageStore.delete(pendingImage)
                pendingImage = path
                attachError = null
            } else {
                attachError = "这张图片没有读出来，换一张试试。"
            }
            captureFile = null
            captureUri = null
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent(),
    ) { uri ->
        if (uri != null) useResult(uri, null)
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture(),
    ) { success ->
        val uri = captureUri
        val file = captureFile
        if (success && uri != null) {
            useResult(uri, file)
        } else {
            file?.delete()
            captureFile = null
            captureUri = null
        }
    }

    fun openCamera() {
        val file = AskImageStore.newCaptureFile(context)
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.updates",
            file,
        )
        captureFile = file
        captureUri = uri
        runCatching { cameraLauncher.launch(uri) }
            .onFailure {
                file.delete()
                captureFile = null
                captureUri = null
                attachError = "没有找到可用的相机，改从相册选择吧。"
            }
    }

    fun openGallery() {
        galleryLauncher.launch("image/*")
    }

    fun startNewConversation() {
        viewModel.newConversation()
        input = ""
        AskImageStore.delete(pendingImage)
        pendingImage = null
        attachError = null
        focusManager.clearFocus()
    }

    fun openConversation(conversationId: String) {
        viewModel.selectConversation(conversationId)
        input = ""
        AskImageStore.delete(pendingImage)
        pendingImage = null
        attachError = null
        showHistory = false
        focusManager.clearFocus()
    }

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
            onHistory = { showHistory = true },
            onNew = { startNewConversation() },
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

        attachError?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.labelSmall,
                color = NocturneColors.ChampagneLight,
                modifier = Modifier.padding(horizontal = 22.dp, vertical = 2.dp),
            )
        }

        pendingImage?.let { path ->
            PendingImageRow(
                path = path,
                onRemove = {
                    AskImageStore.delete(path)
                    pendingImage = null
                },
            )
        }

        ChatInputBar(
            value = input,
            onValueChange = { input = it },
            enabled = providerReady && !ui.thinking,
            onSend = {
                val text = input
                val image = pendingImage
                input = ""
                pendingImage = null
                attachError = null
                focusManager.clearFocus()
                viewModel.ask(text, image)
            },
            placeholder = "把卡住你的那道题说出来…",
            hasAttachment = pendingImage != null,
            onAttach = {
                if (providerReady && !ui.thinking) {
                    attachError = null
                    showAttach = true
                }
            },
        )
    }

    if (showAttach) {
        AlertDialog(
            onDismissRequest = { showAttach = false },
            containerColor = NocturneColors.Night900,
            shape = RoundedCornerShape(26.dp),
            title = {
                Text(
                    text = "添加一张图片",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                )
            },
            text = {
                Column {
                    AttachOption("拍照", "把题目拍下来") {
                        showAttach = false
                        openCamera()
                    }
                    Spacer(Modifier.height(8.dp))
                    AttachOption("从相册选择", "选一张题目截图或笔记") {
                        showAttach = false
                        openGallery()
                    }
                    Spacer(Modifier.height(14.dp))
                    Text(
                        text = "图片追问需要模型支持视觉输入，例如 GPT-4o、Gemini、Qwen-VL 等。",
                        style = MaterialTheme.typography.labelSmall,
                        color = NocturneColors.TextTertiary,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showAttach = false }) {
                    Text("取消", color = NocturneColors.TextTertiary)
                }
            },
        )
    }

    if (showHistory) {
        AskHistorySheet(
            conversations = archive.conversations,
            activeId = archive.activeId,
            onDismiss = { showHistory = false },
            onSelect = { openConversation(it) },
            onDelete = { viewModel.deleteConversation(it) },
            onClearAll = {
                viewModel.clearAllConversations()
                input = ""
                AskImageStore.delete(pendingImage)
                pendingImage = null
                attachError = null
            },
        )
    }
}

@Composable
private fun AttachOption(title: String, subtitle: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White,
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.labelSmall,
            color = NocturneColors.TextTertiary,
        )
    }
}

@Composable
private fun PendingImageRow(path: String, onRemove: () -> Unit) {
    val bitmap = remember(path) {
        BitmapFactory.decodeFile(path)?.asImageBitmap()
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = "待发送图片",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(12.dp)),
            )
            Spacer(Modifier.width(11.dp))
        }
        Text(
            text = "图片已添加 · 发送后由模型读取",
            style = MaterialTheme.typography.labelSmall,
            color = NocturneColors.TextSecondary,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = "移除",
            style = MaterialTheme.typography.labelSmall,
            color = NocturneColors.ChampagneLight,
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .clickable(onClick = onRemove)
                .padding(horizontal = 10.dp, vertical = 6.dp),
        )
    }
}

@Composable
private fun AskHeader(onBack: () -> Unit, onHistory: () -> Unit, onNew: () -> Unit) {
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
                text = "ASK · 独立追问",
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
            text = "历史",
            style = MaterialTheme.typography.labelSmall,
            color = NocturneColors.TextSecondary,
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .clickable(onClick = onHistory)
                .padding(horizontal = 9.dp, vertical = 7.dp),
        )
        Spacer(Modifier.width(1.dp))
        Text(
            text = "新对话",
            style = MaterialTheme.typography.labelSmall,
            color = NocturneColors.ChampagneLight,
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .clickable(onClick = onNew)
                .padding(horizontal = 9.dp, vertical = 7.dp),
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
            text = "把卡住你的那道题、那个概念，或者那句没看懂的话发给我；也可以拍下来，或从相册导入。我会一次问一个问题，陪你走到自己能说出答案的地方。",
            style = MaterialTheme.typography.bodyMedium,
            color = NocturneColors.TextSecondary,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = "这段追问独立于学习航线，不记录进度；对话会留在本机，可以随时新建或删除。",
            style = MaterialTheme.typography.labelSmall,
            color = NocturneColors.TextTertiary,
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
            message.imagePath?.let { path ->
                val bitmap = remember(path) {
                    BitmapFactory.decodeFile(path)?.asImageBitmap()
                }
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap,
                        contentDescription = "提问图片",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 190.dp)
                            .clip(RoundedCornerShape(14.dp)),
                    )
                    if (message.text.isNotBlank()) Spacer(Modifier.height(9.dp))
                }
            }
            if (message.text.isNotBlank()) {
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 15.sp,
                        lineHeight = 24.sp,
                    ),
                    color = if (message.fromUser) Color.White else NocturneColors.TextPrimary,
                )
            }
            if (message.sources.isNotEmpty()) {
                Spacer(Modifier.height(9.dp))
                SourceLinks(message.sources)
            }
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
