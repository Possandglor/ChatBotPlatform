package com.pb.chatbot.orchestrator.service;

import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class ScenarioTransitionService {
    
    private static final Logger LOG = Logger.getLogger(ScenarioTransitionService.class);
    
    // Маппинг переходов между сценариями
    private static final Map<String, Map<String, String>> SCENARIO_TRANSITIONS = new HashMap<>() {{
        put("main-menu-nlu-001", new HashMap<>() {{
            put("check_balance", "balance-check-001");
            put("block_card", "card-blocking-001");
            put("close_account", "account-closure-001");
            put("transfer_money", "money-transfer-001");
            put("get_statement", "statement-request-001");
            put("complaint", "complaint-handling-001");
            put("unknown", "operator-transfer-001");
        }});
        
        put("balance-check-001", new HashMap<>() {{
            put("block_card", "card-blocking-001");
            put("close_account", "account-closure-001");
            put("transfer_money", "money-transfer-001");
            put("default", "main-menu-nlu-001");
        }});
        
        put("card-blocking-001", new HashMap<>() {{
            put("check_balance", "balance-check-001");
            put("close_account", "account-closure-001");
            put("default", "main-menu-nlu-001");
        }});
        
        put("account-closure-001", new HashMap<>() {{
            put("check_balance", "balance-check-001");
            put("block_card", "card-blocking-001");
            put("default", "main-menu-nlu-001");
        }});
    }};
    
    // Fallback сценарии для разных ситуаций
    private static final Map<String, String> FALLBACK_SCENARIOS = new HashMap<>() {{
        put("nlu_error", "operator-transfer-001");
        put("api_error", "error-handling-001");
        put("timeout", "timeout-handling-001");
        put("unknown_intent", "clarification-001");
        put("default", "main-menu-nlu-001");
    }};
    
    /**
     * Найти следующий сценарий на основе текущего сценария и интента
     */
    public String findNextScenario(String currentScenario, String intent, 
                                  List<Object> entities, Map<String, Object> context) {
        LOG.infof("Finding next scenario from %s with intent %s", currentScenario, intent);
        
        // 1. Проверяем прямые переходы по интенту
        Map<String, String> transitions = SCENARIO_TRANSITIONS.get(currentScenario);
        if (transitions != null) {
            String nextScenario = transitions.get(intent);
            if (nextScenario != null) {
                LOG.infof("Found direct transition: %s -> %s", currentScenario, nextScenario);
                return nextScenario;
            }
            
            // Проверяем default переход для текущего сценария
            nextScenario = transitions.get("default");
            if (nextScenario != null) {
                LOG.infof("Using default transition: %s -> %s", currentScenario, nextScenario);
                return nextScenario;
            }
        }
        
        // 2. Проверяем переходы по сущностям
        String entityBasedScenario = findScenarioByEntities(entities, context);
        if (entityBasedScenario != null) {
            LOG.infof("Found entity-based transition: %s", entityBasedScenario);
            return entityBasedScenario;
        }
        
        // 3. Проверяем контекстные переходы
        String contextBasedScenario = findScenarioByContext(context);
        if (contextBasedScenario != null) {
            LOG.infof("Found context-based transition: %s", contextBasedScenario);
            return contextBasedScenario;
        }
        
        // 4. Fallback на основе интента
        String fallbackScenario = getFallbackScenario(intent);
        LOG.infof("Using fallback scenario: %s", fallbackScenario);
        return fallbackScenario;
    }
    
    /**
     * Найти сценарий на основе сущностей
     */
    private String findScenarioByEntities(List<Object> entities, Map<String, Object> context) {
        if (entities == null || entities.isEmpty()) {
            return null;
        }
        
        // Проверяем наличие срочности
        boolean hasUrgency = entities.stream()
            .anyMatch(entity -> entity.toString().contains("urgency"));
        
        if (hasUrgency) {
            return "urgent-handling-001";
        }
        
        // Проверяем подтверждение (да/нет)
        boolean hasConfirmation = entities.stream()
            .anyMatch(entity -> entity.toString().contains("confirmation"));
        
        if (hasConfirmation) {
            String confirmationValue = extractConfirmationValue(entities);
            if ("yes".equals(confirmationValue)) {
                return (String) context.get("pending_scenario");
            } else if ("no".equals(confirmationValue)) {
                return "main-menu-nlu-001";
            }
        }
        
        return null;
    }
    
    /**
     * Найти сценарий на основе контекста
     */
    private String findScenarioByContext(Map<String, Object> context) {
        if (context == null) {
            return null;
        }
        
        // Проверяем есть ли pending операция
        String pendingScenario = (String) context.get("pending_scenario");
        if (pendingScenario != null) {
            return pendingScenario;
        }
        
        // Проверяем уровень ошибок
        Integer errorCount = (Integer) context.get("error_count");
        if (errorCount != null && errorCount > 2) {
            return "operator-transfer-001";
        }
        
        // Проверяем время сессии
        Long sessionStart = (Long) context.get("session_start");
        if (sessionStart != null) {
            long duration = System.currentTimeMillis() - sessionStart;
            if (duration > 30 * 60 * 1000) { // 30 минут
                return "session-timeout-001";
            }
        }
        
        return null;
    }
    
    /**
     * Получить fallback сценарий
     */
    private String getFallbackScenario(String intent) {
        if (intent == null || "unknown".equals(intent)) {
            return FALLBACK_SCENARIOS.get("unknown_intent");
        }
        
        return FALLBACK_SCENARIOS.get("default");
    }
    
    /**
     * Извлечь значение подтверждения из сущностей
     */
    private String extractConfirmationValue(List<Object> entities) {
        for (Object entity : entities) {
            String entityStr = entity.toString();
            if (entityStr.contains("confirmation")) {
                if (entityStr.contains("\"value\":\"yes\"")) {
                    return "yes";
                } else if (entityStr.contains("\"value\":\"no\"")) {
                    return "no";
                }
            }
        }
        return null;
    }
    
    /**
     * Проверить можно ли перейти из одного сценария в другой
     */
    public boolean canTransition(String fromScenario, String toScenario, String intent) {
        Map<String, String> transitions = SCENARIO_TRANSITIONS.get(fromScenario);
        if (transitions == null) {
            return false;
        }
        
        String allowedScenario = transitions.get(intent);
        return toScenario.equals(allowedScenario) || toScenario.equals(transitions.get("default"));
    }
    
    /**
     * Получить все возможные переходы из сценария
     */
    public Map<String, String> getAvailableTransitions(String scenarioId) {
        return SCENARIO_TRANSITIONS.getOrDefault(scenarioId, new HashMap<>());
    }
}
