class TextChunker(
    private val maxChunkSize: Int = 2000,  // Максимальный размер чанка (для очень длинных абзацев)
    private val minChunkSize: Int = 50     // Минимальный размер чанка
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

        // Минимальная очистка текста (сохраняем структуру абзацев)
        val cleanedText = text
            .replace(Regex("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F]"), "")
            .trim()

        // Разбиваем на абзацы (двойной перенос строки или одинарный)
        val paragraphs = cleanedText.split(Regex("\\n\\s*\\n|\\n"))
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        var chunkIndex = 0
        var currentPosition = 0

        for (paragraph in paragraphs) {
            val paragraphLength = paragraph.length

            // Если абзац слишком маленький, пропускаем
            if (paragraphLength < minChunkSize) {
                currentPosition += paragraphLength
                continue
            }

            // Если абзац слишком большой, разбиваем его на предложения
            if (paragraphLength > maxChunkSize) {
                val subChunks = splitLargeParagraph(paragraph, currentPosition)
                subChunks.forEach { (text, start, end) ->
                    chunks.add(
                        Chunk(
                            text = text,
                            index = chunkIndex++,
                            startPosition = start,
                            endPosition = end
                        )
                    )
                }
                currentPosition += paragraphLength
            } else {
                // Абзац подходящего размера - используем как чанк
                chunks.add(
                    Chunk(
                        text = paragraph,
                        index = chunkIndex++,
                        startPosition = currentPosition,
                        endPosition = currentPosition + paragraphLength
                    )
                )
                currentPosition += paragraphLength
            }
        }

        return chunks
    }

    private fun splitLargeParagraph(paragraph: String, basePosition: Int): List<Triple<String, Int, Int>> {
        val result = mutableListOf<Triple<String, Int, Int>>()

        // Разбиваем на предложения
        val sentences = paragraph.split(Regex("(?<=[.!?])\\s+"))

        var currentChunk = StringBuilder()
        var chunkStart = basePosition
        var currentPos = basePosition

        for (sentence in sentences) {
            val sentenceLength = sentence.length

            // Если добавление этого предложения превысит максимум и текущий чанк не пустой
            if (currentChunk.length + sentenceLength > maxChunkSize && currentChunk.isNotEmpty()) {
                // Сохраняем текущий чанк
                val chunkText = currentChunk.toString().trim()
                if (chunkText.length >= minChunkSize) {
                    result.add(Triple(chunkText, chunkStart, currentPos))
                }

                // Начинаем новый чанк
                currentChunk = StringBuilder()
                chunkStart = currentPos
            }

            // Добавляем предложение к текущему чанку
            if (currentChunk.isNotEmpty()) {
                currentChunk.append(" ")
            }
            currentChunk.append(sentence)
            currentPos += sentenceLength
        }

        // Добавляем последний чанк
        val chunkText = currentChunk.toString().trim()
        if (chunkText.length >= minChunkSize) {
            result.add(Triple(chunkText, chunkStart, currentPos))
        }

        return result
    }
}
