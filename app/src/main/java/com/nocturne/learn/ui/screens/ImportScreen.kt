package com.nocturne.learn.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nocturne.learn.ImportPoint
import com.nocturne.learn.ImportSection
import com.nocturne.learn.ImportUiState
import com.nocturne.learn.ImportViewModel
import com.nocturne.learn.ui.components.BackHeader
import com.nocturne.learn.ui.components.Hairline
import com.nocturne.learn.ui.design.ChampagneButton
import com.nocturne.learn.ui.design.GhostButton
import com.nocturne.learn.ui.design.GlassSurface
import com.nocturne.learn.ui.design.JarvisOrb
import com.nocturne.learn.ui.design.RowDot
import com.nocturne.learn.ui.design.SectionLabel
import com.nocturne.learn.ui.theme.NocturneColors

@Composable
fun ImportScreen(
    viewModel: ImportViewModel,
    onBack: () -> Unit,
    onImported: (String) -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) viewModel.start(uri)
    }
    val pick = {
        picker.launch(
            arrayOf(
                "application/pdf",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                "text/plain",
                "text/markdown",
                "text/x-markdown",
            ),
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .imePadding(),
    ) {
        BackHeader(title = "导入教材", onBack = onBack)
        when (val current = state) {
            ImportUiState.Idle -> IdleImport(onPick = pick)
            is ImportUiState.Parsing -> ProgressImport(
                title = "正在读这本书",
                message = current.message,
                progress = current.progress,
                sectionTitle = null,
                onCancel = viewModel::cancel,
            )
            is ImportUiState.Extracting -> ProgressImport(
                title = "正在拆知识点",
                message = "${current.current} / ${current.total} 段 · ${current.bookTitle}",
                progress = (current.current - 1).toFloat() / current.total,
                sectionTitle = current.sectionTitle,
                onCancel = viewModel::cancel,
            )
            is ImportUiState.Preview -> PreviewImport(
                state = current,
                onToggle = viewModel::togglePoint,
                onSelectAll = viewModel::selectAll,
                onClear = viewModel::clearSelection,
                onConfirm = { viewModel.confirm(onImported) },
            )
            ImportUiState.Saving -> ProgressImport(
                title = "正在建立航线",
                message = "把知识点写进本机…",
                progress = null,
                sectionTitle = null,
                onCancel = null,
            )
            is ImportUiState.Error -> ErrorImport(
                message = current.message,
                retryable = current.retryable,
                onRetry = viewModel::retry,
                onBack = onBack,
            )
        }
    }
}

