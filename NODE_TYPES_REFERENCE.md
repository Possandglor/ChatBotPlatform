# 🎭 Полный справочник типов узлов сценария

## 📋 ТЕКУЩИЕ ТИПЫ УЗЛОВ (реализованы)

### 1. **📢 ANNOUNCE** - Объявление
```json
{
  "type": "announce",
  "parameters": {
    "message": "Привет! Что вас интересует?",
    "delay": 500
  },
  "next_nodes": ["ask_operation"]
}
```
**Что делает**: Показывает сообщение пользователю  
**Переходы**: Автоматически к следующему узлу

### 2. **❓ ASK** - Запрос ввода
```json
{
  "type": "ask",
  "parameters": {
    "question": "Введите номер карты:",
    "inputType": "text",
    "required": true,
    "validation": "\\d{4}"
  },
  "next_nodes": ["parse_card"]
}
```
**Что делает**: Задает вопрос, ждет ответа пользователя  
**Переходы**: К узлу обработки ввода

### 3. **🔍 PARSE** - Обработка ввода
```json
{
  "type": "parse",
  "parameters": {
    "script": "context.operation = input.toLowerCase().includes('баланс') ? 'balance' : 'unknown'"
  },
  "next_nodes": ["route_operation"],
  "conditions": {
    "error": "ask_operation"
  }
}
```
**Что делает**: Парсит пользовательский ввод в переменные контекста  
**Переходы**: К следующему узлу или к error при ошибке

### 4. **🔀 CONDITION** - Условное ветвление
```json
{
  "type": "condition",
  "parameters": {
    "condition": "context.operation"
  },
  "conditions": {
    "balance": "balance_api",
    "close": "close_api",
    "block": "block_api",
    "default": "unknown_operation"
  }
}
```
**Что делает**: Проверяет условие и выбирает путь  
**Переходы**: По значению переменной или true/false

### 5. **🌐 API-REQUEST** - Вызов внешнего API
```json
{
  "type": "api-request",
  "parameters": {
    "service": "bank-api",
    "endpoint": "/api/v1/accounts/balance",
    "method": "GET",
    "headers": {
      "Authorization": "Bearer {context.token}",
      "Card-Number": "{context.cardNumber}"
    },
    "data": {
      "user_id": "{context.userId}"
    },
    "timeout": 5000
  },
  "conditions": {
    "success": "show_balance",
    "error": "balance_error",
    "timeout": "timeout_error"
  }
}
```
**Что делает**: HTTP запрос к внешнему сервису  
**Переходы**: success/error/timeout в зависимости от результата

### 6. **🔄 SUB-FLOW** - Подсценарий
```json
{
  "type": "sub-flow",
  "parameters": {
    "scenario_id": "card-verification-001",
    "inherit_context": true,
    "timeout": 30000
  },
  "conditions": {
    "completed": "continue_main_flow",
    "cancelled": "ask_operation",
    "timeout": "timeout_error"
  }
}
```
**Что делает**: Переход в другой сценарий  
**Переходы**: completed/cancelled/timeout

### 7. **📧 NOTIFICATION** - Уведомления
```json
{
  "type": "notification",
  "parameters": {
    "type": "sms",
    "template": "card_blocked",
    "recipient": "{context.userPhone}",
    "data": {
      "card_number": "{context.cardNumber}",
      "timestamp": "{context.blockTime}"
    }
  },
  "next_nodes": ["confirm_notification"]
}
```
**Что делает**: Отправляет SMS/Email/Push уведомление  
**Переходы**: К следующему узлу

### 8. **⏱️ WAIT** - Пауза
```json
{
  "type": "wait",
  "parameters": {
    "duration": 2000,
    "message": "Обрабатываем запрос..."
  },
  "next_nodes": ["continue_process"]
}
```
**Что делает**: Пауза в выполнении сценария  
**Переходы**: Автоматически после паузы

## 🤖 ПЛАНИРУЕМЫЕ ТИПЫ УЗЛОВ (нужно добавить)

### 9. **🧠 LLM-REQUEST** - Запрос к LLM
```json
{
  "type": "llm-request",
  "parameters": {
    "provider": "openai",
    "model": "gpt-4",
    "prompt": "Проанализируй запрос пользователя: {context.userMessage}",
    "temperature": 0.7,
    "max_tokens": 150,
    "system_prompt": "Ты банковский консультант"
  },
  "conditions": {
    "success": "process_llm_response",
    "error": "llm_error"
  }
}
```

