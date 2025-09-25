# ДЕТАЛЬНЫЕ ТЕХНИЧЕСКИЕ СПЕЦИФИКАЦИИ
## Платформа чат-ботов - Техническая реализация

**Дополнение к основному ТЗ**  
**Версия**: 2.0  
**Дата**: 23.09.2025  

---

## 1. АРХИТЕКТУРА МИКРОСЕРВИСОВ

### 1.1 API Gateway (порт 8090)
**Технологии**: Spring Cloud Gateway, Spring Boot 3.3.4

**Функции**:
- Маршрутизация запросов к микросервисам
- Аутентификация и авторизация
- Rate limiting и circuit breaker
- CORS настройки
- Логирование запросов

**Конфигурация маршрутов**:
```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: chat-service
          uri: http://chat-service:8080
          predicates:
            - Path=/api/v1/chat/**
        - id: orchestrator
          uri: http://orchestrator:8080
          predicates:
            - Path=/api/v1/execute/**
        - id: scenario-service
          uri: http://scenario-service:8080
          predicates:
            - Path=/api/v1/scenarios/**
```

**Health Check**: `GET /actuator/health`
**Platform Status**: `GET /api/v1/status`

### 1.2 Chat Service (порт 8091)
**Технологии**: Spring Boot, WebSocket, Spring Data JPA

**Функции**:
- Управление WebSocket соединениями
- Обработка входящих сообщений
- Маршрутизация к Orchestrator
- Управление сессиями пользователей
- Real-time уведомления

**WebSocket Endpoints**:
```
ws://localhost:8091/ws/chat/{sessionId}
```

**REST Endpoints**:
```
POST /api/v1/chat/sessions          - Создание сессии
GET  /api/v1/chat/sessions/{id}     - Получение сессии
POST /api/v1/chat/messages          - Отправка сообщения
GET  /api/v1/chat/history/{id}      - История диалога
```

### 1.3 Orchestrator (порт 8092)
**Технологии**: Spring Boot, GraalVM JavaScript, Spring WebFlux

**Функции**:
- Выполнение сценариев чат-ботов
- Интерпретация JSON сценариев
- Выполнение JavaScript кода в блоках
- Управление контекстом диалога
- Интеграция с внешними API

**Типы блоков сценариев**:
1. **announce** - Отправка сообщения пользователю
2. **ask** - Запрос ввода от пользователя
3. **parse** - Парсинг и валидация ответа
4. **api-request** - HTTP запрос к внешнему API
5. **llm-call** - Обращение к языковой модели
6. **wait** - Пауза в выполнении
7. **condition** - Условный переход
8. **sub-flow** - Вызов вложенного сценария

**Формат блока сценария**:
```json
{
  "id": "node_1",
  "type": "announce",
  "parameters": {
    "message": "Привет! Как дела?",
    "delay": 1000
  },
  "nextNodes": ["node_2"],
  "conditions": {
    "success": "node_2",
    "error": "error_handler"
  }
}
```

### 1.4 Scenario Service (порт 8093)
**Технологии**: Spring Boot, Spring Data JPA, PostgreSQL

**Функции**:
- CRUD операции со сценариями
- Версионирование сценариев
- Каталог с поиском и фильтрацией
- Импорт/экспорт сценариев
- Валидация структуры сценариев

**REST API**:
```
GET    /api/v1/scenarios                    - Список сценариев
POST   /api/v1/scenarios                    - Создание сценария
GET    /api/v1/scenarios/{id}               - Получение сценария
PUT    /api/v1/scenarios/{id}               - Обновление сценария
DELETE /api/v1/scenarios/{id}               - Удаление сценария
GET    /api/v1/scenarios/search?q={query}   - Поиск сценариев
POST   /api/v1/scenarios/{id}/versions      - Создание версии
GET    /api/v1/scenarios/{id}/versions      - Список версий
POST   /api/v1/scenarios/import             - Импорт сценария
GET    /api/v1/scenarios/{id}/export        - Экспорт сценария
```

### 1.5 NLU Service (порт 8095)
**Технологии**: Spring Boot, Python ML модели, REST API

