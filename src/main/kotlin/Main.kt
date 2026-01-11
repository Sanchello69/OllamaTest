import kotlinx.coroutines.runBlocking
import java.io.File

fun main(args: Array<String>) = runBlocking {
    println("=== RTF to Vector Embeddings with Ollama ===\n")

    // Parse command line arguments
    if (args.isEmpty()) {
        printUsage()
        return@runBlocking
    }

    val command = args[0]

    when (command) {
        "index" -> {
            if (args.size < 2) {
                println("Error: Please provide RTF file path")
                println("Usage: index <rtf-file-path> [options]")
                return@runBlocking
            }

            val rtfFilePath = args[1]
            val indexPath = if (args.size > 2) args[2] else "embeddings_index.json"
            val maxChunkSize = if (args.size > 3) args[3].toIntOrNull() ?: 2000 else 2000
            val minChunkSize = if (args.size > 4) args[4].toIntOrNull() ?: 50 else 50

            indexRtfFile(rtfFilePath, indexPath, maxChunkSize, minChunkSize)
        }

        "search" -> {
            if (args.size < 3) {
                println("Error: Please provide index path and search query")
                println("Usage: search <index-path> <query> [top-k]")
                return@runBlocking
            }

            val indexPath = args[1]
            val query = args[2]
            val topK = if (args.size > 3) args[3].toIntOrNull() ?: 5 else 5

            searchIndex(indexPath, query, topK)
        }

        "stats" -> {
            if (args.size < 2) {
                println("Error: Please provide index path")
                println("Usage: stats <index-path>")
                return@runBlocking
            }

            val indexPath = args[1]
            showStats(indexPath)
        }

        "ask" -> {
            if (args.size < 2) {
                println("Error: Please provide question")
                println("Usage: ask <question> [index-path] [--no-rag] [--min-score=0.7] [--save-history] [--history-path=path]")
                return@runBlocking
            }

            val question = args[1]
            val useRag = !args.contains("--no-rag")
            val indexPath = if (args.size > 2 && !args[2].startsWith("--")) args[2] else "embeddings_index.json"

            // Извлекаем минимальный порог похожести из аргументов
            val minScoreArg = args.find { it.startsWith("--min-score=") }
            val minRelevanceScore = minScoreArg?.substringAfter("=")?.toDoubleOrNull() ?: 0.0

            // Параметры истории
            val saveHistory = args.contains("--save-history")
            val historyPathArg = args.find { it.startsWith("--history-path=") }
            val historyPath = historyPathArg?.substringAfter("=") ?: "conversation_history.json"

            // Загрузка истории, если файл существует
            val history = ConversationHistory()
            if (saveHistory && File(historyPath).exists()) {
                try {
                    history.load(historyPath)
                } catch (e: Exception) {
                    println("⚠️  Could not load history: ${e.message}")
                }
            }

            askQuestion(question, indexPath, useRag, minRelevanceScore, history, saveHistory, historyPath)
        }

        "chat" -> {
            if (args.size < 1) {
                println("Error: Please use chat command")
                println("Usage: chat [index-path] [--no-rag] [--min-score=0.7] [--history-path=path]")
                return@runBlocking
            }

            val useRag = !args.contains("--no-rag")
            val indexPath = if (args.size > 1 && !args[1].startsWith("--")) args[1] else "embeddings_index.json"

            // Извлекаем минимальный порог похожести из аргументов
            val minScoreArg = args.find { it.startsWith("--min-score=") }
            val minRelevanceScore = minScoreArg?.substringAfter("=")?.toDoubleOrNull() ?: 0.0

            // Путь к истории
            val historyPathArg = args.find { it.startsWith("--history-path=") }
            val historyPath = historyPathArg?.substringAfter("=") ?: "conversation_history.json"

            startChat(indexPath, useRag, minRelevanceScore, historyPath)
        }

        else -> {
            println("Unknown command: $command")
            printUsage()
        }
    }
}

