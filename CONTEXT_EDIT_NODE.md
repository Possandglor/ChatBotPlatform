# ✏️ Узел Context-Edit - Редактирование параметров контекста

## Обзор

Узел `context-edit` предоставляет полный контроль над параметрами контекста сценария. Позволяет создавать, изменять, удалять и объединять параметры любой сложности.

## 🎯 Назначение

- **Установка переменных** для последующего использования в сценарии
- **Модификация данных** полученных от API или пользователя  
- **Удаление ненужных** параметров для очистки контекста
- **Объединение объектов** для сложных структур данных
- **Динамическое вычисление** значений с подстановкой переменных

## 📋 Синтаксис узла

```json
{
  "id": "edit_context",
  "type": "context-edit",
  "parameters": {
    "operations": [
      {
        "action": "set|delete|add|merge|clear",
        "path": "путь.к.параметру",
        "value": "значение (для set, add, merge)"
      }
    ]
  },
  "next_nodes": ["следующий_узел"]
}
```

## 🛠️ Поддерживаемые операции

### 1. **SET** - Установить значение
Создает или перезаписывает параметр по указанному пути.

```json
{
  "action": "set",
  "path": "user.name",
  "value": "Олександр Петренко"
}
```

**Возможности:**
- Создание вложенных объектов: `user.profile.settings.theme`
- Перезапись существующих значений
- Подстановка переменных в значение: `"Привет, {context.user.name}!"`

### 2. **DELETE/REMOVE** - Удалить значение
Удаляет параметр или элемент массива.

```json
{
  "action": "delete",
  "path": "user.age"
}
```

**Возможности:**
- Удаление простых параметров: `user.age`
- Удаление элементов массива: `user.permissions[1]`
- Удаление вложенных объектов: `user.profile.notifications`

### 3. **ADD** - Добавить в массив
Добавляет элемент в массив или создает новый массив.

```json
{
  "action": "add",
  "path": "user.permissions[]",
  "value": "admin"
}
```

**Возможности:**
- Добавление в существующий массив
- Создание нового массива если не существует
- Добавление сложных объектов в массив

### 4. **MERGE** - Объединить объекты
Объединяет новые данные с существующим объектом.

```json
{
  "action": "merge",
  "path": "user.profile",
  "value": {
    "avatar": "avatar.jpg",
    "bio": "Описание пользователя"
  }
}
```

**Возможности:**
- Объединение объектов (сохраняет существующие ключи)
- Объединение массивов (добавляет элементы)
- Замена если типы не совпадают

### 5. **CLEAR** - Очистить контейнер
Очищает содержимое объекта или массива.

```json
{
  "action": "clear",
  "path": "user.permissions"
}
```

**Возможности:**
- Очистка массивов (удаляет все элементы)
- Очистка объектов (удаляет все ключи)
- Установка null для примитивов

## 🔄 Динамические значения

Узел поддерживает подстановку переменных в значения:

```json
{
  "operations": [
    {
      "action": "set",
      "path": "computed.greeting",
      "value": "Привет, {context.user.name}! Ваша тема: {context.user.profile.theme}"
    },
    {
      "action": "set", 
      "path": "computed.full_info",
      "value": "{context.user.name} ({context.user.age} лет) использует {context.user.profile.theme} тему"
    }
  ]
}
```

## 📝 Примеры использования

### Пример 1: Инициализация пользовательских данных

```json
{
  "id": "init_user_data",
  "type": "context-edit",
  "parameters": {
    "operations": [
      {
        "action": "set",
        "path": "user.name",
        "value": "Новый пользователь"
      },
      {
        "action": "set",
        "path": "user.profile.theme",
        "value": "light"
      },
      {
        "action": "set",
        "path": "user.profile.language",
        "value": "uk"
      },
      {
        "action": "add",
        "path": "user.permissions[]",
        "value": "read"
      },
      {
        "action": "set",
        "path": "session.created_at",
        "value": "2025-01-04T14:37:46Z"
      }
    ]
  },
  "next_nodes": ["welcome_user"]
}
```

### Пример 2: Обработка API ответа

