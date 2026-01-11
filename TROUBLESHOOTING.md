# Устранение проблем

## Быстрая диагностика

Запустите скрипт проверки системы:

```bash
./check_system.sh
```

Это проверит все компоненты и подскажет, что нужно исправить.

## Частые ошибки и решения

### ❌ Error: Connection refused (Ollama)

**Проблема:** Ollama не запущена

**Решение:**
```bash
# Проверка
./check_ollama.sh

# Запуск Ollama (выберите один способ):

# Способ 1: Через приложение (macOS)
# Найдите Ollama в Applications и запустите

# Способ 2: Через терминал
ollama serve  # Оставьте терминал открытым
```

После запуска проверьте:
```bash
curl http://localhost:11434/api/tags
```

### ❌ Field 'choices' is required (OpenRouter API)

**Проблема:** OpenRouter API вернул ошибку вместо ответа

**Решение:**
```bash
# Проверка API ключа
./check_openrouter.sh
```

**Возможные причины:**

1. **Неверный API ключ**
   - Проверьте `local.properties`
   - Убедитесь, что ключ начинается с `sk-or-v1-`
   - Получите новый ключ на https://openrouter.ai/keys

2. **Превышен лимит запросов**
   - Проверьте ваш баланс на https://openrouter.ai/account
   - Используемая модель: `nex-agi/deepseek-v3.1-nex-n1:free` (бесплатная)

3. **Проблемы с сетью**
   - Проверьте интернет-соединение
   - Попробуйте позже

**Исправление (уже сделано):**
Теперь приложение показывает детальные ошибки от API вместо общей ошибки десериализации.

### ❌ Model not found: nomic-embed-text

**Проблема:** Модель для эмбеддингов не установлена

**Решение:**
```bash
ollama pull nomic-embed-text
```

Проверка:
```bash
ollama list
```

### ⚠️ Index not found: embeddings_index.json

**Проблема:** Векторный индекс не создан

**Решение:**
```bash
# Создайте индекс из вашего RTF файла
./gradlew run --args='index example.rtf embeddings_index.json'

# Или из другого файла
./gradlew run --args='index /path/to/your/document.rtf'
```

### ❌ Интерактивный чат сразу завершается

**Проблема:** Gradle не подключает стандартный ввод (уже исправлено в build.gradle.kts)

**Решения:**

**1. Используйте скрипт (рекомендуется):**
```bash
./chat.sh
```

**2. Используйте флаг --console=plain:**
```bash
./gradlew run --args='chat' --console=plain
```

**3. Используйте JAR напрямую:**
```bash
java -jar build/libs/OllamaTest-1.0-SNAPSHOT.jar chat
```

### ❌ No response from LLM

**Проблема:** LLM не вернул ответ

**Возможные причины:**
- Проблемы с OpenRouter API (см. выше)
- Таймаут запроса (по умолчанию 60 секунд)
- Некорректный формат запроса

**Решение:**
```bash
# Проверьте OpenRouter
./check_openrouter.sh

# Попробуйте без RAG
./gradlew run --args='ask "тест" --no-rag' --console=plain
```

## Диагностические скрипты

### Полная проверка системы
```bash
./check_system.sh
```
Проверяет: Ollama, OpenRouter API, индекс, сборку

### Проверка Ollama
```bash
./check_ollama.sh
```
Проверяет: установка, запуск сервера, модель nomic-embed-text

### Проверка OpenRouter API
```bash
./check_openrouter.sh
```
Проверяет: API ключ, подключение, тестовый запрос

## Пошаговая настройка системы

### Шаг 1: Установите Ollama
```bash
# macOS/Linux
curl -fsSL https://ollama.com/install.sh | sh

# Или скачайте с https://ollama.com/download
```

### Шаг 2: Запустите Ollama и загрузите модель
```bash
# В одном терминале:
ollama serve

# В другом терминале:
ollama pull nomic-embed-text
```

### Шаг 3: Настройте OpenRouter API
```bash
# 1. Получите API ключ на https://openrouter.ai/keys

# 2. Создайте local.properties
cp local.properties.example local.properties

# 3. Добавьте ключ в local.properties:
echo "OPENROUTER_API_KEY=ваш-ключ-здесь" >> local.properties
```

### Шаг 4: Соберите проект
```bash
./gradlew build
```

### Шаг 5: Создайте индекс
```bash
./gradlew run --args='index example.rtf'
```

### Шаг 6: Проверьте систему
```bash
./check_system.sh
```

### Шаг 7: Запустите приложение
```bash
# Интерактивный чат
./chat.sh

# Или одиночный вопрос
./gradlew run --args='ask "Ваш вопрос"' --console=plain
```

## Логи и отладка

### Включить подробные логи
```bash
# Установите уровень логирования
export OLLAMA_DEBUG=1

# Запустите с verbose
./gradlew run --args='ask "вопрос"' --console=plain --info
```

### Проверить сырой ответ API

Если нужно увидеть точный ответ от OpenRouter API, можете временно добавить логирование в `OpenRouterClient.kt`:

```kotlin
val response: ChatResponse = client.post(...) {
    ...
}.body<String>().also {
    println("Raw response: $it")
}.let { Json.decodeFromString(it) }
```

## Поддержка и сообщения об ошибках

Если проблема не решена:

1. Запустите все диагностические скрипты:
   ```bash
   ./check_system.sh > diagnostics.txt
   ./check_ollama.sh >> diagnostics.txt
   ./check_openrouter.sh >> diagnostics.txt
   ```

2. Сохраните сообщение об ошибке

3. Проверьте:
   - Версию Java: `java -version`
   - Версию Ollama: `ollama --version`
   - Размер индекса: `ls -lh embeddings_index.json`

## Частые вопросы

**Q: Можно ли использовать другую модель для LLM?**

A: Да, измените параметр `model` в `OpenRouterClient.kt`:
```kotlin
class OpenRouterClient(
    private val model: String = "ваша-модель-здесь",
    ...
)
```

**Q: Как очистить историю диалогов?**

A: В интерактивном чате используйте команду `/clear`, или удалите файл:
```bash
rm conversation_history.json
```

**Q: Можно ли использовать без интернета?**

A: Частично. Ollama работает локально, но OpenRouter API требует интернет. Для полностью локальной работы замените OpenRouter на локальную модель через Ollama.

**Q: Как увеличить количество источников в ответе?**

A: Измените параметр в `Main.kt`, строка с `index.search(queryEmbedding, 5)` - замените `5` на нужное число.

**Q: Слишком медленная генерация ответов**

A: Это зависит от:
- Скорости OpenRouter API (обычно 5-10 секунд)
- Размера контекста (больше источников = медленнее)
- Загруженности бесплатной модели

Попробуйте:
- Использовать меньше источников
- Увеличить `--min-score` для более строгой фильтрации
- Использовать платную модель (быстрее)
