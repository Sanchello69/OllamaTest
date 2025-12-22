# Использование с Docker

Docker упрощает развертывание приложения без необходимости установки JDK, Gradle или других зависимостей.

## Быстрый старт с Docker Compose

### 1. Запуск Ollama с автоматической загрузкой модели

```bash
# Запустить Ollama и загрузить модель эмбеддингов
docker-compose up -d
```

Это запустит:
- Ollama сервер на порту 11434
- Автоматически загрузит модель `nomic-embed-text`

Проверка:
```bash
# Проверить статус
docker-compose ps

# Проверить логи
docker-compose logs ollama

# Проверить доступность API
curl http://localhost:11434/api/tags
```

### 2. Сборка приложения

```bash
# Собрать Docker образ приложения
docker build -t ollama-embeddings .
```

### 3. Использование приложения

#### Индексация RTF файла

```bash
docker run --rm \
  --network host \
  -v $(pwd):/app/data \
  ollama-embeddings \
  index /app/data/example.rtf /app/data/my_index.json
```

Параметры:
- `--rm` - удалить контейнер после выполнения
- `--network host` - использовать сеть хоста для доступа к Ollama
- `-v $(pwd):/app/data` - монтировать текущую директорию

#### Поиск по индексу

```bash
docker run --rm \
  --network host \
  -v $(pwd):/app/data \
  ollama-embeddings \
  search /app/data/my_index.json "machine learning" 5
```

#### Статистика индекса

```bash
docker run --rm \
  -v $(pwd):/app/data \
  ollama-embeddings \
  stats /app/data/my_index.json
```

## Использование docker-compose для запуска приложения

Раскомментируйте секцию `app` в `docker-compose.yml`, затем:

```bash
# Индексация
docker-compose run --rm app index /app/data/example.rtf /app/data/index.json

# Поиск
docker-compose run --rm app search /app/data/index.json "your query" 10

# Статистика
docker-compose run --rm app stats /app/data/index.json
```

## Продвинутые сценарии

### Обработка нескольких файлов

```bash
# Создайте скрипт для обработки всех RTF файлов
for file in *.rtf; do
  docker run --rm \
    --network host \
    -v $(pwd):/app/data \
    ollama-embeddings \
    index "/app/data/$file" "/app/data/${file%.rtf}_index.json"
done
```

### Использование другой модели эмбеддингов

Сначала загрузите модель в Ollama:

```bash
docker exec ollama ollama pull mxbai-embed-large
```

Затем измените модель в коде (см. `OllamaClient.kt`).

### Настройка ресурсов

Если файлы большие, увеличьте память для контейнера:

```bash
docker run --rm \
  --network host \
  -m 4g \
  -v $(pwd):/app/data \
  ollama-embeddings \
  index /app/data/large_file.rtf
```

### Использование GPU (если доступно NVIDIA GPU)

Обновите `docker-compose.yml`:

```yaml
ollama:
  image: ollama/ollama:latest
  deploy:
    resources:
      reservations:
        devices:
          - driver: nvidia
            count: 1
            capabilities: [gpu]
```

## Управление данными

### Резервное копирование индексов

```bash
# Скопировать индексы из контейнера
docker run --rm \
  -v $(pwd):/app/data \
  -v $(pwd)/backup:/backup \
  alpine \
  cp /app/data/*.json /backup/
```

### Очистка

```bash
# Остановить и удалить контейнеры
docker-compose down

# Удалить volumes (включая модели Ollama)
docker-compose down -v

# Удалить образ приложения
docker rmi ollama-embeddings

# Полная очистка Docker
docker system prune -a
```

## Производственное развертывание

### Docker Swarm

```yaml
version: '3.8'

services:
  ollama:
    image: ollama/ollama:latest
    deploy:
      replicas: 1
      placement:
        constraints:
          - node.role == worker
      resources:
        limits:
          cpus: '2'
          memory: 4G
    ports:
      - "11434:11434"
    volumes:
      - ollama_data:/root/.ollama
```

Деплой:
```bash
docker stack deploy -c docker-compose.yml ollama-stack
```

### Kubernetes

Пример манифеста:

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: ollama
spec:
  replicas: 1
  selector:
    matchLabels:
      app: ollama
  template:
    metadata:
      labels:
        app: ollama
    spec:
      containers:
      - name: ollama
        image: ollama/ollama:latest
        ports:
        - containerPort: 11434
        volumeMounts:
        - name: ollama-data
          mountPath: /root/.ollama
      volumes:
      - name: ollama-data
        persistentVolumeClaim:
          claimName: ollama-pvc
---
apiVersion: v1
kind: Service
metadata:
  name: ollama
spec:
  selector:
    app: ollama
  ports:
  - port: 11434
    targetPort: 11434
```

## Мониторинг

### Проверка здоровья Ollama

```bash
# Добавьте healthcheck в docker-compose.yml (уже добавлено)
docker-compose ps  # Покажет health status
```

### Логирование

```bash
# Просмотр логов в реальном времени
docker-compose logs -f ollama

# Логи приложения
docker-compose logs app
```

### Метрики ресурсов

```bash
# Использование CPU/памяти
docker stats ollama

# Размер образов
docker images | grep ollama
```

## Troubleshooting

### Ollama не запускается

```bash
# Проверить логи
docker-compose logs ollama

# Перезапустить
docker-compose restart ollama

# Пересоздать контейнер
docker-compose up -d --force-recreate ollama
```

### Модель не загружается

```bash
# Вручную загрузить модель
docker exec ollama ollama pull nomic-embed-text

# Проверить доступные модели
docker exec ollama ollama list
```

### Проблемы с сетью

```bash
# Использовать bridge сеть вместо host
docker network create ollama-network

docker run --rm \
  --network ollama-network \
  -e OLLAMA_HOST=http://ollama:11434 \
  -v $(pwd):/app/data \
  ollama-embeddings \
  index /app/data/example.rtf
```

### Права доступа к файлам

```bash
# Если Docker не может читать/писать файлы
chmod 644 example.rtf
chmod 755 $(pwd)
```

## Оптимизация

### Многоэтапная сборка уже используется

Dockerfile использует multi-stage build для уменьшения размера образа:
- Build stage: ~1.5 GB
- Runtime stage: ~400 MB

### Кэширование слоев

При изменении только исходного кода:

```bash
# Docker переиспользует кэшированные слои
docker build -t ollama-embeddings .
```

### Использование .dockerignore

Создайте файл `.dockerignore`:

```
.git
.idea
build
.gradle
*.log
*.json
!example.rtf
```

## Альтернативы Docker

### Podman (drop-in replacement для Docker)

```bash
# Установка Podman
brew install podman  # macOS
apt install podman   # Ubuntu

# Использование (те же команды)
podman build -t ollama-embeddings .
podman run --rm --network host -v $(pwd):/app/data ollama-embeddings index /app/data/example.rtf
```

### Containerd + nerdctl

```bash
# Использование nerdctl
nerdctl build -t ollama-embeddings .
nerdctl run --rm --network host -v $(pwd):/app/data ollama-embeddings index /app/data/example.rtf
```
