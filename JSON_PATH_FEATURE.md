# 🚀 Глубокое извлечение параметров из JSON

## Обзор

Реализована поддержка глубокого извлечения параметров из JSON структур любой сложности в платформе чат-ботов. Теперь можно извлекать данные из вложенных объектов и массивов на любом уровне вложенности.

## ✨ Новые возможности

### До обновления (ограничения):
- ❌ Только первые два уровня: `{context.api_response.service}`
- ❌ Нет поддержки массивов: `{context.users[0]}` не работало
- ❌ Нет глубокой вложенности: `{context.data.stats.memory}` не работало

### После обновления (новые возможности):
- ✅ **Неограниченная глубина**: `{context.api_response.data.analytics.reports[0].metrics.conversion_rate}`
- ✅ **Поддержка массивов**: `{context.users[0].name}`, `{context.endpoints[1]}`
- ✅ **Комбинации объектов и массивов**: `{context.users[0].permissions[2]}`
- ✅ **Без префикса context**: `{api_response.service}` работает так же как `{context.api_response.service}`

## 📝 Синтаксис JSONPath

### Базовый синтаксис:
```
{context.path.to.value}  # С префиксом context
{path.to.value}          # Без префикса context (новое!)
```

### Поддерживаемые конструкции:

#### 1. Простые поля
```
{context.api_response.service}        → "ChatBot Platform"
{api_response.service}                → "ChatBot Platform"
```

#### 2. Вложенные объекты
```
{context.api_response.server.name}                    → "orchestrator-service"
{context.api_response.server.stats.memory_usage}     → "256MB"
```

#### 3. Массивы
```
{context.api_response.endpoints[0]}   → "/api/v1/chat/message"
{context.api_response.endpoints[1]}   → "/api/v1/scenarios/execute"
```

#### 4. Объекты в массивах
```
{context.api_response.users[0].name}                          → "Олександр Петренко"
{context.api_response.users[0].profile.settings.theme}       → "dark"
{context.api_response.users[1].profile.settings.language}    → "en"
```

#### 5. Массивы в объектах в массивах
```
{context.api_response.users[0].permissions[0]}    → "read"
{context.api_response.users[0].permissions[1]}    → "write"
{context.api_response.users[1].permissions[0]}    → "read"
```

#### 6. Очень глубокие пути
```
{context.api_response.data.analytics.reports[0].metrics.conversion_rate}     → "23.5%"
{context.api_response.data.analytics.reports[0].metrics.avg_response_time}   → "1.2s"
{context.api_response.data.config.features.nlu_enabled}                      → true
```

## 🛠️ Примеры использования в сценариях

### 1. API Request узел с подстановкой результатов

```json
{
  "id": "show_api_results",
  "type": "announce",
  "parameters": {
    "message": "Результаты API запроса:\n\n🔹 Сервис: {context.api_response.service}\n🔹 Сервер: {context.api_response.server.name}\n🔹 Использование памяти: {context.api_response.server.stats.memory_usage}\n🔹 Первый endpoint: {context.api_response.endpoints[0]}\n🔹 Пользователь: {context.api_response.users[0].name}\n🔹 Тема: {context.api_response.users[0].profile.settings.theme}\n🔹 Права: {context.api_response.users[0].permissions[0]}\n🔹 Конверсия: {context.api_response.data.analytics.reports[0].metrics.conversion_rate}"
  }
}
```

### 2. Condition узел с проверкой глубоких значений

```json
{
  "id": "check_user_theme",
  "type": "condition",
  "parameters": {
    "conditions": [
      "context.api_response.users[0].profile.settings.theme == \"dark\"",
      "context.api_response.users[0].profile.settings.theme == \"light\""
    ]
  },
  "next_nodes": ["dark_theme_response", "light_theme_response", "unknown_theme"]
}
```

### 3. API Request с динамическими параметрами

```json
{
  "id": "dynamic_api_call",
  "type": "api-request",
  "parameters": {
    "url": "https://api.example.com/users/{context.api_response.users[0].id}/profile",
    "method": "GET",
    "headers": {
      "Authorization": "Bearer {context.api_response.auth.token}",
      "User-Language": "{context.api_response.users[0].profile.settings.language}"
    }
  }
}
```

