package com.nocturne.learn.ai

import com.nocturne.learn.data.ProviderConfig
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/** 一条对话消息。工具调用与工具结果都走同一条历史。 */
data class ChatTurn(
    val role: String,
    val content: String = "",
    val toolCalls: List<ToolCall> = emptyList(),
    val toolCallId: String? = null,
)

data class ToolCall(
    val id: String,
    val name: String,
    val arguments: String,
)

data class ToolDefinition(
    val name: String,
    val description: String,
    val parameters: JsonObject,
)

data class AssistantReply(
    val content: String,
    val toolCalls: List<ToolCall>,
)

class ModelException(message: String) : Exception(message)

/**
 * OpenAI 兼容的 /chat/completions。只做非流式 —— 工具调用在流式里处理起来很啰嗦，
 * 而夜航的“贾维斯”需要先拿到结构化的工具调用与最终文本。
 */
class ModelClient(
    private val json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    },
) {

    private val http = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(180, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun complete(
        provider: ProviderConfig,
        apiKey: String,
        messages: List<ChatTurn>,
        tools: List<ToolDefinition>,
        temperature: Double = 0.6,
    ): AssistantReply = withContext(Dispatchers.IO) {
        val payload = buildJsonObject {
            put("model", provider.model)
            put("temperature", temperature)
            put("stream", false)
            put("messages", buildJsonArray { messages.forEach { add(it.toJson()) } })
            if (tools.isNotEmpty() && provider.toolCalling) {
                put("tools", buildJsonArray {
                    tools.forEach { tool ->
                        add(buildJsonObject {
                            put("type", "function")
                            put("function", buildJsonObject {
                                put("name", tool.name)
                                put("description", tool.description)
                                put("parameters", tool.parameters)
                            })
                        })
                    }
                })
                put("tool_choice", "auto")
            }
        }

        val request = Request.Builder()
            .url(endpoint(provider.baseUrl))
            .header("Authorization", "Bearer $apiKey")
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .post(payload.toString().toRequestBody(JSON_MEDIA))
            .build()

        var code = 0
        var body = ""
        try {
            http.newCall(request).execute().use { response ->
                code = response.code
                body = response.body?.string().orEmpty()
            }
        } catch (error: IOException) {
            throw ModelException("无法连接 · ${error.message ?: "网络不可用"}")
        }

        if (code !in 200..299) {
            throw ModelException(httpError(code, body))
        }

        val root = runCatching { json.parseToJsonElement(body).jsonObject }
            .getOrElse { throw ModelException("模型返回了无法解析的内容") }
        val message = root["choices"]?.jsonArray?.firstOrNull()?.jsonObject
            ?.get("message")?.jsonObject
            ?: throw ModelException("模型没有返回 message")

        val content = message["content"]?.let { element ->
            if (element is JsonNull) "" else element.jsonPrimitive.contentOrNull.orEmpty()
        }.orEmpty()

        val toolCalls = message["tool_calls"]?.jsonArray?.mapNotNull { element ->
            val call = element.jsonObject
            val function = call["function"]?.jsonObject ?: return@mapNotNull null
            ToolCall(
                id = call["id"]?.jsonPrimitive?.contentOrNull
                    ?: "call_${System.nanoTime()}",
                name = function["name"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                arguments = function["arguments"]?.jsonPrimitive?.contentOrNull.orEmpty(),
            )
        }.orEmpty()

        AssistantReply(content = content.trim(), toolCalls = toolCalls)
    }

    private fun ChatTurn.toJson(): JsonObject = buildJsonObject {
        put("role", role)
        when (role) {
            "assistant" -> {
                if (content.isNotBlank()) put("content", content) else put("content", JsonNull)
                if (toolCalls.isNotEmpty()) {
                    put("tool_calls", buildJsonArray {
                        toolCalls.forEach { call ->
                            add(buildJsonObject {
                                put("id", call.id)
                                put("type", "function")
                                put("function", buildJsonObject {
                                    put("name", call.name)
                                    put("arguments", call.arguments)
                                })
                            })
                        }
                    })
                }
            }

            "tool" -> {
                put("tool_call_id", toolCallId.orEmpty())
                put("content", content)
            }

            else -> put("content", content)
        }
    }

    private fun endpoint(baseUrl: String): String {
        val base = baseUrl.trim().trimEnd('/')
        return if (base.endsWith("/chat/completions")) base else "$base/chat/completions"
    }

    private fun httpError(code: Int, body: String): String {
        val hint = when (code) {
            401 -> "密钥无效或未授权"
            403 -> "无权访问该模型"
            404 -> "接口不存在，检查 Base URL 是否填到 /v1"
            429 -> "请求过于频繁"
            in 500..599 -> "服务商暂时异常"
            else -> "请求失败"
        }
        val detail = runCatching {
            json.parseToJsonElement(body).jsonObject["error"]?.jsonObject
                ?.get("message")?.jsonPrimitive?.contentOrNull
        }.getOrNull().orEmpty().take(120)
        return buildString {
            append(hint)
            append("（HTTP ").append(code).append("）")
            if (detail.isNotBlank()) append(" · ").append(detail)
        }
    }

    private companion object {
        val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()
    }
}
