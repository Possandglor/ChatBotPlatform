# 🎭 Детальный пример сценария

## 📋 Сценарий: Проверка баланса карты

### 🔍 Полная JSON структура с пояснениями

```json
{
  "id": "balance-check-001",
  "name": "Проверка баланса карты",
  "version": "1.0", 
  "language": "uk",
  "start_node": "greeting",  // ← Стартовый блок
  
  "nodes": [
    // 📢 БЛОК 1: Приветствие
    {
      "id": "greeting",
      "type": "announce",           // Тип: объявление
      "parameters": {
        "message": "Привет! Я помогу вам проверить баланс карты.",
        "delay": 500               // Пауза 500мс
      },
      "next_nodes": ["ask_balance"] // → Переход к следующему блоку
    },
    
    // ❓ БЛОК 2: Запрос желания проверить баланс
    {
      "id": "ask_balance",
      "type": "ask",               // Тип: запрос ввода
      "parameters": {
        "question": "Хотите проверить баланс карты? (да/нет)",
        "inputType": "text",       // Ожидаем текст
        "required": true           // Обязательное поле
      },
      "next_nodes": ["parse_answer"] // → Переход к парсингу
    },
    
    // 🔍 БЛОК 3: Обработка ответа пользователя
    {
      "id": "parse_answer", 
      "type": "parse",             // Тип: обработка ввода
      "parameters": {
        "script": "context.userAnswer = input.toLowerCase(); context.wantsBalance = input.toLowerCase().includes('да') || input.toLowerCase().includes('yes');"
      },
      "next_nodes": ["check_answer"], // → Переход к условию
      "conditions": {
        "error": "ask_balance"     // При ошибке → вернуться к вопросу
      }
    },
    
    // 🔀 БЛОК 4: Условное ветвление
    {
      "id": "check_answer",
      "type": "condition",         // Тип: условие
      "parameters": {
        "condition": "context.wantsBalance == true"  // Проверяем желание
      },
      "conditions": {
        "true": "ask_card_number", // ✅ ДА → запросить карту
        "false": "goodbye"         // ❌ НЕТ → попрощаться
      }
    },
    
    // ❓ БЛОК 5A: Запрос номера карты (путь ДА)
    {
      "id": "ask_card_number",
      "type": "ask",
      "parameters": {
        "question": "Введите последние 4 цифры карты:",
        "inputType": "number",     // Ожидаем цифры
        "required": true
      },
      "next_nodes": ["parse_card"]
    },
    
    // 🔍 БЛОК 6A: Обработка номера карты
    {
      "id": "parse_card",
      "type": "parse", 
      "parameters": {
        "script": "context.cardNumber = input; context.validCard = input.length == 4 && /^\\d+$/.test(input);"
      },
      "next_nodes": ["validate_card"],
      "conditions": {
        "error": "ask_card_number" // При ошибке → повторить запрос
      }
    },
    
    // 🔀 БЛОК 7A: Проверка валидности карты
    {
      "id": "validate_card",
      "type": "condition",
      "parameters": {
        "condition": "context.validCard == true"
      },
      "conditions": {
        "true": "show_balance",    // ✅ Валидная → показать баланс
        "false": "card_error"      // ❌ Невалидная → ошибка
      }
    },
    
    // 💰 БЛОК 8A: Показ баланса (успешный путь)
    {
      "id": "show_balance",
      "type": "announce",
      "parameters": {
        "message": "Баланс карты ****{context.cardNumber}: 15,250.50 грн. Спасибо за обращение!"
      },
      "next_nodes": ["end"]
    },
    
    // ⚠️ БЛОК 8B: Ошибка формата карты
    {
      "id": "card_error",
      "type": "announce",
      "parameters": {
        "message": "Неверный формат номера карты. Попробуйте еще раз."
      },
      "next_nodes": ["ask_card_number"] // → Повторить запрос карты
    },
    
    // 👋 БЛОК 5B: Прощание (путь НЕТ)
    {
      "id": "goodbye",
      "type": "announce",
      "parameters": {
        "message": "Хорошо, если понадобится помощь - обращайтесь!"
      },
      "next_nodes": ["end"]
    },
    
    // 🏁 БЛОК 9: Завершение
    {
      "id": "end",
      "type": "announce",
      "parameters": {
        "message": "До свидания!"
      },
      "next_nodes": []             // Пустой массив = конец сценария
    }
  ],
  
  // 🧠 Контекст сценария (состояние)
  "context": {
    "userAnswer": null,          // Ответ пользователя
    "wantsBalance": false,       // Хочет ли проверить баланс
    "cardNumber": null,          // Номер карты
    "validCard": false           // Валидность карты
  }
}
```

