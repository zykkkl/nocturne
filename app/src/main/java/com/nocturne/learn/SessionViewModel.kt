package com.nocturne.learn

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nocturne.learn.ai.ChatTurn
import com.nocturne.learn.ai.ModelException
import com.nocturne.learn.data.ProviderConfig
import com.nocturne.learn.data.learning.LearningState
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SessionMessage(
    val id: String = UUID.randomUUID().toString(),
    val fromUser: Boolean,
    val text: String,
    val notes: List<String> = emptyList(),
)

data class SessionUiState(
    val messages: List<SessionMessage> = emptyList(),
    val thinking: Boolean = false,
    val error: String? = null,
)

class SessionViewModel(application: Application) : AndroidViewModel(application) {

    private val container = (application as NocturneApp).container
    private val learning = container.learningRepository
    private val settingsRepository = container.settingsRepository
    private val engine = container.tutorEngine

    val learningState: StateFlow<LearningState> = learning.state
    private val _ui = MutableStateFlow(SessionUiState())
    val ui: StateFlow<SessionUiState> = _ui.asStateFlow()

    private val history = mutableListOf<ChatTurn>()
    private var started = false

    fun start() {
        if (started) return
        started = true
        viewModelScope.launch {
            val settings = settingsRepository.settings.first()
            val provider = settings.activeProvider
            val key = provider?.let { container.secretStore.get(it.id) }
            if (provider == null || key.isNullOrBlank()) {
                _ui.update {
                    it.copy(error = "还没有接入模型。先回到今日页，在贾维斯卡里填入 API Key。")
                }
                return@launch
            }
            val subject = learning.state.value
            learning.markSessionStarted(subject.goal)
            runTurn(
                provider = provider,
                apiKey = key,
                dailyMinutes = subject.dailyMinutes,
                userText = "开始今天的学习。",
            )
        }
    }

    fun send(text: String) {
        val clean = text.trim()
        if (clean.isEmpty() || _ui.value.thinking) return
        viewModelScope.launch {
            val settings = settingsRepository.settings.first()
            val provider = settings.activeProvider
            val key = provider?.let { container.secretStore.get(it.id) }
            if (provider == null || key.isNullOrBlank()) {
                _ui.update { it.copy(error = "还没有接入模型。") }
                return@launch
            }
            _ui.update {
                it.copy(messages = it.messages + SessionMessage(fromUser = true, text = clean))
            }
            runTurn(
                provider = provider,
                apiKey = key,
                dailyMinutes = learning.state.value.dailyMinutes,
                userText = clean,
            )
        }
    }

    private suspend fun runTurn(
        provider: ProviderConfig,
        apiKey: String,
        dailyMinutes: Int,
        userText: String,
    ) {
        _ui.update { it.copy(thinking = true, error = null) }
        history += ChatTurn(role = "user", content = userText)
        try {
            val result = engine.respond(
                provider = provider,
                apiKey = apiKey,
                history = history.toList(),
                dailyMinutes = dailyMinutes,
            )
            history.clear()
            history.addAll(result.history)
            val text = result.replies.joinToString("\n\n").ifBlank {
                "这一步已经记下了。你想继续，还是先停一下？"
            }
            _ui.update {
                it.copy(
                    messages = it.messages + SessionMessage(
                        fromUser = false,
                        text = text,
                        notes = result.toolNotes,
                    ),
                    thinking = false,
                )
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            val message = when (error) {
                is ModelException -> error.message ?: "模型没有回应"
                else -> "这一步没有走通 · ${error.message ?: "未知错误"}"
            }
            _ui.update { it.copy(thinking = false, error = message) }
        }
    }
}
