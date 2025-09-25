# РУКОВОДСТВО ПО РАЗВЕРТЫВАНИЮ
## Платформа чат-ботов - Практическое руководство

**Версия**: 2.0  
**Дата**: 23.09.2025  
**Для**: DevOps, системных администраторов, разработчиков  

---

## 1. БЫСТРЫЙ СТАРТ

### 1.1 Предварительные требования

**Системные требования**:
- **OS**: Linux (Ubuntu 20.04+), macOS, Windows 10+
- **CPU**: 4+ cores (рекомендуется 8+ cores)
- **RAM**: 8GB+ (рекомендуется 16GB+)
- **Storage**: 50GB+ SSD
- **Network**: 1Gbps+

**Программное обеспечение**:
```bash
# Java 21
sudo apt update
sudo apt install openjdk-21-jdk

# Docker и Docker Compose
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh
sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose

# Node.js 18+
curl -fsSL https://deb.nodesource.com/setup_18.x | sudo -E bash -
sudo apt-get install -y nodejs

# Maven 3.9+
sudo apt install maven

# kubectl (для Kubernetes)
curl -LO "https://dl.k8s.io/release/$(curl -L -s https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl"
sudo install -o root -g root -m 0755 kubectl /usr/local/bin/kubectl
```

### 1.2 Клонирование и настройка

```bash
# Клонирование репозитория
git clone <repository-url>
cd Chat_bot_platform

# Создание .env файла
cp .env.example .env

# Редактирование конфигурации
nano .env
```

**Пример .env файла**:
```env
# Database
POSTGRES_DB=chatbot_platform
POSTGRES_USER=chatbot_user
POSTGRES_PASSWORD=your_secure_password_here

# Redis
REDIS_PASSWORD=your_redis_password

# External APIs
OPENAI_API_KEY=sk-your-openai-key-here
GOOGLE_CLOUD_API_KEY=your-google-cloud-key
AZURE_API_KEY=your-azure-key

# OAuth 2.0
OAUTH2_ISSUER_URI=https://your-oauth-provider.com
OAUTH2_CLIENT_ID=your-client-id
OAUTH2_CLIENT_SECRET=your-client-secret

# JWT
JWT_SECRET=your-jwt-secret-minimum-32-characters-long

# Environment
ENVIRONMENT=development
LOG_LEVEL=INFO
```

### 1.3 Запуск через Docker Compose

```bash
# Сборка всех сервисов
./deploy.sh build

# Запуск платформы
./deploy.sh deploy --docker

# Проверка статуса
./deploy.sh status --docker

# Просмотр логов
docker-compose -f docker-compose-ports.yml logs -f

# Остановка
./deploy.sh stop --docker
```

**Доступ к сервисам**:
- **Frontend**: http://localhost:3000
- **API Gateway**: http://localhost:8090
- **Swagger UI**: http://localhost:8090/swagger-ui.html
- **PostgreSQL**: localhost:5433
- **Redis**: localhost:6380

---

## 2. ЛОКАЛЬНАЯ РАЗРАБОТКА

### 2.1 Настройка среды разработки

```bash
# Установка зависимостей backend
cd backend
for service in */; do
    cd "$service"
    mvn clean install -DskipTests
    cd ..
done

# Установка зависимостей frontend
cd ../frontend
npm install

# Возврат в корень проекта
cd ..
```

### 2.2 Запуск в режиме разработки

**Терминал 1 - База данных**:
```bash
# Запуск только БД
docker-compose -f docker-compose-db-only.yml up -d
```

**Терминал 2 - Backend сервисы**:
```bash
# API Gateway
cd backend/simple-api-gateway
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# В отдельных терминалах запустить остальные сервисы:
# Chat Service
cd backend/chat-service
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Orchestrator
cd backend/orchestrator
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# И так далее для каждого сервиса...
```

**Терминал 3 - Frontend**:
```bash
cd frontend
npm start
```

### 2.3 Отладка и тестирование

```bash
# Запуск тестов backend
mvn test

# Запуск тестов frontend
cd frontend
npm test

# Линтинг кода
npm run lint

# Проверка типов TypeScript
npm run type-check

# E2E тесты
npm run test:e2e
```

---

## 3. ПРОДАКШН РАЗВЕРТЫВАНИЕ

### 3.1 Подготовка к продакшн

**Создание production конфигурации**:
```bash
# Копирование шаблона
cp docker-compose.yml docker-compose.prod.yml

# Редактирование для продакшн
nano docker-compose.prod.yml
```

