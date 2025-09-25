# 🚀 Полная схема работы Chatbot Platform

## 🏗️ Архитектура сервисов

```
┌─────────────────────────────────────────────────────────────────┐
│                    🌐 FRONTEND (React 19.1.1)                   │
│                         [Планируется]                          │
└─────────────────────┬───────────────────────────────────────────┘
                      │ HTTP/WebSocket
┌─────────────────────▼───────────────────────────────────────────┐
│                🚪 API GATEWAY (:8090)                          │
│  ┌─────────────┐ ┌──────────────┐ ┌─────────────────────────┐   │
│  │ 🔐 PowerStone│ │ 👤 Chameleon │ │ 📊 Health Checks       │   │
│  │ Auth        │ │ Sessions     │ │ 📖 Swagger UI          │   │
│  └─────────────┘ └──────────────┘ └─────────────────────────┘   │
└─────────────────────┬───────────────────────────────────────────┘
                      │ Proxy Requests
        ┌─────────────┼─────────────┬─────────────────────────────┐
        │             │             │                             │
┌───────▼──────┐ ┌────▼─────┐ ┌─────▼──────┐ ┌──────────────────┐
│💬 CHAT       │ │🎭 ORCH-  │ │📋 SCENARIO │ │🔧 FUTURE         │
│SERVICE       │ │ESTRATOR  │ │SERVICE     │ │SERVICES          │
│(:8091)       │ │(:8092)   │ │(:8093)     │ │                  │
│              │ │          │ │            │ │                  │
│✅ Работает   │ │⚠️ Доработка│ │✅ Работает │ │📅 Планируется   │
└──────────────┘ └──────────┘ └────────────┘ └──────────────────┘
        │             │             │
        │             │             │
┌───────▼──────┐ ┌────▼─────┐ ┌─────▼──────┐
│🔄 REDIS      │ │🧠 MEMORY │ │📊 PostgreSQL│
│Cache         │ │Context   │ │Database     │
│[Планируется] │ │Storage   │ │[Планируется]│
└──────────────┘ └──────────┘ └─────────────┘
```

## 🔄 Поток выполнения диалога

```mermaid
sequenceDiagram
    participant U as 👤 User
    participant F as 🌐 Frontend
    participant G as 🚪 Gateway
    participant C as 💬 Chat
    participant O as 🎭 Orchestrator
    participant S as 📋 Scenarios
    participant DB as 📊 Database
    
    Note over U,DB: 🚀 Инициализация сессии
    U->>F: Открыть чат
    F->>G: POST /api/v1/chat/sessions
    G->>G: 🔐 Проверить авторизацию
    G->>C: Создать сессию
    C->>C: 🆔 Генерировать session_id
    C-->>G: {"session_id": "uuid"}
    G-->>F: ✅ Сессия создана
    F-->>U: 💬 Чат готов
    
    Note over U,DB: 💬 Обработка сообщения
    U->>F: "Привет"
    F->>G: POST /messages {"session_id", "message": "Привет"}
    G->>C: Обработать сообщение
    
    C->>O: Выполнить сценарий
    Note over O: 🎭 Определить текущий сценарий
    O->>S: GET /scenarios/balance-check-001
    S->>DB: SELECT scenario WHERE id=...
    DB-->>S: JSON сценария
    S-->>O: Структура сценария
    
    Note over O: 🔍 Выполнить блок "greeting"
    O->>O: executeBlock("greeting", "announce")
    O-->>C: {"type": "announce", "message": "Привет! Помогу с балансом", "next_node": "ask_balance"}
    
    C->>C: 💾 Сохранить состояние сессии
    C-->>G: {"bot_response": "Привет! Помогу с балансом"}
    G-->>F: Ответ бота
    F-->>U: 🤖 "Привет! Помогу с балансом"
    
    Note over U,DB: 🔄 Продолжение диалога
    U->>F: "Да"
    F->>G: POST /messages {"message": "Да"}
    G->>C: Обработать "Да"
    C->>O: Продолжить сценарий с контекстом
    
    Note over O: 🔍 Выполнить блоки: ask_balance → parse_answer → check_answer
    O->>O: executeBlock("parse_answer", "parse")
    Note over O: context.wantsBalance = true
    O->>O: executeBlock("check_answer", "condition")
    Note over O: wantsBalance == true → ask_card_number
    O->>O: executeBlock("ask_card_number", "ask")
    
    O-->>C: {"type": "ask", "message": "Введите 4 цифры карты:", "next_node": "parse_card"}
    C-->>G: bot_response
    G-->>F: Ответ бота
    F-->>U: 🤖 "Введите 4 цифры карты:"
    
    Note over U,DB: 💳 Обработка номера карты
    U->>F: "1234"
    F->>G: POST /messages {"message": "1234"}
    G->>C: Обработать "1234"
    C->>O: Продолжить сценарий
    
    Note over O: 🔍 parse_card → validate_card → show_balance
    O->>O: executeBlock("parse_card", "parse")
    Note over O: context.cardNumber = "1234", validCard = true
    O->>O: executeBlock("validate_card", "condition")
    Note over O: validCard == true → show_balance
    O->>O: executeBlock("show_balance", "announce")
    Note over O: Подстановка: "Баланс ****{cardNumber}: 15,250.50 грн"
    
    O-->>C: {"type": "announce", "message": "Баланс ****1234: 15,250.50 грн", "next_node": "end"}
    C-->>G: bot_response
    G-->>F: Ответ бота
    F-->>U: 🤖 "Баланс ****1234: 15,250.50 грн"
```

