# Структура проекта OllamaTest

## Обзор

Консольное приложение на Kotlin для обработки RTF файлов, генерации векторных эмбеддингов через Ollama и сохранения индекса для семантического поиска.

## Дерево файлов

```
OllamaTest/
├── src/
│   └── main/
│       └── kotlin/
│           ├── Main.kt              # Главное приложение с CLI интерфейсом
│           ├── RtfParser.kt         # Парсер RTF файлов
│           ├── TextChunker.kt       # Разбиение текста на чанки
│           ├── OllamaClient.kt      # HTTP клиент для Ollama API
│           └── VectorIndex.kt       # Векторный индекс с поиском
│
├── build.gradle.kts                 # Конфигурация сборки Gradle
├── settings.gradle.kts              # Настройки проекта Gradle
│
├── Dockerfile                       # Docker образ для приложения
├── docker-compose.yml               # Оркестрация Ollama + приложение
├── .dockerignore                    # Исключения для Docker build
│
├── example.rtf                      # Тестовый RTF файл
├── .gitignore                       # Git исключения
│
├── README.md                        # Основная документация
├── QUICKSTART.md                    # Быстрый старт
├── INSTALL.md                       # Инструкции по установке
├── DOCKER.md                        # Использование Docker
├── PROJECT_STRUCTURE.md             # Этот файл
│
└── run.sh                           # Скрипт запуска (альтернатива Gradle)
```

## Описание компонентов

### Исходный код (src/main/kotlin/)

#### Main.kt
**Назначение**: Точка входа в приложение с CLI интерфейсом

**Основные функции**:
- `main()` - парсинг аргументов командной строки
- `indexRtfFile()` - обработка RTF файла и создание индекса
- `searchIndex()` - поиск по индексу
- `showStats()` - статистика индекса
- `printUsage()` - справка по использованию

**Команды**:
- `index <file> [options]` - индексация файла
- `search <index> <query> [top-k]` - поиск
- `stats <index>` - статистика

**Размер**: ~230 строк

#### RtfParser.kt
**Назначение**: Парсинг RTF файлов и извлечение текста

**Класс**: `RtfParser`

**Методы**:
- `parseRtfFile(filePath: String): String` - парсит RTF и возвращает текст

**Зависимости**:
- `rtfparserkit` - библиотека для парсинга RTF

**Обработка ошибок**:
- Проверка существования файла
- Обработка исключений при парсинге

**Размер**: ~40 строк

#### TextChunker.kt
**Назначение**: Разбиение текста на чанки для эмбеддингов

**Класс**: `TextChunker(chunkSize, overlap)`

**Параметры**:
- `chunkSize` - размер чанка в символах (по умолчанию 500)
- `overlap` - перекрытие между чанками (по умолчанию 50)

**Data классы**:
- `Chunk(text, index, startPosition, endPosition)`

**Методы**:
- `chunkText(text: String): List<Chunk>` - разбивает текст

**Алгоритм**:
1. Очистка текста от лишних пробелов и управляющих символов
2. Разбиение на чанки с учетом границ предложений
3. Поиск оптимальных точек разрыва (., !, ?, \n)
4. Добавление перекрытия для контекста

**Размер**: ~70 строк

#### OllamaClient.kt
**Назначение**: HTTP клиент для взаимодействия с Ollama API

**Класс**: `OllamaClient(baseUrl, model)`