suspend fun indexRtfFile(rtfFilePath: String, indexPath: String, maxChunkSize: Int, minChunkSize: Int) {
    println("📄 Processing RTF file: $rtfFilePath")
    println("Settings:")
    println("  - Chunking mode: По абзацам")
    println("  - Max chunk size: $maxChunkSize characters")
    println("  - Min chunk size: $minChunkSize characters")
    println("  - Index output: $indexPath\n")

    // Step 1: Parse RTF file
    println("Step 1: Parsing RTF file...")
    val parser = RtfParser()
    val text = try {
        parser.parseRtfFile(rtfFilePath)
    } catch (e: Exception) {
        println("❌ Error parsing RTF file: ${e.message}")
        return
    }
    println("✓ Extracted ${text.length} characters\n")

    // Step 2: Split into chunks
    println("Step 2: Splitting text into chunks by paragraphs...")
    val chunker = TextChunker(maxChunkSize, minChunkSize)
    val chunks = chunker.chunkText(text)
    println("✓ Created ${chunks.size} chunks\n")

    if (chunks.isEmpty()) {
        println("❌ No chunks created. The file might be empty.")
        return
    }

    // Step 3: Generate embeddings
    println("Step 3: Generating embeddings with Ollama...")
    println("Note: Make sure Ollama is running with the 'nomic-embed-text' model")
    println("Run: ollama pull nomic-embed-text\n")

    val ollamaClient = OllamaClient()

    val chunkTexts = chunks.map { it.text }
    val embeddings = try {
        ollamaClient.generateEmbeddings(chunkTexts) { current, total ->
            print("\rProgress: $current/$total chunks processed")
        }
    } catch (e: Exception) {
        println("\n❌ Error generating embeddings: ${e.message}")
        println("Make sure Ollama is running: http://localhost:11434")
        ollamaClient.close()
        return
    } finally {
        ollamaClient.close()
    }

    println("\n✓ Generated ${embeddings.size} embeddings\n")

    // Step 4: Create and save index
    println("Step 4: Creating vector index...")
    val index = VectorIndex()

    chunks.forEachIndexed { idx, chunk ->
        val metadata = mapOf(
            "chunk_index" to idx.toString(),
            "start_pos" to chunk.startPosition.toString(),
            "end_pos" to chunk.endPosition.toString(),
            "source_file" to rtfFilePath
        )
        index.add(chunk.text, embeddings[idx], metadata)
    }

    println("✓ Index created with ${index.size()} entries\n")

    // Step 5: Save index
    println("Step 5: Saving index to disk...")
    try {
        index.save(indexPath)
        println("✓ Index saved successfully\n")
        println(index.getStats())
        println("\n✅ Processing complete!")
    } catch (e: Exception) {
        println("❌ Error saving index: ${e.message}")
    }
}

suspend fun searchIndex(indexPath: String, query: String, topK: Int) {
    println("🔍 Searching index: $indexPath")
    println("Query: \"$query\"")
    println("Top K results: $topK\n")

    // Load index
    println("Loading index...")
    val index = VectorIndex()
    try {
        index.load(indexPath)
    } catch (e: Exception) {
        println("❌ Error loading index: ${e.message}")
        return
    }

    // Generate embedding for query
    println("Generating query embedding...")
    val ollamaClient = OllamaClient()
    val queryEmbedding = try {
        ollamaClient.generateEmbedding(query)
    } catch (e: Exception) {
        println("❌ Error generating query embedding: ${e.message}")
        ollamaClient.close()
        return
    } finally {
        ollamaClient.close()
    }

    // Search
    println("Searching...\n")
    val results = index.search(queryEmbedding, topK)

    if (results.isEmpty()) {
        println("No results found.")
        return
    }

    println("Results:\n")
    results.forEachIndexed { idx, result ->
        println("${idx + 1}. Score: ${"%.4f".format(result.score)}")
        println("   Text: ${result.text.take(200)}${if (result.text.length > 200) "..." else ""}")
        println("   Metadata: ${result.metadata}")
        println()
    }
}

