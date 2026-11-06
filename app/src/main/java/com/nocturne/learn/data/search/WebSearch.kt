package com.nocturne.learn.data.search

import kotlinx.serialization.Serializable

/**
 * 联网搜索独立于大模型服务：模型只负责决定“要不要搜、搜什么”，
 * 搜索 Key 只留在本机，不会进入发给模型的上下文。
 */
@Serializable
data class WebSearchSource(
    val title: String,
    val url: String,
    val snippet: String = "",
    val site: String = "",
)

data class WebSearchConfig(
    val providerId: String,
    val apiKey: String,
)

data class SearchProviderInfo(
    val id: String,
    val label: String,
    val baseUrl: String,
    val keyHint: String,
    val note: String,
)

object SearchProviders {
    val Bocha = SearchProviderInfo(
        id = "bocha",
        label = "博查",
        baseUrl = "https://api.bochaai.com/v1/web-search",
        keyHint = "sk-…（博查开放平台）",
        note = "中文网页与中文资料覆盖较好。",
    )

    val DeepSeek = SearchProviderInfo(
        id = "deepseek",
        label = "DeepSeek",
        baseUrl = "https://api.deepseek.com/v1/tools/search",
        keyHint = "sk-…（DeepSeek 开放平台）",
        note = "DeepSeek 自研搜索，中文与权威来源较好；可直接复用 DeepSeek 模型 Key。",
    )

    val Tavily = SearchProviderInfo(
        id = "tavily",
        label = "Tavily",
        baseUrl = "https://api.tavily.com/search",
        keyHint = "tvly-…（Tavily）",
        note = "英文与国际来源较多，适合查国外资料。",
    )

    val all: List<SearchProviderInfo> = listOf(Bocha, DeepSeek, Tavily)

    fun byId(id: String?): SearchProviderInfo =
        all.firstOrNull { it.id == id } ?: Bocha

    fun secretId(providerId: String): String = "search:$providerId"
}