**Функции**:
- Определение интентов (Intent Recognition)
- Извлечение сущностей (Entity Extraction)
- Анализ тональности (Sentiment Analysis)
- Определение языка текста
- Confidence scoring

**REST API**:
```
POST /api/v1/nlu/analyze           - Анализ текста
POST /api/v1/nlu/intents           - Определение интента
POST /api/v1/nlu/entities          - Извлечение сущностей
POST /api/v1/nlu/sentiment         - Анализ тональности
GET  /api/v1/nlu/models            - Список моделей
POST /api/v1/nlu/train             - Обучение модели
```

**Формат ответа**:
```json
{
  "intent": "order_pizza",
  "confidence": 0.95,
  "entities": [
    {
      "type": "pizza_type",
      "value": "маргарита",
      "start": 15,
      "end": 24
    }
  ],
  "sentiment": "positive",
  "language": "uk"
}
```

### 1.6 LLM Service (порт 8097)
**Технологии**: Spring Boot, OpenAI API, Google Cloud AI

**Функции**:
- Интеграция с OpenAI GPT
- Интеграция с Google PaLM/Gemini
- Кэширование ответов
- Управление токенами
- Мониторинг использования

**REST API**:
```
POST /api/v1/llm/generate          - Генерация ответа
POST /api/v1/llm/chat              - Чат с моделью
GET  /api/v1/llm/models            - Доступные модели
POST /api/v1/llm/embeddings        - Создание эмбеддингов
GET  /api/v1/llm/usage             - Статистика использования
```

### 1.7 STT Service (порт 8096)
**Технологии**: Spring Boot, Google Speech-to-Text API

**Функции**:
- Преобразование речи в текст
- Поддержка множественных форматов
- Потоковое распознавание
- Мультиязычность

**REST API**:
```
POST /api/v1/stt/transcribe        - Транскрибация файла
POST /api/v1/stt/stream            - Потоковая транскрибация
GET  /api/v1/stt/languages         - Поддерживаемые языки
GET  /api/v1/stt/formats           - Поддерживаемые форматы
```

### 1.8 Module Service (порт 8094)
**Технологии**: Spring Boot, JavaScript Engine

**Функции**:
- Управление переиспользуемыми модулями
- Библиотека готовых компонентов
- Выполнение пользовательских скриптов
- Версионирование модулей

---

## 2. СХЕМА БАЗЫ ДАННЫХ (ДЕТАЛЬНО)

### 2.1 Основные таблицы

