# Архитектура Chatbot Platform

## 🏗️ Общая схема сервисов

```mermaid
graph TB
    User[👤 Пользователь] --> Gateway[🚪 API Gateway :8090]
    Gateway --> Auth{🔐 PowerStone Auth}
    Auth -->|✅ OK| Chat[💬 Chat Service :8091]
    Auth -->|❌ Fail| Error[❌ 401 Unauthorized]
    
    Chat --> Orchestrator[🎭 Orchestrator :8092]
    Orchestrator --> Scenarios[📋 Scenario Service :8093]
    
    Scenarios --> DB[(📊 PostgreSQL)]
    Chat --> Cache[(🔄 Redis)]
    
    Gateway -.-> Swagger[📖 Swagger UI]
    Gateway -.-> Health[❤️ Health Checks]
```

## 🎭 Структура сценария

```mermaid
graph TD
    Start([🚀 START]) --> Greeting[📢 greeting<br/>announce<br/>"Привет! Помогу с балансом"]
    
    Greeting --> AskBalance[❓ ask_balance<br/>ask<br/>"Хотите проверить баланс? (да/нет)"]
    
    AskBalance --> ParseAnswer[🔍 parse_answer<br/>parse<br/>context.wantsBalance = да/нет]
    
    ParseAnswer --> Condition{🔀 check_answer<br/>condition<br/>wantsBalance == true?}
    
    Condition -->|✅ ДА| AskCard[❓ ask_card<br/>ask<br/>"Введите 4 цифры карты:"]
    Condition -->|❌ НЕТ| Goodbye[👋 goodbye<br/>announce<br/>"Обращайтесь еще!"]
    
    AskCard --> ParseCard[🔍 parse_card<br/>parse<br/>context.cardNumber = input<br/>validCard = /\d{4}/.test()]
    
    ParseCard --> ValidateCard{🔀 validate_card<br/>condition<br/>validCard == true?}
    
    ValidateCard -->|✅ OK| ShowBalance[💰 show_balance<br/>announce<br/>"Баланс ****{cardNumber}: 15,250.50 грн"]
    ValidateCard -->|❌ Error| CardError[⚠️ card_error<br/>announce<br/>"Неверный формат"]
    
    CardError --> AskCard
    ShowBalance --> End([🏁 END])
    Goodbye --> End
    
    style Start fill:#e1f5fe
    style End fill:#f3e5f5
    style Condition fill:#fff3e0
    style ValidateCard fill:#fff3e0
```

## 🔄 Поток выполнения сценария

```mermaid
sequenceDiagram
    participant U as 👤 User
    participant G as 🚪 Gateway
    participant C as 💬 Chat
    participant O as 🎭 Orchestrator
    participant S as 📋 Scenarios
    
    U->>G: POST /api/v1/chat/sessions
    G->>C: Создать сессию
    C-->>G: session_id
    G-->>U: ✅ Session created
    
    U->>G: POST /messages {"message": "Привет"}
    G->>C: Обработать сообщение
    C->>O: Выполнить сценарий
    O->>S: Получить сценарий "balance-check"
    S-->>O: JSON сценария
    
    Note over O: Выполнение блока "greeting"
    O-->>C: {"type": "announce", "message": "Привет! Помогу с балансом"}
    C-->>G: bot_response
    G-->>U: 🤖 "Привет! Помогу с балансом"
    
    U->>G: POST /messages {"message": "Да"}
    G->>C: Обработать "Да"
    C->>O: Продолжить сценарий
    
    Note over O: parse_answer: wantsBalance = true<br/>condition: true → ask_card
    O-->>C: {"type": "ask", "message": "Введите 4 цифры:"}
    C-->>G: bot_response
    G-->>U: 🤖 "Введите 4 цифры карты:"
    
    U->>G: POST /messages {"message": "1234"}
    G->>C: Обработать "1234"
    C->>O: Продолжить сценарий
    
    Note over O: parse_card: cardNumber = "1234", validCard = true<br/>condition: true → show_balance
    O-->>C: {"type": "announce", "message": "Баланс ****1234: 15,250.50 грн"}
    C-->>G: bot_response
    G-->>U: 🤖 "Баланс ****1234: 15,250.50 грн"
```

## 📊 Структура JSON сценария