## 🎯 Детальная схема Orchestrator

```mermaid
flowchart TD
    Start([📥 Получить запрос]) --> LoadScenario[📋 Загрузить сценарий]
    LoadScenario --> GetContext[🧠 Получить контекст сессии]
    GetContext --> FindCurrentNode[🔍 Найти текущий блок]
    
    FindCurrentNode --> CheckNodeType{❓ Тип блока?}
    
    CheckNodeType -->|announce| ExecuteAnnounce[📢 Выполнить announce]
    CheckNodeType -->|ask| ExecuteAsk[❓ Выполнить ask]
    CheckNodeType -->|parse| ExecuteParse[🔍 Выполнить parse]
    CheckNodeType -->|condition| ExecuteCondition[🔀 Выполнить condition]
    CheckNodeType -->|wait| ExecuteWait[⏱️ Выполнить wait]
    
    ExecuteAnnounce --> ProcessMessage[📝 Обработать сообщение]
    ExecuteAsk --> SetWaiting[⏳ Установить ожидание ввода]
    ExecuteParse --> UpdateContext[🧠 Обновить контекст]
    ExecuteCondition --> EvaluateCondition{🔀 Проверить условие}
    ExecuteWait --> Sleep[😴 Пауза]
    
    ProcessMessage --> GetNextNode[➡️ Получить следующий блок]
    SetWaiting --> SaveContext[💾 Сохранить контекст]
    UpdateContext --> GetNextNode
    Sleep --> GetNextNode
    
    EvaluateCondition -->|true| TruePath[✅ Путь TRUE]
    EvaluateCondition -->|false| FalsePath[❌ Путь FALSE]
    
    TruePath --> GetNextNode
    FalsePath --> GetNextNode
    
    GetNextNode --> CheckEnd{🏁 Конец сценария?}
    CheckEnd -->|Да| EndScenario[🏁 Завершить сценарий]
    CheckEnd -->|Нет| SaveContext
    
    SaveContext --> ReturnResponse[📤 Вернуть ответ]
    EndScenario --> ReturnResponse
    
    ReturnResponse --> End([📤 Ответ клиенту])
```

## 🗂️ Структура данных

