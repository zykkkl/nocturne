package com.nocturne.learn

import android.app.Application
import android.util.Base64
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nocturne.learn.ai.ChatTurn
import com.nocturne.learn.ai.ModelException
import com.nocturne.learn.data.AskImageStore
import com.nocturne.learn.data.AppSettings
import com.nocturne.learn.data.ask.AskConversation
import com.nocturne.learn.data.ask.AskMessage
import com.nocturne.learn.data.search.SearchProviders
import com.nocturne.learn.data.search.WebSearchConfig
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class AskUiState(
    val messages: List<AskMessage> = emptyList(),
    val thinking: Boolean = false,
    val error: String? = null,
)

class AskViewModel(application: Application) : AndroidViewModel(application) {

    private val container = (application as NocturneApp).container
    private val settingsRepository = container.settingsRepository
    private val historyRepository = container.askHistoryRepository
    private val engine = container.askEngine

    val archive = historyRepository.state

    private val _ui = MutableStateFlow(AskUiState())
    val ui: StateFlow<AskUiState> = _ui.asStateFlow()

    private val history = mutableListOf<ChatTurn>()
    private var loadedConversationId: String? = null

    init {
        viewModelScope.launch {
            archive.collect { state ->
                val active = state.active()
                _ui.update { it.copy(messages = active?.messages.orEmpty()) }
                if (active?.id != loadedConversationId) {
                    loadedConversationId = active?.id
                    history.clear()
                    rebuildHistory(active)
                }
            }
        }
    }

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
            val userMessage = AskMessage(
                fromUser = true,
                text = clean,
                imagePath = imagePath,
            )
            _ui.update { it.copy(thinking = true, error = null) }
            historyRepository.append(userMessage)
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
                    search = searchConfig(settings),
                )
                history.clear()
                history.addAll(result.history)
                historyRepository.append(
                    AskMessage(
                        fromUser = false,
                        text = result.reply,
                        sources = result.sources,
                    ),
                )
                _ui.update { it.copy(thinking = false) }
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

    fun newConversation() {
        if (_ui.value.thinking) return
        historyRepository.newConversation()
    }

    fun selectConversation(conversationId: String) {
        if (_ui.value.thinking) return
        historyRepository.select(conversationId)
    }

    fun deleteConversation(conversationId: String) {
        if (_ui.value.thinking) return
        historyRepository.delete(conversationId)
    }

    fun clearAllConversations() {
        if (_ui.value.thinking) return
        historyRepository.clearAll()
    }

    /**
     * 从本地历史重建模型上下文。
     * 只把最近一张图片重新编码上传，避免重启后整个会话的图片都塞进请求。
     */
    private suspend fun rebuildHistory(conversation: AskConversation?) {
        val turns = withContext(Dispatchers.IO) {
            val latestImagePath = conversation?.messages
                ?.lastOrNull { it.imagePath != null }
                ?.imagePath
            val latestImage = AskImageStore.toDataUrl(latestImagePath)
            conversation?.messages.orEmpty().mapNotNull { message ->
                val carriesLatestImage = message.imagePath != null &&
                    message.imagePath == latestImagePath
                when {
                    message.fromUser && message.text.isBlank() && message.imagePath != null -> {
                        ChatTurn(
                            role = "user",
                            content = "我发了一张图片，请根据这张图片继续问我。",
                            images = if (carriesLatestImage) listOfNotNull(latestImage) else emptyList(),
                        )
                    }

                    message.text.isNotBlank() -> {
                        ChatTurn(
                            role = if (message.fromUser) "user" else "assistant",
                            content = message.text,
                            images = if (carriesLatestImage) listOfNotNull(latestImage) else emptyList(),
                        )
                    }

                    else -> null
                }
            }
        }
        if (conversation?.id == loadedConversationId) {
            history.clear()
            history.addAll(turns)
        }
    }

    private fun searchConfig(settings: AppSettings): WebSearchConfig? {
        if (!settings.searchEnabled) return null
        val providerId = settings.searchProviderId
        val key = container.secretStore.get(SearchProviders.secretId(providerId))
            ?: return null
        return WebSearchConfig(providerId, key)
    }
}