**Изменения для продакшн**:
```yaml
version: '3.8'

services:
  postgres:
    image: postgres:15
    environment:
      POSTGRES_DB: ${POSTGRES_DB}
      POSTGRES_USER: ${POSTGRES_USER}
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD}
    volumes:
      - postgres_data:/var/lib/postgresql/data
      - ./database/schemas:/docker-entrypoint-initdb.d
    # Убираем порты для безопасности
    # ports:
    #   - "5432:5432"
    networks:
      - chatbot-network
    deploy:
      resources:
        limits:
          memory: 2G
          cpus: '1.0'
        reservations:
          memory: 1G
          cpus: '0.5'

  api-gateway:
    build: 
      context: ./backend/simple-api-gateway
      dockerfile: Dockerfile.prod
    environment:
      - SPRING_PROFILES_ACTIVE=production
      - POSTGRES_URL=jdbc:postgresql://postgres:5432/${POSTGRES_DB}
      - REDIS_URL=redis://redis:6379
      - JWT_SECRET=${JWT_SECRET}
    # Только внешний порт
    ports:
      - "80:8080"
    depends_on:
      - postgres
      - redis
    networks:
      - chatbot-network
    deploy:
      replicas: 2
      resources:
        limits:
          memory: 1G
          cpus: '0.5'
```

### 3.2 SSL/TLS настройка

**Nginx конфигурация**:
```nginx
# /etc/nginx/sites-available/chatbot-platform
server {
    listen 80;
    server_name your-domain.com;
    return 301 https://$server_name$request_uri;
}

server {
    listen 443 ssl http2;
    server_name your-domain.com;

    ssl_certificate /etc/ssl/certs/your-domain.crt;
    ssl_certificate_key /etc/ssl/private/your-domain.key;
    
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers ECDHE-RSA-AES256-GCM-SHA512:DHE-RSA-AES256-GCM-SHA512;
    ssl_prefer_server_ciphers off;

    # API Gateway
    location /api/ {
        proxy_pass http://localhost:8090;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    # WebSocket
    location /ws/ {
        proxy_pass http://localhost:8091;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_set_header Host $host;
    }

    # Frontend
    location / {
        proxy_pass http://localhost:3000;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

### 3.3 Мониторинг и логирование

**Prometheus конфигурация**:
```yaml
# prometheus.yml
global:
  scrape_interval: 15s

scrape_configs:
  - job_name: 'chatbot-platform'
    static_configs:
      - targets: 
        - 'localhost:8090'  # API Gateway
        - 'localhost:8091'  # Chat Service
        - 'localhost:8092'  # Orchestrator
        - 'localhost:8093'  # Scenario Service
        - 'localhost:8094'  # Module Service
        - 'localhost:8095'  # NLU Service
        - 'localhost:8096'  # STT Service
        - 'localhost:8097'  # LLM Service
    metrics_path: '/actuator/prometheus'
    scrape_interval: 5s
```

**Grafana дашборд**:
```json
{
  "dashboard": {
    "title": "Chatbot Platform Monitoring",
    "panels": [
      {
        "title": "Active Dialogs",
        "type": "stat",
        "targets": [
          {
            "expr": "chat_sessions_active",
            "legendFormat": "Active Sessions"
          }
        ]
      },
      {
        "title": "Messages per Second",
        "type": "graph",
        "targets": [
          {
            "expr": "rate(chat_messages_total[1m])",
            "legendFormat": "Messages/sec"
          }
        ]
      },
      {
        "title": "Response Time",
        "type": "graph",
        "targets": [
          {
            "expr": "histogram_quantile(0.95, chat_response_time_seconds_bucket)",
            "legendFormat": "95th percentile"
          }
        ]
      }
    ]
  }
}
```

---

## 4. KUBERNETES РАЗВЕРТЫВАНИЕ

### 4.1 Подготовка кластера

```bash
# Создание namespace
kubectl create namespace chatbot-platform

# Создание секретов
kubectl create secret generic chatbot-secrets \
  --from-literal=POSTGRES_PASSWORD=your_password \
  --from-literal=REDIS_PASSWORD=your_redis_password \
  --from-literal=JWT_SECRET=your_jwt_secret \
  --from-literal=OPENAI_API_KEY=your_openai_key \
  -n chatbot-platform

# Создание ConfigMap
kubectl create configmap chatbot-config \
  --from-literal=POSTGRES_DB=chatbot_platform \
  --from-literal=POSTGRES_USER=chatbot_user \
  --from-literal=ENVIRONMENT=production \
  -n chatbot-platform
```

### 4.2 Развертывание сервисов

```bash
# Развертывание всех компонентов
kubectl apply -f infrastructure/ -n chatbot-platform

# Проверка статуса
kubectl get pods -n chatbot-platform
kubectl get services -n chatbot-platform
kubectl get ingress -n chatbot-platform