### 📊 Сессия чата
```json
{
  "session_id": "uuid",
  "user_id": "user123",
  "scenario_id": "balance-check-001",
  "current_node": "ask_balance",
  "context": {
    "wantsBalance": false,
    "cardNumber": null,
    "validCard": false,
    "attempts": 0
  },
  "created_at": "2025-09-24T13:00:00Z",
  "updated_at": "2025-09-24T13:05:00Z"
}
```

### 🎭 Выполнение блока
```json
{
  "block_id": "parse_answer",
  "block_type": "parse",
  "input": "Да",
  "context_before": {"wantsBalance": false},
  "context_after": {"wantsBalance": true},
  "next_node": "check_answer",
  "execution_time": 15,
  "timestamp": "2025-09-24T13:05:30Z"
}
```

## 🔧 API Endpoints

### 🚪 API Gateway (:8090)
```
GET  /api/v1/status              - Статус шлюза
POST /api/v1/chat/sessions       - Создать сессию → Chat Service
POST /api/v1/chat/messages       - Отправить сообщение → Chat Service
GET  /api/v1/scenarios           - Получить сценарии → Scenario Service
POST /api/v1/execute/scenario    - Выполнить сценарий → Orchestrator
```

### 💬 Chat Service (:8091)
```
GET  /api/v1/chat/status         - Статус сервиса
POST /api/v1/chat/sessions       - Создать сессию
GET  /api/v1/chat/sessions/{id}  - Получить сессию
POST /api/v1/chat/messages       - Обработать сообщение
DELETE /api/v1/chat/sessions/{id} - Удалить сессию
```

### 🎭 Orchestrator (:8092)
```
GET  /api/v1/execute/status      - Статус оркестратора
POST /api/v1/execute/scenario    - Выполнить сценарий
POST /api/v1/execute/test        - Тестовое выполнение
```

### 📋 Scenario Service (:8093)
```
GET  /api/v1/scenarios/status    - Статус сервиса
GET  /api/v1/scenarios           - Получить все сценарии
GET  /api/v1/scenarios/{id}      - Получить сценарий
POST /api/v1/scenarios           - Создать сценарий
PUT  /api/v1/scenarios/{id}      - Обновить сценарий
DELETE /api/v1/scenarios/{id}    - Удалить сценарий
```

## 📈 Мониторинг и логирование

### 🔍 Health Checks
```
http://localhost:8090/q/health   - API Gateway
http://localhost:8091/q/health   - Chat Service
http://localhost:8092/q/health   - Orchestrator
http://localhost:8093/q/health   - Scenario Service
```

### 📊 Метрики
- Количество активных сессий
- Время выполнения сценариев
- Успешность завершения диалогов
- Ошибки выполнения блоков

### 📝 Логирование
```
2025-09-24 13:05:30 INFO [Chat] Session created: uuid
2025-09-24 13:05:31 INFO [Orchestrator] Executing scenario: balance-check-001
2025-09-24 13:05:32 DEBUG [Orchestrator] Block executed: greeting → ask_balance
2025-09-24 13:05:33 INFO [Chat] User input: "Да" → Bot response: "Введите 4 цифры"
```

## 🚀 Статус реализации

| Компонент | Статус | Функциональность |
|-----------|--------|------------------|
| 🚪 API Gateway | ✅ Готов | Авторизация, проксирование, health checks |
| 💬 Chat Service | ✅ Готов | Сессии, сообщения, простые ответы |
| 📋 Scenario Service | ✅ Готов | CRUD сценариев, поиск, валидация |
| 🎭 Orchestrator | ⚠️ Доработка | Выполнение сценариев (ошибки типизации) |
| 🌐 Frontend | 📅 Планируется | React 19.1.1 интерфейс |
| 📊 Database | 📅 Планируется | PostgreSQL для сценариев |
| 🔄 Cache | 📅 Планируется | Redis для сессий |
