#!/bin/bash

echo "🔍 Проверка Ollama..."
echo ""

# Проверка 1: Установлена ли Ollama
if ! command -v ollama &> /dev/null; then
    echo "❌ Ollama не установлена"
    echo ""
    echo "Установите Ollama:"
    echo "  curl -fsSL https://ollama.com/install.sh | sh"
    echo ""
    echo "Или скачайте с: https://ollama.com/download"
    exit 1
fi

echo "✅ Ollama установлена: $(which ollama)"
echo ""

# Проверка 2: Запущена ли Ollama
echo "🔍 Проверка сервера Ollama..."
if curl -s http://localhost:11434/api/tags &> /dev/null; then
    echo "✅ Ollama сервер запущен"
    echo ""

    # Проверка 3: Установлена ли модель nomic-embed-text
    echo "🔍 Проверка модели nomic-embed-text..."
    if ollama list | grep -q "nomic-embed-text"; then
        echo "✅ Модель nomic-embed-text установлена"
        echo ""
        echo "🎉 Все готово! Можете использовать приложение."
        echo ""
        echo "Попробуйте:"
        echo "  ./chat.sh"
        echo "  или"
        echo "  ./gradlew run --args='ask \"Ваш вопрос\"' --console=plain"
    else
        echo "❌ Модель nomic-embed-text не установлена"
        echo ""
        echo "Установите модель:"
        echo "  ollama pull nomic-embed-text"
        echo ""
        echo "После установки можете использовать приложение."
    fi
else
    echo "❌ Ollama сервер не запущен"
    echo ""
    echo "Запустите Ollama одним из способов:"
    echo ""
    echo "Способ 1 (приложение macOS):"
    echo "  - Найдите Ollama в Applications"
    echo "  - Запустите приложение"
    echo "  - Иконка появится в menu bar"
    echo ""
    echo "Способ 2 (терминал):"
    echo "  - Откройте новый терминал"
    echo "  - Выполните: ollama serve"
    echo "  - Оставьте терминал открытым"
    echo ""
    echo "После запуска Ollama снова выполните этот скрипт для проверки:"
    echo "  ./check_ollama.sh"
fi

echo ""
