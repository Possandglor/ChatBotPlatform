package com.pb.chatbot.orchestrator.engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pb.chatbot.orchestrator.model.Scenario;
import com.pb.chatbot.orchestrator.model.ScenarioBlock;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
            case "context-edit":
                return executeContextEdit(block, context);
            case "calculate":
                return executeCalculate(block, context);
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
        
        // Используем регулярное выражение для поиска всех плейсхолдеров
        Pattern pattern = Pattern.compile("\\{([^}]+)\\}");
        Matcher matcher = pattern.matcher(text);
        
        while (matcher.find()) {
            String fullPlaceholder = matcher.group(0); // {context.api_response.data[0].name}
            String path = matcher.group(1); // context.api_response.data[0].name
            
            Object value = getValueByPath(context, path);
            if (value != null) {
                result = result.replace(fullPlaceholder, String.valueOf(value));
            }
        }
        
        return result;
    }
    
    private Object getValueByPath(Map<String, Object> context, String path) {
        if (path == null || path.isEmpty()) return null;
        
        try {
            // Убираем префикс "context." если есть
            if (path.startsWith("context.")) {
                path = path.substring(8);
            }
            
            // Разбиваем путь на части, учитывая массивы
            String[] parts = parseJsonPath(path);
            Object current = context;
            
            for (String part : parts) {
                if (current == null) return null;
                
                // Проверяем, является ли часть индексом массива [0]
                if (part.matches("\\[\\d+\\]")) {
                    int index = Integer.parseInt(part.substring(1, part.length() - 1));
                    if (current instanceof java.util.List) {
                        java.util.List<?> list = (java.util.List<?>) current;
                        if (index >= 0 && index < list.size()) {
                            current = list.get(index);
                        } else {
                            return null; // Индекс вне границ
                        }
                    } else {
                        return null; // Не массив
                    }
                }
                // Обычное поле объекта
                else {
                    if (current instanceof Map) {
                        Map<?, ?> map = (Map<?, ?>) current;
                        current = map.get(part);
                    } else if (current instanceof String && ((String) current).startsWith("{")) {
                        // Пытаемся распарсить JSON строку
                        try {
                            current = objectMapper.readValue((String) current, Map.class);
                            if (current instanceof Map) {
                                Map<?, ?> map = (Map<?, ?>) current;
                                current = map.get(part);
                            }
                        } catch (Exception e) {
                            LOG.warnf("Failed to parse JSON string: %s", e.getMessage());
                            return null;
                        }
                    } else {
                        return null; // Не объект
                    }
                }
            }
            
            return current;
            
        } catch (Exception e) {
            LOG.warnf("Error extracting value by path '%s': %s", path, e.getMessage());
            return null;
        }
    }
    
    /**
     * Разбирает JSONPath на части, правильно обрабатывая массивы
     * Пример: "api_response.users[0].profile.settings[1].value"
     * Результат: ["api_response", "users", "[0]", "profile", "settings", "[1]", "value"]
     */
    private String[] parseJsonPath(String path) {
        java.util.List<String> parts = new java.util.ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inBrackets = false;
        
        for (char c : path.toCharArray()) {
            if (c == '[') {
                if (current.length() > 0) {
                    parts.add(current.toString());
                    current = new StringBuilder();
                }
                current.append(c);
                inBrackets = true;
            } else if (c == ']') {
                current.append(c);
                parts.add(current.toString());
                current = new StringBuilder();
                inBrackets = false;
            } else if (c == '.' && !inBrackets) {
                if (current.length() > 0) {
                    parts.add(current.toString());
                    current = new StringBuilder();
                }
            } else {
                current.append(c);
            }
        }
        
        if (current.length() > 0) {
            parts.add(current.toString());
        }
        
        return parts.toArray(new String[0]);
    }
    
    private Map<String, Object> executeContextEdit(ScenarioBlock block, Map<String, Object> context) {
        LOG.infof("Executing context edit for block: %s", block.id);
        
        if (block.parameters == null) {
            return createResponse("context-edit", "No parameters", getNextNode(block), context);
        }
        
        Object operationsObj = block.parameters.get("operations");
        if (operationsObj instanceof List) {
            List<Map<String, Object>> operations = (List<Map<String, Object>>) operationsObj;
            
            for (Map<String, Object> op : operations) {
                String action = (String) op.get("action");
                String path = (String) op.get("path");
                Object value = op.get("value");
                
                if ("set".equals(action) && path != null) {
                    setSimpleContextValue(context, path, value);
                } else if ("delete".equals(action) && path != null) {
                    deleteSimpleContextValue(context, path);
                }
            }
        }
        
        return createResponse("context-edit", "Context updated", getNextNode(block), context);
    }
    
    private void setSimpleContextValue(Map<String, Object> context, String path, Object value) {
        if (value instanceof String) {
            value = replaceVariables((String) value, context);
        }
        context.put(path, value);
    }
    
    private void deleteSimpleContextValue(Map<String, Object> context, String path) {
        context.remove(path);
    }
    
    private Map<String, Object> executeCalculate(ScenarioBlock block, Map<String, Object> context) {
        LOG.infof("Executing calculate for block: %s", block.id);
        
        if (block.parameters == null) {
            return createResponse("calculate", "No parameters", getNextNode(block), context);
        }
        
        Object operationsObj = block.parameters.get("operations");
        if (operationsObj instanceof List) {
            List<Map<String, Object>> operations = (List<Map<String, Object>>) operationsObj;
            
            for (Map<String, Object> op : operations) {
                String target = (String) op.get("target");
                String operation = (String) op.get("operation");
                Object value = op.get("value");
                
                if (target != null && operation != null) {
                    executeSimpleCalculateOperation(context, target, operation, value);
                }
            }
        }
        
        return createResponse("calculate", "Calculations completed", getNextNode(block), context);
    }
    
    private void executeSimpleCalculateOperation(Map<String, Object> context, String target, String operation, Object value) {
        Object current = context.get(target);
        double currentNum = parseSimpleNumber(current);
        double valueNum = parseSimpleNumber(value);
        
        double result = switch (operation.toLowerCase()) {
            case "add", "increment", "+" -> currentNum + valueNum;
            case "subtract", "decrement", "-" -> currentNum - valueNum;
            case "multiply", "*" -> currentNum * valueNum;
            case "divide", "/" -> valueNum != 0 ? currentNum / valueNum : currentNum;
            case "set", "=" -> valueNum;
            default -> currentNum;
        };
        
        context.put(target, (result == Math.floor(result)) ? (int) result : result);
    }
    
    private double parseSimpleNumber(Object value) {
        if (value == null) return 0.0;
        if (value instanceof Number) return ((Number) value).doubleValue();
        if (value instanceof String) {
            try {
                return Double.parseDouble((String) value);
            } catch (NumberFormatException e) {
                return 0.0;
            }
        }
        return 0.0;
    }
}
