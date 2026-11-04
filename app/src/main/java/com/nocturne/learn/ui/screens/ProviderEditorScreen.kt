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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nocturne.learn.data.ConnectionResult
import com.nocturne.learn.data.ProviderConfig
import com.nocturne.learn.data.ProviderTemplate
import com.nocturne.learn.data.ProviderTemplates
import com.nocturne.learn.ui.components.BackHeader
import com.nocturne.learn.ui.components.Hairline
import com.nocturne.learn.ui.components.ProviderMonogram
import com.nocturne.learn.ui.components.QuietNote
import com.nocturne.learn.ui.design.ChampagneButton
import com.nocturne.learn.ui.design.GhostButton
import com.nocturne.learn.ui.design.GlassSurface
import com.nocturne.learn.ui.design.NocturneTextField
import com.nocturne.learn.ui.design.RowDot
import com.nocturne.learn.ui.design.SectionLabel
import com.nocturne.learn.ui.theme.NocturneColors
import java.util.UUID
import kotlinx.coroutines.launch

@Composable
fun ProviderEditorScreen(
    template: ProviderTemplate?,
    provider: ProviderConfig?,
    savedKey: String?,
    isActive: Boolean,
    onBack: () -> Unit,
    onTest: suspend (String, String) -> ConnectionResult,
    onSave: (ProviderConfig, String?, Boolean) -> Unit,
    onDelete: (() -> Unit)? = null,
) {
    var displayName by remember { mutableStateOf(provider?.displayName ?: template?.name.orEmpty()) }
    var baseUrl by remember { mutableStateOf(provider?.baseUrl ?: template?.baseUrl.orEmpty()) }
    var model by remember { mutableStateOf(provider?.model ?: template?.model.orEmpty()) }
    var apiKey by remember { mutableStateOf("") }
    var keyVisible by remember { mutableStateOf(false) }
    var streaming by remember { mutableStateOf(provider?.streaming ?: true) }
    var toolCalling by remember { mutableStateOf(provider?.toolCalling ?: true) }
    var makeActive by remember { mutableStateOf(isActive) }
    var isTesting by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<ConnectionResult?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val monogram = template?.monogram
        ?: provider?.let { ProviderTemplates.byId(it.templateId)?.monogram }
        ?: "API"
    val canSave = displayName.isNotBlank() && baseUrl.isNotBlank() && model.isNotBlank()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
    ) {
        Spacer(Modifier.height(18.dp))
        BackHeader(
            title = if (provider == null) "接入模型" else "编辑模型",
            onBack = onBack,
        )
        Spacer(Modifier.height(18.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            ProviderMonogram(text = monogram, size = 48.dp, highlighted = true)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text = displayName.ifBlank { "新的服务" },
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = if (provider == null) "尚未保存" else "已保存到本机",
                    style = MaterialTheme.typography.bodySmall,
                    color = NocturneColors.TextTertiary,
                )
            }
        }

        Spacer(Modifier.height(24.dp))
        SectionLabel("连接")
        Spacer(Modifier.height(10.dp))
        NocturneTextField(
            value = displayName,
            onValueChange = { displayName = it.take(40) },
            label = "名称",
            placeholder = "给这个服务起个名字",
        )
        Spacer(Modifier.height(12.dp))
        NocturneTextField(
            value = baseUrl,
            onValueChange = { baseUrl = it },
            label = "Base URL",
            placeholder = "https://api.example.com/v1",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
        )
        Spacer(Modifier.height(12.dp))
        NocturneTextField(
            value = model,
            onValueChange = { model = it },
            label = "模型",
            placeholder = "例如 deepseek-chat",
        )
        Spacer(Modifier.height(12.dp))
        NocturneTextField(
            value = apiKey,
            onValueChange = { apiKey = it },
            label = "API Key",
            placeholder = when {
                savedKey != null -> "已保存，留空则不修改"
                template?.id == "ollama" -> "本地服务通常无需密钥"
                else -> "sk-…"
            },
            visualTransformation = if (keyVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                Text(
                    text = if (keyVisible) "隐藏" else "显示",
                    style = MaterialTheme.typography.labelSmall,
                    color = NocturneColors.TextTertiary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable { keyVisible = !keyVisible }
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                )
            },
        )
        Spacer(Modifier.height(10.dp))
        QuietNote("夜航会自己拼上 /models 与 /chat/completions。地址填到 /v1 即可。")

        Spacer(Modifier.height(22.dp))
        SectionLabel("模型行为")
        Spacer(Modifier.height(10.dp))
        GlassSurface(contentPadding = PaddingValues(horizontal = 18.dp, vertical = 4.dp)) {
            ToggleRow(
                title = "流式输出",
                subtitle = "回答逐字出现；少数兼容服务不支持。",
                checked = streaming,
                onCheckedChange = { streaming = it },
            )
            Hairline()
            ToggleRow(
                title = "工具调用",
                subtitle = "让 AI 能更新掌握度、安排复习；需要模型支持。",
                checked = toolCalling,
                onCheckedChange = { toolCalling = it },
            )
            Hairline()
            if (isActive) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = "当前引擎",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "正在被夜航使用",
                            style = MaterialTheme.typography.bodySmall,
                            color = NocturneColors.TextTertiary,
                        )
                    }
                    RowDot(NocturneColors.Champagne, 6.dp)
                }
            } else {
                ToggleRow(
                    title = "设为当前引擎",
                    subtitle = "保存后立即生效。",
                    checked = makeActive,
                    onCheckedChange = { makeActive = it },
                )
            }
        }

        Spacer(Modifier.height(18.dp))
        testResult?.let { result ->
            val tone = if (result.success) NocturneColors.ChampagneLight else Color(0xFFE08A8A)
            GlassSurface(contentPadding = PaddingValues(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RowDot(if (result.success) NocturneColors.Champagne else tone, 6.dp)
                    Text(
                        text = "  ${if (result.success) "握手成功" else "连接失败"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = tone,
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = result.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = NocturneColors.TextSecondary,
                )
                if (result.models.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = result.models.joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = NocturneColors.TextTertiary,
                        maxLines = 2,
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
        }

        GhostButton(
            text = if (isTesting) "正在握手…" else "测试连接",
            onClick = {
                scope.launch {
                    isTesting = true
                    testResult = null
                    testResult = runCatching {
                        onTest(baseUrl.trim(), apiKey.ifBlank { savedKey.orEmpty() })
                    }.getOrElse { error ->
                        ConnectionResult(false, error.message ?: "连接失败")
                    }
                    isTesting = false
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isTesting && baseUrl.isNotBlank(),
        )
        Spacer(Modifier.height(10.dp))
        ChampagneButton(
            text = "保存",
            onClick = {
                onSave(
                    ProviderConfig(
                        id = provider?.id ?: UUID.randomUUID().toString(),
                        templateId = provider?.templateId ?: template?.id ?: "custom",
                        displayName = displayName.trim(),
                        baseUrl = baseUrl.trim(),
                        model = model.trim(),
                        streaming = streaming,
                        toolCalling = toolCalling,
                    ),
                    apiKey.trim().ifBlank { null },
                    makeActive,
                )
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = canSave && !isTesting,
        )

        if (onDelete != null) {
            Spacer(Modifier.height(16.dp))
            Text(
                text = "移除此服务",
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFFE08A8A),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { showDeleteConfirm = true }
                    .padding(vertical = 12.dp),
            )
        }
        Spacer(Modifier.height(30.dp))
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = NocturneColors.Night900,
            titleContentColor = Color.White,
            textContentColor = NocturneColors.TextSecondary,
            shape = RoundedCornerShape(26.dp),
            title = {
                Text(
                    text = "移除这个服务？",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                )
            },
            text = {
                Text(
                    text = "密钥会一并删除。已经生成的学习计划与进度不受影响。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = NocturneColors.TextSecondary,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete?.invoke()
                    },
                ) {
                    Text("移除", color = Color(0xFFE08A8A))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("取消", color = NocturneColors.TextTertiary)
                }
            },
        )
    }
}

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = NocturneColors.TextTertiary,
            )
        }
        Spacer(Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = NocturneColors.Ink,
                checkedTrackColor = NocturneColors.Champagne,
                uncheckedThumbColor = NocturneColors.TextSecondary,
                uncheckedTrackColor = Color.White.copy(alpha = 0.10f),
                uncheckedBorderColor = Color.White.copy(alpha = 0.18f),
            ),
        )
    }
}
