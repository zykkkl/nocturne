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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import com.nocturne.learn.data.ProviderConfig
import com.nocturne.learn.data.ProviderTemplate
import com.nocturne.learn.data.ProviderTemplates
import com.nocturne.learn.ui.components.BackHeader
import com.nocturne.learn.ui.components.EmptyState
import com.nocturne.learn.ui.components.Hairline
import com.nocturne.learn.ui.components.ProviderMonogram
import com.nocturne.learn.ui.components.QuietNote
import com.nocturne.learn.ui.design.ChampagneButton
import com.nocturne.learn.ui.design.GlassSurface
import com.nocturne.learn.ui.design.JarvisOrb
import com.nocturne.learn.ui.design.RowDot
import com.nocturne.learn.ui.design.SectionLabel
import com.nocturne.learn.ui.theme.NocturneColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderListScreen(
    providers: List<ProviderConfig>,
    activeProviderId: String?,
    onBack: () -> Unit,
    onAdd: (String) -> Unit,
    onEdit: (String) -> Unit,
    onActivate: (String) -> Unit,
) {
    var showTemplateSheet by remember { mutableStateOf(false) }
    val active = providers.firstOrNull { it.id == activeProviderId } ?: providers.firstOrNull()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
    ) {
        Spacer(Modifier.height(18.dp))
        BackHeader(title = "模型服务", onBack = onBack)
        Spacer(Modifier.height(18.dp))
        Text(
            text = "夜航使用你自己的模型。密钥只保存在本机，请求从手机直接发往服务商。",
            style = MaterialTheme.typography.bodyMedium,
            color = NocturneColors.TextSecondary,
        )
        Spacer(Modifier.height(22.dp))

        if (active != null) {
            GlassSurface {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        SectionLabel("当前引擎")
                        Spacer(Modifier.height(7.dp))
                        Text(
                            text = active.displayName,
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.White,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = active.model,
                            style = MaterialTheme.typography.bodySmall,
                            color = NocturneColors.TextTertiary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    JarvisOrb(diameter = 44.dp, active = true)
                }
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RowDot(NocturneColors.Champagne.copy(alpha = 0.8f), 5.dp)
                    Text(
                        text = "  已接入 · 请求直连",
                        style = MaterialTheme.typography.bodySmall,
                        color = NocturneColors.TextSecondary,
                    )
                }
            }
            Spacer(Modifier.height(22.dp))
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = if (providers.isEmpty()) "还没有服务" else "全部服务",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "${providers.size} 个",
                style = MaterialTheme.typography.labelSmall,
                color = NocturneColors.TextTertiary,
            )
        }
        Spacer(Modifier.height(10.dp))

        if (providers.isEmpty()) {
            EmptyState(
                title = "还没有接入模型",
                body = "夜航需要一个大模型作为思考引擎。填入你自己的 API Key，它只会存在这台设备上。",
                actionText = "选择服务商",
                onAction = { showTemplateSheet = true },
            )
        } else {
            GlassSurface(contentPadding = PaddingValues(horizontal = 18.dp, vertical = 6.dp)) {
                providers.forEachIndexed { index, provider ->
                    ProviderRow(
                        provider = provider,
                        active = provider.id == active?.id,
                        onEdit = { onEdit(provider.id) },
                        onActivate = { onActivate(provider.id) },
                    )
                    if (index != providers.lastIndex) {
                        Hairline()
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            ChampagneButton(
                text = "添加服务商",
                onClick = { showTemplateSheet = true },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Spacer(Modifier.height(16.dp))
        QuietNote(
            "密钥使用 Android Keystore 加密。网络请求不经过夜航的服务器——夜航没有服务器。",
        )
        Spacer(Modifier.height(30.dp))
    }

    if (showTemplateSheet) {
        ModalBottomSheet(
            onDismissRequest = { showTemplateSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = NocturneColors.Night900,
            contentColor = Color.White,
            scrimColor = Color.Black.copy(alpha = 0.55f),
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(vertical = 14.dp)
                        .size(width = 36.dp, height = 4.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.18f)),
                )
            },
        ) {
            TemplateSheetContent(
                onPick = { template ->
                    showTemplateSheet = false
                    onAdd(template.id)
                },
            )
        }
    }
}

@Composable
private fun ProviderRow(
    provider: ProviderConfig,
    active: Boolean,
    onEdit: () -> Unit,
    onActivate: () -> Unit,
) {
    val monogram = ProviderTemplates.byId(provider.templateId)?.monogram ?: "API"
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onEdit)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ProviderMonogram(text = monogram, highlighted = active)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = provider.displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (active) {
                    Spacer(Modifier.width(8.dp))
                    ActiveTag()
                }
            }
            Spacer(Modifier.height(3.dp))
            Text(
                text = provider.model,
                style = MaterialTheme.typography.bodySmall,
                color = NocturneColors.TextTertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (!active) {
            Text(
                text = "设为当前",
                style = MaterialTheme.typography.labelSmall,
                color = NocturneColors.ChampagneLight,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .clickable(onClick = onActivate)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            )
        }
    }
}

@Composable
private fun ActiveTag() {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(NocturneColors.Champagne.copy(alpha = 0.13f))
            .border(1.dp, NocturneColors.Champagne.copy(alpha = 0.30f), RoundedCornerShape(50))
            .padding(horizontal = 8.dp, vertical = 2.dp),
    ) {
        Text(
            text = "当前",
            style = MaterialTheme.typography.labelSmall,
            color = NocturneColors.ChampagneLight,
        )
    }
}

@Composable
private fun TemplateSheetContent(onPick: (ProviderTemplate) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 22.dp)
            .padding(bottom = 24.dp),
    ) {
        Text(
            text = "选择服务商",
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "都走 OpenAI 兼容协议。选模板后，名称、地址、模型都可以改。",
            style = MaterialTheme.typography.bodySmall,
            color = NocturneColors.TextTertiary,
        )
        Spacer(Modifier.height(14.dp))
        LazyColumn(modifier = Modifier.heightIn(max = 520.dp)) {
            items(ProviderTemplates.all, key = { it.id }) { template ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onPick(template) }
                        .padding(horizontal = 8.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ProviderMonogram(text = template.monogram)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = template.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = template.note,
                            style = MaterialTheme.typography.bodySmall,
                            color = NocturneColors.TextTertiary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
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
    }
}
