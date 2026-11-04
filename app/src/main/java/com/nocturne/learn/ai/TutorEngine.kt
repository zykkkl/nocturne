package com.nocturne.learn.ai

import com.nocturne.learn.data.ProviderConfig
import com.nocturne.learn.data.learning.LearningRepository
import com.nocturne.learn.data.learning.LearningState
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

data class TutorResult(
    val history: List<ChatTurn>,
    val replies: List<String>,
    val toolNotes: List<String>,
)

/**
 * 贾维斯的“大脑壳”：每轮把当前学习状态塞进 system prompt，模型可以调用工具改写状态，
 * 改完再让模型继续说。所有落库都由 App 完成，模型只负责决策。
 */
class TutorEngine(
    private val client: ModelClient,
    private val learning: LearningRepository,
    private val json: Json = Json { ignoreUnknownKeys = true; isLenient = true },
) {

    suspend fun respond(
        provider: ProviderConfig,
        apiKey: String,
        history: List<ChatTurn>,
        dailyMinutes: Int,
    ): TutorResult {
        val working = history.toMutableList()
        val replies = mutableListOf<String>()
        val notes = mutableListOf<String>()

        var round = 0
        while (round < MAX_TOOL_ROUNDS) {
            val state = learning.state.value
            val messages = buildList {
                add(ChatTurn("system", systemPrompt(state, dailyMinutes)))
                addAll(working)
            }
            val reply = client.complete(
                provider = provider,
                apiKey = apiKey,
                messages = messages,
                tools = if (provider.toolCalling) tools else emptyList(),
            )

            if (reply.toolCalls.isEmpty()) {
                if (reply.content.isNotBlank()) {
                    working += ChatTurn("assistant", reply.content)
                    replies += reply.content
                }
                break
            }

            working += ChatTurn(
                role = "assistant",
                content = reply.content,
                toolCalls = reply.toolCalls,
            )
            if (reply.content.isNotBlank()) replies += reply.content

            reply.toolCalls.forEach { call ->
                val outcome = execute(call)
                working += ChatTurn(role = "tool", content = outcome.message, toolCallId = call.id)
                if (outcome.note.isNotBlank()) notes += outcome.note
            }
            round++
        }

        if (replies.isEmpty()) {
            val fallback = "我在。刚才那一步已经记下了。你可以继续，或者告诉我现在最想弄懂哪一块。"
            working += ChatTurn("assistant", fallback)
            replies += fallback
        }

        return TutorResult(history = working, replies = replies, toolNotes = notes)
    }

    private fun execute(call: ToolCall): ToolOutcome {
        val args = runCatching { json.parseToJsonElement(call.arguments).jsonObject }
            .getOrElse { JsonObject(emptyMap()) }
        return when (call.name) {
            "get_next_kp" -> nextKnowledgePoint()

            "save_plan" -> {
                val goal = args.string("goal")
                val level = args.string("level")
                val plan = args.string("plan")
                val points = args["kps"]?.let { element ->
                    if (element is JsonArray) {
                        element.mapNotNull { item ->
                            val obj = runCatching { item.jsonObject }.getOrNull() ?: return@mapNotNull null
                            val title = obj.string("title")
                            if (title.isBlank()) null else title to obj.string("note")
                        }
                    } else null
                }.orEmpty()

                if (points.isEmpty()) {
                    ToolOutcome("""{"status":"error","message":"kps 不能为空"}""")
                } else {
                    learning.savePlan(goal, level, plan, points)
                    ToolOutcome(
                        message = buildJsonObject {
                            put("status", "ok")
                            put("count", points.size)
                            put("titles", buildJsonArray { points.forEach { add(it.first) } })
                        }.toString(),
                        note = "已生成学习路径 · ${points.size} 个知识点",
                    )
                }
            }

            "update_mastery" -> {
                val title = args.string("kp_title")
                val mastery = (args["mastery"]?.jsonPrimitive?.floatOrNull ?: 0.3f).coerceIn(0f, 1f)
                val evidence = args.string("evidence")
                if (title.isBlank()) {
                    ToolOutcome("""{"status":"error","message":"kp_title 不能为空"}""")
                } else {
                    val point = learning.upsertKnowledgePoint(title, mastery, evidence = evidence)
                    val days = when {
                        mastery < 0.4f -> 1
                        mastery < 0.75f -> 3
                        else -> 7
                    }
                    learning.scheduleReview(title, days)
                    ToolOutcome(
                        message = buildJsonObject {
                            put("status", "ok")
                            put("title", point.title)
                            put("mastery", point.mastery)
                            put("review_in_days", days)
                        }.toString(),
                        note = "掌握度更新 · ${point.title} ${(mastery * 100).toInt()}%",
                    )
                }
            }

            "schedule_review" -> {
                val title = args.string("kp_title")
                val days = (args["days"]?.jsonPrimitive?.intOrNull ?: 1).coerceIn(1, 365)
                if (title.isBlank() || learning.state.value.findPoint(title) == null) {
                    ToolOutcome("""{"status":"error","message":"知识点不存在"}""")
                } else {
                    learning.scheduleReview(title, days)
                    ToolOutcome(
                        """{"status":"ok","review_in_days":$days}""",
                        note = "已安排复习 · $title（$days 天后）",
                    )
                }
            }

            "record_note" -> {
                val text = args.string("text")
                if (text.isBlank()) {
                    ToolOutcome("""{"status":"error","message":"text 不能为空"}""")
                } else {
                    learning.addNote(text)
                    ToolOutcome("""{"status":"ok"}""", note = "已记下一条笔记")
                }
            }

            "end_session" -> {
                val summary = args.string("summary")
                learning.completeSession(summary)
                ToolOutcome("""{"status":"ok"}""", note = "本次学习已收尾")
            }

            else -> ToolOutcome("""{"status":"error","message":"未知工具"}""")
        }
    }

    private fun nextKnowledgePoint(): ToolOutcome {
        val state = learning.state.value
        if (state.knowledgePoints.isEmpty()) {
            return ToolOutcome(
                """{"status":"empty","message":"还没有知识点，请先做诊断并调用 save_plan"}""",
            )
        }
        val target = state.nextTarget()
        val due = state.dueReviews().mapNotNull { state.knowledgePointById(it.kpId)?.title }
        return ToolOutcome(
            message = buildJsonObject {
                put("status", "ok")
                if (target != null) {
                    put("kp", buildJsonObject {
                        put("title", target.title)
                        put("mastery", target.mastery)
                        put("note", target.note.take(160))
                    })
                }
                put("due_titles", buildJsonArray { due.forEach { add(it) } })
            }.toString(),
        )
    }

    private fun systemPrompt(state: LearningState, dailyMinutes: Int): String {
        val points = state.knowledgePoints
            .sortedBy { it.mastery }
            .take(14)
            .joinToString("\n") { point ->
                val due = state.reviews.any { it.kpId == point.id && it.dueAt <= System.currentTimeMillis() }
                "- ${point.title}｜掌握度 ${(point.mastery * 100).toInt()}%${if (due) "｜今天到期" else ""}"
            }
            .ifBlank { "（还没有知识点）" }

        return """
            你是「夜航」的学习向导，名字叫贾维斯。你不是客服，也不是夸夸机器人；你像一位耐心、敏锐的私人导师，负责判断学生现在最该学什么，并带着他一步一步学会。

            ## 当前学习状态
            目标：${state.goal.ifBlank { "未填写" }}
            水平：${state.level.ifBlank { "未知" }}
            每日投入：$dailyMinutes 分钟
            连续学习：${state.streak} 天｜累计学习：${state.sessions} 次
            学习路径：${state.plan.ifBlank { "尚未生成" }}
            知识点：
            $points

            ## 教学协议
            1. 没有知识点时，先做诊断：一次只问一个问题，从易到难问 3~6 个，判断真实水平；诊断结束后调用 save_plan 生成路径。
            2. 有知识点时，先调用 get_next_kp，优先处理到期复习，其次补最薄弱的知识点。
            3. 讲授采用苏格拉底式：一次只讲一个概念，给一个具体例子，然后立刻问一个小问题让学生回答；不要一次灌太多。
            4. 学生答完，先指出对在哪里、错在哪里，再调用 update_mastery 记录证据；掌握度低于 0.4 视为未掌握，不要虚假拔高。
            5. 讲完一个知识点，调用 schedule_review 安排复习。重要事实用 record_note 记下。
            6. 学生说结束、或本次目标完成时，调用 end_session，用一句话总结本次学习，并告诉学生下次从哪继续。
            7. 全程使用简体中文，语气克制、温和、具体。不要提“工具调用”“JSON”“函数”这些词，也不要把内部状态原样念给学生。
            8. 每次回复控制在 120 字以内，除非学生在问一个需要展开的问题。不要用表情符号。

            ## 工具
            你可以调用 get_next_kp、save_plan、update_mastery、schedule_review、record_note、end_session。工具执行后，用自然语言把结果告诉学生，然后继续教学。
        """.trimIndent()
    }

    private fun JsonObject.string(key: String): String =
        this[key]?.jsonPrimitive?.contentOrNull.orEmpty().trim()

    private data class ToolOutcome(val message: String, val note: String = "")

    private val tools: List<ToolDefinition> = listOf(
        ToolDefinition(
            name = "get_next_kp",
            description = "获取当前最该学习的知识点。优先返回到期复习，否则返回掌握度最低的知识点。",
            parameters = schema(emptyMap()),
        ),
        ToolDefinition(
            name = "save_plan",
            description = "诊断完成后，保存学习目标、水平判断、学习路径和知识点清单。",
            parameters = schema(
                properties = mapOf(
                    "goal" to stringProperty("学习目标"),
                    "level" to stringProperty("诊断出的当前水平，例如“零基础”“会写简单循环”"),
                    "plan" to stringProperty("整体学习路径，2~4 句话"),
                    "kps" to buildJsonObject {
                        put("type", "array")
                        put("description", "按学习顺序排列的知识点")
                        put("items", buildJsonObject {
                            put("type", "object")
                            put("properties", buildJsonObject {
                                put("title", stringProperty("知识点名称"))
                                put("note", stringProperty("一句话说明"))
                            })
                            put("required", buildJsonArray { add("title") })
                        })
                    },
                ),
                required = listOf("goal", "level", "plan", "kps"),
            ),
        ),
        ToolDefinition(
            name = "update_mastery",
            description = "根据学生的回答证据更新某个知识点的掌握度（0~1）。答对、答得快、隔久了仍记得，都可以更高。",
            parameters = schema(
                properties = mapOf(
                    "kp_title" to stringProperty("知识点名称，必须与已保存的名称一致"),
                    "mastery" to buildJsonObject {
                        put("type", "number")
                        put("description", "0 到 1 之间的掌握度")
                        put("minimum", 0)
                        put("maximum", 1)
                    },
                    "evidence" to stringProperty("学生这次回答的证据，一句话"),
                ),
                required = listOf("kp_title", "mastery", "evidence"),
            ),
        ),
        ToolDefinition(
            name = "schedule_review",
            description = "为某个知识点安排间隔复习。刚学会通常 1 天，掌握较好可 3~7 天。",
            parameters = schema(
                properties = mapOf(
                    "kp_title" to stringProperty("知识点名称"),
                    "days" to buildJsonObject {
                        put("type", "integer")
                        put("description", "几天后复习")
                        put("minimum", 1)
                        put("maximum", 365)
                    },
                ),
                required = listOf("kp_title", "days"),
            ),
        ),
        ToolDefinition(
            name = "record_note",
            description = "记录一条值得长期记住的学习笔记或学生特点。",
            parameters = schema(
                properties = mapOf("text" to stringProperty("笔记内容")),
                required = listOf("text"),
            ),
        ),
        ToolDefinition(
            name = "end_session",
            description = "本次学习结束时调用，总结本次内容并说明下次从哪里继续。",
            parameters = schema(
                properties = mapOf("summary" to stringProperty("本次学习总结")),
                required = listOf("summary"),
            ),
        ),
    )

    private fun schema(
        properties: Map<String, JsonObject>,
        required: List<String> = emptyList(),
    ): JsonObject = buildJsonObject {
        put("type", "object")
        put("properties", buildJsonObject {
            properties.forEach { (key, value) -> put(key, value) }
        })
        put("required", buildJsonArray { required.forEach { add(it) } })
    }

    private fun stringProperty(description: String): JsonObject = buildJsonObject {
        put("type", "string")
        put("description", description)
    }

    private companion object {
        const val MAX_TOOL_ROUNDS = 6
    }
}