fun showStats(indexPath: String) {
    println("📊 Index Statistics\n")

    val index = VectorIndex()
    try {
        index.load(indexPath)
        println(index.getStats())
    } catch (e: Exception) {
        println("❌ Error loading index: ${e.message}")
    }
}

suspend fun askQuestion(
    question: String,
    indexPath: String,
    useRag: Boolean,
    minRelevanceScore: Double = 0.0,
    conversationHistory: ConversationHistory? = null,
    saveHistory: Boolean = false,
    historyPath: String = "conversation_history.json"
) {
    println("🤖 AI Assistant ${if (useRag) "with RAG" else "without RAG"}\n")
    println("Question: \"$question\"")
    if (useRag && minRelevanceScore > 0.0) {
        println("🔍 Relevance filter: minimum score = ${"%.2f".format(minRelevanceScore)}")
    }
    if (conversationHistory != null && conversationHistory.size() > 0) {
        println("📜 История диалога: ${conversationHistory.size()} раундов")
    }
    println()

    val openRouterClient = OpenRouterClient()
    val history = conversationHistory ?: ConversationHistory()

    try {
        if (useRag) {
            // RAG режим: поиск контекста + ответ
            println("Step 1: Loading index and searching for relevant context...")

            val index = VectorIndex()
            try {
                index.load(indexPath)
                println("✓ Index loaded (${index.size()} entries)\n")
            } catch (e: Exception) {
                println("❌ Error loading index: ${e.message}")
                println("Falling back to non-RAG mode...\n")
                val answer = if (history.size() > 0) {
                    openRouterClient.askQuestionWithHistory(question, history.getMessagesForLLM())
                } else {
                    openRouterClient.askQuestion(question)
                }
                println("💬 Answer:\n$answer")

                // Сохраняем в историю без источников
                history.addTurn(question, answer, emptyList(), useRag = false)
                if (saveHistory) history.save(historyPath)

                println(history.formatSources(emptyList()))
                return
            }

            // Генерируем эмбеддинг для вопроса
            println("Step 2: Generating question embedding...")
            val ollamaClient = OllamaClient()
            val queryEmbedding = try {
                ollamaClient.generateEmbedding(question)
            } catch (e: Exception) {
                println("❌ Error generating query embedding: ${e.message}")
                ollamaClient.close()
                println("Falling back to non-RAG mode...\n")
                val answer = if (history.size() > 0) {
                    openRouterClient.askQuestionWithHistory(question, history.getMessagesForLLM())
                } else {
                    openRouterClient.askQuestion(question)
                }
                println("💬 Answer:\n$answer")

                // Сохраняем в историю без источников
                history.addTurn(question, answer, emptyList(), useRag = false)
                if (saveHistory) history.save(historyPath)

                println(history.formatSources(emptyList()))
                return
            } finally {
                ollamaClient.close()
            }
            println("✓ Embedding generated\n")

            // Ищем релевантные чанки
            println("Step 3: Searching for relevant chunks...")
            val allResults = index.search(queryEmbedding, 5)

            // Фильтруем по порогу релевантности
            val results = if (minRelevanceScore > 0.0) {
                allResults.filter { it.score >= minRelevanceScore }
            } else {
                allResults
            }

            if (results.isEmpty()) {
                if (minRelevanceScore > 0.0) {
                    println("⚠️  No chunks found with score >= ${"%.2f".format(minRelevanceScore)}")
                    println("   Top result score was: ${"%.4f".format(allResults.firstOrNull()?.score ?: 0.0)}")
                } else {
                    println("⚠️  No relevant chunks found")
                }
                println("Falling back to non-RAG mode...\n")
                val answer = if (history.size() > 0) {
                    openRouterClient.askQuestionWithHistory(question, history.getMessagesForLLM())
                } else {
                    openRouterClient.askQuestion(question)
                }
                println("💬 Answer:\n$answer")

                // Сохраняем в историю без источников
                history.addTurn(question, answer, emptyList(), useRag = false)
                if (saveHistory) history.save(historyPath)

                println(history.formatSources(emptyList()))
                return
            }

            println("✓ Found ${results.size} relevant chunks")
            if (minRelevanceScore > 0.0 && results.size < allResults.size) {
                println("   (filtered ${allResults.size - results.size} chunks below threshold)")
            }
            println()

            // Отображаем найденные чанки
            println("📚 Relevant chunks:")
            results.forEachIndexed { idx, result ->
                println("  ${idx + 1}. [Score: ${"%.4f".format(result.score)}] ${result.text.take(100)}...")
            }
            println()

            // Объединяем чанки в контекст
            val context = results.joinToString("\n\n---\n\n") { it.text }

            // Отправляем вопрос с контекстом в LLM (с учетом истории)
            println("Step 4: Sending question with context to LLM...")
            val answer = if (history.size() > 0) {
                openRouterClient.askQuestionWithHistory(question, history.getMessagesForLLM(), context)
            } else {
                openRouterClient.askQuestion(question, context)
            }

            println("✓ Response received\n")
            println("💬 Answer:\n$answer")

            // Сохраняем в историю с источниками
            history.addTurn(question, answer, results, useRag = true)
            if (saveHistory) {
                history.save(historyPath)
            }

            // Выводим источники
            println(history.formatSources(history.getAllTurns().last().sources))

        } else {
            // Без RAG: просто вопрос к LLM
            println("Sending question to LLM (without context)...")
            val answer = if (history.size() > 0) {
                openRouterClient.askQuestionWithHistory(question, history.getMessagesForLLM())
            } else {
                openRouterClient.askQuestion(question)
            }

            println("✓ Response received\n")
            println("💬 Answer:\n$answer")

            // Сохраняем в историю без источников
            history.addTurn(question, answer, emptyList(), useRag = false)
            if (saveHistory) {
                history.save(historyPath)
            }

            // Выводим источники (их нет)
            println(history.formatSources(emptyList()))
        }
    } catch (e: Exception) {
        println("❌ Error: ${e.message}")
        e.printStackTrace()
    } finally {
        openRouterClient.close()
    }
}

