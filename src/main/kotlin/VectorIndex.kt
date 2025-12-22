import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import kotlin.math.sqrt

/**
 * Simple vector index implementation for storing and searching embeddings.
 *
 * NOTE: This is a basic implementation using cosine similarity.
 * For production use with large datasets, consider integrating FAISS:
 *
 * FAISS Integration Steps:
 * 1. Install FAISS: https://github.com/facebookresearch/faiss
 * 2. Build Java/Kotlin bindings using SWIG
 * 3. Replace this class with FAISS IndexFlatIP or IndexIVFFlat
 *
 * FAISS provides:
 * - Faster similarity search (especially for large datasets)
 * - Approximate nearest neighbor search
 * - GPU acceleration support
 * - Various index types optimized for different use cases
 */
class VectorIndex {
    @Serializable
    data class IndexEntry(
        val id: Int,
        val text: String,
        val embedding: List<Double>,
        val metadata: Map<String, String> = emptyMap()
    )

    @Serializable
    data class IndexData(
        val entries: List<IndexEntry>,
        val dimension: Int,
        val totalEntries: Int
    )

    @Serializable
    data class SearchResult(
        val id: Int,
        val text: String,
        val score: Double,
        val metadata: Map<String, String>
    )

    private val entries = mutableListOf<IndexEntry>()
    private var dimension: Int = 0

    /**
     * Add a single vector to the index
     */
    fun add(text: String, embedding: List<Double>, metadata: Map<String, String> = emptyMap()) {
        if (entries.isEmpty()) {
            dimension = embedding.size
        } else if (embedding.size != dimension) {
            throw IllegalArgumentException("Embedding dimension mismatch. Expected $dimension, got ${embedding.size}")
        }

        entries.add(
            IndexEntry(
                id = entries.size,
                text = text,
                embedding = embedding,
                metadata = metadata
            )
        )
    }

    /**
     * Add multiple vectors to the index
     */
    fun addBatch(texts: List<String>, embeddings: List<List<Double>>, metadata: List<Map<String, String>> = emptyList()) {
        if (texts.size != embeddings.size) {
            throw IllegalArgumentException("Number of texts and embeddings must match")
        }

        texts.forEachIndexed { index, text ->
            val meta = if (index < metadata.size) metadata[index] else emptyMap()
            add(text, embeddings[index], meta)
        }
    }

    /**
     * Search for the k most similar vectors using cosine similarity
     */
    fun search(queryEmbedding: List<Double>, k: Int = 5): List<SearchResult> {
        if (entries.isEmpty()) {
            return emptyList()
        }

        if (queryEmbedding.size != dimension) {
            throw IllegalArgumentException("Query embedding dimension mismatch. Expected $dimension, got ${queryEmbedding.size}")
        }

        return entries
            .map { entry ->
                val similarity = cosineSimilarity(queryEmbedding, entry.embedding)
                SearchResult(
                    id = entry.id,
                    text = entry.text,
                    score = similarity,
                    metadata = entry.metadata
                )
            }
            .sortedByDescending { it.score }
            .take(k)
    }

    /**
     * Save the index to a file
     *
     * For FAISS integration, use: faiss.write_index(index, filename)
     */
    fun save(filePath: String) {
        val indexData = IndexData(
            entries = entries,
            dimension = dimension,
            totalEntries = entries.size
        )

        val json = Json { prettyPrint = true }
        val jsonString = json.encodeToString(indexData)

        File(filePath).writeText(jsonString)
        println("Index saved to $filePath (${entries.size} entries, dimension: $dimension)")
    }

    /**
     * Load the index from a file
     *
     * For FAISS integration, use: faiss.read_index(filename)
     */
    fun load(filePath: String) {
        val file = File(filePath)
        if (!file.exists()) {
            throw IllegalArgumentException("Index file not found: $filePath")
        }

        val json = Json { ignoreUnknownKeys = true }
        val indexData = json.decodeFromString<IndexData>(file.readText())

        entries.clear()
        entries.addAll(indexData.entries)
        dimension = indexData.dimension

        println("Index loaded from $filePath (${entries.size} entries, dimension: $dimension)")
    }

    /**
     * Get index statistics
     */
    fun getStats(): String {
        return """
            |Index Statistics:
            |  Total entries: ${entries.size}
            |  Dimension: $dimension
            |  Index size: ~${(entries.size * dimension * 8) / 1024 / 1024} MB
        """.trimMargin()
    }

    /**
     * Calculate cosine similarity between two vectors
     */
    private fun cosineSimilarity(a: List<Double>, b: List<Double>): Double {
        if (a.size != b.size) {
            throw IllegalArgumentException("Vectors must have the same dimension")
        }

        var dotProduct = 0.0
        var normA = 0.0
        var normB = 0.0

        for (i in a.indices) {
            dotProduct += a[i] * b[i]
            normA += a[i] * a[i]
            normB += b[i] * b[i]
        }

        val denominator = sqrt(normA) * sqrt(normB)
        return if (denominator == 0.0) 0.0 else dotProduct / denominator
    }

    /**
     * Clear all entries from the index
     */
    fun clear() {
        entries.clear()
        dimension = 0
    }

    /**
     * Get the number of entries in the index
     */
    fun size(): Int = entries.size
}