### 10. **📊 DATABASE** - Запрос к БД
```json
{
  "type": "database",
  "parameters": {
    "query": "SELECT balance FROM accounts WHERE card_number = ?",
    "params": ["{context.cardNumber}"],
    "datasource": "main_db"
  },
  "conditions": {
    "success": "show_db_result",
    "error": "db_error"
  }
}
```

### 11. **🔐 VALIDATION** - Валидация данных
```json
{
  "type": "validation",
  "parameters": {
    "rules": [
      {"field": "context.cardNumber", "type": "regex", "pattern": "\\d{16}"},
      {"field": "context.amount", "type": "range", "min": 1, "max": 50000}
    ]
  },
  "conditions": {
    "valid": "proceed",
    "invalid": "validation_error"
  }
}
```

### 12. **🔄 LOOP** - Цикл
```json
{
  "type": "loop",
  "parameters": {
    "condition": "context.attempts < 3",
    "body": ["ask_pin", "validate_pin"],
    "increment": "context.attempts++"
  },
  "conditions": {
    "continue": "ask_pin",
    "break": "max_attempts_reached"
  }
}
```

## 🛠️ ЧТО НУЖНО ДЛЯ API СЕРВИСОВ

### **Bank API (порт 8094)**
```
GET  /api/v1/accounts/balance?card={number}
POST /api/v1/accounts/transfer
GET  /api/v1/transactions/history?card={number}&days=30
POST /api/v1/cards/block
POST /api/v1/cards/unblock
```

### **CRM Service (порт 8095)**
```
POST /api/v1/cards/close-request
GET  /api/v1/requests/{id}
POST /api/v1/support/ticket
GET  /api/v1/users/{id}/profile
```

### **Notification Service (порт 8096)**
```
POST /api/v1/sms/send
POST /api/v1/email/send
POST /api/v1/push/send
GET  /api/v1/templates/{id}
```

### **LLM Service (порт 8097)**
```
POST /api/v1/chat/completion
POST /api/v1/embeddings
POST /api/v1/classification
```

## 📝 ПРИМЕРЫ ИСПОЛЬЗОВАНИЯ

### Простой диалог:
```
announce → ask → parse → condition → announce
```

### С API вызовом:
```
announce → ask → parse → condition → api-request → announce
```

### С LLM анализом:
```
ask → parse → llm-request → condition → announce
```

### Сложный сценарий:
```
announce → ask → parse → validation → condition → 
  ├─ api-request → notification → sub-flow
  └─ llm-request → database → announce
```

## 🎯 ПРИОРИТЕТЫ РЕАЛИЗАЦИИ

### **Высокий приоритет** (нужно сейчас):
1. **LLM-REQUEST** - для умных ответов
2. **DATABASE** - для реальных данных
3. **VALIDATION** - для проверки ввода

### **Средний приоритет**:
4. **LOOP** - для повторных попыток
5. **FILE-UPLOAD** - для документов
6. **WEBHOOK** - для внешних событий

### **Низкий приоритет**:
7. **SCHEDULE** - для отложенных действий
8. **CACHE** - для оптимизации
9. **METRICS** - для аналитики

## 🔧 ТРЕБОВАНИЯ ДЛЯ РАЗРАБОТКИ

### **От тебя нужно**:
1. **Какие банковские операции** нужны в Bank API?
2. **Какие LLM провайдеры** использовать? (OpenAI, Anthropic, local?)
3. **Какие уведомления** отправлять? (SMS, Email, Push?)
4. **Какие валидации** нужны для банковских данных?

### **Я создам**:
- Mock API сервисы с реальными endpoints
- LLM интеграцию (OpenAI/Anthropic)
- Database узлы для PostgreSQL
- Validation узлы с правилами

## ✅ ТЕКУЩИЙ СТАТУС

**Реализовано**: 8 типов узлов (базовые)  
**Планируется**: +4 типа (LLM, DB, Validation, Loop)  
**Всего будет**: 12+ типов узлов

**Готовность**: Можно создавать сложные сценарии уже сейчас!