#### chatbot_instance - Экземпляр чат-бота
```sql
CREATE TABLE chatbot_instance (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL DEFAULT 'Global Chatbot Platform',
    description TEXT,
    is_active BOOLEAN DEFAULT true,
    settings JSONB DEFAULT '{}',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

#### scenario_versions - Версии сценариев
```sql
CREATE TABLE scenario_versions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    scenario_id UUID REFERENCES scenarios(id) ON DELETE CASCADE,
    version INTEGER NOT NULL,
    environment VARCHAR(20) NOT NULL DEFAULT 'test',
    content JSONB NOT NULL,
    is_active BOOLEAN DEFAULT false,
    changelog TEXT,
    created_by UUID REFERENCES users(id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(scenario_id, version, environment)
);
```

#### dialogs - История диалогов
```sql
CREATE TABLE dialogs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id VARCHAR(255) NOT NULL,
    scenario_id UUID REFERENCES scenarios(id),
    scenario_version INTEGER,
    user_id VARCHAR(255),
    channel VARCHAR(50) DEFAULT 'web',
    status VARCHAR(20) DEFAULT 'active',
    context JSONB DEFAULT '{}',
    started_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ended_at TIMESTAMP,
    total_messages INTEGER DEFAULT 0,
    completion_rate DECIMAL(5,2)
) PARTITION BY RANGE (started_at);
```

#### dialog_messages - Сообщения диалогов
```sql
CREATE TABLE dialog_messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    dialog_id UUID REFERENCES dialogs(id) ON DELETE CASCADE,
    message_type VARCHAR(20) NOT NULL, -- 'user', 'bot', 'system'
    content TEXT NOT NULL,
    metadata JSONB DEFAULT '{}',
    node_id VARCHAR(100),
    intent VARCHAR(100),
    confidence DECIMAL(3,2),
    processing_time INTEGER, -- в миллисекундах
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) PARTITION BY RANGE (created_at);
```

#### intents - Интенты для NLU
```sql
CREATE TABLE intents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) UNIQUE NOT NULL,
    description TEXT,
    language VARCHAR(10) NOT NULL DEFAULT 'uk',
    examples TEXT[] NOT NULL,
    entities TEXT[] DEFAULT '{}',
    is_active BOOLEAN DEFAULT true,
    confidence_threshold DECIMAL(3,2) DEFAULT 0.7,
    created_by UUID REFERENCES users(id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

#### entities - Сущности для NLU
```sql
CREATE TABLE entities (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) UNIQUE NOT NULL,
    type VARCHAR(50) NOT NULL, -- 'regex', 'list', 'ml'
    pattern TEXT, -- для regex типа
    values JSONB, -- для list типа
    description TEXT,
    language VARCHAR(10) NOT NULL DEFAULT 'uk',
    is_active BOOLEAN DEFAULT true,
    created_by UUID REFERENCES users(id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

#### analytics_events - События для аналитики
```sql
CREATE TABLE analytics_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_type VARCHAR(50) NOT NULL,
    dialog_id UUID REFERENCES dialogs(id),
    scenario_id UUID REFERENCES scenarios(id),
    user_id VARCHAR(255),
    properties JSONB DEFAULT '{}',
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) PARTITION BY RANGE (timestamp);
```

### 2.2 Индексы для производительности

```sql
-- Индексы для scenarios
CREATE INDEX idx_scenarios_search ON scenarios USING gin(search_vector);
CREATE INDEX idx_scenarios_tags ON scenarios USING gin(tags);
CREATE INDEX idx_scenarios_category ON scenarios(category);
CREATE INDEX idx_scenarios_active ON scenarios(is_active) WHERE is_active = true;

-- Индексы для dialogs
CREATE INDEX idx_dialogs_session ON dialogs(session_id);
CREATE INDEX idx_dialogs_scenario ON dialogs(scenario_id);
CREATE INDEX idx_dialogs_started_at ON dialogs(started_at);
CREATE INDEX idx_dialogs_status ON dialogs(status);

-- Индексы для dialog_messages
CREATE INDEX idx_messages_dialog ON dialog_messages(dialog_id);
CREATE INDEX idx_messages_type ON dialog_messages(message_type);
CREATE INDEX idx_messages_created_at ON dialog_messages(created_at);
CREATE INDEX idx_messages_intent ON dialog_messages(intent);

-- Индексы для analytics_events
CREATE INDEX idx_analytics_type ON analytics_events(event_type);
CREATE INDEX idx_analytics_timestamp ON analytics_events(timestamp);
CREATE INDEX idx_analytics_scenario ON analytics_events(scenario_id);
```

### 2.3 Партиционирование для масштабирования

```sql
-- Партиционирование dialogs по месяцам
CREATE TABLE dialogs_y2025m01 PARTITION OF dialogs 
FOR VALUES FROM ('2025-01-01') TO ('2025-02-01');

CREATE TABLE dialogs_y2025m02 PARTITION OF dialogs 
FOR VALUES FROM ('2025-02-01') TO ('2025-03-01');

-- Партиционирование dialog_messages по месяцам
CREATE TABLE dialog_messages_y2025m01 PARTITION OF dialog_messages 
FOR VALUES FROM ('2025-01-01') TO ('2025-02-01');

