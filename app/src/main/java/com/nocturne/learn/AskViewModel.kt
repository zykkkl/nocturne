package com.nocturne.learn

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nocturne.learn.ai.ChatTurn
import com.nocturne.learn.ai.ModelException
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AskMessage(
    val id: String = UUID.randomUUID().toString(),
    val fromUser: Boolean,
    val text: String,
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

    fun ask(text: String) {
        val clean = text.trim()
        if (clean.isEmpty() || _ui.value.thinking) return
        viewModelScope.launch {
            val settings = settingsRepository.settings.first()
            val provider = settings.activeProvider
            val key = provider?.let { container.secretStore.get(it.id) }
            if (provider == null || key.isNullOrBlank()) {
                _ui.update { it.copy(error = "还没有接入模型。先回到今日页，接入一个模型再问。") }
                return@launch
            }
            _ui.update {
                it.copy(
                    messages = it.messages + AskMessage(fromUser = true, text = clean),
                    thinking = true,
                    error = null,
                )
            }
            history += ChatTurn("user", clean)
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