# Просмотр логов
kubectl logs -f deployment/api-gateway -n chatbot-platform
```

### 4.3 Автомасштабирование

**HPA конфигурация**:
```yaml
# hpa.yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: api-gateway-hpa
  namespace: chatbot-platform
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: api-gateway
  minReplicas: 2
  maxReplicas: 10
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
  - type: Resource
    resource:
      name: memory
      target:
        type: Utilization
        averageUtilization: 80
```

### 4.4 Ingress настройка

```yaml
# ingress.yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: chatbot-platform-ingress
  namespace: chatbot-platform
  annotations:
    kubernetes.io/ingress.class: nginx
    cert-manager.io/cluster-issuer: letsencrypt-prod
    nginx.ingress.kubernetes.io/ssl-redirect: "true"
    nginx.ingress.kubernetes.io/proxy-body-size: "10m"
spec:
  tls:
  - hosts:
    - your-domain.com
    secretName: chatbot-platform-tls
  rules:
  - host: your-domain.com
    http:
      paths:
      - path: /api
        pathType: Prefix
        backend:
          service:
            name: api-gateway-service
            port:
              number: 8080
      - path: /ws
        pathType: Prefix
        backend:
          service:
            name: chat-service
            port:
              number: 8080
      - path: /
        pathType: Prefix
        backend:
          service:
            name: frontend-service
            port:
              number: 3000
```

---

## 5. МОНИТОРИНГ И АЛЕРТЫ

### 5.1 Настройка мониторинга

**Prometheus Operator**:
```bash
# Установка Prometheus Operator
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
helm repo update

helm install prometheus prometheus-community/kube-prometheus-stack \
  --namespace monitoring \
  --create-namespace \
  --set grafana.adminPassword=admin123
```

**ServiceMonitor для сбора метрик**:
```yaml
apiVersion: monitoring.coreos.com/v1
kind: ServiceMonitor
metadata:
  name: chatbot-platform-metrics
  namespace: chatbot-platform
spec:
  selector:
    matchLabels:
      app: chatbot-platform
  endpoints:
  - port: http
    path: /actuator/prometheus
    interval: 30s
```

### 5.2 Алерты

**PrometheusRule для алертов**:
```yaml
apiVersion: monitoring.coreos.com/v1
kind: PrometheusRule
metadata:
  name: chatbot-platform-alerts
  namespace: chatbot-platform
spec:
  groups:
  - name: chatbot-platform
    rules:
    - alert: HighErrorRate
      expr: rate(http_requests_total{status=~"5.."}[5m]) > 0.1
      for: 5m
      labels:
        severity: critical
      annotations:
        summary: "High error rate detected"
        description: "Error rate is {{ $value }} errors per second"
    
    - alert: HighResponseTime
      expr: histogram_quantile(0.95, http_request_duration_seconds_bucket) > 2
      for: 5m
      labels:
        severity: warning
      annotations:
        summary: "High response time detected"
        description: "95th percentile response time is {{ $value }} seconds"
    
    - alert: ServiceDown
      expr: up == 0
      for: 1m
      labels:
        severity: critical
      annotations:
        summary: "Service is down"
        description: "{{ $labels.instance }} has been down for more than 1 minute"
```

### 5.3 Логирование

**ELK Stack конфигурация**:
```yaml
# filebeat.yml
filebeat.inputs:
- type: container
  paths:
    - '/var/lib/docker/containers/*/*.log'
  processors:
  - add_kubernetes_metadata:
      host: ${NODE_NAME}
      matchers:
      - logs_path:
          logs_path: "/var/lib/docker/containers/"

output.elasticsearch:
  hosts: ["elasticsearch:9200"]
  index: "chatbot-platform-%{+yyyy.MM.dd}"

setup.template.name: "chatbot-platform"
setup.template.pattern: "chatbot-platform-*"
```

---

## 6. РЕЗЕРВНОЕ КОПИРОВАНИЕ

### 6.1 Backup стратегия

**PostgreSQL backup**:
```bash
#!/bin/bash
# backup-postgres.sh

BACKUP_DIR="/backups/postgres"
DATE=$(date +%Y%m%d_%H%M%S)
BACKUP_FILE="chatbot_platform_${DATE}.sql"

# Создание backup
docker exec postgres pg_dump -U chatbot_user chatbot_platform > "${BACKUP_DIR}/${BACKUP_FILE}"

# Сжатие
gzip "${BACKUP_DIR}/${BACKUP_FILE}"

# Удаление старых backup (старше 30 дней)
find ${BACKUP_DIR} -name "*.sql.gz" -mtime +30 -delete

