package com.nocturne.learn.ai

import com.nocturne.learn.data.ProviderConfig
import com.nocturne.learn.data.textbook.DocumentSection
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

data class ExtractedPoint(
    val title: String,
    val note: String,
    val sourceRef: String,
)

data class ExtractedSection(
    val title: String,
    val summary: String,
    val points: List<ExtractedPoint>,
)

/**
 * 教材拆解：一次处理一个章节/片段，输出可以落库的知识点。
 * 只输出 JSON，不依赖模型的工具调用能力，兼容普通 Chat Completions。
 */
class TextbookEngine(private val client: ModelClient) {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    suspend fun extract(
        provider: ProviderConfig,
        apiKey: String,
        section: DocumentSection,
    ): ExtractedSection {
        val messages = listOf(
            ChatTurn(role = "system", content = SYSTEM_PROMPT),
            ChatTurn(
                role = "user",
                content = buildString {
                    append("章节：").append(section.title).append('\n')
                    append("来源：").append(section.sourceRef).append('\n')
                    append("原文：\n")
                    append(section.text.take(6000))
                },
            ),
        )
        val first = runCatching {
            client.complete(provider, apiKey, messages, tools = emptyList(), temperature = 0.3)
        }.getOrElse { throw it }
        parse(first.content, section)?.let { return it }

        // 有些模型会带解释或代码围栏，给一次“只输出 JSON”的修复机会。
        val retry = client.complete(
            provider,
            apiKey,
            messages + ChatTurn(
                role = "user",
                content = "上一次回复不是合法 JSON。请只输出 JSON 对象，不要任何解释、Markdown 或代码围栏。",
            ),
            tools = emptyList(),
            temperature = 0.2,
        )
        return parse(retry.content, section) ?: throw ModelException("模型没有返回可用的知识点")
    }

    private fun parse(raw: String, section: DocumentSection): ExtractedSection? {
        val start = raw.indexOf('{')
        val end = raw.lastIndexOf('}')
        if (start < 0 || end <= start) return null
        val candidate = raw.substring(start, end + 1)
        val root = runCatching { json.parseToJsonElement(candidate).jsonObject }.getOrNull() ?: return null
        val pointsJson = root["points"] as? JsonArray ?: return null
        val points = pointsJson.mapNotNull { element ->
            val point = element as? JsonObject ?: return@mapNotNull null
            val title = point["title"]?.jsonPrimitive?.contentOrNull.orEmpty().trim()
            if (title.isBlank()) return@mapNotNull null
            ExtractedPoint(
                title = title.take(60),
                note = point["note"]?.jsonPrimitive?.contentOrNull.orEmpty().trim().take(280),
                sourceRef = point["source_ref"]?.jsonPrimitive?.contentOrNull.orEmpty()
                    .trim().ifBlank { section.sourceRef },
            )
        }.distinctBy { it.title.lowercase() }.take(12)
        if (points.isEmpty()) return null
        return ExtractedSection(
            title = root["title"]?.jsonPrimitive?.contentOrNull.orEmpty().trim().ifBlank { section.title },
            summary = root["summary"]?.jsonPrimitive?.contentOrNull.orEmpty().trim().take(300),
            points = points,
        )
    }

    private companion object {
        const val SYSTEM_PROMPT = """
            你是「夜航」的教材结构分析器。你的任务是把一段教材原文拆成可以教学、可以练习的知识点。

            ## 规则
            1. 只输出一个 JSON 对象，不要 Markdown、代码围栏或任何解释。
            2. 知识点必须是这段原文里明确讲到的内容，不要引入原文没有的概念。
            3. 每个知识点是一个可独立讲解、独立检查的小概念，不要拆得过碎，也不要合并成空泛的大章节。
            4. 4~10 个知识点为宜；原文很短时少一些。
            5. note 用 60~140 个中文字写清楚“这个概念是什么、为什么重要、学到什么程度算懂”。
            6. source_ref 尽量写具体页码或幻灯片，例如“第 12 页”“第 3 张幻灯片”；不确定就留空。
            7. 每个知识点的 title 控制在 6~16 个中文字，直接点出概念；不要重复章节名，不要加“核心概念”“关键方法”这类空泛后缀。
            8. 简体中文，语气克制。

            ## 输出格式
            {
              "title": "这一段的短标题",
              "summary": "一句话概括这一段讲了什么",
              "points": [
                {"title": "知识点标题", "note": "60~140 字说明", "source_ref": "第 12 页"}
              ]
            }
        """
    }
}
