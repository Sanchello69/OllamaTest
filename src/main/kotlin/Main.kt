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
                println("Usage: ask <question> [index-path] [--no-rag]")
                return@runBlocking
            }

            val question = args[1]
            val useRag = !args.contains("--no-rag")
            val indexPath = if (args.size > 2 && !args[2].startsWith("--")) args[2] else "embeddings_index.json"

            askQuestion(question, indexPath, useRag)
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

suspend fun askQuestion(question: String, indexPath: String, useRag: Boolean) {
    println("🤖 AI Assistant ${if (useRag) "with RAG" else "without RAG"}\n")
    println("Question: \"$question\"\n")

    val openRouterClient = OpenRouterClient()

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
                val answer = openRouterClient.askQuestion(question)
                println("💬 Answer:\n$answer\n")
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
                val answer = openRouterClient.askQuestion(question)
                println("💬 Answer:\n$answer\n")
                return
            } finally {
                ollamaClient.close()
            }
            println("✓ Embedding generated\n")

            // Ищем релевантные чанки
            println("Step 3: Searching for relevant chunks...")
            val results = index.search(queryEmbedding, 5)

            if (results.isEmpty()) {
                println("⚠️  No relevant chunks found")
                println("Falling back to non-RAG mode...\n")
                val answer = openRouterClient.askQuestion(question)
                println("💬 Answer:\n$answer\n")
                return
            }

            println("✓ Found ${results.size} relevant chunks\n")

            // Отображаем найденные чанки
            println("📚 Relevant chunks:")
            results.forEachIndexed { idx, result ->
                println("  ${idx + 1}. [Score: ${"%.4f".format(result.score)}] ${result.text.take(100)}...")
            }
            println()

            // Объединяем чанки в контекст
            val context = results.joinToString("\n\n---\n\n") { it.text }

            // Отправляем вопрос с контекстом в LLM
            println("Step 4: Sending question with context to LLM...")
            val answer = openRouterClient.askQuestion(question, context)

            println("✓ Response received\n")
            println("💬 Answer:\n$answer\n")

        } else {
            // Без RAG: просто вопрос к LLM
            println("Sending question to LLM (without context)...")
            val answer = openRouterClient.askQuestion(question)

            println("✓ Response received\n")
            println("💬 Answer:\n$answer\n")
        }
    } catch (e: Exception) {
        println("❌ Error: ${e.message}")
        e.printStackTrace()
    } finally {
        openRouterClient.close()
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

          ask <question> [index-path] [--no-rag]
              Ask a question to AI assistant (with or without RAG)

              Arguments:
                question     - Your question
                index-path   - Path to the index file (default: embeddings_index.json)
                --no-rag     - Disable RAG mode (no context retrieval)

              Examples:
                ask "Как звали степного волка?"                    # With RAG
                ask "Что такое машинное обучение?" --no-rag       # Without RAG
                ask "Кто главный герой?" my_index.json            # Custom index

              RAG Mode (default):
                1. Finds relevant chunks from the index
                2. Combines them with your question
                3. Sends to LLM for answer

              Without RAG (--no-rag):
                Sends question directly to LLM without context

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