suspend fun startChat(
    indexPath: String,
    useRag: Boolean,
    minRelevanceScore: Double = 0.0,
    historyPath: String = "conversation_history.json"
) {
    println("💬 Интерактивный чат ${if (useRag) "с RAG" else "без RAG"}")
    println("📝 История сохраняется в: $historyPath")
    if (useRag && minRelevanceScore > 0.0) {
        println("🔍 Фильтр релевантности: >= ${"%.2f".format(minRelevanceScore)}")
    }
    println("\nКоманды:")
    println("  - Введите вопрос для получения ответа")
    println("  - /history - показать историю диалога")
    println("  - /stats - показать статистику")
    println("  - /clear - очистить историю")
    println("  - /exit или /quit - выход\n")

    // Загрузка или создание истории
    val history = ConversationHistory()
    if (File(historyPath).exists()) {
        try {
            history.load(historyPath)
            println("✓ Загружена история: ${history.size()} раундов\n")
        } catch (e: Exception) {
            println("⚠️  Не удалось загрузить историю: ${e.message}")
            println("Создана новая история\n")
        }
    } else {
        println("Создана новая история\n")
    }

    // Основной цикл чата
    while (true) {
        print("Вы: ")
        val input = readLine()?.trim() ?: break

        if (input.isEmpty()) continue

        when (input.lowercase()) {
            "/exit", "/quit" -> {
                println("\n💾 Сохранение истории...")
                history.save(historyPath)
                println("До свидания!")
                break
            }

            "/history" -> {
                println("\n📜 История диалога:")
                if (history.size() == 0) {
                    println("  История пуста")
                } else {
                    history.getAllTurns().forEachIndexed { idx, turn ->
                        println("\n--- Раунд ${idx + 1} [${turn.timestamp}] ---")
                        println("Вы: ${turn.question}")
                        println("AI: ${turn.answer.take(200)}${if (turn.answer.length > 200) "..." else ""}")
                        if (turn.sources.isNotEmpty()) {
                            println("Источников: ${turn.sources.size}")
                        }
                    }
                }
                println()
                continue
            }

            "/stats" -> {
                println("\n${history.getStats()}\n")
                continue
            }

            "/clear" -> {
                history.clear()
                println("\n✓ История очищена\n")
                continue
            }

            else -> {
                // Обрабатываем вопрос
                println()
                askQuestion(input, indexPath, useRag, minRelevanceScore, history, saveHistory = true, historyPath)
                println()
            }
        }
    }
}

