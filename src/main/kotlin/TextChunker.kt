class TextChunker(
    private val chunkSize: Int = 500,
    private val overlap: Int = 50
) {
    data class Chunk(
        val text: String,
        val index: Int,
        val startPosition: Int,
        val endPosition: Int
    )

    fun chunkText(text: String): List<Chunk> {
        val chunks = mutableListOf<Chunk>()

        if (text.isEmpty()) {
            return chunks
        }

        // Clean the text
        val cleanedText = text.trim()
            .replace(Regex("\\s+"), " ")
            .replace(Regex("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F]"), "")

        var startPosition = 0
        var chunkIndex = 0

        while (startPosition < cleanedText.length) {
            val endPosition = minOf(startPosition + chunkSize, cleanedText.length)

            // Try to find a good breaking point (sentence end, paragraph, etc.)
            var actualEndPosition = endPosition
            if (endPosition < cleanedText.length) {
                // Look for sentence ending
                val searchStart = maxOf(startPosition, endPosition - 100)
                val searchText = cleanedText.substring(searchStart, endPosition)
                val lastPeriod = searchText.lastIndexOfAny(charArrayOf('.', '!', '?', '\n'))

                if (lastPeriod >= 0) {
                    actualEndPosition = searchStart + lastPeriod + 1
                }
            }

            val chunkText = cleanedText.substring(startPosition, actualEndPosition).trim()

            if (chunkText.isNotEmpty()) {
                chunks.add(
                    Chunk(
                        text = chunkText,
                        index = chunkIndex,
                        startPosition = startPosition,
                        endPosition = actualEndPosition
                    )
                )
                chunkIndex++
            }

            // Move to next chunk with overlap
            startPosition = if (actualEndPosition < cleanedText.length) {
                maxOf(startPosition + 1, actualEndPosition - overlap)
            } else {
                actualEndPosition
            }
        }

        return chunks
    }
}
