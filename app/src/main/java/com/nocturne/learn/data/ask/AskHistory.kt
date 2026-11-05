package com.nocturne.learn.data.ask

import android.content.Context
import com.nocturne.learn.data.AskImageStore
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class AskMessage(
    val id: String = UUID.randomUUID().toString(),
    val fromUser: Boolean,
    val text: String,
    val imagePath: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
)

@Serializable
data class AskConversation(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val messages: List<AskMessage> = emptyList(),
) {
    val displayTitle: String
        get() = title.ifBlank { "新追问" }
}

@Serializable
data class AskArchive(
    val conversations: List<AskConversation> = emptyList(),
    val activeId: String? = null,
) {
    fun active(): AskConversation? = conversations.firstOrNull { it.id == activeId }
}

/**
 * 追问历史完全独立于学习航线：按会话保存在本机 JSON 文件里，
 * 新建、切换、删除都不会触碰 learning_state。
 */
class AskHistoryRepository(context: Context) {

    private val appContext = context.applicationContext
    private val file = File(appContext.filesDir, "ask_history.json")
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    private val lock = Any()

    private val _state = MutableStateFlow(load())
    val state: StateFlow<AskArchive> = _state.asStateFlow()

    init {
        synchronized(lock) {
            var current = _state.value
            if (current.conversations.isEmpty()) {
                val first = AskConversation()
                current = current.copy(
                    conversations = listOf(first),
                    activeId = first.id,
                )
                write(current)
            } else if (current.conversations.none { it.id == current.activeId }) {
                current = current.copy(
                    activeId = current.conversations.maxByOrNull { it.updatedAt }?.id,
                )
                write(current)
            }
            _state.value = current
            AskImageStore.cleanup(appContext, referencedImages(current))
        }
    }

    fun newConversation(): AskConversation = synchronized(lock) {
        val current = _state.value
        val active = current.active()
        if (active != null && active.messages.isEmpty()) return active
        val existingBlank = current.conversations.firstOrNull { it.messages.isEmpty() }
        if (existingBlank != null) {
            if (current.activeId != existingBlank.id) {
                publish(current.copy(activeId = existingBlank.id))
            }
            return existingBlank
        }
        val conversation = AskConversation()
        publish(
            current.copy(
                conversations = listOf(conversation) + current.conversations,
                activeId = conversation.id,
            ),
        )
        conversation
    }

    fun select(conversationId: String) = synchronized(lock) {
        val current = _state.value
        if (current.activeId == conversationId) return
        if (current.conversations.none { it.id == conversationId }) return
        publish(current.copy(activeId = conversationId))
    }

    fun append(message: AskMessage) = synchronized(lock) {
        val current = _state.value
        val active = current.active() ?: return
        val updated = active.copy(
            title = active.title.ifBlank { titleOf(message) },
            updatedAt = message.createdAt,
            messages = active.messages + message,
        )
        val conversations = listOf(updated) + current.conversations.filterNot { it.id == active.id }
        publish(current.copy(conversations = conversations, activeId = active.id))
    }

    fun delete(conversationId: String) = synchronized(lock) {
        val current = _state.value
        val target = current.conversations.firstOrNull { it.id == conversationId } ?: return
        target.messages.forEach { AskImageStore.delete(it.imagePath) }
        val remaining = current.conversations.filterNot { it.id == conversationId }
        if (remaining.isEmpty()) {
            val fresh = AskConversation()
            publish(AskArchive(conversations = listOf(fresh), activeId = fresh.id))
        } else {
            val activeId = if (current.activeId == conversationId) {
                remaining.firstOrNull { it.messages.isNotEmpty() }?.id
                    ?: remaining.first().id
            } else {
                current.activeId
            }
            publish(current.copy(conversations = remaining, activeId = activeId))
        }
    }

    fun clearAll() = synchronized(lock) {
        val current = _state.value
        current.conversations.forEach { conversation ->
            conversation.messages.forEach { AskImageStore.delete(it.imagePath) }
        }
        val fresh = AskConversation()
        publish(AskArchive(conversations = listOf(fresh), activeId = fresh.id))
    }

    private fun publish(next: AskArchive) {
        _state.value = next
        write(next)
    }

    private fun load(): AskArchive {
        if (!file.exists()) return AskArchive()
        return runCatching {
            json.decodeFromString<AskArchive>(file.readText())
        }.getOrDefault(AskArchive())
    }

    private fun write(archive: AskArchive) {
        runCatching {
            file.parentFile?.mkdirs()
            val temp = File(file.parentFile, "${file.name}.tmp")
            temp.writeText(json.encodeToString(AskArchive.serializer(), archive))
            Files.move(
                temp.toPath(),
                file.toPath(),
                StandardCopyOption.REPLACE_EXISTING,
            )
        }
    }

    private fun titleOf(message: AskMessage): String {
        val clean = message.text.replace(Regex("\\s+"), " ").trim()
        if (clean.isEmpty()) return if (message.imagePath != null) "图片追问" else "新追问"
        return if (clean.length <= 22) clean else clean.take(22) + "…"
    }

    private fun referencedImages(archive: AskArchive): Set<String> =
        archive.conversations
            .flatMap { it.messages }
            .mapNotNull { it.imagePath }
            .toSet()
}
