#!/bin/bash

echo "🔍 Проверка OpenRouter API..."
echo ""

# Проверка 1: Существует ли local.properties
if [ ! -f local.properties ]; then
    echo "❌ Файл local.properties не найден"
    echo ""
    echo "Создайте файл local.properties на основе local.properties.example:"
    echo "  cp local.properties.example local.properties"
    echo ""
    echo "И добавьте ваш API ключ от OpenRouter:"
    echo "  OPENROUTER_API_KEY=ваш-ключ-здесь"
    echo ""
    echo "Получить ключ можно на: https://openrouter.ai/keys"
    exit 1
fi

# Проверка 2: Есть ли API ключ в файле
API_KEY=$(grep "OPENROUTER_API_KEY=" local.properties | cut -d'=' -f2)

if [ -z "$API_KEY" ]; then
    echo "❌ API ключ не найден в local.properties"
    echo ""
    echo "Добавьте строку в local.properties:"
    echo "  OPENROUTER_API_KEY=ваш-ключ-здесь"
    echo ""
    echo "Получить ключ можно на: https://openrouter.ai/keys"
    exit 1
fi

echo "✅ API ключ найден в local.properties"
echo "   Ключ: ${API_KEY:0:20}...${API_KEY: -10}"
echo ""

# Проверка 3: Тестовый запрос к API
echo "🔍 Тестирование подключения к OpenRouter API..."
echo ""

RESPONSE=$(curl -s -X POST "https://openrouter.ai/api/v1/chat/completions" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $API_KEY" \
  -H "HTTP-Referer: https://ollamatest.app" \
  -H "X-Title: Ollama Test RAG" \
  -d '{
    "model": "nousresearch/hermes-3-llama-3.1-405b:free",
    "messages": [
      {"role": "user", "content": "Say hello"}
    ]
  }')

# Проверяем на наличие ошибки
if echo "$RESPONSE" | grep -q '"error"'; then
    ERROR_MSG=$(echo "$RESPONSE" | grep -o '"message":"[^"]*"' | cut -d'"' -f4)
    ERROR_CODE=$(echo "$RESPONSE" | grep -o '"code":"[^"]*"' | cut -d'"' -f4)

    echo "❌ OpenRouter API вернул ошибку:"
    echo "   Сообщение: $ERROR_MSG"
    if [ ! -z "$ERROR_CODE" ]; then
        echo "   Код: $ERROR_CODE"
    fi
    echo ""
    echo "Возможные причины:"
    echo "  - Неверный API ключ"
    echo "  - Истек срок действия ключа"
    echo "  - Превышен лимит запросов"
    echo "  - Нет баланса на счету"
    echo ""
    echo "Проверьте ваш аккаунт на: https://openrouter.ai/account"
    echo ""
    echo "Полный ответ API:"
    echo "$RESPONSE" | jq '.' 2>/dev/null || echo "$RESPONSE"
    exit 1
fi

# Проверяем на наличие choices
if echo "$RESPONSE" | grep -q '"choices"'; then
    echo "✅ OpenRouter API работает корректно!"
    echo ""

    # Извлекаем ответ
    CONTENT=$(echo "$RESPONSE" | grep -o '"content":"[^"]*"' | head -1 | cut -d'"' -f4)
    if [ ! -z "$CONTENT" ]; then
        echo "Тестовый ответ от LLM:"
        echo "  \"$CONTENT\""
        echo ""
    fi

    echo "🎉 Все готово для работы с OpenRouter!"
    echo ""
    echo "Используемая модель: nex-agi/deepseek-v3.1-nex-n1:free"
    echo ""
    echo "Теперь можете использовать приложение:"
    echo "  ./chat.sh"
    echo "  или"
    echo "  ./gradlew run --args='ask \"Ваш вопрос\"' --console=plain"
else
    echo "⚠️  Неожиданный формат ответа от API"
    echo ""
    echo "Полный ответ:"
    echo "$RESPONSE" | jq '.' 2>/dev/null || echo "$RESPONSE"
fi

echo ""
