# Обновленная архитектура под ТЗ

## Ключевые изменения архитектуры

### 1. Модель данных: Один чат-бот с миллионом сценариев

```sql
-- Упрощенная модель: один глобальный чат-бот
CREATE TABLE chatbot_instance (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL DEFAULT 'Global Chatbot',
    is_active BOOLEAN DEFAULT true,
    settings JSONB DEFAULT '{}',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Сценарии привязаны к единственному чат-боту
CREATE TABLE scenarios (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    chatbot_id UUID REFERENCES chatbot_instance(id) DEFAULT (SELECT id FROM chatbot_instance LIMIT 1),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    category VARCHAR(100), -- для каталогизации
    language VARCHAR(10) NOT NULL DEFAULT 'uk',
    fallback_language VARCHAR(10) DEFAULT 'en',
    is_main BOOLEAN DEFAULT false,
    is_active BOOLEAN DEFAULT true,
    version INTEGER DEFAULT 1,
    created_by UUID REFERENCES users(id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- Индексы для поиска по миллиону сценариев
    INDEX idx_scenarios_category (category),
    INDEX idx_scenarios_language (language),
    INDEX idx_scenarios_name (name),
    INDEX idx_scenarios_created_by (created_by),
    INDEX idx_scenarios_created_at (created_at)
);
```

### 2. Гео-резервирование и Failover

```yaml
# Multi-region deployment
apiVersion: v1
kind: ConfigMap
metadata:
  name: geo-failover-config
  namespace: chatbot-platform
data:
  PRIMARY_REGION: "ukraine-central"
  BACKUP_REGIONS: "eu-west-1,us-east-1"
  FAILOVER_TIMEOUT: "30s"
  HEALTH_CHECK_INTERVAL: "10s"
  
  # Service discovery для регионов
  UKRAINE_CENTRAL_ENDPOINT: "https://ua-central.chatbot-platform.com"
  EU_WEST_ENDPOINT: "https://eu-west.chatbot-platform.com"
  US_EAST_ENDPOINT: "https://us-east.chatbot-platform.com"
```

### 3. Упрощенная модель пользователей

```sql
-- Фиксированные роли без детализации
CREATE TYPE user_role AS ENUM ('ADMIN', 'EDITOR', 'VIEWER');

CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username VARCHAR(255) UNIQUE NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role user_role NOT NULL DEFAULT 'EDITOR',
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Убираем организации - все работают с одним чат-ботом
-- Убираем сложную систему ролей
```

### 4. Версионирование по средам

```sql
-- Версии сценариев с разделением по средам
CREATE TABLE scenario_versions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    scenario_id UUID REFERENCES scenarios(id) ON DELETE CASCADE,
    version INTEGER NOT NULL,
    environment VARCHAR(20) NOT NULL DEFAULT 'test', -- 'test' или 'production'
    content JSONB NOT NULL,
    is_published BOOLEAN DEFAULT false,
    is_approved BOOLEAN DEFAULT false, -- для продакшена
    created_by UUID REFERENCES users(id),
    approved_by UUID REFERENCES users(id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    UNIQUE(scenario_id, version, environment)
);

-- Архивация старых версий
CREATE TABLE scenario_versions_archive (
    LIKE scenario_versions INCLUDING ALL,
    archived_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### 5. Мультиязычность в блоках

```json
{
  "blockType": "announce",
  "config": {
    "messages": {
      "uk": "Привіт! Як справи?",
      "en": "Hello! How are you?"
    },
    "fallbackLanguage": "en"
  }
}
```

### 6. Каталог сценариев с расширенным поиском

```sql
-- Расширенные метаданные для поиска
ALTER TABLE scenarios ADD COLUMN tags TEXT[]; -- массив тегов
ALTER TABLE scenarios ADD COLUMN block_types TEXT[]; -- типы блоков в сценарии
ALTER TABLE scenarios ADD COLUMN complexity_level INTEGER DEFAULT 1; -- 1-5
ALTER TABLE scenarios ADD COLUMN estimated_duration INTEGER; -- минуты
ALTER TABLE scenarios ADD COLUMN target_audience VARCHAR(100);

