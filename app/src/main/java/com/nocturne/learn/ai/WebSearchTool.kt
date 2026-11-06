package com.nocturne.learn.ai

import com.nocturne.learn.data.search.WebSearchSource
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * 联网搜索在模型侧只暴露成一个普通工具；没有搜索 Key 时这个工具根本不会出现。
 */
internal val webSearchTool: ToolDefinition = ToolDefinition(
    name = "web_search",
    description = "搜索互联网，用于核实事实、查询最新信息或补充教材之外的知识。返回标题、链接和摘要。",
    parameters = buildJsonObject {
        put("type", "object")
        put("properties", buildJsonObject {
            put("query", buildJsonObject {
                put("type", "string")
                put("description", "搜索关键词，尽量具体")
            })
        })
        put("required", buildJsonArray { add("query") })
    },
)

internal fun sourcesPayload(sources: List<WebSearchSource>): String =
    buildJsonObject {
        put("status", "ok")
        put("count", sources.size)
        put("results", buildJsonArray {
            sources.forEach { source ->
                add(
                    buildJsonObject {
                        put("title", source.title)
                        put("url", source.url)
                        put("snippet", source.snippet)
                        put("site", source.site)
                    },
                )
            }
        })
    }.toString()

internal fun searchErrorPayload(message: String): String =
    buildJsonObject {
        put("status", "error")
        put("message", message)
    }.toString()
