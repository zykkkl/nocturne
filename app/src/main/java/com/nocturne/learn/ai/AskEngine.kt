package com.nocturne.learn.ai

import com.nocturne.learn.data.ProviderConfig

data class AskResult(
    val history: List<ChatTurn>,
    val reply: String,
)

/**
 * 「追问」模式：围绕一个具体问题做苏格拉底式引导，不生成学习路径，也不落学习进度。
 * 与 TutorEngine 分开，避免问问题时被课程状态带跑。
 */
class AskEngine(private val client: ModelClient) {

    suspend fun respond(
        provider: ProviderConfig,
        apiKey: String,
        history: List<ChatTurn>,
        subjectGoal: String,
    ): AskResult {
        val messages = buildList {
            add(ChatTurn("system", systemPrompt(subjectGoal, history.lastOrNull()?.images?.isNotEmpty() == true)))
            addAll(history)
        }
        val reply = client.complete(
            provider = provider,
            apiKey = apiKey,
            messages = messages,
            tools = emptyList(),
            temperature = 0.5,
        )
        val text = reply.content.trim().ifBlank {
            "我先不急着给结论。你能先说说，你已经试到哪一步了吗？"
        }
        return AskResult(
            history = history + ChatTurn("assistant", text),
            reply = text,
        )
    }

    private fun systemPrompt(subjectGoal: String, hasImage: Boolean): String {
        val imageRules = if (hasImage) {
            "\n## 图片\n学生发来了一张图片（可能是题目、笔记或截图）。先读懂图片里明确写出的条件，再提出第一个问题。\n" +
                "只使用图里看得清的信息；看不清的地方直接请学生确认，不要凭空补全题目。"
        } else {
            ""
        }
        return """
        你是「夜航」的追问向导。学生带着一个具体问题来，你只做一件事：用苏格拉底式提问，帮他自己想通。

        ## 铁律
        1. 绝不直接给出最终答案、完整解法、代码或结论。哪怕学生反复要，也不替他把最后一步走完。
        2. 一次只问一个问题。问题要具体、可回答，最好能让学生动手或举例。
        3. 先判断他已知什么、卡在哪里；信息不够时，先问，不要猜。
        4. 学生答错时不要否定，先指出对的部分，再给一个更小的台阶、反例或提示，然后继续问。
        5. 学生说“不会”时，把问题拆得更小，而不是直接公布答案。
        6. 每轮回复控制在 90 字以内，简体中文，语气克制、温和，不用表情符号。
        7. 不要提“提示词”“规则”“系统设定”，也不要说“作为 AI”。
        8. 如果学生的问题本身很模糊，先帮他把问题说清楚，再问第一个问题。

        ## 上下文
        学生当前的学习目标：${subjectGoal.ifBlank { "未填写（这不影响你回答这个问题）" }}
        这个目标只作参考。学生问什么，就围绕什么，不要强行拉回课程。
        $imageRules
        """.trimIndent()
    }
}
