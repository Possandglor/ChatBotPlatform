# ПРИМЕРЫ ИСПОЛЬЗОВАНИЯ API
## Платформа чат-ботов - Практические примеры

**Версия**: 2.0  
**Дата**: 23.09.2025  

---

## 1. ПРИМЕРЫ СЦЕНАРИЕВ

### 1.1 Простой сценарий приветствия

```json
{
  "id": "greeting_scenario",
  "name": "Приветствие пользователя",
  "version": "1.0",
  "language": "uk",
  "startNode": "welcome",
  "nodes": [
    {
      "id": "welcome",
      "type": "announce",
      "parameters": {
        "message": "Привіт! Я ваш віртуальний асистент. Як справи?",
        "delay": 1000
      },
      "nextNodes": ["ask_name"]
    },
    {
      "id": "ask_name",
      "type": "ask",
      "parameters": {
        "question": "Як вас звати?",
        "inputType": "text",
        "required": true
      },
      "nextNodes": ["process_name"]
    },
    {
      "id": "process_name",
      "type": "parse",
      "parameters": {
        "script": "context.userName = input.trim(); return input.length > 0;"
      },
      "nextNodes": ["personalized_greeting"]
    },
    {
      "id": "personalized_greeting",
      "type": "announce",
      "parameters": {
        "message": "Приємно познайомитися, {{context.userName}}! Чим можу допомогти?"
      },
      "nextNodes": ["main_menu"]
    }
  ]
}
```

### 1.2 Сценарий с API интеграцией

```json
{
  "id": "weather_scenario",
  "name": "Прогноз погоды",
  "startNode": "ask_city",
  "nodes": [
    {
      "id": "ask_city",
      "type": "ask",
      "parameters": {
        "question": "В якому місті ви хочете дізнатися погоду?",
        "inputType": "text"
      },
      "nextNodes": ["get_weather"]
    },
    {
      "id": "get_weather",
      "type": "api-request",
      "parameters": {
        "url": "https://api.openweathermap.org/data/2.5/weather",
        "method": "GET",
        "params": {
          "q": "{{input}}",
          "appid": "{{env.WEATHER_API_KEY}}",
          "units": "metric",
          "lang": "uk"
        },
        "timeout": 5000
      },
      "nextNodes": ["show_weather", "weather_error"]
    },
    {
      "id": "show_weather",
      "type": "announce",
      "parameters": {
        "message": "Погода в {{apiResponse.name}}: {{apiResponse.weather[0].description}}, температура {{apiResponse.main.temp}}°C"
      },
      "conditions": {
        "success": "end"
      }
    },
    {
      "id": "weather_error",
      "type": "announce",
      "parameters": {
        "message": "Вибачте, не вдалося отримати дані про погоду. Спробуйте ще раз."
      },
      "nextNodes": ["ask_city"]
    }
  ]
}
```

---

## 2. REST API ПРИМЕРЫ

### 2.1 Управление сценариями

**Создание сценария**:
```bash
curl -X POST http://localhost:8090/api/v1/scenarios \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "name": "Тестовый сценарий",
    "description": "Простой сценарий для тестирования",
    "category": "test",
    "language": "uk",
    "content": {
      "startNode": "start",
      "nodes": [...]
    }
  }'
```

**Получение списка сценариев**:
```bash
curl -X GET "http://localhost:8090/api/v1/scenarios?page=0&size=10&category=test" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

**Поиск сценариев**:
```bash
curl -X GET "http://localhost:8090/api/v1/scenarios/search?q=погода&language=uk" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### 2.2 Выполнение диалогов

**Начало диалога**:
```bash
curl -X POST http://localhost:8090/api/v1/dialogs/start \
  -H "Content-Type: application/json" \
  -d '{
    "scenarioId": "greeting_scenario",
    "userId": "user123",
    "channel": "web"
  }'
```

**Отправка сообщения**:
```bash
curl -X POST http://localhost:8090/api/v1/dialogs/session123/message \
  -H "Content-Type: application/json" \
  -d '{
    "content": "Привет!",
    "type": "text"
  }'
```

### 2.3 NLU анализ

**Анализ текста**:
```bash
curl -X POST http://localhost:8090/api/v1/nlu/analyze \
  -H "Content-Type: application/json" \
  -d '{
    "text": "Хочу заказать пиццу маргарита",
    "language": "uk"
  }'
```

**Ответ**:
```json
{
  "intent": "order_food",
  "confidence": 0.95,
  "entities": [
    {
      "type": "food_type",
      "value": "пицца",
      "start": 13,
      "end": 18
    },
    {
      "type": "pizza_type", 
      "value": "маргарита",
      "start": 19,
      "end": 28
    }
  ],
  "sentiment": "neutral",
  "language": "uk"
}
```

---

## 3. WEBSOCKET ПРИМЕРЫ

### 3.1 JavaScript клиент

```javascript
// Подключение к WebSocket
const socket = io('ws://localhost:8091/ws/chat/session123');

// Обработка событий
socket.on('connect', () => {
  console.log('Подключено к чату');
});

socket.on('message', (data) => {
  console.log('Получено сообщение:', data);
  displayMessage(data.content, 'bot');
});

socket.on('typing', (data) => {
  showTypingIndicator(data.isTyping);
});

socket.on('scenario_change', (data) => {
  console.log('Смена сценария:', data.scenarioId);
});

// Отправка сообщения
function sendMessage(text) {
  socket.emit('message', {
    content: text,
    type: 'text',
    timestamp: new Date().toISOString()
  });
}

// Индикатор печати
function startTyping() {
  socket.emit('typing', { isTyping: true });
}

function stopTyping() {
  socket.emit('typing', { isTyping: false });
}
```

