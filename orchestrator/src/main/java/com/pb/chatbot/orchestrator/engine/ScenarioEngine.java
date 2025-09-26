package com.pb.chatbot.orchestrator.engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pb.chatbot.orchestrator.model.Scenario;
import com.pb.chatbot.orchestrator.model.ScenarioBlock;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@ApplicationScoped
public class ScenarioEngine {
    
    private static final Logger LOG = Logger.getLogger(ScenarioEngine.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    public Map<String, Object> executeScenario(Scenario scenario, String userInput, Map<String, Object> context) {
        LOG.infof("Executing scenario: %s", scenario.name);
        
        if (context == null) {
            context = new HashMap<>();
        }
        
        String currentNodeId = (String) context.getOrDefault("current_node", scenario.startNode);
        ScenarioBlock currentNode = scenario.findNode(currentNodeId);
        
        if (currentNode == null) {
            return createErrorResponse("Node not found: " + currentNodeId);
        }
        
        return executeBlock(currentNode, userInput, context, scenario);
    }
    
    private Map<String, Object> executeBlock(ScenarioBlock block, String userInput, 
                                           Map<String, Object> context, Scenario scenario) {
        LOG.debugf("Executing block: %s (%s)", block.id, block.type);
        
        switch (block.type) {
            case "announce":
                return executeAnnounce(block, context);
            case "ask":
                return executeAsk(block, context);
            case "parse":
                return executeParse(block, userInput, context);
            case "condition":
                return executeCondition(block, context, scenario);
            case "wait":
                return executeWait(block, context);
            case "nlu-request":
                return executeNluRequest(block, userInput, context, scenario);
            default:
                return createErrorResponse("Unknown block type: " + block.type);
        }
    }
    
    private Map<String, Object> executeAnnounce(ScenarioBlock block, Map<String, Object> context) {
        String message = (String) block.parameters.get("message");
        Integer delay = (Integer) block.parameters.getOrDefault("delay", 0);
        
        // Подстановка переменных из контекста с поддержкой JSON path
        if (message != null) {
            message = replaceVariables(message, context);
        }
        
        if (delay > 0) {
            try {
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        return createResponse("announce", message, getNextNode(block), context);
    }
    
    private Map<String, Object> executeAsk(ScenarioBlock block, Map<String, Object> context) {
        String question = "Введите ответ:";
        if (block.parameters != null && block.parameters.containsKey("question")) {
            question = (String) block.parameters.get("question");
        }
        
        context.put("waiting_for_input", true);
        context.put("input_type", "text");
        context.put("required", true);
        
        // ВАЖНО: устанавливаем следующий узел для обработки ответа пользователя
        String nextNodeId = getNextNode(block);
        context.put("current_node", nextNodeId);
        
        return createResponse("ask", question, nextNodeId, context);
    }
    
    private Map<String, Object> executeParse(ScenarioBlock block, String userInput, Map<String, Object> context) {
        String script = (String) block.parameters.get("script");
        
        // Простая заглушка для парсинга
        boolean parseResult = userInput != null && !userInput.trim().isEmpty();
        
        if (script != null) {
            // Простое извлечение переменных
            if (script.contains("context.userName")) {
                context.put("userName", userInput);
            }
            if (script.contains("context.userAnswer")) {
                context.put("userAnswer", userInput.toLowerCase());
            }
            if (script.contains("context.wantsBalance")) {
                boolean wantsBalance = userInput.toLowerCase().contains("да") || 
                                     userInput.toLowerCase().contains("yes");
                context.put("wantsBalance", wantsBalance);
            }
            if (script.contains("context.cardNumber")) {
                context.put("cardNumber", userInput);
                boolean validCard = userInput.length() == 4 && userInput.matches("\\d+");
                context.put("validCard", validCard);
            }
        }
        
        context.put("parse_result", parseResult);
        context.put("user_input", userInput);
        
        String nextNode = parseResult ? getNextNode(block) : 
                         block.conditions != null ? block.conditions.get("error") : null;
        
        return createResponse("parse", "Parsed: " + userInput, nextNode, context);
    }
    
    private Map<String, Object> executeCondition(ScenarioBlock block, Map<String, Object> context, Scenario scenario) {
        String condition = (String) block.parameters.get("condition");
        
        // Простая логика условий
        boolean conditionResult = evaluateCondition(condition, context);
        
        String nextNode = conditionResult ? 
                         block.conditions.get("true") : 
                         block.conditions.get("false");
        
        return createResponse("condition", "Condition: " + conditionResult, nextNode, context);
    }
    
    private Map<String, Object> executeWait(ScenarioBlock block, Map<String, Object> context) {
        Integer duration = (Integer) block.parameters.getOrDefault("duration", 1000);
        
        try {
            Thread.sleep(duration);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        return createResponse("wait", "Waited " + duration + "ms", getNextNode(block), context);
    }
    
    private boolean evaluateCondition(String condition, Map<String, Object> context) {
        if (condition == null) return true;
        
        // Простые условия
        if (condition.contains("parse_result")) {
            return (Boolean) context.getOrDefault("parse_result", false);
        }
        
        if (condition.contains("context.wantsBalance == true")) {
            return (Boolean) context.getOrDefault("wantsBalance", false);
        }
        
        if (condition.contains("context.validCard == true")) {
            return (Boolean) context.getOrDefault("validCard", false);
        }
        
        return true;
    }
    
    private String getNextNode(ScenarioBlock block) {
        return block.nextNodes != null && !block.nextNodes.isEmpty() ? 
               block.nextNodes.get(0) : null;
    }
    
    private ScenarioBlock findBlockById(Scenario scenario, String blockId) {
        if (scenario.nodes != null) {
            for (ScenarioBlock block : scenario.nodes) {
                if (blockId.equals(block.id)) {
                    return block;
                }
            }
        }
        return null;
    }
    
    private Map<String, Object> createResponse(String type, String message, String nextNode, Map<String, Object> context) {
        Map<String, Object> response = new HashMap<>();
        response.put("type", type);
        response.put("message", message);
        response.put("next_node", nextNode);
        response.put("context", context);
        response.put("timestamp", System.currentTimeMillis());
        return response;
    }
    
    private Map<String, Object> executeNluRequest(ScenarioBlock block, String userInput, 
                                                 Map<String, Object> context, Scenario scenario) {
        LOG.debugf("Executing NLU request for input: %s", userInput);
        
        // Простое определение интентов по ключевым словам
        String intent = "unknown";
        if (userInput != null && userInput.toLowerCase().contains("баланс")) {
            intent = "check_balance";
        } else if (userInput != null && userInput.toLowerCase().contains("карт")) {
            intent = "card_info";
        } else if (userInput != null && userInput.toLowerCase().contains("перевод")) {
            intent = "transfer";
        }
        
        // Сохраняем результаты в контекст
        context.put("intent", intent);
        context.put("entities", new java.util.ArrayList<>());
        context.put("confidence", 0.8);
        
        LOG.debugf("NLU analysis completed. Intent: %s", intent);
        
        // Переходим к следующему узлу через success условие
        String nextNodeId = null;
        if (block.conditions != null && block.conditions.containsKey("success")) {
            nextNodeId = (String) block.conditions.get("success");
        }
        if (nextNodeId == null) {
            nextNodeId = getNextNode(block);
        }
        
        context.put("current_node", nextNodeId);
        
        // ВАЖНО: Сразу выполняем следующий узел
        if (nextNodeId != null) {
            ScenarioBlock nextBlock = findBlockById(scenario, nextNodeId);
            if (nextBlock != null) {
                LOG.debugf("Continuing to next node after NLU: %s (%s)", nextNodeId, nextBlock.type);
                return executeBlock(nextBlock, userInput, context, scenario);
            }
        }
        
        return createResponse("nlu-request", "NLU analysis completed", nextNodeId, context);
    }
    
    private Map<String, Object> createErrorResponse(String error) {
        Map<String, Object> response = new HashMap<>();
        response.put("type", "error");
        response.put("error", error);
        response.put("timestamp", System.currentTimeMillis());
        return response;
    }
    
    private String replaceVariables(String text, Map<String, Object> context) {
        if (text == null) return null;
        
        String result = text;
        Pattern pattern = Pattern.compile("\\{context\\.([^}]+)\\}");
        Matcher matcher = pattern.matcher(text);
        
        while (matcher.find()) {
            String path = matcher.group(1);
            Object value = getValueByPath(context, path);
            if (value != null) {
                result = result.replace(matcher.group(0), String.valueOf(value));
            }
        }
        
        return result;
    }
    
    private Object getValueByPath(Map<String, Object> context, String path) {
        String[] parts = path.split("\\.");
        Object current = context.get(parts[0]);
        
        // Если это JSON строка - парсим
        if (current instanceof String && ((String) current).startsWith("{")) {
            try {
                current = objectMapper.readValue((String) current, Map.class);
            } catch (Exception e) {
                LOG.warnf("Failed to parse JSON: %s", e.getMessage());
                return current; // Возвращаем как есть если не JSON
            }
        }
        
        // Проходим по пути
        for (int i = 1; i < parts.length && current != null; i++) {
            if (current instanceof Map) {
                current = ((Map<?, ?>) current).get(parts[i]);
            } else {
                return null;
            }
        }
        
        return current;
    }
}
