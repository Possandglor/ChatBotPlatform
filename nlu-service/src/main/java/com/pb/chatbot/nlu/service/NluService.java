package com.pb.chatbot.nlu.service;

import com.pb.chatbot.nlu.model.NluResult;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class NluService {
    
    private static final Logger LOG = Logger.getLogger(NluService.class);
    
    // Динамическое хранилище интентов
    private static final Map<String, Map<String, Object>> DYNAMIC_INTENTS = new ConcurrentHashMap<>();
    
    // Статичные паттерны (fallback)
    private static final Map<String, String> STATIC_INTENT_PATTERNS = new HashMap<>() {{
        put("баланс", "check_balance");
        put("balance", "check_balance");
        put("блок", "block_card");
        put("заблок", "block_card");
        put("закрыть", "close_account");
        put("закрытие", "close_account");
        put("перевод", "transfer_money");
        put("история", "get_statement");
        put("операции", "get_statement");
        put("жалоба", "complaint");
        put("проблема", "complaint");
    }};
    
    // Инициализация базовых интентов
    static {
        initializeDefaultIntents();
    }
    
    private static void initializeDefaultIntents() {
        DYNAMIC_INTENTS.put("greeting", Map.of(
            "id", "greeting",
            "name", "Приветствие",
            "description", "Пользователь здоровается",
            "examples", List.of("Привет", "Здравствуйте", "Добрый день", "Доброе утро"),
            "usage_count", 234
        ));
        
        DYNAMIC_INTENTS.put("check_balance", Map.of(
            "id", "check_balance",
            "name", "Проверка баланса",
            "description", "Пользователь хочет узнать баланс карты",
            "examples", List.of("Хочу проверить баланс", "Какой у меня баланс?", "Сколько денег на карте", "Проверить остаток на счету"),
            "usage_count", 156
        ));
        
        DYNAMIC_INTENTS.put("block_card", Map.of(
            "id", "block_card",
            "name", "Блокировка карты",
            "description", "Пользователь хочет заблокировать карту",
            "examples", List.of("Заблокировать карту", "Нужно срочно заблокировать", "Карта украдена", "Блокировка карты"),
            "usage_count", 89
        ));
        
        DYNAMIC_INTENTS.put("close_account", Map.of(
            "id", "close_account",
            "name", "Закрытие счета",
            "description", "Пользователь хочет закрыть счет",
            "examples", List.of("Хочу закрыть счет", "Закрытие счета", "Закрыть банковский счет"),
            "usage_count", 45
        ));
        
        DYNAMIC_INTENTS.put("transfer_money", Map.of(
            "id", "transfer_money",
            "name", "Перевод денег",
            "description", "Пользователь хочет перевести деньги",
            "examples", List.of("Перевести деньги", "Сделать перевод", "Отправить деньги"),
            "usage_count", 78
        ));
        
        DYNAMIC_INTENTS.put("get_statement", Map.of(
            "id", "get_statement",
            "name", "Выписка по счету",
            "description", "Пользователь запрашивает выписку",
            "examples", List.of("Нужна выписка", "История операций", "Выписка по счету"),
            "usage_count", 67
        ));
        
        DYNAMIC_INTENTS.put("complaint", Map.of(
            "id", "complaint",
            "name", "Жалоба",
            "description", "Пользователь подает жалобу",
            "examples", List.of("Хочу пожаловаться", "У меня проблема", "Жалоба на сервис"),
            "usage_count", 23
        ));
    }
    
    // Методы для управления интентами
    public List<Map<String, Object>> getAllIntents() {
        return new ArrayList<>(DYNAMIC_INTENTS.values());
    }
    
    public String createIntent(String name, String description, List<String> examples) {
        String id = name.toLowerCase().replaceAll("[^a-z0-9]", "_");
        
        DYNAMIC_INTENTS.put(id, Map.of(
            "id", id,
            "name", name,
            "description", description,
            "examples", examples,
            "usage_count", 0
        ));
        
        LOG.infof("Created intent: %s", id);
        return id;
    }
    
    public boolean updateIntent(String id, String name, String description, List<String> examples) {
        if (!DYNAMIC_INTENTS.containsKey(id)) {
            return false;
        }
        
        Map<String, Object> existingIntent = DYNAMIC_INTENTS.get(id);
        int usageCount = (Integer) existingIntent.get("usage_count");
        
        DYNAMIC_INTENTS.put(id, Map.of(
            "id", id,
            "name", name,
            "description", description,
            "examples", examples,
            "usage_count", usageCount
        ));
        
        LOG.infof("Updated intent: %s", id);
        return true;
    }
    
    public boolean deleteIntent(String id) {
        if (DYNAMIC_INTENTS.remove(id) != null) {
            LOG.infof("Deleted intent: %s", id);
            return true;
        }
        return false;
    }
    
    // Сущности
    private static final Map<String, List<String>> ENTITY_PATTERNS = new HashMap<>() {{
        put("yes_answers", List.of("да", "хорошо", "согласен", "окей", "ок", "yes", "конечно", "давайте"));
        put("no_answers", List.of("нет", "не хочу", "против", "no", "отказываюсь", "не надо"));
        put("urgency", List.of("срочно", "быстро", "немедленно", "сейчас"));
        put("card_reasons", List.of("кража", "потеря", "подозрительные", "мошенники"));
    }};
    
    public NluResult analyze(String text, Map<String, Object> context) {
        LOG.infof("Analyzing text: %s", text);
        
        String normalizedText = text.toLowerCase().trim();
        
        NluResult result = new NluResult();
        result.entities = new ArrayList<>();
        result.context = new HashMap<>(context != null ? context : new HashMap<>());
        
        // Определяем интент
        result.intent = detectIntent(normalizedText);
        result.confidence = calculateConfidence(normalizedText, result.intent);
        
        // Определяем предлагаемый сценарий
        result.suggested_scenario = getSuggestedScenario(result.intent);
        
        // Извлекаем сущности
        extractEntities(normalizedText, result);
        
        LOG.infof("Detected intent: %s (confidence: %.2f)", 
                 result.intent, result.confidence);
        
        return result;
    }
    
    private String getSuggestedScenario(String intent) {
        // Сопоставление интентов со сценариями
        Map<String, String> intentToScenario = Map.of(
            "greeting", "main-menu-001",
            "check_balance", "balance-check-001", 
            "transfer_money", "money-transfer-001",
            "get_statement", "statement-001",
            "close_account", "account-close-001",
            "complaint", "complaint-001"
        );
        
        return intentToScenario.getOrDefault(intent, "main-menu-001");
    }
    
    private String detectIntent(String text) {
        // Проверяем приветствия
        if (text.matches(".*(привет|hello|добр|здравств).*")) {
            incrementUsageCount("greeting");
            return "greeting";
        }
        
        // Сначала проверяем точное совпадение с названием интента
        for (Map<String, Object> intent : DYNAMIC_INTENTS.values()) {
            String intentId = (String) intent.get("id");
            if (text.contains(intentId)) {
                incrementUsageCount(intentId);
                return intentId;
            }
        }
        
        // Ищем в динамических интентах по примерам
        for (Map<String, Object> intent : DYNAMIC_INTENTS.values()) {
            String intentId = (String) intent.get("id");
            List<String> examples = (List<String>) intent.get("examples");
            
            for (String example : examples) {
                String[] words = example.toLowerCase().split("\\s+");
                boolean allWordsFound = true;
                
                for (String word : words) {
                    if (!text.contains(word)) {
                        allWordsFound = false;
                        break;
                    }
                }
                
                if (allWordsFound) {
                    incrementUsageCount(intentId);
                    return intentId;
                }
            }
        }
        
        // Fallback на статичные паттерны
        for (Map.Entry<String, String> entry : STATIC_INTENT_PATTERNS.entrySet()) {
            if (text.contains(entry.getKey())) {
                incrementUsageCount(entry.getValue());
                return entry.getValue();
            }
        }
        
        // Если ничего не найдено
        return "unknown";
    }
    
    private void incrementUsageCount(String intentId) {
        Map<String, Object> intent = DYNAMIC_INTENTS.get(intentId);
        if (intent != null) {
            int currentCount = (Integer) intent.get("usage_count");
            Map<String, Object> updatedIntent = new HashMap<>(intent);
            updatedIntent.put("usage_count", currentCount + 1);
            DYNAMIC_INTENTS.put(intentId, updatedIntent);
        }
    }
    
    private double calculateConfidence(String text, String intent) {
        if ("unknown".equals(intent)) {
            return 0.1;
        }
        
        // Для динамических интентов
        Map<String, Object> intentData = DYNAMIC_INTENTS.get(intent);
        if (intentData != null) {
            List<String> examples = (List<String>) intentData.get("examples");
            
            // Ищем наиболее подходящий пример
            double maxConfidence = 0.5;
            for (String example : examples) {
                String[] exampleWords = example.toLowerCase().split("\\s+");
                int matchedWords = 0;
                
                for (String word : exampleWords) {
                    if (text.contains(word)) {
                        matchedWords++;
                    }
                }
                
                double confidence = 0.5 + (double) matchedWords / exampleWords.length * 0.4;
                maxConfidence = Math.max(maxConfidence, confidence);
            }
            
            return Math.min(maxConfidence, 0.95);
        }
        
        // Fallback для статичных паттернов
        long matchCount = STATIC_INTENT_PATTERNS.entrySet().stream()
            .filter(entry -> intent.equals(entry.getValue()))
            .filter(entry -> text.contains(entry.getKey()))
            .count();
        
        return Math.min(0.5 + (matchCount * 0.3), 0.95);
    }
    
    private void extractEntities(String text, NluResult result) {
        // Извлекаем да/нет ответы
        extractYesNoEntities(text, result);
        
        // Извлекаем срочность
        extractUrgencyEntities(text, result);
        
        // Извлекаем причины для карт
        extractCardReasonEntities(text, result);
        
        // Извлекаем числа (суммы, номера карт)
        extractNumberEntities(text, result);
    }
    
    private void extractYesNoEntities(String text, NluResult result) {
        for (String yesWord : ENTITY_PATTERNS.get("yes_answers")) {
            if (text.contains(yesWord)) {
                NluResult.Entity entity = new NluResult.Entity();
                entity.type = "confirmation";
                entity.value = "yes";
                entity.confidence = 0.9;
                entity.start = text.indexOf(yesWord);
                entity.end = entity.start + yesWord.length();
                result.entities.add(entity);
                return;
            }
        }
        
        for (String noWord : ENTITY_PATTERNS.get("no_answers")) {
            if (text.contains(noWord)) {
                NluResult.Entity entity = new NluResult.Entity();
                entity.type = "confirmation";
                entity.value = "no";
                entity.confidence = 0.9;
                entity.start = text.indexOf(noWord);
                entity.end = entity.start + noWord.length();
                result.entities.add(entity);
                return;
            }
        }
    }
    
    private void extractUrgencyEntities(String text, NluResult result) {
        for (String urgencyWord : ENTITY_PATTERNS.get("urgency")) {
            if (text.contains(urgencyWord)) {
                NluResult.Entity entity = new NluResult.Entity();
                entity.type = "urgency";
                entity.value = "high";
                entity.confidence = 0.8;
                entity.start = text.indexOf(urgencyWord);
                entity.end = entity.start + urgencyWord.length();
                result.entities.add(entity);
                break;
            }
        }
    }
    
    private void extractCardReasonEntities(String text, NluResult result) {
        for (String reason : ENTITY_PATTERNS.get("card_reasons")) {
            if (text.contains(reason)) {
                NluResult.Entity entity = new NluResult.Entity();
                entity.type = "card_reason";
                entity.value = reason;
                entity.confidence = 0.85;
                entity.start = text.indexOf(reason);
                entity.end = entity.start + reason.length();
                result.entities.add(entity);
                break;
            }
        }
    }
    
    private void extractNumberEntities(String text, NluResult result) {
        // Ищем 4-значные числа (номера карт)
        if (text.matches(".*\\b\\d{4}\\b.*")) {
            String cardNumber = text.replaceAll(".*\\b(\\d{4})\\b.*", "$1");
            NluResult.Entity entity = new NluResult.Entity();
            entity.type = "card_number";
            entity.value = cardNumber;
            entity.confidence = 0.95;
            result.entities.add(entity);
        }
        
        // Ищем суммы денег
        if (text.matches(".*\\b\\d+\\s*(грн|гривен|рублей|долларов)\\b.*")) {
            String amount = text.replaceAll(".*\\b(\\d+)\\s*(?:грн|гривен|рублей|долларов)\\b.*", "$1");
            NluResult.Entity entity = new NluResult.Entity();
            entity.type = "amount";
            entity.value = amount;
            entity.confidence = 0.9;
            result.entities.add(entity);
        }
    }
}
