import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class ChatMessage(
    @SerialName("role") val role: String,
    @SerialName("content") val content: String
)

@Serializable
data class ChatRequest(
    @SerialName("model") val model: String,
    @SerialName("messages") val messages: List<ChatMessage>
)

@Serializable
data class ChatChoice(
    @SerialName("message") val message: ChatMessage,
    @SerialName("finish_reason") val finishReason: String? = null
)

@Serializable
data class ChatResponse(
    @SerialName("id") val id: String? = null,
    @SerialName("choices") val choices: List<ChatChoice>? = null,
    @SerialName("model") val model: String? = null,
    @SerialName("error") val error: ApiError? = null
)

@Serializable
data class ApiError(
    @SerialName("message") val message: String,
    @SerialName("type") val type: String? = null,
    @SerialName("code") val code: String? = null
)

class OpenRouterClient(
    private val apiKey: String = Config.openRouterApiKey,
    private val model: String = "tngtech/deepseek-r1t2-chimera:free",
    private val baseUrl: String = "https://openrouter.ai/api/v1"
) {
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 60000  // 60 seconds
            connectTimeoutMillis = 30000  // 30 seconds
            socketTimeoutMillis = 60000   // 60 seconds
        }
    }

    suspend fun chat(messages: List<ChatMessage>): String {
        val request = ChatRequest(
            model = model,
            messages = messages
        )

        val response: ChatResponse = try {
            client.post("$baseUrl/chat/completions") {
                contentType(ContentType.Application.Json)
                header("Authorization", "Bearer $apiKey")
                header("HTTP-Referer", "https://ollamatest.app")
                header("X-Title", "Ollama Test RAG")
                setBody(request)
            }.body()
        } catch (e: Exception) {
            throw Exception("Error calling OpenRouter API: ${e.message}", e)
        }

        // Проверяем наличие ошибки в ответе
        if (response.error != null) {
            throw Exception("OpenRouter API error: ${response.error.message} (type: ${response.error.type}, code: ${response.error.code})")
        }

        // Проверяем наличие choices
        if (response.choices == null || response.choices.isEmpty()) {
            throw Exception("No response from LLM: choices field is missing or empty")
        }

        return response.choices.firstOrNull()?.message?.content
            ?: throw Exception("No content in LLM response")
    }

    suspend fun askQuestion(question: String, context: String? = null): String {
        val messages = if (context != null) {
            listOf(
                ChatMessage(
                    role = "system",
                    content = """Ты - помощник, который отвечает на вопросы на основе предоставленного контекста.
                        |
                        |Контекст:
                        |$context
                        |
                        |Отвечай на вопросы только на основе этого контекста. Если информации недостаточно, скажи об этом.""".trimMargin()
                ),
                ChatMessage(role = "user", content = question)
            )
        } else {
            listOf(
                ChatMessage(role = "user", content = question)
            )
        }

        return chat(messages)
    }

    /**
     * Задать вопрос с учетом истории диалога
     */
    suspend fun askQuestionWithHistory(
        question: String,
        history: List<ChatMessage>,
        context: String? = null
    ): String {
        val messages = buildList {
            // Добавляем системное сообщение с контекстом, если есть
            if (context != null) {
                add(
                    ChatMessage(
                        role = "system",
                        content = """Ты - помощник, который отвечает на вопросы на основе предоставленного контекста.
                            |
                            |Контекст:
                            |$context
                            |
                            |Отвечай на вопросы только на основе этого контекста. Если информации недостаточно, скажи об этом.""".trimMargin()
                    )
                )
            }

            // Добавляем историю предыдущих сообщений
            addAll(history)

            // Добавляем текущий вопрос
            add(ChatMessage(role = "user", content = question))
        }

        return chat(messages)
    }

    fun close() {
        client.close()
    }
}