-- Партиционирование analytics_events по неделям
CREATE TABLE analytics_events_w202501 PARTITION OF analytics_events 
FOR VALUES FROM ('2025-01-01') TO ('2025-01-08');
```

---

## 3. FRONTEND АРХИТЕКТУРА

### 3.1 Структура React приложения

```
src/
├── components/           # Переиспользуемые компоненты
│   ├── common/          # Общие UI компоненты
│   ├── scenario/        # Компоненты сценариев
│   ├── chat/           # Чат компоненты
│   └── analytics/      # Аналитические компоненты
├── pages/              # Страницы приложения
│   ├── Dashboard/      # Главная страница
│   ├── ScenarioEditor/ # Редактор сценариев
│   ├── ChatTest/       # Тестирование чатов
│   └── Analytics/      # Аналитика
├── hooks/              # Custom React hooks
├── services/           # API сервисы
├── store/              # Zustand stores
├── types/              # TypeScript типы
├── utils/              # Утилитарные функции
└── constants/          # Константы приложения
```

### 3.2 Визуальный редактор сценариев

**Технологии**: React Flow, Material-UI

**Компоненты блоков**:
```typescript
interface ScenarioNode {
  id: string;
  type: 'announce' | 'ask' | 'parse' | 'api-request' | 
        'llm-call' | 'wait' | 'condition' | 'sub-flow';
  position: { x: number; y: number };
  data: {
    label: string;
    parameters: Record<string, any>;
    validation?: ValidationRule[];
  };
}

interface ScenarioEdge {
  id: string;
  source: string;
  target: string;
  type: 'default' | 'conditional';
  data?: {
    condition?: string;
    label?: string;
  };
}
```

**Функции редактора**:
- Drag & Drop блоков из палитры
- Соединение блоков стрелками
- Настройка параметров блоков
- Валидация структуры сценария
- Предварительный просмотр
- Сохранение/загрузка сценариев

### 3.3 Чат-виджет

**WebSocket интеграция**:
```typescript
class ChatService {
  private socket: Socket;
  
  connect(sessionId: string) {
    this.socket = io(`ws://localhost:8091/ws/chat/${sessionId}`);
    
    this.socket.on('message', (data) => {
      // Обработка входящих сообщений
    });
    
    this.socket.on('typing', (data) => {
      // Индикатор печати
    });
  }
  
  sendMessage(message: string) {
    this.socket.emit('message', { content: message });
  }
}
```

### 3.4 Управление состоянием (Zustand)

```typescript
interface AppState {
  // Сценарии
  scenarios: Scenario[];
  currentScenario: Scenario | null;
  
  // Чат
  activeChats: ChatSession[];
  
  // Пользователь
  user: User | null;
  
  // UI состояние
  loading: boolean;
  error: string | null;
}

const useAppStore = create<AppState>((set, get) => ({
  scenarios: [],
  currentScenario: null,
  activeChats: [],
  user: null,
  loading: false,
  error: null,
  
  // Actions
  loadScenarios: async () => {
    set({ loading: true });
    try {
      const scenarios = await scenarioService.getAll();
      set({ scenarios, loading: false });
    } catch (error) {
      set({ error: error.message, loading: false });
    }
  }
}));
```

---

## 4. ИНТЕГРАЦИИ И API

### 4.1 Внешние API интеграции

#### OpenAI GPT Integration
```java
@Service
public class OpenAIService {
    
    @Value("${openai.api.key}")
    private String apiKey;
    
    public CompletableFuture<String> generateResponse(String prompt) {
        return webClient
            .post()
            .uri("https://api.openai.com/v1/chat/completions")
            .header("Authorization", "Bearer " + apiKey)
            .bodyValue(createRequest(prompt))
            .retrieve()
            .bodyToMono(OpenAIResponse.class)
            .map(response -> response.getChoices().get(0).getMessage().getContent())
            .toFuture();
    }
}
```

#### Google Cloud Speech-to-Text
```java
@Service
public class GoogleSTTService {
    
    private final SpeechClient speechClient;
    
    public String transcribeAudio(byte[] audioData) {
        RecognitionAudio audio = RecognitionAudio.newBuilder()
            .setContent(ByteString.copyFrom(audioData))
            .build();
            
        RecognitionConfig config = RecognitionConfig.newBuilder()
            .setEncoding(RecognitionConfig.AudioEncoding.LINEAR16)
            .setSampleRateHertz(16000)
            .setLanguageCode("uk-UA")
            .build();
            
        RecognizeResponse response = speechClient.recognize(config, audio);
        
        return response.getResultsList().stream()
            .map(result -> result.getAlternativesList().get(0).getTranscript())
            .collect(Collectors.joining(" "));
    }
}
```

### 4.2 Webhook интеграции

```java
@RestController
@RequestMapping("/api/v1/webhooks")
public class WebhookController {
    
