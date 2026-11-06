package com.nocturne.learn.ui.screens

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.nocturne.learn.data.AppSettings
import com.nocturne.learn.data.search.SearchProviders
import com.nocturne.learn.data.search.WebSearchSource
import com.nocturne.learn.ui.components.BackHeader
import com.nocturne.learn.ui.components.QuietNote
import com.nocturne.learn.ui.design.ChampagneButton
import com.nocturne.learn.ui.design.GlassSurface
import com.nocturne.learn.ui.design.GhostButton
import com.nocturne.learn.ui.design.NocturneChip
import com.nocturne.learn.ui.design.NocturneTextField
import com.nocturne.learn.ui.design.RowDot
import com.nocturne.learn.ui.design.SectionLabel
import com.nocturne.learn.ui.theme.NocturneColors
import kotlinx.coroutines.launch

@Composable
fun SearchSettingsScreen(
    settings: AppSettings,
    savedKey: (String) -> String?,
    onBack: () -> Unit,
    onSave: (Boolean, String, String?) -> Unit,
    onTest: suspend (String, String, String) -> List<WebSearchSource>,
) {
    var enabled by remember { mutableStateOf(settings.searchEnabled) }
    var providerId by remember { mutableStateOf(settings.searchProviderId) }
    var apiKey by remember { mutableStateOf("") }
    var reveal by remember { mutableStateOf(false) }
    var testing by remember { mutableStateOf(false) }
    var testMessage by remember { mutableStateOf<String?>(null) }
    var testOk by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val provider = SearchProviders.byId(providerId)
    val existingKey = remember(providerId) { savedKey(providerId) }
    val effectiveKey = apiKey.trim().ifBlank { existingKey.orEmpty() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
    ) {
        Spacer(Modifier.height(18.dp))
        BackHeader(title = "联网搜索", onBack = onBack)
        Spacer(Modifier.height(18.dp))

        GlassSurface {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "让贾维斯查资料",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "需要核实事实、查最新信息时，模型可以主动搜索。搜索用独立的 Key，不经过模型服务商。",
                        style = MaterialTheme.typography.bodySmall,
                        color = NocturneColors.TextSecondary,
                    )
                }
                Switch(
                    checked = enabled,
                    onCheckedChange = {
                        enabled = it
                        testMessage = null
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = NocturneColors.Ink,
                        checkedTrackColor = NocturneColors.Champagne,
                        uncheckedThumbColor = NocturneColors.TextTertiary,
                        uncheckedTrackColor = Color.White.copy(alpha = 0.08f),
                        uncheckedBorderColor = Color.White.copy(alpha = 0.15f),
                    ),
                )
            }
        }

        Spacer(Modifier.height(22.dp))
        SectionLabel("搜索服务")
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SearchProviders.all.forEach { item ->
                NocturneChip(
                    text = item.label,
                    selected = providerId == item.id,
                    onClick = {
                        providerId = item.id
                        apiKey = ""
                        testMessage = null
                        error = null
                    },
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            RowDot(NocturneColors.Champagne.copy(alpha = 0.8f), diameter = 4.dp)
            Text(
                text = "  ${provider.note}",
                style = MaterialTheme.typography.labelSmall,
                color = NocturneColors.TextTertiary,
            )
        }

        Spacer(Modifier.height(16.dp))
        NocturneTextField(
            value = apiKey,
            onValueChange = { apiKey = it.trim() },
            label = "${provider.label} API Key",
            placeholder = if (existingKey.isNullOrBlank()) provider.keyHint else "已保存，留空不改",
            singleLine = true,
            visualTransformation = if (reveal) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            trailingIcon = {
                Text(
                    text = if (reveal) "隐藏" else "显示",
                    style = MaterialTheme.typography.labelSmall,
                    color = NocturneColors.TextTertiary,
                    modifier = Modifier
                        .clickable { reveal = !reveal }
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                )
            },
        )
        Spacer(Modifier.height(8.dp))
        QuietNote("Key 只保存在这台设备，使用 Android Keystore 加密。")

        Spacer(Modifier.height(18.dp))
        GhostButton(
            text = if (testing) "正在搜索…" else "测试连接",
            onClick = {
                if (effectiveKey.isBlank()) {
                    error = "先填入搜索 API Key"
                    return@GhostButton
                }
                testing = true
                error = null
                testMessage = null
                scope.launch {
                    runCatching {
                        onTest(providerId, effectiveKey, "夜航 Nocturne 安卓应用")
                    }.onSuccess { results ->
                        testOk = results.isNotEmpty()
                        testMessage = if (results.isEmpty()) {
                            "连接成功，但没有返回结果。"
                        } else {
                            "连接成功 · ${results.size} 条结果 · ${results.first().title}"
                        }
                    }.onFailure {
                        testOk = false
                        testMessage = it.message ?: "测试失败"
                    }
                    testing = false
                }
            },
            enabled = !testing,
            modifier = Modifier.fillMaxWidth(),
        )
        testMessage?.let { message ->
            Spacer(Modifier.height(10.dp))
            GlassSurface(
                shape = RoundedCornerShape(16.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(14.dp),
            ) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (testOk) NocturneColors.ChampagneLight else NocturneColors.TextSecondary,
                )
            }
        }
        error?.let { message ->
            Spacer(Modifier.height(10.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFE7A28D),
            )
        }

        Spacer(Modifier.height(24.dp))
        ChampagneButton(
            text = "保存",
            onClick = {
                if (enabled && effectiveKey.isBlank()) {
                    error = "开启联网搜索前，先填入搜索 API Key"
                } else {
                    onSave(enabled, providerId, apiKey.trim().ifBlank { null })
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(24.dp))
        QuietNote("搜索结果是参考资料，不是指令。模型会标注来源；查不到时应直接说明。")
        Spacer(Modifier.height(110.dp))
    }
}