```json
{
  "id": "balance-check-001",
  "name": "Проверка баланса карты",
  "start_node": "greeting",
  "nodes": [
    {
      "id": "greeting",
      "type": "announce",
      "parameters": {"message": "Привет! Помогу с балансом"},
      "next_nodes": ["ask_balance"]
    },
    {
      "id": "ask_balance", 
      "type": "ask",
      "parameters": {"question": "Хотите проверить баланс? (да/нет)"},
      "next_nodes": ["parse_answer"]
    },
    {
      "id": "parse_answer",
      "type": "parse",
      "parameters": {"script": "context.wantsBalance = input.includes('да')"},
      "next_nodes": ["check_answer"]
    },
    {
      "id": "check_answer",
      "type": "condition", 
      "parameters": {"condition": "context.wantsBalance == true"},
      "conditions": {
        "true": "ask_card",
        "false": "goodbye"
      }
    }
  ],
  "context": {"wantsBalance": false, "cardNumber": null}
}
```

## 🎯 Типы блоков и их логика

### 📢 ANNOUNCE - Объявление
```
Вход: context
Действие: Показать сообщение пользователю
Выход: message + next_node
```

### ❓ ASK - Запрос ввода
```
Вход: context
Действие: Задать вопрос, ждать ответа
Выход: question + waiting_for_input = true
```

### 🔍 PARSE - Обработка ввода
```
Вход: user_input + context
Действие: Выполнить script, обновить context
Выход: updated_context + next_node
```

### 🔀 CONDITION - Условие
```
Вход: context
Действие: Проверить условие
Выход: next_node (true/false path)
```

## 🧠 Логика выбора ответов

```mermaid
flowchart TD
    Input[📥 Пользовательский ввод] --> Parse[🔍 PARSE блок]
    Parse --> Script{📝 Выполнить script}
    
    Script --> UpdateContext[📝 Обновить context]
    UpdateContext --> Condition[🔀 CONDITION блок]
    
    Condition --> CheckCondition{❓ Проверить условие}
    CheckCondition -->|true| TruePath[✅ conditions.true]
    CheckCondition -->|false| FalsePath[❌ conditions.false]
    
    TruePath --> NextNodeTrue[➡️ Следующий блок TRUE]
    FalsePath --> NextNodeFalse[➡️ Следующий блок FALSE]
    
    NextNodeTrue --> Execute[⚡ Выполнить блок]
    NextNodeFalse --> Execute
    
    Execute --> Response[📤 Ответ пользователю]
```

## 🔧 Примеры условий

### Простые условия:
```javascript
// Проверка согласия
context.wantsBalance == true

// Валидация карты  
context.validCard == true

// Проверка результата парсинга
parse_result == true
```

### Сложные условия:
```javascript
// Множественная проверка
context.wantsBalance == true && context.hasCard == true

// Проверка значений
context.amount > 0 && context.amount <= 10000

// Строковые операции
context.userInput.toLowerCase().includes('да')
```

## 📈 Состояние сессии

```mermaid
stateDiagram-v2
    [*] --> Start: Создание сессии
    Start --> Greeting: Первое сообщение
    Greeting --> AskBalance: announce → ask
    AskBalance --> WaitingInput: Ждем ответа
    WaitingInput --> ParseAnswer: Получили ввод
    ParseAnswer --> CheckCondition: parse → condition
    
    CheckCondition --> AskCard: wantsBalance = true
    CheckCondition --> Goodbye: wantsBalance = false
    
    AskCard --> WaitingCard: Ждем номер карты
    WaitingCard --> ParseCard: Получили номер
    ParseCard --> ValidateCard: parse → condition
    
    ValidateCard --> ShowBalance: validCard = true
    ValidateCard --> CardError: validCard = false
    
    CardError --> AskCard: Повторный запрос
    ShowBalance --> End: Завершение
    Goodbye --> End: Завершение
    End --> [*]
```

## 🚀 Статус реализации

### ✅ Работает:
- **API Gateway** - авторизация, проксирование
- **Chat Service** - сессии, сообщения  
- **Scenario Service** - CRUD сценариев

### ⚠️ В разработке:
- **Orchestrator** - выполнение сценариев (требует доработки)

### 📋 Готово к интеграции:
- JSON структура сценариев
- Документация по блокам
- Схемы архитектуры
- Примеры диалогов
