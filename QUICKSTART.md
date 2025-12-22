# Быстрый старт

## Предварительные требования

1. **Установите Java 17+**
   ```bash
   java -version
   ```

2. **Установите и запустите Ollama**
   ```bash
   # macOS/Linux
   curl -fsSL https://ollama.com/install.sh | sh

   # Запустите Ollama в отдельном терминале
   ollama serve
   ```

3. **Загрузите модель эмбеддингов**
   ```bash
   ollama pull nomic-embed-text
   ```

## Быстрая сборка и запуск

### Вариант 1: Использование IntelliJ IDEA (рекомендуется)

1. Откройте проект в IntelliJ IDEA
2. IDEA автоматически загрузит зависимости
3. Запустите `Main.kt`

### Вариант 2: Командная строка с Gradle

```bash
# Если у вас установлен Gradle
gradle build
gradle run --args="index example.rtf"
```

### Вариант 3: Использование Docker (без установки JDK локально)

Создайте `Dockerfile`:
```dockerfile
FROM gradle:8.5-jdk17 AS build
WORKDIR /app
COPY . .
RUN gradle build --no-daemon

FROM openjdk:17-slim
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
```

Соберите и запустите:
```bash
docker build -t ollama-test .
docker run --network host -v $(pwd):/data ollama-test index /data/example.rtf
```

## Тестирование приложения

### Шаг 1: Индексация примера

```bash
# Если собрали JAR
java -jar build/libs/OllamaTest-1.0-SNAPSHOT.jar index example.rtf

# Или через Gradle
gradle run --args="index example.rtf"
```

Вы должны увидеть:
```
📄 Processing RTF file: example.rtf
Step 1: Parsing RTF file...
✓ Extracted 3245 characters

Step 2: Splitting text into chunks...
✓ Created 8 chunks

Step 3: Generating embeddings with Ollama...
Progress: 8/8 chunks processed
✓ Generated 8 embeddings

Step 4: Creating vector index...
✓ Index created with 8 entries

Step 5: Saving index to disk...
✓ Index saved successfully
```

### Шаг 2: Поиск

```bash
# Поиск по теме машинного обучения
java -jar build/libs/OllamaTest-1.0-SNAPSHOT.jar search embeddings_index.json "deep learning neural networks"

# Или через Gradle
gradle run --args="search embeddings_index.json 'what is supervised learning'"
```

Результат:
```
🔍 Searching index: embeddings_index.json
Query: "deep learning neural networks"

Results:

1. Score: 0.8542
   Text: Deep learning is a specialized subset of machine learning that uses neural networks...
   Metadata: {chunk_index=5, source_file=example.rtf}

2. Score: 0.7234
   Text: Types of Machine Learning. There are three main types...
```

### Шаг 3: Статистика

```bash
java -jar build/libs/OllamaTest-1.0-SNAPSHOT.jar stats embeddings_index.json
```

## Работа с вашими RTF файлами

```bash
# Индексация вашего документа
java -jar build/libs/OllamaTest-1.0-SNAPSHOT.jar index /path/to/your/document.rtf my_index.json

# Поиск с большим количеством результатов
java -jar build/libs/OllamaTest-1.0-SNAPSHOT.jar search my_index.json "ваш запрос" 10

# Настройка размера чанков (большие чанки для лучшего контекста)
java -jar build/libs/OllamaTest-1.0-SNAPSHOT.jar index document.rtf index.json 1000 100
```

## Устранение проблем

### "Connection refused" при обращении к Ollama

```bash
# Проверьте, запущен ли Ollama
curl http://localhost:11434/api/tags

# Если нет, запустите
ollama serve
```

### "Model not found"

```bash
# Загрузите модель
ollama pull nomic-embed-text

# Проверьте список моделей
ollama list
```

### Медленная генерация эмбеддингов

- Нормально: ~0.1-0.5 секунды на чанк
- Если медленнее, проверьте загрузку CPU/памяти
- Ollama может загружать модель при первом запросе (подождите ~30 секунд)

## Следующие шаги

1. Прочитайте полную документацию в [README.md](README.md)
2. Экспериментируйте с разными размерами чанков
3. Попробуйте другие модели эмбеддингов (mxbai-embed-large, all-minilm)
4. Интегрируйте с FAISS для больших датасетов

## Полезные команды

```bash
# Пересоздать индекс с новыми параметрами
java -jar build/libs/OllamaTest-1.0-SNAPSHOT.jar index document.rtf new_index.json 300 30

# Сравнить результаты разных чанк-стратегий
java -jar build/libs/OllamaTest-1.0-SNAPSHOT.jar search index1.json "query"
java -jar build/libs/OllamaTest-1.0-SNAPSHOT.jar search index2.json "query"

# Проверить размер индекса
ls -lh *.json
```
