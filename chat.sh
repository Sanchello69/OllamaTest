#!/bin/bash

# Удобный скрипт для запуска интерактивного чата

# Проверяем, собран ли JAR
JAR_FILE="build/libs/OllamaTest-1.0-SNAPSHOT.jar"

if [ ! -f "$JAR_FILE" ]; then
    echo "JAR файл не найден. Собираем проект..."
    ./gradlew jar
    echo ""
fi

# Запускаем чат с переданными аргументами
echo "Запуск интерактивного чата..."
echo ""
java -jar "$JAR_FILE" chat "$@"
