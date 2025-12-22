# Инструкции по установке

## Вариант 1: IntelliJ IDEA (Рекомендуется - самый простой способ)

### Шаги:

1. **Скачайте IntelliJ IDEA**
   - Community Edition (бесплатно): https://www.jetbrains.com/idea/download/
   - Или Ultimate Edition

2. **Откройте проект**
   - File → Open
   - Выберите папку `OllamaTest`
   - IDEA автоматически обнаружит Gradle проект и предложит импортировать его

3. **Дождитесь загрузки зависимостей**
   - В правом нижнем углу вы увидите прогресс загрузки
   - Это может занять 2-5 минут при первом открытии

4. **Соберите проект**
   - View → Tool Windows → Gradle
   - В панели Gradle: Tasks → build → build (двойной клик)

5. **Запустите приложение**
   - Откройте `src/main/kotlin/Main.kt`
   - Нажмите зеленый треугольник возле `fun main`
   - Или используйте Run → Run 'MainKt'

6. **Настройте аргументы запуска**
   - Run → Edit Configurations
   - В поле "Program arguments" введите: `index example.rtf`
   - Нажмите OK и запустите

## Вариант 2: Установка Gradle вручную

### macOS (Homebrew)

```bash
# Установка Homebrew (если еще не установлен)
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"

# Установка Gradle
brew install gradle

# Проверка
gradle --version
```

### Linux (Ubuntu/Debian)

```bash
# Установка SDKMAN (менеджер SDK для Java инструментов)
curl -s "https://get.sdkman.io" | bash
source "$HOME/.sdkman/bin/sdkman-init.sh"

# Установка Gradle
sdk install gradle 8.5

# Проверка
gradle --version
```

### Windows

1. Скачайте Gradle: https://gradle.org/releases/
2. Распакуйте в `C:\Gradle`
3. Добавьте в PATH: `C:\Gradle\gradle-8.5\bin`
4. Откройте новый CMD и проверьте: `gradle --version`

### После установки Gradle

```bash
# Перейдите в директорию проекта
cd OllamaTest

# Инициализируйте Gradle Wrapper
gradle wrapper

# Соберите проект
./gradlew build

# Запустите приложение
./gradlew run --args="index example.rtf"
```

## Вариант 3: Docker (без установки JDK/Gradle)

### Создайте Dockerfile

Создан файл `Dockerfile` в корне проекта - см. ниже.

### Использование Docker

```bash
# Сборка образа
docker build -t ollama-embeddings .

# Запуск Ollama в отдельном контейнере
docker run -d -p 11434:11434 --name ollama ollama/ollama
docker exec ollama ollama pull nomic-embed-text

# Использование приложения
docker run --rm \
  --network host \
  -v $(pwd):/app \
  -w /app \
  ollama-embeddings index example.rtf

# Поиск
docker run --rm \
  --network host \
  -v $(pwd):/app \
  -w /app \
  ollama-embeddings search embeddings_index.json "your query"
```

## Вариант 4: Ручная компиляция (для продвинутых пользователей)

Если вы хотите скомпилировать без Gradle:

```bash
# Установите Kotlin compiler
brew install kotlin  # macOS
# или скачайте с https://kotlinlang.org/docs/command-line.html

# Скачайте зависимости вручную (сложно, не рекомендуется)
# Лучше используйте один из вариантов выше
```

## Проверка установки

После установки любым способом, проверьте работу:

### 1. Убедитесь, что Ollama запущен

```bash
curl http://localhost:11434/api/tags
```

Должны увидеть JSON с моделями.

### 2. Проверьте наличие модели

```bash
ollama list
```

Должна быть `nomic-embed-text`.

### 3. Запустите тест

```bash
# Через Gradle
./gradlew run --args="index example.rtf"

# Или через JAR
java -jar build/libs/OllamaTest-1.0-SNAPSHOT.jar index example.rtf
```

Должны увидеть успешную обработку файла.

## Типичные проблемы

### "Could not determine Java version"

Установите Java 17 или выше:

```bash
# macOS
brew install openjdk@17

# Ubuntu
sudo apt install openjdk-17-jdk

# Проверка
java -version
```

### "Connection to localhost:11434 refused"

Запустите Ollama:

```bash
ollama serve
```

В отдельном терминале:

```bash
ollama pull nomic-embed-text
```

### "BUILD FAILED" при компиляции

Очистите кэш Gradle:

```bash
./gradlew clean
rm -rf .gradle build
./gradlew build
```

### Проблемы с правами доступа (Linux/macOS)

```bash
chmod +x gradlew
./gradlew build
```

## Минимальные требования системы

- **ОС**: macOS 10.14+, Linux (Ubuntu 20.04+), Windows 10+
- **RAM**: 4 GB (рекомендуется 8 GB для больших файлов)
- **Диск**: 2 GB свободного места
- **Java**: JDK 17 или выше
- **Ollama**: Последняя версия

## Дополнительная помощь

Если возникли проблемы:

1. Проверьте версию Java: `java -version` (должна быть 17+)
2. Проверьте Ollama: `curl http://localhost:11434/api/tags`
3. Проверьте структуру проекта: `ls -la src/main/kotlin/`
4. Посмотрите логи ошибок и обратитесь к README.md

## Рекомендуемый путь для начинающих

**IntelliJ IDEA Community Edition** - это самый простой способ:
- Автоматическое управление зависимостями
- Встроенный Gradle
- Отладка и профилирование
- Автодополнение кода
- Бесплатно

Скачайте здесь: https://www.jetbrains.com/idea/download/