-- Полнотекстовый поиск
ALTER TABLE scenarios ADD COLUMN search_vector tsvector;
CREATE INDEX idx_scenarios_search ON scenarios USING gin(search_vector);

-- Функция для обновления поискового вектора
CREATE OR REPLACE FUNCTION update_scenario_search_vector()
RETURNS TRIGGER AS $$
BEGIN
    NEW.search_vector := 
        setweight(to_tsvector('simple', COALESCE(NEW.name, '')), 'A') ||
        setweight(to_tsvector('simple', COALESCE(NEW.description, '')), 'B') ||
        setweight(to_tsvector('simple', array_to_string(NEW.tags, ' ')), 'C');
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER update_scenarios_search_vector
    BEFORE INSERT OR UPDATE ON scenarios
    FOR EACH ROW EXECUTE FUNCTION update_scenario_search_vector();
```

## Обновленные микросервисы

### 1. Scenario Catalog Service (новый)

```java
@RestController
@RequestMapping("/api/v1/catalog")
public class ScenarioCatalogController {
    
    @GetMapping("/search")
    public ResponseEntity<PagedResponse<ScenarioSummary>> searchScenarios(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String language,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String author,
            @RequestParam(required = false) List<String> blockTypes,
            @RequestParam(required = false) Integer complexityLevel,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdAfter,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdBefore,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        
        ScenarioSearchCriteria criteria = ScenarioSearchCriteria.builder()
                .query(query)
                .language(language)
                .category(category)
                .author(author)
                .blockTypes(blockTypes)
                .complexityLevel(complexityLevel)
                .createdAfter(createdAfter)
                .createdBefore(createdBefore)
                .build();
        
        Pageable pageable = PageRequest.of(page, size, 
                Sort.by(Sort.Direction.fromString(sortDir), sortBy));
        
        Page<ScenarioSummary> results = scenarioCatalogService.search(criteria, pageable);
        
        return ResponseEntity.ok(PagedResponse.of(results));
    }
}
```

### 2. Geo-Failover Service

```java
@Service
@Slf4j
public class GeoFailoverService {
    
    private final List<String> regions = Arrays.asList(
        "ukraine-central", "eu-west-1", "us-east-1"
    );
    
    private final Map<String, String> regionEndpoints = Map.of(
        "ukraine-central", "https://ua-central.chatbot-platform.com",
        "eu-west-1", "https://eu-west.chatbot-platform.com", 
        "us-east-1", "https://us-east.chatbot-platform.com"
    );
    
    @Scheduled(fixedDelay = 10000) // каждые 10 секунд
    public void checkRegionHealth() {
        for (String region : regions) {
            boolean isHealthy = performHealthCheck(region);
            updateRegionStatus(region, isHealthy);
            
            if (!isHealthy && region.equals(getCurrentPrimaryRegion())) {
                performFailover();
            }
        }
    }
    
    private void performFailover() {
        String newPrimaryRegion = findHealthyRegion();
        if (newPrimaryRegion != null) {
            log.warn("Performing failover to region: {}", newPrimaryRegion);
            updatePrimaryRegion(newPrimaryRegion);
            notifyFailover(newPrimaryRegion);
        } else {
            log.error("No healthy regions available for failover!");
            triggerEmergencyAlert();
        }
    }
}
```

### 3. Test Chat Service (упрощенный)

```java
@Service
public class TestChatService {
    
    private final Map<String, String> mockResponses = Map.of(
        "api-request", "{ \"status\": \"success\", \"data\": \"mock_data\" }",
        "llm-call", "Это тестовый ответ от LLM модели",
        "nlu-parse", "{ \"intent\": \"greeting\", \"confidence\": 0.95 }"
    );
    
    public ExecutionResult executeTestScenario(UUID scenarioId, List<String> userInputs) {
        log.info("Executing test scenario: {} with inputs: {}", scenarioId, userInputs);
        
        // Получаем сценарий из тестовой среды
        Scenario scenario = scenarioService.getScenario(scenarioId, "test");
        
        // Создаем тестовый контекст
        ExecutionContext testContext = createTestContext(userInputs);
        
        // Выполняем с моками
        return executeWithMocks(scenario, testContext);
    }
    