```json
{
  "id": "process_api_response",
  "type": "context-edit",
  "parameters": {
    "operations": [
      {
        "action": "set",
        "path": "account.balance",
        "value": "{context.api_response.balance}"
      },
      {
        "action": "set",
        "path": "account.currency",
        "value": "{context.api_response.currency}"
      },
      {
        "action": "set",
        "path": "account.last_transaction",
        "value": "{context.api_response.transactions[0].amount}"
      },
      {
        "action": "delete",
        "path": "api_response"
      }
    ]
  },
  "next_nodes": ["show_balance"]
}
```

### Пример 3: Управление состоянием диалога

```json
{
  "id": "update_dialog_state",
  "type": "context-edit", 
  "parameters": {
    "operations": [
      {
        "action": "set",
        "path": "dialog.current_step",
        "value": "collecting_info"
      },
      {
        "action": "add",
        "path": "dialog.completed_steps[]",
        "value": "authentication"
      },
      {
        "action": "merge",
        "path": "dialog.metadata",
        "value": {
          "start_time": "2025-01-04T14:37:46Z",
          "channel": "web"
        }
      }
    ]
  },
  "next_nodes": ["ask_user_info"]
}
```

### Пример 4: Очистка и сброс данных

```json
{
  "id": "reset_user_data",
  "type": "context-edit",
  "parameters": {
    "operations": [
      {
        "action": "clear",
        "path": "user.permissions"
      },
      {
        "action": "delete",
        "path": "user.profile.notifications"
      },
      {
        "action": "set",
        "path": "user.status",
        "value": "guest"
      }
    ]
  },
  "next_nodes": ["show_guest_menu"]
}
```

## 🔍 Сложные сценарии

### Работа с массивами объектов

```json
{
  "operations": [
    {
      "action": "add",
      "path": "orders[]",
      "value": {
        "id": "ORD-001",
        "amount": 1500,
        "status": "pending"
      }
    },
    {
      "action": "set",
      "path": "orders[0].status",
      "value": "confirmed"
    }
  ]
}
```

### Условная обработка через подстановку

```json
{
  "operations": [
    {
      "action": "set",
      "path": "user.greeting",
      "value": "{context.user.profile.language == 'uk' ? 'Привіт' : 'Hello'}, {context.user.name}!"
    }
  ]
}
```

## ⚠️ Ограничения и особенности

### Ограничения:
- **Создание массивов по индексу** не поддерживается: `users[5] = value`
- **Сложные JSONPath выражения** пока не поддерживаются: `$.users[?(@.age > 18)]`
- **Циклические ссылки** могут вызвать проблемы при сериализации

### Особенности:
- **Автоматическое создание пути**: если путь не существует, он создается
- **Безопасное удаление**: удаление несуществующего пути не вызывает ошибку
- **Подстановка переменных**: работает только для строковых значений
- **Системный узел**: выполняется мгновенно, не показывает сообщения пользователю

## 🧪 Тестирование

### Загрузка тестового сценария:

```bash
curl -X POST http://localhost:8093/api/v1/scenarios \
  -H "Content-Type: application/json" \
  -d @context-edit-test-scenario.json
```

### Создание сессии и тестирование:

```bash
# Создать сессию
curl -X POST http://localhost:8092/api/v1/chat/sessions

# Отправить сообщение для запуска тестового сценария
curl -X POST http://localhost:8092/api/v1/chat/messages \
  -H "Content-Type: application/json" \
  -d '{
    "session_id": "your-session-id",
    "content": "test context edit"
  }'
```

## 🔧 Технические детали

### Реализация:
- **Класс**: `AdvancedScenarioEngine.executeContextEdit()`
- **Поддержка JSONPath**: через `parseJsonPath()` и `getValueByJsonPath()`
- **Операции**: `setContextValue()`, `deleteContextValue()`, `addContextValue()`, `mergeContextValue()`, `clearContextPath()`

### Производительность:
- **Системный узел**: не блокирует выполнение сценария
- **Пакетные операции**: все операции выполняются за один вызов
- **Логирование**: детальные логи для отладки операций

## 🎯 Заключение

Узел `context-edit` предоставляет мощные возможности для управления состоянием сценария:

- ✅ **Полный контроль** над параметрами контекста
- ✅ **Гибкие операции** для любых структур данных  
- ✅ **Динамические значения** с подстановкой переменных
- ✅ **Безопасное выполнение** с обработкой ошибок
- ✅ **Простой синтаксис** для сложных операций

Теперь можно создавать более интеллектуальные сценарии с динамическим управлением данными!
