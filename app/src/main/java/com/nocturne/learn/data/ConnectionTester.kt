package com.nocturne.learn.data

import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request

data class ConnectionResult(
    val success: Boolean,
    val message: String,
    val models: List<String> = emptyList(),
)

/**
 * 用 GET /models 做一次最小握手：能列出模型，说明地址、密钥与网络都通了。
 * 有些兼容服务不实现 /models，此时以 HTTP 401/403 之外的响应作为「服务可达」。
 */
class ConnectionTester(private val client: OkHttpClient = defaultClient()) {

    suspend fun test(baseUrl: String, apiKey: String): ConnectionResult = withContext(Dispatchers.IO) {
        val trimmed = baseUrl.trim().trimEnd('/')
        if (trimmed.isEmpty()) {
            return@withContext ConnectionResult(false, "Base URL 不能为空")
        }
        val url = if (trimmed.endsWith("/models")) trimmed else "$trimmed/models"
        val request = Request.Builder()
            .url(url)
            .get()
            .header("Accept", "application/json")
            .apply { if (apiKey.isNotBlank()) header("Authorization", "Bearer ${apiKey.trim()}") }
            .build()

        runCatching {
            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (response.isSuccessful) {
                    val models = parseModelIds(body)
                    val suffix = if (models.isEmpty()) "" else " · ${models.size} 个模型可用"
                    ConnectionResult(true, "连接成功 · HTTP ${response.code}$suffix", models.take(8))
                } else {
                    ConnectionResult(false, "HTTP ${response.code} · ${extractError(body)}")
                }
            }
        }.getOrElse { error ->
            ConnectionResult(false, "无法连接 · ${error.message ?: error.javaClass.simpleName}")
        }
    }

    private fun parseModelIds(body: String): List<String> = runCatching {
        Json.parseToJsonElement(body).jsonObject["data"]?.jsonArray?.mapNotNull { element ->
            element.jsonObject["id"]?.jsonPrimitive?.content
        }.orEmpty()
    }.getOrDefault(emptyList())

    private fun extractError(body: String): String = runCatching {
        val root = Json.parseToJsonElement(body).jsonObject
        val message = root["error"]?.jsonObject?.get("message")?.jsonPrimitive?.content
            ?: root["message"]?.jsonPrimitive?.content
        (message ?: body).take(180).ifBlank { "请求被拒绝" }
    }.getOrDefault(body.take(180).ifBlank { "请求被拒绝" })

    companion object {
        fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }
}