    private ExecutionResult executeWithMocks(Scenario scenario, ExecutionContext context) {
        // Заменяем все внешние вызовы на моки
        for (ScenarioBlock block : scenario.getBlocks()) {
            if (requiresExternalCall(block.getBlockType())) {
                String mockResponse = mockResponses.get(block.getBlockType());
                context.addVariable("mock_" + block.getBlockId(), mockResponse);
            }
        }
        
        return scenarioEngine.execute(scenario, context);
    }
}
```

## Обновленный Frontend

### 1. Каталог сценариев с расширенным поиском

```typescript
// ScenarioCatalog.tsx
export const ScenarioCatalog: React.FC = () => {
    const [searchCriteria, setSearchCriteria] = useState<ScenarioSearchCriteria>({
        query: '',
        language: '',
        category: '',
        author: '',
        blockTypes: [],
        complexityLevel: null,
        createdAfter: null,
        createdBefore: null
    });
    
    const { data: scenarios, isLoading } = useQuery(
        ['scenarios', searchCriteria],
        () => scenarioApi.search(searchCriteria)
    );
    
    return (
        <Box>
            <Typography variant="h4" gutterBottom>
                Каталог сценариев
            </Typography>
            
            {/* Расширенные фильтры */}
            <Paper sx={{ p: 2, mb: 2 }}>
                <Grid container spacing={2}>
                    <Grid item xs={12} md={4}>
                        <TextField
                            fullWidth
                            label="Поиск по названию и описанию"
                            value={searchCriteria.query}
                            onChange={(e) => setSearchCriteria(prev => ({
                                ...prev,
                                query: e.target.value
                            }))}
                        />
                    </Grid>
                    
                    <Grid item xs={12} md={2}>
                        <FormControl fullWidth>
                            <InputLabel>Язык</InputLabel>
                            <Select
                                value={searchCriteria.language}
                                onChange={(e) => setSearchCriteria(prev => ({
                                    ...prev,
                                    language: e.target.value
                                }))}
                            >
                                <MenuItem value="">Все</MenuItem>
                                <MenuItem value="uk">Українська</MenuItem>
                                <MenuItem value="en">English</MenuItem>
                            </Select>
                        </FormControl>
                    </Grid>
                    
                    <Grid item xs={12} md={2}>
                        <FormControl fullWidth>
                            <InputLabel>Категория</InputLabel>
                            <Select
                                value={searchCriteria.category}
                                onChange={(e) => setSearchCriteria(prev => ({
                                    ...prev,
                                    category: e.target.value
                                }))}
                            >
                                <MenuItem value="">Все</MenuItem>
                                <MenuItem value="customer-service">Клиентский сервис</MenuItem>
                                <MenuItem value="sales">Продажи</MenuItem>
                                <MenuItem value="support">Поддержка</MenuItem>
                                <MenuItem value="onboarding">Онбординг</MenuItem>
                            </Select>
                        </FormControl>
                    </Grid>
                    
                    <Grid item xs={12} md={2}>
                        <FormControl fullWidth>
                            <InputLabel>Сложность</InputLabel>
                            <Select
                                value={searchCriteria.complexityLevel || ''}
                                onChange={(e) => setSearchCriteria(prev => ({
                                    ...prev,
                                    complexityLevel: e.target.value ? Number(e.target.value) : null
                                }))}
                            >
                                <MenuItem value="">Любая</MenuItem>
                                <MenuItem value={1}>Простая</MenuItem>
                                <MenuItem value={2}>Средняя</MenuItem>
                                <MenuItem value={3}>Сложная</MenuItem>
                                <MenuItem value={4}>Очень сложная</MenuItem>
                                <MenuItem value={5}>Экспертная</MenuItem>
                            </Select>
                        </FormControl>
                    </Grid>
                    
                    <Grid item xs={12} md={2}>
                        <Autocomplete
                            multiple
                            options={BLOCK_TYPES}
                            value={searchCriteria.blockTypes}
                            onChange={(_, newValue) => setSearchCriteria(prev => ({
                                ...prev,
                                blockTypes: newValue
                            }))}
                            renderInput={(params) => (
                                <TextField {...params} label="Типы блоков" />
                            )}
                        />
                    </Grid>
                </Grid>
            </Paper>
            
            {/* Результаты поиска */}
            <ScenarioGrid scenarios={scenarios} loading={isLoading} />
        </Box>
    );
};
```

### 2. Тестовый чат

```typescript
// TestChat.tsx
export const TestChat: React.FC<{ scenarioId: string }> = ({ scenarioId }) => {
    const [messages, setMessages] = useState<ChatMessage[]>([]);
    const [currentInput, setCurrentInput] = useState('');
    const [isExecuting, setIsExecuting] = useState(false);
    
    const executeTestScenario = async (userInputs: string[]) => {
        setIsExecuting(true);
        try {
            const result = await testChatApi.executeScenario(scenarioId, userInputs);
            
            // Добавляем сообщения из результата выполнения
            const newMessages = result.messages.map((msg, index) => ({
                id: `msg-${index}`,
                text: msg.content,
                sender: msg.direction === 'outgoing' ? 'bot' : 'user',
                timestamp: new Date()
            }));
            
            setMessages(prev => [...prev, ...newMessages]);
        } catch (error) {
            console.error('Test execution failed:', error);
        } finally {
            setIsExecuting(false);
        }
    };
    
    return (
        <Paper sx={{ height: 600, display: 'flex', flexDirection: 'column' }}>
            <Box sx={{ p: 2, borderBottom: 1, borderColor: 'divider' }}>
                <Typography variant="h6">
                    Тестовый чат
                    <Chip 
                        label="ТЕСТ" 
                        color="warning" 
                        size="small" 
                        sx={{ ml: 1 }} 
                    />
                </Typography>
            </Box>
            
            <Box sx={{ flex: 1, overflow: 'auto', p: 1 }}>
                {messages.map((message) => (
                    <ChatBubble key={message.id} message={message} />
                ))}
                {isExecuting && <TypingIndicator />}
            </Box>
            
            <Box sx={{ p: 2, borderTop: 1, borderColor: 'divider' }}>
                <TextField
                    fullWidth
                    placeholder="Введите сообщение для тестирования..."
                    value={currentInput}
                    onChange={(e) => setCurrentInput(e.target.value)}
                    onKeyPress={(e) => {
                        if (e.key === 'Enter' && !isExecuting) {
                            executeTestScenario([currentInput]);
                            setCurrentInput('');
                        }
                    }}
                    disabled={isExecuting}
                    InputProps={{
                        endAdornment: (
                            <IconButton 
                                onClick={() => {
                                    executeTestScenario([currentInput]);
                                    setCurrentInput('');
                                }}
                                disabled={isExecuting || !currentInput.trim()}
                            >
                                <SendIcon />
                            </IconButton>
                        )
                    }}
                />
            </Box>
        </Paper>
    );
};
```

## Обновленная схема развертывания

### Multi-region Kubernetes

```yaml
# ukraine-central region
apiVersion: v1
kind: ConfigMap
metadata:
  name: region-config
  namespace: chatbot-platform
data:
  REGION: "ukraine-central"
  IS_PRIMARY: "true"
  BACKUP_REGIONS: "eu-west-1,us-east-1"
  
---
# Geo-aware load balancer
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: geo-loadbalancer
  annotations:
    nginx.ingress.kubernetes.io/server-snippet: |
      set $region "ukraine-central";
      if ($geoip_country_code = "US") {
        set $region "us-east-1";
      }
      if ($geoip_country_code ~* "^(DE|FR|GB|IT|ES)$") {
        set $region "eu-west-1";
      }
spec:
  rules:
  - host: chatbot-platform.com
    http:
      paths:
      - path: /
        pathType: Prefix
        backend:
          service:
            name: geo-router-service
            port:
              number: 80
```

Эта обновленная архитектура полностью соответствует новому ТЗ и готова к реализации миллиона сценариев с гео-резервированием и упрощенной моделью ролей.