### 3.2 React компонент чата

```typescript
import React, { useState, useEffect } from 'react';
import io, { Socket } from 'socket.io-client';

interface Message {
  id: string;
  content: string;
  type: 'user' | 'bot';
  timestamp: string;
}

const ChatWidget: React.FC = () => {
  const [socket, setSocket] = useState<Socket | null>(null);
  const [messages, setMessages] = useState<Message[]>([]);
  const [inputText, setInputText] = useState('');
  const [isTyping, setIsTyping] = useState(false);

  useEffect(() => {
    const newSocket = io('ws://localhost:8091/ws/chat/session123');
    
    newSocket.on('message', (data) => {
      setMessages(prev => [...prev, {
        id: data.id,
        content: data.content,
        type: 'bot',
        timestamp: data.timestamp
      }]);
    });

    newSocket.on('typing', (data) => {
      setIsTyping(data.isTyping);
    });

    setSocket(newSocket);

    return () => newSocket.close();
  }, []);

  const sendMessage = () => {
    if (inputText.trim() && socket) {
      const message: Message = {
        id: Date.now().toString(),
        content: inputText,
        type: 'user',
        timestamp: new Date().toISOString()
      };

      setMessages(prev => [...prev, message]);
      socket.emit('message', { content: inputText });
      setInputText('');
    }
  };

  return (
    <div className="chat-widget">
      <div className="messages">
        {messages.map(msg => (
          <div key={msg.id} className={`message ${msg.type}`}>
            {msg.content}
          </div>
        ))}
        {isTyping && <div className="typing">Бот печатает...</div>}
      </div>
      
      <div className="input-area">
        <input
          value={inputText}
          onChange={(e) => setInputText(e.target.value)}
          onKeyPress={(e) => e.key === 'Enter' && sendMessage()}
          placeholder="Введите сообщение..."
        />
        <button onClick={sendMessage}>Отправить</button>
      </div>
    </div>
  );
};
```

---

## 4. ИНТЕГРАЦИЯ С LLM

### 4.1 Вызов GPT в сценарии

```json
{
  "id": "llm_node",
  "type": "llm-call",
  "parameters": {
    "provider": "openai",
    "model": "gpt-4",
    "prompt": "Ответь на вопрос пользователя: {{input}}. Отвечай кратко и по делу.",
    "maxTokens": 150,
    "temperature": 0.7,
    "systemPrompt": "Ты полезный ассистент банка. Отвечай на украинском языке."
  },
  "nextNodes": ["show_llm_response"]
}
```

### 4.2 REST API для LLM

```bash
curl -X POST http://localhost:8090/api/v1/llm/generate \
  -H "Content-Type: application/json" \
  -d '{
    "model": "gpt-4",
    "prompt": "Объясни что такое блокчейн простыми словами",
    "maxTokens": 200,
    "temperature": 0.5
  }'
```

---

## 5. АНАЛИТИКА И МЕТРИКИ

### 5.1 Получение метрик

```bash
# Основные метрики платформы
curl http://localhost:8090/api/v1/analytics/metrics

# Статистика по сценариям
curl http://localhost:8090/api/v1/analytics/scenarios?period=7d

# Популярные интенты
curl http://localhost:8090/api/v1/analytics/intents?limit=10
```

### 5.2 Пример ответа метрик

```json
{
  "totalDialogs": 15420,
  "activeDialogs": 234,
  "averageResponseTime": 1.2,
  "completionRate": 0.87,
  "topScenarios": [
    {
      "id": "greeting_scenario",
      "name": "Приветствие",
      "executions": 5420,
      "completionRate": 0.95
    }
  ],
  "errorRate": 0.02,
  "period": "24h"
}
```

---

## 6. ТЕСТИРОВАНИЕ

### 6.1 Тестирование сценария

```bash
# Запуск тестового диалога
curl -X POST http://localhost:8090/api/v1/test/scenario \
  -H "Content-Type: application/json" \
  -d '{
    "scenarioId": "greeting_scenario",
    "testMessages": [
      "Привет",
      "Меня зовут Иван",
      "Хочу узнать баланс"
    ]
  }'
```

### 6.2 Нагрузочное тестирование

```javascript
// k6 скрипт для нагрузочного тестирования
import http from 'k6/http';
import { check } from 'k6';

export let options = {
  stages: [
    { duration: '2m', target: 100 },
    { duration: '5m', target: 100 },
    { duration: '2m', target: 0 },
  ],
};

export default function() {
  let response = http.post('http://localhost:8090/api/v1/dialogs/start', 
    JSON.stringify({
      scenarioId: 'greeting_scenario',
      userId: `user_${__VU}_${__ITER}`,
      channel: 'web'
    }), 
    { headers: { 'Content-Type': 'application/json' } }
  );
  
  check(response, {
    'status is 200': (r) => r.status === 200,
    'response time < 500ms': (r) => r.timings.duration < 500,
  });
}
```

---

**Документ подготовлен**: 23.09.2025  
**Версия**: 2.0  
**Связанные документы**: TECHNICAL_SPECIFICATION.md, DEPLOYMENT_GUIDE.md
