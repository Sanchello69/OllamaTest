# Multi-stage build для минимизации размера образа

# Stage 1: Build
FROM gradle:8.5-jdk17 AS build

WORKDIR /app

# Копируем файлы сборки
COPY build.gradle.kts settings.gradle.kts ./
COPY src ./src

# Собираем проект
RUN gradle build --no-daemon -x test

# Stage 2: Runtime
FROM openjdk:17-slim

WORKDIR /app

# Копируем собранный JAR из stage 1
COPY --from=build /app/build/libs/OllamaTest-1.0-SNAPSHOT.jar ./app.jar

# Создаем точку монтирования для данных
VOLUME /data

# Указываем точку входа
ENTRYPOINT ["java", "-jar", "app.jar"]

# По умолчанию показываем справку
CMD ["--help"]
