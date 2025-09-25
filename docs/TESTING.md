# План тестирования платформы чат-ботов

## Обзор стратегии тестирования

Комплексная стратегия тестирования включает несколько уровней для обеспечения качества и надежности платформы.

## Пирамида тестирования

### 1. Unit тесты (70%)
**Цель**: Тестирование отдельных компонентов и функций

**Покрытие**:
- Бизнес-логика сервисов
- Утилитарные функции
- Валидация данных
- Обработка ошибок

**Инструменты**:
- JUnit 5 для Java
- Jest для TypeScript/JavaScript
- Mockito для мокирования

**Примеры тестов**:
```java
@Test
void shouldCreateDialogForNewUser() {
    // Given
    UUID chatbotId = UUID.randomUUID();
    String userIdentifier = "user123";
    
    // When
    Dialog dialog = dialogService.getOrCreateDialog(chatbotId, userIdentifier, "web");
    
    // Then
    assertThat(dialog).isNotNull();
    assertThat(dialog.getUserIdentifier()).isEqualTo(userIdentifier);
    assertThat(dialog.getStatus()).isEqualTo(Dialog.DialogStatus.ACTIVE);
}
```

### 2. Integration тесты (20%)
**Цель**: Тестирование взаимодействия между компонентами

**Покрытие**:
- API endpoints
- Взаимодействие с базой данных
- Интеграция между сервисами
- Обработка сообщений

**Инструменты**:
- Spring Boot Test
- TestContainers для БД
- WireMock для внешних API
- React Testing Library

**Примеры тестов**:
```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class ChatServiceIntegrationTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");
    
    @Test
    void shouldProcessMessageAndUpdateDialog() {
        // Integration test implementation
    }
}
```

### 3. End-to-End тесты (10%)
**Цель**: Тестирование полных пользовательских сценариев

**Покрытие**:
- Создание и редактирование сценариев
- Выполнение диалогов
- Интеграция с внешними каналами
- Аналитика и отчеты

**Инструменты**:
- Playwright для веб-тестирования
- Postman/Newman для API тестов
- K6 для нагрузочного тестирования

## Типы тестирования

### Функциональное тестирование

#### 1. API тестирование
```javascript
// Postman/Newman тест
pm.test("Create chatbot", function () {
    const responseJson = pm.response.json();
    pm.expect(pm.response.code).to.equal(201);
    pm.expect(responseJson).to.have.property('id');
    pm.expect(responseJson.name).to.equal('Test Bot');
});
```

#### 2. UI тестирование
```typescript
// Playwright тест
test('should create new scenario', async ({ page }) => {
    await page.goto('/scenarios');
    await page.click('[data-testid="create-scenario-btn"]');
    await page.fill('[data-testid="scenario-name"]', 'Test Scenario');
    await page.click('[data-testid="save-btn"]');
    
    await expect(page.locator('[data-testid="scenario-list"]')).toContainText('Test Scenario');
});
```

#### 3. Сценарное тестирование
```javascript
// Тест выполнения сценария
describe('Scenario Execution', () => {
    test('should execute announce block', async () => {
        const scenario = {
            blocks: [
                { id: 'start', type: 'announce', config: { message: 'Hello!' } }
            ]
        };
        
        const result = await orchestrator.executeScenario(scenario, context);
        expect(result.response).toBe('Hello!');
    });
});
```

### Нефункциональное тестирование

#### 1. Нагрузочное тестирование
```javascript
// K6 нагрузочный тест
import http from 'k6/http';
import { check } from 'k6';

export let options = {
    stages: [
        { duration: '2m', target: 100 },
        { duration: '5m', target: 100 },
        { duration: '2m', target: 200 },
        { duration: '5m', target: 200 },
        { duration: '2m', target: 0 },
    ],
};

export default function () {
    let response = http.post('http://api-gateway/api/v1/chat/message', {
        chatbotId: 'test-bot-id',
        message: 'Hello',
        userIdentifier: 'load-test-user'
    });
    
    check(response, {
        'status is 200': (r) => r.status === 200,
        'response time < 500ms': (r) => r.timings.duration < 500,
    });
}
```

#### 2. Тестирование производительности
- **Время отклика**: < 200ms для API, < 2s для UI
- **Пропускная способность**: 1000 RPS на сервис
- **Использование ресурсов**: < 80% CPU, < 70% Memory

#### 3. Тестирование безопасности
```javascript
// Тест аутентификации
test('should reject unauthorized requests', async () => {
    const response = await fetch('/api/v1/scenarios', {
        method: 'GET'
    });
    
    expect(response.status).toBe(401);
});

// Тест SQL инъекций
test('should prevent SQL injection', async () => {
    const maliciousInput = "'; DROP TABLE users; --";
    const response = await createUser({ name: maliciousInput });
    
    expect(response.status).toBe(400);
});
```

## Тестовые данные

### Фикстуры для тестов
```sql
-- test-data.sql
INSERT INTO users (id, username, email, role) VALUES 
('550e8400-e29b-41d4-a716-446655440000', 'testuser', 'test@example.com', 'USER');

INSERT INTO organizations (id, name, owner_id) VALUES 
('550e8400-e29b-41d4-a716-446655440001', 'Test Org', '550e8400-e29b-41d4-a716-446655440000');

INSERT INTO chatbots (id, organization_id, name, language) VALUES 
('550e8400-e29b-41d4-a716-446655440002', '550e8400-e29b-41d4-a716-446655440001', 'Test Bot', 'uk');
```

