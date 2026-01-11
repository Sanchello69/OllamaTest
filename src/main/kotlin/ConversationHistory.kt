import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Класс для хранения истории диалога с пользователем
 */
@Serializable
data class SourceChunk(
    val text: String,
    val score: Double,
    val metadata: Map<String, String> = emptyMap()
)

@Serializable
data class ConversationTurn(
    val timestamp: String,
    val question: String,
    val answer: String,
    val sources: List<SourceChunk> = emptyList(),
    val useRag: Boolean = true
)

@Serializable
data class ConversationData(
    val turns: List<ConversationTurn>,
    val createdAt: String,
    val lastModified: String
)

class ConversationHistory {
    private val turns = mutableListOf<ConversationTurn>()
    private var createdAt: String = getCurrentTimestamp()

    /**
     * Добавить новый раунд диалога
     */
    fun addTurn(
        question: String,
        answer: String,
        sources: List<VectorIndex.SearchResult> = emptyList(),
        useRag: Boolean = true
    ) {
        val sourceChunks = sources.map {
            SourceChunk(
                text = it.text,
                score = it.score,
                metadata = it.metadata
            )
        }

        turns.add(
            ConversationTurn(
                timestamp = getCurrentTimestamp(),
                question = question,
                answer = answer,
                sources = sourceChunks,
                useRag = useRag
            )
        )
    }

    /**
     * Получить все сообщения для передачи в LLM (без источников)
     */
    fun getMessagesForLLM(): List<ChatMessage> {
        return turns.flatMap { turn ->
            listOf(
                ChatMessage(role = "user", content = turn.question),
                ChatMessage(role = "assistant", content = turn.answer)
            )
        }
    }

    /**
     * Получить последние N раундов диалога
     */
    fun getLastTurns(n: Int): List<ConversationTurn> {
        return turns.takeLast(n)
    }

    /**
     * Получить все раунды диалога
     */
    fun getAllTurns(): List<ConversationTurn> {
        return turns.toList()
    }

    /**
     * Получить количество раундов
     */
    fun size(): Int = turns.size

    /**
     * Очистить историю
     */
    fun clear() {
        turns.clear()
        createdAt = getCurrentTimestamp()
    }

    /**
     * Сохранить историю в файл
     */
    fun save(filePath: String) {
        val conversationData = ConversationData(
            turns = turns,
            createdAt = createdAt,
            lastModified = getCurrentTimestamp()
        )

        val json = Json { prettyPrint = true }
        val jsonString = json.encodeToString(conversationData)

        File(filePath).writeText(jsonString)
        println("💾 История диалога сохранена: $filePath (${turns.size} раундов)")
    }

    /**
     * Загрузить историю из файла
     */
    fun load(filePath: String) {
        val file = File(filePath)
        if (!file.exists()) {
            throw IllegalArgumentException("Файл истории не найден: $filePath")
        }

        val json = Json { ignoreUnknownKeys = true }
        val conversationData = json.decodeFromString<ConversationData>(file.readText())

        turns.clear()
        turns.addAll(conversationData.turns)
        createdAt = conversationData.createdAt

        println("📂 История диалога загружена: $filePath (${turns.size} раундов)")
    }

    /**
     * Получить статистику
     */
    fun getStats(): String {
        val totalQuestions = turns.size
        val withRag = turns.count { it.useRag }
        val withoutRag = turns.count { !it.useRag }
        val totalSources = turns.sumOf { it.sources.size }

        return """
            |История диалога:
            |  Всего раундов: $totalQuestions
            |  С RAG: $withRag
            |  Без RAG: $withoutRag
            |  Всего источников: $totalSources
            |  Создано: $createdAt
        """.trimMargin()
    }

    /**
     * Форматировать источники для вывода пользователю
     */
    fun formatSources(sources: List<SourceChunk>): String {
        if (sources.isEmpty()) {
            return "\n📚 Источники: Нет (ответ без RAG)"
        }

        val formatted = StringBuilder("\n\n📚 Источники:\n")
        sources.forEachIndexed { idx, source ->
            formatted.append("\n${idx + 1}. [Релевантность: ${"%.4f".format(source.score)}]\n")

            // Метаданные
            if (source.metadata.isNotEmpty()) {
                formatted.append("   Метаданные: ")
                formatted.append(source.metadata.entries.joinToString(", ") { "${it.key}=${it.value}" })
                formatted.append("\n")
            }

            // Превью текста
            val preview = if (source.text.length > 150) {
                source.text.take(150) + "..."
            } else {
                source.text
            }
            formatted.append("   Текст: $preview\n")
        }

        return formatted.toString()
    }

    private fun getCurrentTimestamp(): String {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
    }
}