    @PostMapping("/telegram")
    public ResponseEntity<Void> handleTelegramWebhook(@RequestBody TelegramUpdate update) {
        chatService.processMessage(
            update.getMessage().getChat().getId().toString(),
            update.getMessage().getText(),
            "telegram"
        );
        return ResponseEntity.ok().build();
    }
    
    @PostMapping("/viber")
    public ResponseEntity<Void> handleViberWebhook(@RequestBody ViberMessage message) {
        chatService.processMessage(
            message.getSender().getId(),
            message.getText(),
            "viber"
        );
        return ResponseEntity.ok().build();
    }
}
```

---

## 5. МОНИТОРИНГ И ЛОГИРОВАНИЕ

### 5.1 Метрики (Micrometer + Prometheus)

```java
@Component
public class ChatMetrics {
    
    private final Counter messagesCounter;
    private final Timer responseTimer;
    private final Gauge activeSessionsGauge;
    
    public ChatMetrics(MeterRegistry meterRegistry) {
        this.messagesCounter = Counter.builder("chat.messages.total")
            .description("Total number of chat messages")
            .tag("type", "user")
            .register(meterRegistry);
            
        this.responseTimer = Timer.builder("chat.response.time")
            .description("Chat response time")
            .register(meterRegistry);
            
        this.activeSessionsGauge = Gauge.builder("chat.sessions.active")
            .description("Number of active chat sessions")
            .register(meterRegistry, this, ChatMetrics::getActiveSessionsCount);
    }
    
    public void incrementMessages(String messageType) {
        messagesCounter.increment(Tags.of("type", messageType));
    }
    
    public Timer.Sample startResponseTimer() {
        return Timer.start(responseTimer);
    }
}
```

### 5.2 Структурированное логирование

```java
@Component
public class StructuredLogger {
    
    private final Logger logger = LoggerFactory.getLogger(StructuredLogger.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    public void logChatEvent(String event, String sessionId, Object data) {
        try {
            Map<String, Object> logEntry = Map.of(
                "timestamp", Instant.now(),
                "event", event,
                "sessionId", sessionId,
                "data", data,
                "service", "chat-service"
            );
            
            logger.info(objectMapper.writeValueAsString(logEntry));
        } catch (Exception e) {
            logger.error("Failed to log structured event", e);
        }
    }
}
```

### 5.3 Health Checks

```java
@Component
public class DatabaseHealthIndicator implements HealthIndicator {
    
    private final DataSource dataSource;
    
    @Override
    public Health health() {
        try (Connection connection = dataSource.getConnection()) {
            if (connection.isValid(1)) {
                return Health.up()
                    .withDetail("database", "PostgreSQL")
                    .withDetail("status", "Connected")
                    .build();
            }
        } catch (SQLException e) {
            return Health.down()
                .withDetail("database", "PostgreSQL")
                .withDetail("error", e.getMessage())
                .build();
        }
        
        return Health.down()
            .withDetail("database", "PostgreSQL")
            .withDetail("status", "Connection failed")
            .build();
    }
}
```

---

## 6. ПРОИЗВОДИТЕЛЬНОСТЬ И ОПТИМИЗАЦИЯ

### 6.1 Кэширование (Redis)

```java
@Service
public class ScenarioCacheService {
    
    private final RedisTemplate<String, Object> redisTemplate;
    
    @Cacheable(value = "scenarios", key = "#scenarioId")
    public Scenario getScenario(String scenarioId) {
        return scenarioRepository.findById(scenarioId)
            .orElseThrow(() -> new ScenarioNotFoundException(scenarioId));
    }
    
    @CacheEvict(value = "scenarios", key = "#scenario.id")
    public void updateScenario(Scenario scenario) {
        scenarioRepository.save(scenario);
    }
    