**Параметры**:
- `baseUrl` - адрес Ollama (по умолчанию http://localhost:11434)
- `model` - модель эмбеддингов (по умолчанию nomic-embed-text)

**Data классы**:
- `EmbeddingRequest(model, prompt)` - запрос
- `EmbeddingResponse(embedding)` - ответ

**Методы**:
- `generateEmbedding(text: String): List<Double>` - генерация одного эмбеддинга
- `generateEmbeddings(texts: List<String>): List<List<Double>>` - пакетная генерация
- `close()` - закрытие HTTP клиента

**Особенности**:
- Асинхронные вызовы через coroutines
- Прогресс-бар при пакетной обработке
- Обработка ошибок с fallback на нулевой вектор
- Использование Ktor HTTP client

**Зависимости**:
- `ktor-client-core`, `ktor-client-cio`
- `kotlinx-serialization-json`

**Размер**: ~70 строк

#### VectorIndex.kt
**Назначение**: Хранение и поиск векторных эмбеддингов

**Класс**: `VectorIndex`

**Data классы**:
- `IndexEntry(id, text, embedding, metadata)` - элемент индекса
- `IndexData(entries, dimension, totalEntries)` - данные индекса
- `SearchResult(id, text, score, metadata)` - результат поиска

**Методы**:
- `add(text, embedding, metadata)` - добавить вектор
- `addBatch(texts, embeddings, metadata)` - добавить множество векторов
- `search(queryEmbedding, k): List<SearchResult>` - поиск похожих векторов
- `save(filePath)` - сохранить индекс в файл
- `load(filePath)` - загрузить индекс из файла
- `getStats(): String` - статистика индекса
- `cosineSimilarity(a, b): Double` - вычисление косинусного сходства

**Формат хранения**: JSON

**Алгоритм поиска**:
1. Вычисление косинусного сходства с каждым вектором в индексе
2. Сортировка по убыванию сходства
3. Возврат топ-K результатов

**Комментарии по FAISS**:
- Включены инструкции по интеграции с FAISS
- Текущая реализация оптимальна для индексов до 10K векторов
- Для больших датасетов рекомендуется FAISS

**Размер**: ~180 строк

### Конфигурация

#### build.gradle.kts
**Зависимости**:
- Kotlin stdlib + coroutines
- kotlinx-serialization-json - сериализация
- Ktor client - HTTP запросы к Ollama
- rtfparserkit - парсинг RTF
- slf4j-simple - логирование

**Настройки**:
- JVM target: 17
- Kotlin version: 1.9.22
- Fat JAR с зависимостями

#### Dockerfile
**Тип**: Multi-stage build

**Stage 1 (Build)**:
- Базовый образ: `gradle:8.5-jdk17`
- Действия: компиляция проекта
- Размер: ~1.5 GB

**Stage 2 (Runtime)**:
- Базовый образ: `openjdk:17-slim`
- Содержимое: только JAR файл
- Размер: ~400 MB

**Оптимизации**:
- Использование кэширования слоев Gradle
- Минимальный runtime образ
- .dockerignore для исключения лишних файлов

#### docker-compose.yml
**Сервисы**:

1. **ollama**:
   - Образ: `ollama/ollama:latest`
   - Порт: 11434
   - Healthcheck для проверки готовности

2. **ollama-init**:
   - Загрузка модели `nomic-embed-text`
   - Запуск один раз при старте

3. **app** (закомментирован):
   - Приложение с доступом к Ollama
   - Примеры использования в комментариях

### Документация

#### README.md (10KB)
**Разделы**:
- Возможности
- Требования
- Установка и сборка
- Использование (примеры)
- Структура проекта
- Компоненты (детальное описание)
- Интеграция с FAISS
- Настройка модели
- Устранение неполадок
- Производительность

#### QUICKSTART.md (5KB)
**Разделы**:
- Предварительные требования
- Быстрая сборка (3 варианта)
- Тестирование (пошагово)
- Работа с файлами
- Устранение проблем
- Полезные команды

#### INSTALL.md (7KB)
**Разделы**:
- Вариант 1: IntelliJ IDEA (рекомендуется)
- Вариант 2: Установка Gradle вручную
- Вариант 3: Docker
- Вариант 4: Ручная компиляция
- Проверка установки
- Типичные проблемы
- Минимальные требования

#### DOCKER.md (7KB)
**Разделы**:
- Быстрый старт с Docker Compose
- Использование приложения
- Продвинутые сценарии
- Управление данными
- Производственное развертывание
- Мониторинг
- Troubleshooting
- Оптимизация

#### PROJECT_STRUCTURE.md (этот файл)
**Разделы**:
- Дерево файлов
- Описание компонентов
- Архитектура
- Flow данных

### Тестовые файлы

#### example.rtf (4KB)
**Содержимое**: Текст о машинном обучении

**Структура**:
- Введение
- Типы ML (Supervised, Unsupervised, Reinforcement)
- Применения
- Deep Learning
- Проблемы
- Будущее
- Заключение

**Назначение**: Демонстрация работы приложения

## Архитектура

### Pipeline обработки

```
RTF File
   ↓
[RtfParser] → Plain Text
   ↓
[TextChunker] → List<Chunk>
   ↓
[OllamaClient] → List<Embedding>
   ↓
[VectorIndex] → Saved Index (JSON)
```

### Поиск

```
Query String
   ↓
[OllamaClient] → Query Embedding
   ↓
[VectorIndex.search()] → Top-K Results
   ↓
Display Results
```

## Flow данных

### Команда: index

1. **Input**: RTF файл
2. **Parse**: RtfParser извлекает текст
3. **Chunk**: TextChunker разбивает на чанки
4. **Embed**: OllamaClient генерирует эмбеддинги для каждого чанка
5. **Index**: VectorIndex сохраняет пары (текст, эмбеддинг, метаданные)
6. **Save**: Индекс сериализуется в JSON
7. **Output**: Файл индекса

### Команда: search

1. **Input**: Запрос + индекс
2. **Load**: VectorIndex загружает индекс из JSON
3. **Embed**: OllamaClient генерирует эмбеддинг запроса
4. **Search**: Вычисление косинусного сходства со всеми векторами
5. **Rank**: Сортировка по убыванию сходства
6. **Output**: Топ-K результатов с текстом и метаданными

## Зависимости между компонентами

```
Main.kt
├── RtfParser.kt
├── TextChunker.kt
├── OllamaClient.kt (зависит от Ktor)
└── VectorIndex.kt (зависит от kotlinx.serialization)
```

**Независимые компоненты**:
- RtfParser (можно использовать отдельно)
- TextChunker (можно использовать с любым текстом)
- OllamaClient (можно использовать для других задач)
- VectorIndex (можно использовать с любыми векторами)

## Расширяемость

### Добавление нового формата файлов

Создайте интерфейс `DocumentParser`:

```kotlin
interface DocumentParser {
    fun parse(filePath: String): String
}

class RtfParser : DocumentParser { ... }
class PdfParser : DocumentParser { ... }
class DocxParser : DocumentParser { ... }
```

### Добавление других моделей эмбеддингов

Создайте интерфейс `EmbeddingProvider`:

```kotlin
interface EmbeddingProvider {
    suspend fun generateEmbedding(text: String): List<Double>
}

class OllamaEmbedding : EmbeddingProvider { ... }
class OpenAIEmbedding : EmbeddingProvider { ... }
class HuggingFaceEmbedding : EmbeddingProvider { ... }
```

### Добавление других индексов

```kotlin
interface VectorStore {
    fun add(text: String, embedding: List<Double>)
    fun search(query: List<Double>, k: Int): List<SearchResult>
}

class SimpleVectorIndex : VectorStore { ... }
class FaissVectorIndex : VectorStore { ... }
class MilvusVectorIndex : VectorStore { ... }
```

## Производительность

### Метрики (на примере example.rtf)

- **Файл**: 4KB, ~3200 символов
- **Чанки**: 8 чанков (500 символов каждый)
- **Время парсинга**: < 0.1s
- **Время чанкинга**: < 0.01s
- **Время генерации эмбеддингов**: ~0.8s (8 чанков × 0.1s)
- **Время сохранения индекса**: < 0.01s
- **Размер индекса**: ~15KB (JSON)

### Оптимизации

1. **Параллельная генерация** (TODO):
   ```kotlin
   texts.mapParallel { client.generateEmbedding(it) }
   ```

2. **Пакетная обработка эмбеддингов** (TODO):
   ```kotlin
   client.generateEmbeddingsBatch(texts)  // Один API вызов
   ```

3. **Кэширование результатов** (TODO):
   ```kotlin
   val cache = LRUCache<String, List<Double>>(maxSize = 1000)
   ```

## Тестирование

### Юнит-тесты (TODO)

```kotlin
class TextChunkerTest {
    @Test
    fun testChunking() {
        val chunker = TextChunker(100, 10)
        val chunks = chunker.chunkText("test text")
        assert(chunks.isNotEmpty())
    }
}
```

### Интеграционные тесты (TODO)

```kotlin
class OllamaClientTest {
    @Test
    suspend fun testEmbeddingGeneration() {
        val client = OllamaClient()
        val embedding = client.generateEmbedding("test")
        assertEquals(384, embedding.size)
    }
}
```

## Развертывание

### Локальное

```bash
./gradlew build
java -jar build/libs/OllamaTest-1.0-SNAPSHOT.jar index file.rtf
```

### Docker

```bash
docker-compose up -d
docker run --network host -v $(pwd):/data ollama-embeddings index /data/file.rtf
```

### Облако (TODO)

- AWS Lambda + S3
- Google Cloud Run + Cloud Storage
- Azure Functions + Blob Storage

## Безопасность

### Текущие меры

- Валидация путей к файлам
- Обработка ошибок при парсинге
- Ограничение размера запросов
- Нет хранения чувствительных данных

### Рекомендации для продакшена

- [ ] Добавить аутентификацию для API
- [ ] Валидация входных данных
- [ ] Rate limiting для API вызовов
- [ ] Шифрование индексов
- [ ] Логирование и мониторинг
- [ ] Sandboxing для парсинга файлов

## Лицензия

MIT License

## Контакты и поддержка

- Issues: GitHub Issues
- Документация: README.md
- Примеры: example.rtf

---

**Версия**: 1.0-SNAPSHOT
**Последнее обновление**: 2024
**Язык**: Kotlin 1.9.22
**JVM**: 17+
