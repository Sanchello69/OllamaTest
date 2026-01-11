#!/bin/bash

# Скрипт для демонстрации новых возможностей: история диалогов и вывод источников

echo "========================================="
echo "Тест 1: Простой вопрос с источниками"
echo "========================================="
echo ""

./gradlew run --args='ask "Как звали степного волка?"' --quiet

echo ""
echo ""
echo "========================================="
echo "Тест 2: Вопрос с сохранением истории"
echo "========================================="
echo ""

# Удаляем старую историю для чистоты теста
rm -f test_history.json

./gradlew run --args='ask "О чем книга Степной волк?" --save-history --history-path=test_history.json' --quiet

echo ""
echo ""
echo "========================================="
echo "Тест 3: Уточняющий вопрос (использует историю)"
echo "========================================="
echo ""

./gradlew run --args='ask "А кто автор?" --save-history --history-path=test_history.json' --quiet

echo ""
echo ""
echo "========================================="
echo "Тест 4: Вопрос без RAG (без источников)"
echo "========================================="
echo ""

./gradlew run --args='ask "Что такое роман?" --no-rag --save-history --history-path=test_history.json' --quiet

echo ""
echo ""
echo "========================================="
echo "Результаты:"
echo "========================================="
echo ""
echo "История диалога сохранена в test_history.json"
echo ""

# Показываем размер файла истории
if [ -f test_history.json ]; then
    echo "Размер файла истории: $(du -h test_history.json | cut -f1)"
    echo ""
    echo "Превью истории (первые 30 строк):"
    echo "---"
    head -n 30 test_history.json
    echo "..."
    echo ""
fi

echo "========================================="
echo "Тест завершен!"
echo "========================================="
echo ""
echo "Попробуйте интерактивный чат:"
echo "  ./gradlew run --args='chat'"
echo ""
