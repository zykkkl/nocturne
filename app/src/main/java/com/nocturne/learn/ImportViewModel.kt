package com.nocturne.learn

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nocturne.learn.data.learning.LearningRepository
import com.nocturne.learn.data.textbook.BookFormat
import com.nocturne.learn.data.textbook.ImportedBookFile
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ImportPoint(
    val id: String,
    val title: String,
    val note: String,
    val sourceRef: String,
)

data class ImportSection(
    val title: String,
    val summary: String,
    val sourceRef: String,
    val excerpt: String,
    val points: List<ImportPoint>,
)

sealed interface ImportUiState {
    data object Idle : ImportUiState
    data class Parsing(val message: String, val progress: Float) : ImportUiState
    data class Extracting(
        val bookTitle: String,
        val current: Int,
        val total: Int,
        val sectionTitle: String,
    ) : ImportUiState
    data class Preview(
        val title: String,
        val format: BookFormat,
        val sections: List<ImportSection>,
        val selected: Set<String>,
        val warnings: List<String>,
    ) : ImportUiState {
        val allIds: Set<String> get() = sections.flatMap { s -> s.points.map { it.id } }.toSet()
        val selectedCount: Int get() = allIds.count { it in selected }
    }
    data object Saving : ImportUiState
    data class Error(val message: String, val retryable: Boolean) : ImportUiState
}

class ImportViewModel(application: Application) : AndroidViewModel(application) {

    private val container = (application as NocturneApp).container
    private val parser = container.textbookParser
    private val engine = container.textbookEngine
    private val learning = container.learningRepository
    private val settingsRepository = container.settingsRepository
    private val secretStore = container.secretStore

    private val _state = MutableStateFlow<ImportUiState>(ImportUiState.Idle)
    val state: StateFlow<ImportUiState> = _state.asStateFlow()

    private var lastUri: Uri? = null
    private var activeBook: ImportedBookFile? = null
    private var job: kotlinx.coroutines.Job? = null

    fun start(uri: Uri) {
        lastUri = uri
        job?.cancel()
        job = viewModelScope.launch {
            var book: ImportedBookFile? = null
            try {
                _state.value = ImportUiState.Parsing("正在读取文件…", 0f)
                val settings = settingsRepository.settings.first()
                val provider = settings.activeProvider
                    ?: throw IllegalStateException("还没有接入模型，先去「模型服务」配置一个服务商")
                val apiKey = secretStore.get(provider.id)
                    ?: throw IllegalStateException("这个服务商还没有保存 API Key")

                book = parser.copyToPrivate(uri)
                activeBook = book
                val parsed = parser.parse(book) { message, progress ->
                    _state.value = ImportUiState.Parsing(message, progress)
                }
                if (parsed.sections.isEmpty()) {
                    throw IllegalStateException("没有从文件里读到可用内容")
                }
                if (parsed.sections.size > MAX_SECTIONS) {
                    throw IllegalStateException(
                        "这本教材被拆成 ${parsed.sections.size} 段，第一版一次最多处理 $MAX_SECTIONS 段。请先按章节拆分后再导入。",
                    )
                }

                val sections = mutableListOf<ImportSection>()
                val warnings = mutableListOf<String>()
                parsed.sections.forEachIndexed { index, section ->
                    ensureActive()
                    _state.value = ImportUiState.Extracting(
                        bookTitle = parsed.title,
                        current = index + 1,
                        total = parsed.sections.size,
                        sectionTitle = section.title,
                    )
                    try {
                        val result = engine.extract(provider, apiKey, section)
                        sections += ImportSection(
                            title = result.title,
                            summary = result.summary,
                            sourceRef = section.sourceRef,
                            excerpt = section.text.take(700),
                            points = result.points.map { point ->
                                ImportPoint(
                                    id = UUID.randomUUID().toString(),
                                    title = point.title,
                                    note = point.note,
                                    sourceRef = point.sourceRef,
                                )
                            },
                        )
                    } catch (error: Exception) {
                        if (error is CancellationException) throw error
                        warnings += "${section.title}：${error.message ?: "拆解失败"}"
                    }
                }
                if (sections.sumOf { it.points.size } == 0) {
                    throw IllegalStateException(
                        warnings.firstOrNull() ?: "AI 没有从教材里拆出知识点，请换一个文件或模型试试",
                    )
                }
                val all = sections.flatMap { it.points }.map { it.id }.toSet()
                _state.value = ImportUiState.Preview(
                    title = parsed.title,
                    format = parsed.format,
                    sections = sections,
                    selected = all,
                    warnings = warnings,
                )
                parser.delete(book)
                activeBook = null
            } catch (error: CancellationException) {
                parser.delete(book)
                activeBook = null
                _state.value = ImportUiState.Idle
                throw error
            } catch (error: Exception) {
                parser.delete(book)
                activeBook = null
                _state.value = ImportUiState.Error(
                    message = error.message ?: "导入失败，请重试",
                    retryable = lastUri != null,
                )
            }
        }
    }

    fun retry() {
        lastUri?.let(::start)
    }

    fun cancel() {
        job?.cancel()
        parser.delete(activeBook)
        activeBook = null
        _state.value = ImportUiState.Idle
    }

    fun togglePoint(id: String) {
        val current = _state.value as? ImportUiState.Preview ?: return
        val selected = if (id in current.selected) current.selected - id else current.selected + id
        _state.value = current.copy(selected = selected)
    }

    fun selectAll() {
        val current = _state.value as? ImportUiState.Preview ?: return
        _state.value = current.copy(selected = current.allIds)
    }

    fun clearSelection() {
        val current = _state.value as? ImportUiState.Preview ?: return
        _state.value = current.copy(selected = emptySet())
    }

    fun confirm(onImported: (String) -> Unit) {
        val preview = _state.value as? ImportUiState.Preview ?: return
        val points = preview.sections.flatMap { section ->
            section.points.filter { it.id in preview.selected }.map { point ->
                LearningRepository.ImportedPoint(
                    title = point.title,
                    note = point.note,
                    chapter = section.title,
                    sourceRef = point.sourceRef.ifBlank { section.sourceRef },
                    sourceExcerpt = section.excerpt,
                )
            }
        }
        if (points.isEmpty()) {
            _state.value = preview.copy(warnings = preview.warnings + "至少选择一个知识点")
            return
        }
        _state.value = ImportUiState.Saving
        viewModelScope.launch {
            try {
                val subject = withContext(Dispatchers.IO) {
                    val plan = preview.sections.joinToString("\n") { "· ${it.title}" }
                    val imported = learning.importTextbook(preview.title, plan, points)
                    settingsRepository.updatePlan(imported.goal, imported.dailyMinutes)
                    parser.delete(activeBook)
                    activeBook = null
                    imported
                }
                _state.value = ImportUiState.Idle
                onImported(subject.goal)
            } catch (error: Exception) {
                _state.value = ImportUiState.Error(
                    message = error.message ?: "保存失败",
                    retryable = false,
                )
            }
        }
    }

    private companion object {
        const val MAX_SECTIONS = 40
    }
}
