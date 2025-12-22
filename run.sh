#!/bin/bash

# Скрипт для запуска приложения
# Использование: ./run.sh <command> [args...]

# Проверка наличия Kotlin компилятора
if ! command -v kotlinc &> /dev/null; then
    echo "Kotlin compiler not found. Installing dependencies with Gradle..."

    # Попытка использовать gradlew или gradle
    if [ -f "./gradlew" ]; then
        ./gradlew build
        java -jar build/libs/OllamaTest-1.0-SNAPSHOT.jar "$@"
    elif command -v gradle &> /dev/null; then
        gradle build
        java -jar build/libs/OllamaTest-1.0-SNAPSHOT.jar "$@"
    else
        echo "Error: Neither gradlew nor gradle found."
        echo "Please install Gradle: https://gradle.org/install/"
        exit 1
    fi
else
    # Прямая компиляция и запуск (для разработки)
    echo "Compiling with kotlinc..."
    kotlinc -cp ".:lib/*" src/main/kotlin/*.kt -include-runtime -d OllamaTest.jar
    java -jar OllamaTest.jar "$@"
fi
