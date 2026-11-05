package com.nocturne.learn

import android.app.Application
import android.util.Base64
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nocturne.learn.ai.ChatTurn
import com.nocturne.learn.ai.ModelException
import java.io.File
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class AskMessage(
    val id: String = UUID.randomUUID().toString(),
    val fromUser: Boolean,
    val text: String,
    val imagePath: String? = null,
)

data class AskUiState(
    val messages: List<AskMessage> = emptyList(),
    val thinking: Boolean = false,
    val error: String? = null,
)

class AskViewModel(application: Application) : AndroidViewModel(application) {

    private val container = (application as NocturneApp).container
    private val settingsRepository = container.settingsRepository
    private val engine = container.askEngine

    private val _ui = MutableStateFlow(AskUiState())
    val ui: StateFlow<AskUiState> = _ui.asStateFlow()

    private val history = mutableListOf<ChatTurn>()

    fun ask(text: String, imagePath: String? = null) {
        val clean = text.trim()
        if (clean.isEmpty() && imagePath == null) return
        if (_ui.value.thinking) return
        viewModelScope.launch {
            val settings = settingsRepository.settings.first()
            val provider = settings.activeProvider
            val key = provider?.let { container.secretStore.get(it.id) }
            if (provider == null || key.isNullOrBlank()) {
                _ui.update { it.copy(error = "还没有接入模型。先回到今日页，接入一个模型再问。") }
                return@launch
            }
            val imageData = try {
                imagePath?.let { path ->
                    withContext(Dispatchers.IO) {
                        val bytes = File(path).readBytes()
                        if (bytes.isEmpty()) throw IllegalStateException("图片为空")
                        if (bytes.size > 4 * 1024 * 1024) {
                            throw IllegalStateException("图片太大，请重新选择")
                        }
                        "data:image/jpeg;base64," +
                            Base64.encodeToString(bytes, Base64.NO_WRAP)
                    }
                }
            } catch (error: Throwable) {
                _ui.update { it.copy(error = "图片没有读出来 · ${error.message ?: "未知错误"}") }
                return@launch
            }
            val modelText = clean.ifBlank { "请看我发的这张图片。" }
            _ui.update {
                it.copy(
                    messages = it.messages + AskMessage(
                        fromUser = true,
                        text = clean,
                        imagePath = imagePath,
                    ),
                    thinking = true,
                    error = null,
                )
            }
            history += ChatTurn(
                role = "user",
                content = modelText,
                images = listOfNotNull(imageData),
            )
            try {
                val result = engine.respond(
                    provider = provider,
                    apiKey = key,
                    history = history.toList(),
                    subjectGoal = container.learningRepository.state.value.goal,
                )
                history.clear()
                history.addAll(result.history)
                _ui.update {
                    it.copy(
                        messages = it.messages + AskMessage(fromUser = false, text = result.reply),
                        thinking = false,
                    )
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                val message = when (error) {
                    is ModelException -> error.message ?: "模型没有回应"
                    else -> "这个问题没有走通 · ${error.message ?: "未知错误"}"
                }
                _ui.update { it.copy(thinking = false, error = message) }
            }
        }
    }

    fun reset() {
        history.clear()
        _ui.value = AskUiState()
    }
}
