package com.nocturne.learn.data

/**
 * 主流 OpenAI 兼容服务模板。模型名只是起点，用户可以在编辑页改成自己账户下可用的型号。
 */
data class ProviderTemplate(
    val id: String,
    val name: String,
    val baseUrl: String,
    val model: String,
    val note: String,
    val monogram: String,
)

object ProviderTemplates {

    val all: List<ProviderTemplate> = listOf(
        ProviderTemplate(
            id = "deepseek",
            name = "DeepSeek",
            baseUrl = "https://api.deepseek.com/v1",
            model = "deepseek-chat",
            note = "中文与推理能力好，价格低",
            monogram = "DS",
        ),
        ProviderTemplate(
            id = "openai",
            name = "OpenAI",
            baseUrl = "https://api.openai.com/v1",
            model = "gpt-4o-mini",
            note = "全能力基准，需要海外网络",
            monogram = "OA",
        ),
        ProviderTemplate(
            id = "dashscope",
            name = "阿里云百炼",
            baseUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1",
            model = "qwen-plus",
            note = "通义千问，兼容模式接入",
            monogram = "QW",
        ),
        ProviderTemplate(
            id = "moonshot",
            name = "月之暗面 Kimi",
            baseUrl = "https://api.moonshot.cn/v1",
            model = "kimi-latest",
            note = "长上下文，中文写作与阅读",
            monogram = "KM",
        ),
        ProviderTemplate(
            id = "minimax",
            name = "MiniMax",
            baseUrl = "https://api.minimax.cn/v1",
            model = "MiniMax-M3.1-Flash",
            note = "海螺 AI 的模型，国内直连",
            monogram = "MM",
        ),
        ProviderTemplate(
            id = "zhipu",
            name = "智谱 GLM",
            baseUrl = "https://open.bigmodel.cn/api/paas/v4",
            model = "glm-4-flash",
            note = "有免费档位，适合先跑通",
            monogram = "GL",
        ),
        ProviderTemplate(
            id = "volcengine",
            name = "火山方舟",
            baseUrl = "https://ark.cn-beijing.volces.com/api/v3",
            model = "doubao-seed-1-6",
            note = "模型处填方舟的推理接入点 ID",
            monogram = "DB",
        ),
        ProviderTemplate(
            id = "siliconflow",
            name = "硅基流动",
            baseUrl = "https://api.siliconflow.cn/v1",
            model = "Qwen/Qwen3-8B",
            note = "开源模型聚合，可选国产模型",
            monogram = "SF",
        ),
        ProviderTemplate(
            id = "openrouter",
            name = "OpenRouter",
            baseUrl = "https://openrouter.ai/api/v1",
            model = "openai/gpt-4o-mini",
            note = "一个 Key 访问多家模型",
            monogram = "OR",
        ),
        ProviderTemplate(
            id = "gemini",
            name = "Google Gemini",
            baseUrl = "https://generativelanguage.googleapis.com/v1beta/openai",
            model = "gemini-2.5-flash",
            note = "走 Google 的 OpenAI 兼容端点",
            monogram = "GM",
        ),
        ProviderTemplate(
            id = "ollama",
            name = "本地 Ollama",
            baseUrl = "http://127.0.0.1:11434/v1",
            model = "qwen2.5:7b",
            note = "手机与电脑同一局域网，地址改成电脑 IP",
            monogram = "OL",
        ),
        ProviderTemplate(
            id = "custom",
            name = "自定义兼容接口",
            baseUrl = "",
            model = "",
            note = "任何 OpenAI 兼容的 /chat/completions",
            monogram = "＋",
        ),
    )

    fun byId(id: String?): ProviderTemplate? = all.firstOrNull { it.id == id }
}