@Composable
private fun IdleImport(onPick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
    ) {
        Spacer(Modifier.height(22.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            JarvisOrb(diameter = 46.dp)
            Spacer(Modifier.width(14.dp))
            Column {
                Text(
                    text = "把一本书交给夜航",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "AI 会先读懂目录与正文，再拆成可以逐个教学的知识点。",
                    style = MaterialTheme.typography.bodySmall,
                    color = NocturneColors.TextSecondary,
                )
            }
        }
        Spacer(Modifier.height(22.dp))
        GlassSurface {
            SectionLabel("支持格式")
            Spacer(Modifier.height(10.dp))
            Text(
                text = "文字版 PDF · DOCX · PPTX · TXT · Markdown",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = "扫描版 PDF 暂不支持；旧版 .doc / .ppt 请先另存为 .docx / .pptx。一次最多 40 段，超长教材请按章节拆分。",
                style = MaterialTheme.typography.bodySmall,
                color = NocturneColors.TextSecondary,
            )
        }
        Spacer(Modifier.height(16.dp))
        GlassSurface {
            SectionLabel("拆解流程")
            Spacer(Modifier.height(10.dp))
            StepLine("1", "本地读取文字，不把整份文件上传")
            StepLine("2", "按页/章节分段，逐段交给你的模型")
            StepLine("3", "预览勾选，建立一条独立的教材航线")
        }
        Spacer(Modifier.weight(1f))
        ChampagneButton(
            text = "选择教材文件",
            onClick = onPick,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = "模型会看到分段后的教材文字；请确认你有权使用这份材料。",
            style = MaterialTheme.typography.labelSmall,
            color = NocturneColors.TextTertiary,
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun StepLine(index: String, text: String) {
    Row(modifier = Modifier.padding(vertical = 5.dp)) {
        Text(
            text = index,
            style = MaterialTheme.typography.labelSmall,
            color = NocturneColors.ChampagneLight,
            modifier = Modifier.width(22.dp),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = NocturneColors.TextSecondary,
        )
    }
}

@Composable
private fun ProgressImport(
    title: String,
    message: String,
    progress: Float?,
    sectionTitle: String?,
    onCancel: (() -> Unit)?,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 30.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(1f))
        JarvisOrb(diameter = 64.dp)
        Spacer(Modifier.height(22.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = NocturneColors.TextSecondary,
        )
        Spacer(Modifier.height(18.dp))
        if (progress != null) {
            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
                color = NocturneColors.Champagne,
                trackColor = Color.White.copy(alpha = 0.10f),
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "${(progress.coerceIn(0f, 1f) * 100).toInt()}%",
                style = MaterialTheme.typography.labelSmall,
                color = NocturneColors.TextTertiary,
            )
        } else {
            CircularProgressIndicator(color = NocturneColors.Champagne)
        }
        if (sectionTitle != null) {
            Spacer(Modifier.height(16.dp))
            Text(
                text = "正在读：$sectionTitle",
                style = MaterialTheme.typography.bodySmall,
                color = NocturneColors.TextTertiary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.height(24.dp))
        Text(
            text = "长教材会花一些时间；请保持页面打开，或者随时取消。",
            style = MaterialTheme.typography.labelSmall,
            color = NocturneColors.TextTertiary,
        )
        Spacer(Modifier.weight(1f))
        if (onCancel != null) {
            GhostButton(text = "取消导入", onClick = onCancel, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(30.dp))
        }
    }
}

@Composable
private fun PreviewImport(
    state: ImportUiState.Preview,
    onToggle: (String) -> Unit,
    onSelectAll: () -> Unit,
    onClear: () -> Unit,
    onConfirm: () -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 14.dp, bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Column {
                    Text(
                        text = state.title,
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "${state.format.label} · ${state.sections.size} 段 · 已选 ${state.selectedCount} 个知识点",
                        style = MaterialTheme.typography.bodySmall,
                        color = NocturneColors.TextTertiary,
                    )
                }
            }
            if (state.warnings.isNotEmpty()) {
                item {
                    GlassSurface {
                        SectionLabel("需要留意")
                        Spacer(Modifier.height(8.dp))
                        state.warnings.take(3).forEach { warning ->
                            Text(
                                text = "· $warning",
                                style = MaterialTheme.typography.bodySmall,
                                color = NocturneColors.ChampagneLight,
                            )
                        }
                    }
                }
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    GhostButton(text = "全选", onClick = onSelectAll, modifier = Modifier.weight(1f))
                    GhostButton(text = "全不选", onClick = onClear, modifier = Modifier.weight(1f))
                }
            }
            itemsIndexed(state.sections, key = { _, section -> section.title + section.sourceRef }) { index, section ->
                SectionCard(index = index, section = section, selected = state.selected, onToggle = onToggle)
            }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(NocturneColors.Night950.copy(alpha = 0.92f))
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 12.dp),
        ) {
            ChampagneButton(
                text = if (state.selectedCount == 0) "至少选择一个知识点" else "建立教材航线 · ${state.selectedCount} 个知识点",
                onClick = onConfirm,
                enabled = state.selectedCount > 0,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun SectionCard(
    index: Int,
    section: ImportSection,
    selected: Set<String>,
    onToggle: (String) -> Unit,
) {
    GlassSurface {
        val source = section.sourceRef.takeIf { it.isNotBlank() && it != section.title }
        SectionLabel(
            buildString {
                append("第 ${index + 1} 段")
                if (source != null) append(" · $source")
            },
        )
        Spacer(Modifier.height(7.dp))
        Text(
            text = section.title,
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
        )
        if (section.summary.isNotBlank()) {
            Spacer(Modifier.height(5.dp))
            Text(
                text = section.summary,
                style = MaterialTheme.typography.bodySmall,
                color = NocturneColors.TextSecondary,
            )
        }
        Spacer(Modifier.height(10.dp))
        Hairline()
        section.points.forEach { point ->
            PointRow(point = point, checked = point.id in selected, onToggle = onToggle)
        }
    }
}

@Composable
private fun PointRow(point: ImportPoint, checked: Boolean, onToggle: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = { onToggle(point.id) },
            colors = CheckboxDefaults.colors(
                checkedColor = NocturneColors.Champagne,
                uncheckedColor = Color.White.copy(alpha = 0.35f),
                checkmarkColor = NocturneColors.Ink,
            ),
        )
        Column(Modifier.weight(1f)) {
            Text(
                text = point.title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = if (checked) Color.White else NocturneColors.TextTertiary,
            )
            if (point.note.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = point.note,
                    style = MaterialTheme.typography.labelSmall,
                    color = NocturneColors.TextTertiary,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun ErrorImport(
    message: String,
    retryable: Boolean,
    onRetry: () -> Unit,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(1f))
        JarvisOrb(diameter = 58.dp, active = false)
        Spacer(Modifier.height(18.dp))
        Text(
            text = "这本书没有拆成",
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = NocturneColors.TextSecondary,
        )
        Spacer(Modifier.height(24.dp))
        if (retryable) {
            ChampagneButton(text = "再试一次", onClick = onRetry, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(10.dp))
        }
        GhostButton(text = "换一个文件", onClick = onBack, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.weight(1f))
    }
}