    @Cacheable(value = "dialog_context", key = "#sessionId", unless = "#result == null")
    public DialogContext getDialogContext(String sessionId) {
        return dialogContextRepository.findBySessionId(sessionId);
    }
}
```

### 6.2 Connection Pooling

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      idle-timeout: 300000
      max-lifetime: 1200000
      connection-timeout: 20000
      
  data:
    redis:
      lettuce:
        pool:
          max-active: 20
          max-idle: 8
          min-idle: 2
          max-wait: -1ms
```

### 6.3 Асинхронная обработка

```java
@Service
public class AsyncMessageProcessor {
    
    @Async("chatExecutor")
    public CompletableFuture<Void> processMessage(ChatMessage message) {
        try {
            // Обработка NLU
            NLUResult nluResult = nluService.analyze(message.getContent());
            
            // Выполнение сценария
            ScenarioResult result = orchestratorService.execute(
                message.getSessionId(), 
                nluResult
            );
            
            // Отправка ответа
            chatService.sendResponse(message.getSessionId(), result.getResponse());
            
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            logger.error("Failed to process message", e);
            return CompletableFuture.failedFuture(e);
        }
    }
}

@Configuration
@EnableAsync
public class AsyncConfig {
    
    @Bean("chatExecutor")
    public TaskExecutor chatExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(50);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("chat-");
        executor.initialize();
        return executor;
    }
}
```

---

## 7. БЕЗОПАСНОСТЬ (ДЕТАЛЬНО)

### 7.1 JWT Authentication

```java
@Component
public class JwtTokenProvider {
    
    @Value("${jwt.secret}")
    private String jwtSecret;
    
    @Value("${jwt.expiration}")
    private int jwtExpirationInMs;
    
    public String generateToken(UserPrincipal userPrincipal) {
        Date expiryDate = new Date(System.currentTimeMillis() + jwtExpirationInMs);
        
        return Jwts.builder()
            .setSubject(userPrincipal.getId().toString())
            .setIssuedAt(new Date())
            .setExpiration(expiryDate)
            .signWith(SignatureAlgorithm.HS512, jwtSecret)
            .compact();
    }
    
    public String getUserIdFromToken(String token) {
        Claims claims = Jwts.parser()
            .setSigningKey(jwtSecret)
            .parseClaimsJws(token)
            .getBody();
            
        return claims.getSubject();
    }
}
```

### 7.2 Rate Limiting

```java
@Component
public class RateLimitingFilter implements Filter {
    
    private final RedisTemplate<String, String> redisTemplate;
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, 
                        FilterChain chain) throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String clientIp = getClientIp(httpRequest);
        String key = "rate_limit:" + clientIp;
        
        String currentCount = redisTemplate.opsForValue().get(key);
        
        if (currentCount == null) {
            redisTemplate.opsForValue().set(key, "1", Duration.ofMinutes(1));
        } else if (Integer.parseInt(currentCount) >= 100) {
            HttpServletResponse httpResponse = (HttpServletResponse) response;
            httpResponse.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            return;
        } else {
            redisTemplate.opsForValue().increment(key);
        }
        
        chain.doFilter(request, response);
    }
}
```

### 7.3 Input Validation

```java
@RestController
@Validated
public class ScenarioController {
    
    @PostMapping("/scenarios")
    public ResponseEntity<Scenario> createScenario(
            @Valid @RequestBody CreateScenarioRequest request) {
        
        // Валидация структуры сценария
        validateScenarioStructure(request.getContent());
        
        Scenario scenario = scenarioService.create(request);
        return ResponseEntity.ok(scenario);
    }
    
    private void validateScenarioStructure(JsonNode content) {
        // Проверка на наличие обязательных полей
        if (!content.has("startNode")) {
            throw new ValidationException("Missing startNode");
        }
        
        // Проверка на циклические зависимости
        if (hasCyclicDependencies(content)) {
            throw new ValidationException("Cyclic dependencies detected");
        }
        
        // Валидация JavaScript кода в блоках
        validateJavaScriptCode(content);
    }
}
```

---

**Документ подготовлен**: 23.09.2025  
**Версия**: 2.0  
**Статус**: Техническая спецификация  
**Связанный документ**: TECHNICAL_SPECIFICATION.md
