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
    @SerialName("choices") val choices: List<ChatChoice>,
    @SerialName("model") val model: String? = null
)

class OpenRouterClient(
    private val apiKey: String = Config.openRouterApiKey,
    private val model: String = "nex-agi/deepseek-v3.1-nex-n1:free",
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

        val response: ChatResponse = client.post("$baseUrl/chat/completions") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer $apiKey")
            header("HTTP-Referer", "https://ollamatest.app")
            header("X-Title", "Ollama Test RAG")
            setBody(request)
        }.body()

        return response.choices.firstOrNull()?.message?.content
            ?: throw Exception("No response from LLM")
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

    fun close() {
        client.close()
    }
}