fun printUsage() {
    println("""
        Usage: java -jar OllamaTest.jar <command> [options]

        Commands:
          index <rtf-file> [index-path] [max-chunk-size] [min-chunk-size]
              Process RTF file and create embeddings index
              Текст разбивается по абзацам, а не по символам

              Arguments:
                rtf-file         - Path to RTF file to process
                index-path       - Output path for index (default: embeddings_index.json)
                max-chunk-size   - Max size for large paragraphs (default: 2000)
                min-chunk-size   - Min size to filter small paragraphs (default: 50)

              Example:
                index document.rtf my_index.json 2000 50

          search <index-path> <query> [top-k]
              Search the index with a query

              Arguments:
                index-path   - Path to the index file
                query        - Search query text
                top-k        - Number of top results to return (default: 5)

              Example:
                search my_index.json "machine learning" 10

          ask <question> [index-path] [--no-rag] [--min-score=THRESHOLD] [--save-history] [--history-path=PATH]
              Ask a question to AI assistant (with or without RAG)

              Arguments:
                question           - Your question
                index-path         - Path to the index file (default: embeddings_index.json)
                --no-rag           - Disable RAG mode (no context retrieval)
                --min-score=X.X    - Minimum relevance score threshold (0.0-1.0, default: 0.0)
                --save-history     - Save conversation history
                --history-path=PATH - Path to history file (default: conversation_history.json)

              Examples:
                ask "Как звали степного волка?"                    # With RAG
                ask "Что такое машинное обучение?" --no-rag       # Without RAG
                ask "Кто главный герой?" my_index.json            # Custom index
                ask "Детали сюжета?" --min-score=0.75             # High relevance only
                ask "Продолжение?" --save-history                 # Save to history

              RAG Mode (default):
                1. Finds relevant chunks from the index
                2. Filters by relevance score (if --min-score specified)
                3. Combines them with your question
                4. Sends to LLM for answer
                5. Shows sources used for the answer

              Relevance Filter (--min-score):
                - 0.0-0.5: Very loose (includes marginally relevant chunks)
                - 0.5-0.7: Moderate filtering (recommended for general use)
                - 0.7-0.9: Strict filtering (only highly relevant chunks)
                - 0.9-1.0: Very strict (almost exact matches only)

              Without RAG (--no-rag):
                Sends question directly to LLM without context

          chat [index-path] [--no-rag] [--min-score=THRESHOLD] [--history-path=PATH]
              Start interactive chat session with AI assistant

              Arguments:
                index-path         - Path to the index file (default: embeddings_index.json)
                --no-rag           - Disable RAG mode (no context retrieval)
                --min-score=X.X    - Minimum relevance score threshold (0.0-1.0, default: 0.0)
                --history-path=PATH - Path to history file (default: conversation_history.json)

              Interactive commands:
                <question>   - Ask a question
                /history     - Show conversation history
                /stats       - Show statistics
                /clear       - Clear history
                /exit, /quit - Exit chat

              Examples:
                chat                              # Start chat with RAG
                chat --no-rag                     # Start chat without RAG
                chat my_index.json --min-score=0.7  # Custom index with filter

              Features:
                - Maintains conversation context across questions
                - Automatically saves history after each question
                - Shows sources for each answer (with RAG)
                - Loads previous history on startup if exists

          stats <index-path>
              Show statistics about the index

              Example:
                stats my_index.json

        Prerequisites:
          - Ollama must be running (http://localhost:11434) for embeddings
          - Install embedding model: ollama pull nomic-embed-text
          - OpenRouter API key configured in OpenRouterClient.kt
    """.trimIndent())
}