## 🎯 Пошаговое выполнение

### Шаг 1: Приветствие
```
Блок: greeting (announce)
Действие: Показать сообщение
Результат: "Привет! Я помогу вам проверить баланс карты."
Переход: → ask_balance
```

### Шаг 2: Запрос действия
```
Блок: ask_balance (ask)
Действие: Задать вопрос
Результат: "Хотите проверить баланс карты? (да/нет)"
Ожидание: Ввод пользователя
```

### Шаг 3: Обработка ответа
```
Пользователь: "Да"
Блок: parse_answer (parse)
Действие: context.wantsBalance = true
Переход: → check_answer
```

### Шаг 4: Условное ветвление
```
Блок: check_answer (condition)
Условие: context.wantsBalance == true
Результат: true
Переход: → ask_card_number
```

### Шаг 5: Запрос карты
```
Блок: ask_card_number (ask)
Действие: Задать вопрос
Результат: "Введите последние 4 цифры карты:"
Ожидание: Ввод пользователя
```

### Шаг 6: Обработка карты
```
Пользователь: "1234"
Блок: parse_card (parse)
Действие: context.cardNumber = "1234", context.validCard = true
Переход: → validate_card
```

### Шаг 7: Валидация карты
```
Блок: validate_card (condition)
Условие: context.validCard == true
Результат: true
Переход: → show_balance
```

### Шаг 8: Показ баланса
```
Блок: show_balance (announce)
Действие: Подставить переменные
Результат: "Баланс карты ****1234: 15,250.50 грн. Спасибо за обращение!"
Переход: → end
```

## 🔄 Альтернативные пути

### Путь "НЕТ":
```
parse_answer → check_answer → goodbye → end
```

### Путь "Ошибка карты":
```
parse_card → validate_card → card_error → ask_card_number (цикл)
```

## 🧠 Логика условий

### Простые условия:
```javascript
// Булевые значения
context.wantsBalance == true
context.validCard == false

// Сравнения
context.amount > 1000
context.attempts < 3
```

### Сложные условия:
```javascript
// Логические операторы
context.wantsBalance == true && context.hasCard == true

// Строковые операции
context.userInput.toLowerCase().includes('да')

// Регулярные выражения
/^\d{4}$/.test(context.cardNumber)
```

## 📊 Состояние контекста по шагам

| Шаг | userAnswer | wantsBalance | cardNumber | validCard |
|-----|------------|--------------|------------|-----------|
| 1   | null       | false        | null       | false     |
| 2   | null       | false        | null       | false     |
| 3   | "да"       | true         | null       | false     |
| 4   | "да"       | true         | null       | false     |
| 5   | "да"       | true         | null       | false     |
| 6   | "да"       | true         | "1234"     | true      |
| 7   | "да"       | true         | "1234"     | true      |
| 8   | "да"       | true         | "1234"     | true      |

## 🎮 Интерактивная схема

```
START
  ↓
📢 "Привет! Помогу с балансом"
  ↓
❓ "Хотите проверить баланс? (да/нет)"
  ↓
🔍 parse: wantsBalance = да/нет
  ↓
🔀 condition: wantsBalance == true?
  ├─✅ ДА → ❓ "Введите 4 цифры карты:"
  │         ↓
  │       🔍 parse: cardNumber, validCard
  │         ↓
  │       🔀 condition: validCard == true?
  │         ├─✅ OK → 💰 "Баланс ****1234: 15,250.50 грн"
  │         └─❌ Error → ⚠️ "Неверный формат" → (повтор)
  │
  └─❌ НЕТ → 👋 "Обращайтесь еще!"
             ↓
           🏁 END
```