### Генерация тестовых данных
```java
@Component
public class TestDataGenerator {
    
    public Scenario createTestScenario() {
        return Scenario.builder()
                .name("Test Scenario")
                .blocks(List.of(
                    createAnnounceBlock("start", "Привет!"),
                    createAskBlock("question", "Как дела?", "user_response"),
                    createConditionBlock("condition", "user_response", "equals", "хорошо")
                ))
                .build();
    }
}
```

## Автоматизация тестирования

### CI/CD Pipeline
```yaml
# .github/workflows/test.yml
name: Test Pipeline

on: [push, pull_request]

jobs:
  unit-tests:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-java@v3
        with:
          java-version: '17'
      - name: Run unit tests
        run: mvn test
      
  integration-tests:
    runs-on: ubuntu-latest
    services:
      postgres:
        image: postgres:15
        env:
          POSTGRES_PASSWORD: test
        options: >-
          --health-cmd pg_isready
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5
    steps:
      - uses: actions/checkout@v3
      - name: Run integration tests
        run: mvn verify -P integration-tests
        
  e2e-tests:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Start services
        run: docker-compose up -d
      - name: Run E2E tests
        run: npm run test:e2e
```

### Тестовые окружения

#### 1. Локальное окружение
- Docker Compose с тестовыми данными
- Моки внешних сервисов
- Быстрая обратная связь для разработчиков

#### 2. Staging окружение
- Полная копия продакшена
- Реальные интеграции (с тестовыми ключами)
- Автоматическое развертывание из main ветки

#### 3. Production окружение
- Smoke тесты после развертывания
- Мониторинг критических путей
- Canary deployments

## Метрики качества

### Покрытие кода
- **Цель**: > 80% для unit тестов
- **Инструменты**: JaCoCo для Java, Istanbul для JavaScript
- **Отчеты**: Интеграция с SonarQube

### Качество тестов
- **Mutation testing** для проверки качества тестов
- **Test smells** детекция
- **Flaky tests** мониторинг

### SLA для тестирования
- **Unit тесты**: < 5 минут
- **Integration тесты**: < 15 минут
- **E2E тесты**: < 30 минут
- **Нагрузочные тесты**: < 60 минут

## Специфичные тесты для платформы

### 1. Тестирование сценариев
```java
@Test
void shouldExecuteComplexScenario() {
    // Создание сложного сценария с условиями и подсценариями
    Scenario scenario = createComplexScenario();
    
    // Симуляция пользовательского ввода
    List<String> userInputs = List.of("Привет", "Хорошо", "Да");
    
    // Выполнение сценария
    ExecutionResult result = scenarioExecutor.execute(scenario, userInputs);
    
    // Проверка результата
    assertThat(result.isCompleted()).isTrue();
    assertThat(result.getMessages()).hasSize(6); // 3 вопроса + 3 ответа
}
```

### 2. Тестирование JavaScript модулей
```java
@Test
void shouldExecuteJavaScriptModule() {
    String jsCode = """
        function processData(input) {
            return {
                processed: true,
                result: input.toUpperCase()
            };
        }
        """;
    
    Module module = Module.builder()
            .scriptContent(jsCode)
            .build();
    
    Map<String, Object> input = Map.of("input", "hello");
    Map<String, Object> result = moduleExecutor.execute(module, input);
    
    assertThat(result.get("result")).isEqualTo("HELLO");
}
```

### 3. Тестирование многоканальности
```java
@Test
void shouldHandleMultipleChannels() {
    // Тест обработки сообщений из разных каналов
    UUID chatbotId = UUID.randomUUID();
    
    // Telegram
    processMessage(chatbotId, "telegram_user_123", "telegram", "Привет");
    
    // Web
    processMessage(chatbotId, "web_user_456", "web", "Hello");
    
    // Viber
    processMessage(chatbotId, "viber_user_789", "viber", "Привіт");
    
    // Проверка, что все диалоги созданы корректно
    List<Dialog> dialogs = dialogRepository.findByChatbotId(chatbotId);
    assertThat(dialogs).hasSize(3);
}
```

## Мониторинг тестов в продакшене

### Synthetic мониторинг
```javascript
// Синтетический тест для мониторинга
const syntheticTest = async () => {
    try {
        // Создание тестового диалога
        const response = await fetch('/api/v1/chat/message', {
            method: 'POST',
            body: JSON.stringify({
                chatbotId: 'monitoring-bot',
                message: 'health-check',
                userIdentifier: 'synthetic-user'
            })
        });
        
        if (response.status !== 200) {
            throw new Error(`API returned ${response.status}`);
        }
        
        const data = await response.json();
        if (!data.response) {
            throw new Error('No response from bot');
        }
        
        return { success: true, responseTime: Date.now() - startTime };
    } catch (error) {
        return { success: false, error: error.message };
    }
};
```

### Chaos Engineering
```yaml
# Chaos Monkey для Kubernetes
apiVersion: v1
kind: ConfigMap
metadata:
  name: chaos-monkey-config
data:
  config.yaml: |
    chaosMonkey:
      enabled: true
      schedule:
        enabled: true
        frequency: 30
      assaults:
        level: 5
        latencyActive: true
        exceptionsActive: true
        killApplicationActive: true
```

## Отчетность

### Дашборд тестирования
- **Test execution trends**
- **Coverage metrics**
- **Flaky test detection**
- **Performance regression tracking**

### Интеграция с инструментами
- **JIRA** для связи тестов с требованиями
- **Slack** для уведомлений о результатах
- **Grafana** для визуализации метрик

## Заключение

Комплексная стратегия тестирования обеспечивает:
- **Высокое качество** кода и функциональности
- **Быструю обратную связь** для разработчиков
- **Уверенность** в стабильности релизов
- **Автоматизацию** процессов контроля качества
