#!/bin/bash

echo "=================================="
echo "  Проверка системы RAG"
echo "=================================="
echo ""

ALL_OK=true

# Проверка 1: Ollama
echo "1️⃣  Проверка Ollama..."
echo ""

if ! command -v ollama &> /dev/null; then
    echo "   ❌ Ollama не установлена"
    ALL_OK=false
else
    echo "   ✅ Ollama установлена"

    if curl -s http://localhost:11434/api/tags &> /dev/null; then
        echo "   ✅ Ollama сервер запущен"

        if ollama list | grep -q "nomic-embed-text"; then
            echo "   ✅ Модель nomic-embed-text установлена"
        else
            echo "   ❌ Модель nomic-embed-text не установлена"
            echo "      Установите: ollama pull nomic-embed-text"
            ALL_OK=false
        fi
    else
        echo "   ❌ Ollama сервер не запущен"
        echo "      Запустите: ollama serve"
        ALL_OK=false
    fi
fi

echo ""

# Проверка 2: OpenRouter API
echo "2️⃣  Проверка OpenRouter API..."
echo ""

if [ ! -f local.properties ]; then
    echo "   ❌ Файл local.properties не найден"
    ALL_OK=false
else
    API_KEY=$(grep "OPENROUTER_API_KEY=" local.properties | cut -d'=' -f2)

    if [ -z "$API_KEY" ]; then
        echo "   ❌ API ключ не настроен"
        ALL_OK=false
    else
        echo "   ✅ API ключ настроен"

        # Быстрая проверка API
        RESPONSE=$(curl -s -X POST "https://openrouter.ai/api/v1/chat/completions" \
          -H "Content-Type: application/json" \
          -H "Authorization: Bearer $API_KEY" \
          -H "HTTP-Referer: https://ollamatest.app" \
          -d '{"model":"nousresearch/hermes-3-llama-3.1-405b:free","messages":[{"role":"user","content":"hi"}]}' \
          --max-time 10)

        if echo "$RESPONSE" | grep -q '"choices"'; then
            echo "   ✅ OpenRouter API работает"
        else
            echo "   ❌ OpenRouter API вернул ошибку"
            ALL_OK=false
        fi
    fi
fi

echo ""

# Проверка 3: Индекс
echo "3️⃣  Проверка векторного индекса..."
echo ""

if [ -f embeddings_index.json ]; then
    SIZE=$(du -h embeddings_index.json | cut -f1)
    echo "   ✅ Индекс найден (размер: $SIZE)"
else
    echo "   ⚠️  Индекс не найден (embeddings_index.json)"
    echo "      Создайте индекс: ./gradlew run --args='index example.rtf'"
fi

echo ""

# Проверка 4: JAR файл
echo "4️⃣  Проверка сборки проекта..."
echo ""

if [ -f build/libs/OllamaTest-1.0-SNAPSHOT.jar ]; then
    echo "   ✅ JAR файл собран"
else
    echo "   ⚠️  JAR файл не найден"
    echo "      Соберите проект: ./gradlew jar"
fi

echo ""
echo "=================================="

# Итоговый результат
if [ "$ALL_OK" = true ]; then
    echo "🎉 Все компоненты готовы к работе!"
    echo ""
    echo "Запустите приложение:"
    echo "  ./chat.sh                          # Интерактивный чат"
    echo "  ./gradlew run --args='ask \"вопрос\"' --console=plain"
    echo ""
    echo "Дополнительные проверки:"
    echo "  ./check_ollama.sh       # Детальная проверка Ollama"
    echo "  ./check_openrouter.sh   # Детальная проверка OpenRouter"
else
    echo "⚠️  Некоторые компоненты требуют настройки"
    echo ""
    echo "Детальная диагностика:"
    echo "  ./check_ollama.sh       # Проверка Ollama"
    echo "  ./check_openrouter.sh   # Проверка OpenRouter"
fi

echo "=================================="
echo ""
