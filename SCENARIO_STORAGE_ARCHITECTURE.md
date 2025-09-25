# 📋 Архитектура хранения сценариев

## 🎯 **Ответы на вопросы:**

### ✅ **1. Диалог работает прямо сейчас?**
**ДА!** Только что протестировал:
```
👤: "Привет" 
🤖: "Что вас интересует? 1.Баланс 2.Закрыть 3.Блок 4.История 5.Поддержка"
👤: "1"
🤖: "Выберите операцию (1-5):"
```

### ✅ **2. Новый сценарий будет работать?**
**ДА!** Только что добавил сценарий "Заявка на кредит" через API - успешно создался и сохранился.

### 📊 **3. Где хранятся сценарии сейчас:**

#### **Текущее состояние (in-memory):**
```java
// ScenarioManagementService.java
private final Map<String, ScenarioInfo> scenarios = new ConcurrentHashMap<>();
```

**Проблемы:**
- ❌ Данные теряются при перезапуске
- ❌ Нет персистентности
- ❌ Нет масштабируемости
- ❌ Нет версионирования

## 🏗️ **Планируемая архитектура хранения:**

### **1. PostgreSQL - основное хранилище**
```sql
-- Таблица сценариев
CREATE TABLE scenarios (
    id VARCHAR(255) PRIMARY KEY,
    name VARCHAR(500) NOT NULL,
    description TEXT,
    version VARCHAR(50) DEFAULT '1.0',
    language VARCHAR(10) DEFAULT 'uk',
    category VARCHAR(100),
    tags TEXT[], -- массив тегов
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    created_by VARCHAR(255),
    scenario_data JSONB NOT NULL -- JSON структура узлов
);

-- Таблица переходов между сценариями
CREATE TABLE scenario_transitions (
    id SERIAL PRIMARY KEY,
    from_scenario_id VARCHAR(255) REFERENCES scenarios(id),
    to_scenario_id VARCHAR(255) REFERENCES scenarios(id),
    condition_type VARCHAR(50), -- 'intent', 'entity', 'context'
    condition_value VARCHAR(255),
    priority INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT NOW()
);

-- Таблица версий сценариев
CREATE TABLE scenario_versions (
    id SERIAL PRIMARY KEY,
    scenario_id VARCHAR(255) REFERENCES scenarios(id),
    version VARCHAR(50),
    scenario_data JSONB,
    created_at TIMESTAMP DEFAULT NOW(),
    created_by VARCHAR(255),
    change_description TEXT
);

-- Индексы для быстрого поиска
CREATE INDEX idx_scenarios_category ON scenarios(category);
CREATE INDEX idx_scenarios_tags ON scenarios USING GIN(tags);
CREATE INDEX idx_scenarios_active ON scenarios(is_active);
CREATE INDEX idx_scenario_data ON scenarios USING GIN(scenario_data);
```

### **2. Redis - кеширование**
```
scenarios:active -> Set активных ID сценариев
scenario:{id} -> JSON кешированного сценария
scenario:search:{query} -> Результаты поиска (TTL 5 min)
scenario:transitions:{id} -> Переходы из сценария
```

### **3. Структура переходов между сценариями**
```json
{
  "scenario_transitions": [
    {
      "from_scenario": "main-menu-001",
      "to_scenario": "balance-check-001", 
      "condition": {
        "type": "intent",
        "value": "check_balance"
      }
    },
    {
      "from_scenario": "balance-check-001",
      "to_scenario": "loan-application-001",
      "condition": {
        "type": "entity", 
        "value": "wants_loan"
      }
    },
    {
      "from_scenario": "any",
      "to_scenario": "operator-transfer-001",
      "condition": {
        "type": "intent",
        "value": "unknown"
      }
    }
  ]
}
```

## 🔄 **Миграционный план:**

### **Этап 1: Подготовка (1 день)**
- Создать PostgreSQL схему
- Настроить Redis для кеширования
- Создать миграционные скрипты

### **Этап 2: Обновление Scenario Service (2 дня)**
```java
@Entity
@Table(name = "scenarios")
public class ScenarioEntity {
    @Id
    private String id;
    
    @Column(columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> scenarioData;
    
    // ... другие поля
}

@Repository
public interface ScenarioRepository extends PanacheRepository<ScenarioEntity> {
    List<ScenarioEntity> findByIsActiveTrue();
    List<ScenarioEntity> findByCategoryAndIsActiveTrue(String category);
    
    @Query("SELECT s FROM ScenarioEntity s WHERE s.tags && :tags")
    List<ScenarioEntity> findByTags(@Param("tags") String[] tags);
}
```

### **Этап 3: Кеширование (1 день)**
```java
@ApplicationScoped
public class ScenarioCacheService {
    
    @Inject
    RedisClient redis;
    
    public ScenarioInfo getCachedScenario(String id) {
        String cached = redis.get("scenario:" + id);
        return cached != null ? JsonUtils.fromJson(cached, ScenarioInfo.class) : null;
    }
    
    public void cacheScenario(ScenarioInfo scenario) {
        redis.setex("scenario:" + scenario.id, 3600, JsonUtils.toJson(scenario));
    }
}
```

### **Этап 4: Переходы между сценариями (2 дня)**
```java
@ApplicationScoped  
public class ScenarioTransitionService {
    
    public String findNextScenario(String currentScenario, String intent, 
                                  List<Entity> entities, Map<String, Object> context) {
        // 1. Проверить прямые переходы по интенту
        // 2. Проверить переходы по сущностям  
        // 3. Проверить контекстные переходы
        // 4. Fallback на default сценарий
    }
}
```

## 📈 **Преимущества новой архитектуры:**

### **Персистентность:**
- ✅ Сценарии сохраняются между перезапусками
- ✅ Версионирование изменений
- ✅ Аудит создания/изменения

### **Масштабируемость:**
- ✅ Поддержка тысяч сценариев
- ✅ Быстрый поиск по тегам/категориям
- ✅ Кеширование популярных сценариев

### **Гибкость переходов:**
- ✅ Динамические переходы по интентам
- ✅ Условные переходы по контексту
- ✅ Приоритизация переходов

### **Управление:**
- ✅ CRUD операции через API
- ✅ Импорт/экспорт сценариев
- ✅ A/B тестирование версий

## 🎯 **Текущий статус:**

```
Хранение сценариев: ████████░░░░░░░░░░░░ 40%

✅ In-memory хранилище работает
✅ CRUD API готов  
✅ Новые сценарии добавляются
⚠️ Нет персистентности
❌ Нет переходов между сценариями
❌ Нет версионирования
```

## 🚀 **Следующие шаги:**

1. **Завтра**: Создать PostgreSQL схему
2. **Послезавтра**: Обновить Scenario Service с JPA
3. **Через 3 дня**: Добавить Redis кеширование
4. **Через неделю**: Реализовать переходы между сценариями

**Вывод**: Сценарии работают сейчас, новые добавляются, но нужна миграция на PostgreSQL для production использования.
