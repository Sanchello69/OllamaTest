import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class OllamaClient(
    private val baseUrl: String = "http://localhost:11434",
    private val model: String = "nomic-embed-text"
) {
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
    }

    @Serializable
    data class EmbeddingRequest(
        val model: String,
        val prompt: String
    )

    @Serializable
    data class EmbeddingResponse(
        val embedding: List<Double>
    )

    suspend fun generateEmbedding(text: String): List<Double> {
        try {
            val response = client.post("$baseUrl/api/embeddings") {
                contentType(ContentType.Application.Json)
                setBody(EmbeddingRequest(model = model, prompt = text))
            }

            if (response.status.isSuccess()) {
                val embeddingResponse: EmbeddingResponse = response.body()
                return embeddingResponse.embedding
            } else {
                throw Exception("Failed to generate embedding: ${response.status}")
            }
        } catch (e: Exception) {
            throw Exception("Error calling Ollama API: ${e.message}", e)
        }
    }

    suspend fun generateEmbeddings(texts: List<String>, onProgress: ((Int, Int) -> Unit)? = null): List<List<Double>> {
        val embeddings = mutableListOf<List<Double>>()

        texts.forEachIndexed { index, text ->
            try {
                val embedding = generateEmbedding(text)
                embeddings.add(embedding)
                onProgress?.invoke(index + 1, texts.size)
            } catch (e: Exception) {
                println("Error generating embedding for chunk $index: ${e.message}")
                // Add zero vector as placeholder
                embeddings.add(List(384) { 0.0 }) // nomic-embed-text has 384 dimensions
            }
        }

        return embeddings
    }

    fun close() {
        client.close()
    }
}