## 🧪 Тестирование

### Запуск тестового сценария:

1. **Запустите mock сервер:**
   ```bash
   node json-path-mock-server.js
   ```

2. **Загрузите тестовый сценарий:**
   ```bash
   curl -X POST http://localhost:8093/api/v1/scenarios \
     -H "Content-Type: application/json" \
     -d @json-path-test-scenario.json
   ```

3. **Запустите тестовый скрипт:**
   ```bash
   ./test-json-path.sh
   ```

### Пример тестовых данных:

Mock сервер возвращает сложную JSON структуру:

```json
{
  "service": "ChatBot Platform",
  "server": {
    "name": "orchestrator-service",
    "stats": {
      "memory_usage": "256MB",
      "cpu_usage": "15%"
    }
  },
  "endpoints": [
    "/api/v1/chat/message",
    "/api/v1/scenarios/execute"
  ],
  "users": [
    {
      "name": "Олександр Петренко",
      "profile": {
        "settings": {
          "theme": "dark",
          "language": "uk"
        }
      },
      "permissions": ["read", "write", "admin"]
    }
  ],
  "data": {
    "analytics": {
      "reports": [
        {
          "metrics": {
            "conversion_rate": "23.5%"
          }
        }
      ]
    }
  }
}
```

## 🔧 Технические детали

### Реализация

Функциональность реализована в двух классах:
- `AdvancedScenarioEngine.java` - основной движок сценариев
- `ScenarioEngine.java` - базовый движок (для совместимости)

### Ключевые методы:

#### `getValueByJsonPath(Map<String, Object> context, String path)`
- Извлекает значение по JSONPath из контекста
- Поддерживает массивы `[0]`, `[1]`, etc.
- Поддерживает вложенные объекты любой глубины
- Автоматически парсит JSON строки

#### `parseJsonPath(String path)`
- Разбирает JSONPath на части
- Правильно обрабатывает массивы в пути
- Пример: `"users[0].profile.settings[1]"` → `["users", "[0]", "profile", "settings", "[1]"]`

### Обработка ошибок:
- Безопасное извлечение (не падает при отсутствии ключей)
- Логирование предупреждений при ошибках парсинга
- Возврат `null` при невалидных путях

## 📋 Совместимость

### Обратная совместимость:
- ✅ Все существующие сценарии продолжают работать
- ✅ Старый синтаксис `{context.key}` поддерживается
- ✅ Двухуровневые пути `{context.api_response.field}` работают как раньше

### Новые возможности:
- ✅ Неограниченная глубина вложенности
- ✅ Поддержка массивов на любом уровне
- ✅ Синтаксис без префикса `context`
- ✅ Автоматический парсинг JSON строк

## 🚀 Примеры реальных сценариев

### Банковский API:
```json
{
  "message": "Ваш баланс: {context.bank_api_response.accounts[0].balance} {context.bank_api_response.accounts[0].currency}\nПоследняя транзакция: {context.bank_api_response.transactions[0].amount} от {context.bank_api_response.transactions[0].date}"
}
```

### CRM система:
```json
{
  "message": "Клиент: {context.crm_response.customer.name}\nСтатус: {context.crm_response.customer.status}\nПоследний заказ: {context.crm_response.customer.orders[0].id} на сумму {context.crm_response.customer.orders[0].total}"
}
```

### Аналитика:
```json
{
  "message": "Статистика за сегодня:\n📊 Сессий: {context.analytics.daily.sessions}\n💬 Сообщений: {context.analytics.daily.messages}\n📈 Конверсия: {context.analytics.daily.conversion_rate}\n⚡ Среднее время ответа: {context.analytics.daily.avg_response_time}"
}
```

## 🎯 Заключение

Новая функциональность глубокого извлечения JSON параметров значительно расширяет возможности платформы чат-ботов:

- **Гибкость**: Работа с API любой сложности
- **Простота**: Интуитивный JSONPath синтаксис  
- **Надежность**: Безопасное извлечение с обработкой ошибок
- **Совместимость**: Полная обратная совместимость

Теперь можно создавать более сложные и интеллектуальные сценарии, которые эффективно работают с данными из внешних API и сервисов.
