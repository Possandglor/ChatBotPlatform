# Структура сценариев чат-ботов

## Формат JSON сценария

```json
{
  "id": "balance-check-001",
  "name": "Проверка баланса карты", 
  "version": "1.0",
  "language": "uk",
  "start_node": "greeting",
  "nodes": [
    {
      "id": "greeting",
      "type": "announce",
      "parameters": {
        "message": "Привет! Я помогу вам проверить баланс карты.",
        "delay": 500
      },
      "next_nodes": ["ask_card_check"]
    },
    {
      "id": "ask_card_check", 
      "type": "ask",
      "parameters": {
        "question": "Хотите проверить баланс карты? (да/нет)",
        "inputType": "text",
        "required": true
      },
      "next_nodes": ["parse_answer"]
    },
    {
      "id": "parse_answer",
      "type": "parse", 
      "parameters": {
        "script": "context.wantsBalance = input.toLowerCase().includes('да')"
      },
      "next_nodes": ["check_answer"],
      "conditions": {
        "error": "ask_card_check"
      }
    },
    {
      "id": "check_answer",
      "type": "condition",
      "parameters": {
        "condition": "context.wantsBalance == true"
      },
      "conditions": {
        "true": "ask_card_number",
        "false": "goodbye"
      }
    }
  ],
  "context": {
    "wantsBalance": false,
    "cardNumber": null
  }
}
```

## Типы блоков (nodes)

### 1. **announce** - Объявление/сообщение
```json
{
  "id": "greeting",
  "type": "announce", 
  "parameters": {
    "message": "Привет! Как дела?",
    "delay": 1000
  },
  "next_nodes": ["next_step"]
}
```

### 2. **ask** - Запрос ввода от пользователя
```json
{
  "id": "ask_name",
  "type": "ask",
  "parameters": {
    "question": "Как вас зовут?",
    "inputType": "text",
    "required": true
  },
  "next_nodes": ["parse_name"]
}
```

### 3. **parse** - Обработка пользовательского ввода
```json
{
  "id": "parse_card",
  "type": "parse",
  "parameters": {
    "script": "context.cardNumber = input; context.validCard = /^\\d{4}$/.test(input);"
  },
  "next_nodes": ["validate_card"],
  "conditions": {
    "error": "ask_card_number"
  }
}
```

### 4. **condition** - Условное ветвление
```json
{
  "id": "check_balance_request",
  "type": "condition",
  "parameters": {
    "condition": "context.wantsBalance == true"
  },
  "conditions": {
    "true": "ask_card_number",
    "false": "goodbye"
  }
}
```

### 5. **wait** - Пауза
```json
{
  "id": "wait_processing",
  "type": "wait",
  "parameters": {
    "duration": 2000
  },
  "next_nodes": ["show_result"]
}
```

## Пример полного диалога

### Сценарий: Проверка баланса карты

**Шаг 1: Приветствие**
- 👤 Пользователь: "Привет"
- 🤖 Бот: "Привет! Я помогу вам проверить баланс карты."

**Шаг 2: Запрос действия**
- 🤖 Бот: "Хотите проверить баланс карты? (да/нет)"
- 👤 Пользователь: "Да"

**Шаг 3: Запрос данных карты**
- 🤖 Бот: "Введите последние 4 цифры карты:"
- 👤 Пользователь: "1234"

**Шаг 4: Показ результата**
- 🤖 Бот: "Баланс карты ****1234: 15,250.50 грн. Спасибо за обращение!"

**Альтернативный путь (НЕТ):**
- 👤 Пользователь: "Нет"
- 🤖 Бот: "Хорошо, если понадобится помощь - обращайтесь!"

## Контекст сценария

Контекст хранит состояние диалога:

```json
{
  "context": {
    "userName": null,
    "wantsBalance": false,
    "cardNumber": null,
    "validCard": false,
    "currentStep": "greeting"
  }
}
```

## Переменные в сообщениях

Можно использовать переменные из контекста:

```json
{
  "parameters": {
    "message": "Привет, {context.userName}! Баланс карты ****{context.cardNumber}: 15,250.50 грн."
  }
}
```

## Валидация ввода

### Типы ввода:
- `text` - любой текст
- `number` - только цифры
- `email` - email адрес
- `phone` - номер телефона

### Условия парсинга:
```javascript
// Проверка на 4 цифры
context.validCard = /^\\d{4}$/.test(input);

// Проверка на согласие
context.wantsBalance = input.toLowerCase().includes('да') || 
                      input.toLowerCase().includes('yes');
```

## Обработка ошибок

```json
{
  "id": "parse_card",
  "type": "parse",
  "conditions": {
    "error": "ask_card_number"  // Возврат к предыдущему шагу при ошибке
  }
}
```

## Завершение сценария

```json
{
  "id": "end",
  "type": "announce",
  "parameters": {
    "message": "До свидания!"
  },
  "next_nodes": []  // Пустой массив = конец сценария
}
```

## Интеграция с Orchestrator

Для выполнения сценария через Orchestrator:

```bash
curl -X POST http://localhost:8092/api/v1/execute/scenario \
  -H "Content-Type: application/json" \
  -d '{
    "scenario": { /* JSON сценария */ },
    "user_input": "Привет",
    "context": {}
  }'
```

## Лучшие практики

1. **Четкие вопросы**: Всегда указывайте ожидаемый формат ответа
2. **Валидация**: Проверяйте пользовательский ввод
3. **Обработка ошибок**: Предусматривайте возврат к предыдущим шагам
4. **Альтернативные пути**: Учитывайте разные варианты ответов пользователя
5. **Контекст**: Сохраняйте важную информацию между шагами
6. **Завершение**: Всегда предусматривайте логическое завершение диалога
