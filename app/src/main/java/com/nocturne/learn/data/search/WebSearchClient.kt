package com.nocturne.learn.data.search

import android.net.Uri
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class WebSearchException(message: String) : Exception(message)

/**
 * 博查 / Tavily 的联网搜索。返回结果会被裁剪成标题、链接、摘要和站点，
 * 交给模型的是参考资料，而不是完整网页。
 */
class WebSearchClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    suspend fun search(
        config: WebSearchConfig,
        query: String,
        limit: Int = 5,
    ): List<WebSearchSource> = withContext(Dispatchers.IO) {
        val clean = query.trim()
        if (clean.isEmpty()) return@withContext emptyList()
        when (config.providerId) {
            SearchProviders.Tavily.id -> searchTavily(config, clean, limit)
            else -> searchBocha(config, clean, limit)
        }
    }

    private fun searchBocha(
        config: WebSearchConfig,
        query: String,
        limit: Int,
    ): List<WebSearchSource> {
        val body = JSONObject()
            .put("query", query)
            .put("count", limit.coerceIn(1, 10))
            .put("summary", true)
            .put("freshness", "noLimit")
            .toString()
        val root = post(SearchProviders.Bocha.baseUrl, config.apiKey, body)
        val pages = root.optJSONObject("data")
            ?.optJSONObject("webPages")
            ?.optJSONArray("value")
            ?: root.optJSONObject("webPages")?.optJSONArray("value")
            ?: return emptyList()
        return buildList {
            for (index in 0 until pages.length()) {
                val item = pages.optJSONObject(index) ?: continue
                val url = item.optString("url").trim()
                if (!isHttpUrl(url)) continue
                add(
                    WebSearchSource(
                        title = item.optString("name").ifBlank { hostOf(url) },
                        url = url,
                        snippet = item.optString("summary")
                            .ifBlank { item.optString("snippet") }
                            .trim()
                            .take(600),
                        site = item.optString("siteName").ifBlank { hostOf(url) },
                    ),
                )
                if (size >= limit) break
            }
        }
    }

    private fun searchTavily(
        config: WebSearchConfig,
        query: String,
        limit: Int,
    ): List<WebSearchSource> {
        val body = JSONObject()
            .put("query", query)
            .put("search_depth", "basic")
            .put("max_results", limit.coerceIn(1, 10))
            .put("include_answer", false)
            .put("include_raw_content", false)
            .toString()
        val root = post(SearchProviders.Tavily.baseUrl, config.apiKey, body)
        val results = root.optJSONArray("results") ?: return emptyList()
        return buildList {
            for (index in 0 until results.length()) {
                val item = results.optJSONObject(index) ?: continue
                val url = item.optString("url").trim()
                if (!isHttpUrl(url)) continue
                add(
                    WebSearchSource(
                        title = item.optString("title").ifBlank { hostOf(url) },
                        url = url,
                        snippet = item.optString("content").trim().take(600),
                        site = hostOf(url),
                    ),
                )
                if (size >= limit) break
            }
        }
    }

    private fun post(url: String, apiKey: String, body: String): JSONObject {
        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer ${apiKey.trim()}")
            .header("Content-Type", "application/json")
            .header("User-Agent", USER_AGENT)
            .post(body.toRequestBody(JSON_MEDIA))
            .build()
        client.newCall(request).execute().use { response ->
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                val detail = runCatching {
                    val json = JSONObject(text)
                    json.optString("message")
                        .ifBlank { json.optJSONObject("detail")?.optString("error").orEmpty() }
                        .ifBlank { json.optString("detail") }
                }.getOrNull().orEmpty()
                val reason = when (response.code) {
                    401, 403 -> "Key 无效或没有权限"
                    429 -> "请求过于频繁"
                    in 500..599 -> "搜索服务暂时不可用"
                    else -> "搜索请求失败"
                }
                throw WebSearchException(
                    "HTTP ${response.code} · $reason${if (detail.isBlank()) "" else " · $detail"}",
                )
            }
            if (text.isBlank()) throw WebSearchException("搜索服务返回空响应")
            return runCatching { JSONObject(text) }
                .getOrElse { throw WebSearchException("搜索服务返回了无法解析的内容") }
        }
    }

    private fun isHttpUrl(url: String): Boolean =
        url.startsWith("https://") || url.startsWith("http://")

    private fun hostOf(url: String): String =
        runCatching { Uri.parse(url).host.orEmpty() }.getOrDefault("")

    private companion object {
        const val USER_AGENT = "Nocturne-Android"
        val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()
    }
}