# Загрузка в облако (опционально)
aws s3 cp "${BACKUP_DIR}/${BACKUP_FILE}.gz" s3://your-backup-bucket/postgres/
```

**Redis backup**:
```bash
#!/bin/bash
# backup-redis.sh

BACKUP_DIR="/backups/redis"
DATE=$(date +%Y%m%d_%H%M%S)

# Создание snapshot
docker exec redis redis-cli BGSAVE

# Копирование dump файла
docker cp redis:/data/dump.rdb "${BACKUP_DIR}/dump_${DATE}.rdb"

# Сжатие и загрузка в облако
gzip "${BACKUP_DIR}/dump_${DATE}.rdb"
aws s3 cp "${BACKUP_DIR}/dump_${DATE}.rdb.gz" s3://your-backup-bucket/redis/
```

### 6.2 Восстановление

**Восстановление PostgreSQL**:
```bash
#!/bin/bash
# restore-postgres.sh

BACKUP_FILE=$1

if [ -z "$BACKUP_FILE" ]; then
    echo "Usage: $0 <backup_file.sql.gz>"
    exit 1
fi

# Распаковка
gunzip -c "$BACKUP_FILE" > /tmp/restore.sql

# Остановка приложений
docker-compose stop api-gateway chat-service orchestrator scenario-service

# Восстановление
docker exec -i postgres psql -U chatbot_user -d chatbot_platform < /tmp/restore.sql

# Запуск приложений
docker-compose start api-gateway chat-service orchestrator scenario-service

# Очистка
rm /tmp/restore.sql
```

---

## 7. TROUBLESHOOTING

### 7.1 Частые проблемы

**Проблема**: Сервисы не могут подключиться к БД
```bash
# Проверка статуса PostgreSQL
docker-compose exec postgres pg_isready -U chatbot_user

# Проверка логов
docker-compose logs postgres

# Проверка сетевого подключения
docker-compose exec api-gateway ping postgres
```

**Проблема**: Высокое использование памяти
```bash
# Проверка использования ресурсов
docker stats

# Настройка JVM heap
export JAVA_OPTS="-Xmx1g -Xms512m"

# Перезапуск с новыми настройками
docker-compose restart api-gateway
```

**Проблема**: WebSocket соединения не работают
```bash
# Проверка портов
netstat -tulpn | grep 8091

# Проверка Nginx конфигурации
nginx -t

# Тест WebSocket соединения
wscat -c ws://localhost:8091/ws/chat/test-session
```

### 7.2 Диагностические команды

```bash
# Проверка здоровья всех сервисов
./deploy.sh health --docker

# Просмотр метрик
curl http://localhost:8090/actuator/metrics

# Проверка конфигурации
curl http://localhost:8090/actuator/configprops

# Анализ производительности
curl http://localhost:8090/actuator/threaddump

# Проверка подключений к БД
curl http://localhost:8090/actuator/health/db
```

### 7.3 Логи и отладка

```bash
# Просмотр логов в реальном времени
docker-compose logs -f --tail=100

# Фильтрация логов по сервису
docker-compose logs api-gateway | grep ERROR

# Экспорт логов
docker-compose logs --no-color > platform-logs.txt

# Анализ производительности
docker-compose exec api-gateway jstack 1
```

---

## 8. ОБНОВЛЕНИЕ И МИГРАЦИЯ

### 8.1 Обновление версий

```bash
# Создание backup перед обновлением
./backup-all.sh

# Остановка сервисов
docker-compose down

# Обновление кода
git pull origin main

# Пересборка образов
./deploy.sh build --no-cache

# Запуск с новой версией
./deploy.sh deploy --docker

# Проверка работоспособности
./deploy.sh health --docker
```

### 8.2 Миграция базы данных

```bash
# Flyway миграции
cd backend/api-gateway
mvn flyway:migrate -Dflyway.url=jdbc:postgresql://localhost:5433/chatbot_platform

# Ручная миграция
docker-compose exec postgres psql -U chatbot_user -d chatbot_platform -f /migrations/V2__add_new_features.sql
```

### 8.3 Blue-Green развертывание

```bash
# Подготовка Green окружения
docker-compose -f docker-compose.green.yml up -d

# Проверка Green окружения
curl http://localhost:8190/actuator/health

# Переключение трафика (Nginx)
sudo cp nginx.green.conf /etc/nginx/sites-available/chatbot-platform
sudo nginx -s reload

# Остановка Blue окружения
docker-compose -f docker-compose.blue.yml down
```

---

**Документ подготовлен**: 23.09.2025  
**Версия**: 2.0  
**Для поддержки**: support@chatbot-platform.com  
**Связанные документы**: TECHNICAL_SPECIFICATION.md, TECHNICAL_SPECIFICATIONS_DETAILED.md
