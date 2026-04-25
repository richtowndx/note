package com.videonote.data.repository

import com.videonote.data.remote.api.VideoNoteApi
import com.videonote.domain.model.Provider
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 测试数据提供者接口
 * 提供来自rest.ipynb的测试数据
 */
interface TestDataProvider {
    fun getTestProviders(): List<Provider>
    fun getTestModels(providerId: String): List<String>
}

/**
 * 测试数据提供者
 * 提供来自rest.ipynb的测试数据
 */
@Singleton
class TestDataProviderImpl @Inject constructor(
    private val api: VideoNoteApi
) : TestDataProvider {

    /**
     * 模拟rest.ipynb中测试的供应商数据
     */
    override fun getTestProviders(): List<Provider> {
        return listOf(
            Provider(
                id = "qwen",
                name = "Qwen",
                logo = "Qwen",
                type = "built-in",
                enabled = 1,
                apiKey = "sk-5b1f6830a6a14a68b2ef6c1f04130666",
                baseUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1",
                createdAt = "2025-10-26T01:26:24"
            ),
            Provider(
                id = "deepseek",
                name = "DeepSeek",
                logo = "DeepSeek",
                type = "built-in",
                enabled = 0,
                apiKey = "",
                baseUrl = "https://api.deepseek.com",
                createdAt = "2025-10-26T01:26:24"
            ),
            Provider(
                id = "claude",
                name = "Claude",
                logo = "Claude",
                type = "built-in",
                enabled = 0,
                apiKey = "",
                baseUrl = "https://",
                createdAt = "2025-10-26T01:26:24"
            ),
            Provider(
                id = "gemini",
                name = "Gemini",
                logo = "Gemini",
                type = "built-in",
                enabled = 0,
                apiKey = "",
                baseUrl = "https://generativelanguage.googleapis.com/v1beta/openai/",
                createdAt = "2025-10-26T01:26:24"
            ),
            Provider(
                id = "groq",
                name = "Groq",
                logo = "Groq",
                type = "built-in",
                enabled = 0,
                apiKey = "sk-5b1f6830a6a14a68b2ef6c1f04130666",
                baseUrl = "https://api.groq.com/openai/v1/",
                createdAt = "2025-10-26T01:26:24"
            ),
            Provider(
                id = "ollama",
                name = "Ollama",
                logo = "Ollama",
                type = "built-in",
                enabled = 1,
                apiKey = "",
                baseUrl = "http://127.0.0.1:11434/v1",
                createdAt = "2025-10-26T01:26:24"
            )
        )
    }

    /**
     * 根据供应商ID获取对应的模型列表
     * 对于测试数据，直接返回预定义的模型
     */
    override fun getTestModels(providerId: String): List<String> {
        return when (providerId) {
            "qwen" -> listOf("qwen-plus", "qwen-turbo", "qwen-max")
            "deepseek" -> listOf("deepseek-chat", "deepseek-coder", "deepseek-v2")
            "claude" -> listOf("claude-3-5-sonnet-20241022", "claude-3-opus-20240229")
            "gemini" -> listOf("gemini-pro", "gemini-1.5-pro", "gemini-1.5-flash")
            "groq" -> listOf("groq-beta", "groq-2.0")
            "ollama" -> listOf("llama3:8b", "codellama:13b", "mistral:7b")
            else -> emptyList()
        }
    }
}