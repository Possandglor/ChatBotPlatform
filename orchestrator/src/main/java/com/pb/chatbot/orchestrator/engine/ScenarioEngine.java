package com.pb.chatbot.orchestrator.engine;

import com.pb.chatbot.orchestrator.model.Scenario;
import com.pb.chatbot.orchestrator.model.ScenarioBlock;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import java.util.HashMap;
import java.util.Map;

@ApplicationScoped
public class ScenarioEngine {
    
    private static final Logger LOG = Logger.getLogger(ScenarioEngine.class);
    
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
            default:
                return createErrorResponse("Unknown block type: " + block.type);
        }
    }
    
    private Map<String, Object> executeAnnounce(ScenarioBlock block, Map<String, Object> context) {
        String message = (String) block.parameters.get("message");
        Integer delay = (Integer) block.parameters.getOrDefault("delay", 0);
        
        // Подстановка переменных из контекста
        if (message != null && message.contains("{context.")) {
            if (context.containsKey("cardNumber")) {
                message = message.replace("{context.cardNumber}", (String) context.get("cardNumber"));
            }
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
        String question = (String) block.parameters.get("question");
        String inputType = (String) block.parameters.getOrDefault("inputType", "text");
        Boolean required = (Boolean) block.parameters.getOrDefault("required", false);
        
        context.put("waiting_for_input", true);
        context.put("input_type", inputType);
        context.put("required", required);
        
        return createResponse("ask", question, block.id, context);
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
    
    private Map<String, Object> createResponse(String type, String message, String nextNode, Map<String, Object> context) {
        Map<String, Object> response = new HashMap<>();
        response.put("type", type);
        response.put("message", message);
        response.put("next_node", nextNode);
        response.put("context", context);
        response.put("timestamp", System.currentTimeMillis());
        return response;
    }
    
    private Map<String, Object> createErrorResponse(String error) {
        Map<String, Object> response = new HashMap<>();
        response.put("type", "error");
        response.put("error", error);
        response.put("timestamp", System.currentTimeMillis());
        return response;
    }
}
