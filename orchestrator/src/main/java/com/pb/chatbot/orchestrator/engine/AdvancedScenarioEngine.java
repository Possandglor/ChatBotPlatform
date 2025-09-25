package com.pb.chatbot.orchestrator.engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pb.chatbot.orchestrator.model.Scenario;
import com.pb.chatbot.orchestrator.model.ScenarioBlock;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class AdvancedScenarioEngine {
    
    private static final Logger LOG = Logger.getLogger(AdvancedScenarioEngine.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    private final HttpClient httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5))
        .build();
    
    public Map<String, Object> executeScenario(Scenario scenario, String userInput, Map<String, Object> context) {
        LOG.infof("Executing scenario: %s with input: %s", scenario.name, userInput);
        
        if (context == null) {
            context = new HashMap<>();
        }
        
        // Определяем текущий узел
        String currentNodeId = (String) context.getOrDefault("current_node", scenario.startNode);
        
        // Если current_node null, используем startNode
        if (currentNodeId == null) {
            currentNodeId = scenario.startNode;
        }
        
        ScenarioBlock currentNode = findNodeById(scenario, currentNodeId);
        
        if (currentNode == null) {
            LOG.errorf("Node not found: %s", currentNodeId);
            return createErrorResponse("Node not found: " + currentNodeId);
        }
        
        // Выполняем узел в зависимости от типа
        return executeNodeByType(currentNode, userInput, context, scenario);
    }
    
    private Map<String, Object> executeNodeByType(ScenarioBlock node, String userInput, 
                                                 Map<String, Object> context, Scenario scenario) {
        LOG.debugf("Executing node: %s (type: %s)", node.id, node.type);
        
        switch (node.type.toLowerCase()) {
            case "announce":
                return executeAnnounce(node, context, scenario);
                
            case "ask":
                return executeAsk(node, context, scenario);
                
            case "parse":
                return executeParse(node, userInput, context, scenario);
                
            case "condition":
                return executeCondition(node, context, scenario);
                
            case "api-request":
                return executeApiRequest(node, context, scenario);
                
            case "nlu-request":
                return executeNluRequest(node, userInput, context, scenario);
                
            case "sub-flow":
                return executeSubFlow(node, context, scenario);
                
            case "notification":
                return executeNotification(node, context, scenario);
                
            case "wait":
                return executeWait(node, context, scenario);
                
            default:
                LOG.errorf("Unknown node type: %s", node.type);
                return createErrorResponse("Unknown node type: " + node.type);
        }
    }
    
    // 📢 ANNOUNCE - Показать сообщение
    private Map<String, Object> executeAnnounce(ScenarioBlock node, Map<String, Object> context, Scenario scenario) {
        String message = (String) node.parameters.get("message");
        
        // Подстановка переменных из контекста
        message = substituteVariables(message, context);
        
        String nextNode = getNextNode(node, context);
        updateContext(context, nextNode);
        
        return createResponse("announce", message, nextNode, context);
    }
    
    // ❓ ASK - Запросить ввод от пользователя
    private Map<String, Object> executeAsk(ScenarioBlock node, Map<String, Object> context, Scenario scenario) {
        String question = (String) node.parameters.get("question");
        String inputType = (String) node.parameters.getOrDefault("inputType", "text");
        
        // Устанавливаем флаг ожидания ввода
        context.put("waiting_for_input", true);
        context.put("expected_input_type", inputType);
        
        String nextNode = getNextNode(node, context);
        updateContext(context, nextNode);
        
        return createResponse("ask", question, nextNode, context);
    }
    
    // 🔍 PARSE - Обработать пользовательский ввод
    private Map<String, Object> executeParse(ScenarioBlock node, String userInput, 
                                           Map<String, Object> context, Scenario scenario) {
        String script = (String) node.parameters.get("script");
        
        // Выполняем скрипт парсинга
        boolean parseSuccess = executeParseScript(script, userInput, context);
        
        String nextNode;
        if (parseSuccess) {
            nextNode = getNextNode(node, context);
        } else {
            // При ошибке парсинга - переход к error узлу
            nextNode = (String) node.conditions.get("error");
            if (nextNode == null) {
                nextNode = getNextNode(node, context);
            }
        }
        
        updateContext(context, nextNode);
        context.put("waiting_for_input", false);
        
        // Сразу выполняем следующий узел вместо возврата технического сообщения
        if (nextNode != null) {
            ScenarioBlock nextNodeBlock = findNodeById(scenario, nextNode);
            if (nextNodeBlock != null) {
                return executeNodeByType(nextNodeBlock, userInput, context, scenario);
            }
        }
        
        return createResponse("parse", "Input processed", nextNode, context);
    }
    
    // 🔀 CONDITION - Условное ветвление
    private Map<String, Object> executeCondition(ScenarioBlock node, Map<String, Object> context, Scenario scenario) {
        String condition = (String) node.parameters.get("condition");
        
        // Вычисляем условие
        boolean conditionResult = evaluateCondition(condition, context);
        
        String nextNode;
        if (conditionResult) {
            nextNode = (String) node.conditions.get("true");
        } else {
            nextNode = (String) node.conditions.get("false");
        }
        
        // Если нет true/false, ищем по значению переменной
        if (nextNode == null) {
            String conditionValue = getConditionValue(condition, context);
            nextNode = (String) node.conditions.get(conditionValue);
        }
        
        if (nextNode == null) {
            nextNode = (String) node.conditions.get("default");
        }
        
        updateContext(context, nextNode);
        
        // Сразу выполняем следующий узел вместо возврата технического сообщения
        if (nextNode != null) {
            ScenarioBlock nextNodeBlock = findNodeById(scenario, nextNode);
            if (nextNodeBlock != null) {
                return executeNodeByType(nextNodeBlock, "", context, scenario);
            }
        }
        
        return createResponse("condition", "Condition evaluated", nextNode, context);
    }
    
    // 🧠 NLU-REQUEST - Анализ текста через NLU Service
    private Map<String, Object> executeNluRequest(ScenarioBlock node, String userInput,
                                                 Map<String, Object> context, Scenario scenario) {
        String service = (String) node.parameters.getOrDefault("service", "nlu-service");
        String endpoint = (String) node.parameters.getOrDefault("endpoint", "/api/v1/nlu/analyze");
        
        LOG.infof("Making NLU request for text: %s", userInput);
        
        try {
            // Подготавливаем данные для NLU
            Map<String, Object> nluData = new HashMap<>();
            nluData.put("text", userInput);
            nluData.put("context", context);
            
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(getNluServiceUrl() + endpoint))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(nluData)))
                .timeout(Duration.ofSeconds(10))
                .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                // Парсим ответ NLU через Jackson
                Map<String, Object> nluResponse = objectMapper.readValue(response.body(), Map.class);
                
                // Сохраняем результат в контекст
                context.put("nlu_response", nluResponse);
                context.put("intent", nluResponse.get("intent"));
                context.put("entities", nluResponse.get("entities"));
                context.put("confidence", nluResponse.get("confidence"));
                
                // Безопасная обработка suggested_scenario
                Object suggestedScenario = nluResponse.get("suggested_scenario");
                context.put("suggested_scenario", suggestedScenario != null ? suggestedScenario : "");
                
                LOG.infof("NLU analysis completed: intent=%s, confidence=%s", 
                    nluResponse.get("intent"), nluResponse.get("confidence"));
                
                String nextNode = (String) node.conditions.get("success");
                if (nextNode == null) {
                    nextNode = getNextNode(node, context);
                }
                
                updateContext(context, nextNode);
                
                // Сразу выполняем следующий узел
                if (nextNode != null) {
                    ScenarioBlock nextNodeBlock = findNodeById(scenario, nextNode);
                    if (nextNodeBlock != null) {
                        return executeNodeByType(nextNodeBlock, userInput, context, scenario);
                    }
                }
                
                return createResponse("nlu-request", "NLU analysis completed", nextNode, context);
                
            } else {
                LOG.errorf("NLU request failed with status: %d", response.statusCode());
                context.put("nlu_error", "NLU service unavailable");
                
                String errorNode = (String) node.conditions.get("error");
                if (errorNode == null) {
                    errorNode = getNextNode(node, context);
                }
                
                updateContext(context, errorNode);
                return createResponse("nlu-request", "NLU analysis failed", errorNode, context);
            }
            
        } catch (Exception e) {
            LOG.errorf(e, "NLU request failed: %s", e.getMessage());
            context.put("nlu_error", e.getMessage());
            
            String errorNode = (String) node.conditions.get("error");
            if (errorNode == null) {
                errorNode = getNextNode(node, context);
            }
            
            updateContext(context, errorNode);
            return createResponse("nlu-request", "NLU request failed", errorNode, context);
        }
    }
    private Map<String, Object> executeApiRequest(ScenarioBlock node, Map<String, Object> context, Scenario scenario) {
        String service = (String) node.parameters.get("service");
        String endpoint = (String) node.parameters.get("endpoint");
        String method = (String) node.parameters.getOrDefault("method", "GET");
        String baseUrl = (String) node.parameters.get("baseUrl");
        Map<String, Object> data = (Map<String, Object>) node.parameters.get("data");
        Map<String, String> headers = (Map<String, String>) node.parameters.get("headers");
        Integer timeout = (Integer) node.parameters.getOrDefault("timeout", 30000);
        
        LOG.infof("Making API request to %s: %s %s", service, method, endpoint);
        
        try {
            // Определяем URL
            String url;
            if (baseUrl != null) {
                url = baseUrl + endpoint;
            } else {
                url = getServiceUrl(service) + endpoint;
            }
            
            // Подстановка переменных в URL
            url = substituteVariables(url, context);
            
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofMillis(timeout));
            
            // Добавляем заголовки
            if (headers != null) {
                for (Map.Entry<String, String> header : headers.entrySet()) {
                    String value = substituteVariables(header.getValue(), context);
                    requestBuilder.header(header.getKey(), value);
                }
            }
            
            // Устанавливаем метод и тело запроса
            if ("POST".equals(method) || "PUT".equals(method)) {
                if (data != null) {
                    // Подставляем переменные в данные
                    Map<String, Object> processedData = substituteVariablesInData(data, context);
                    String jsonData = objectMapper.writeValueAsString(processedData);
                    requestBuilder.header("Content-Type", "application/json");
                    requestBuilder.method(method, HttpRequest.BodyPublishers.ofString(jsonData));
                } else {
                    requestBuilder.method(method, HttpRequest.BodyPublishers.noBody());
                }
            } else {
                requestBuilder.GET();
            }
            
            HttpRequest request = requestBuilder.build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            LOG.infof("API response status: %d", response.statusCode());
            
            // Обрабатываем ответ
            try {
                Map<String, Object> apiResponse = objectMapper.readValue(response.body(), Map.class);
                context.put("api_response", apiResponse);
            } catch (Exception e) {
                context.put("api_response", response.body());
            }
            context.put("api_status_code", response.statusCode());
            
            String nextNode;
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                nextNode = (String) node.conditions.get("success");
            } else {
                nextNode = (String) node.conditions.get("error");
            }
            
            if (nextNode == null) {
                nextNode = getNextNode(node, context);
            }
            
            updateContext(context, nextNode);
            
            // Сразу выполняем следующий узел
            if (nextNode != null) {
                ScenarioBlock nextNodeBlock = findNodeById(scenario, nextNode);
                if (nextNodeBlock != null) {
                    return executeNodeByType(nextNodeBlock, "", context, scenario);
                }
            }
            
            return createResponse("api-request", "API call completed", nextNode, context);
            
        } catch (java.net.http.HttpTimeoutException e) {
            LOG.errorf("API request timeout: %s", e.getMessage());
            context.put("api_error", "timeout");
            
            String timeoutNode = (String) node.conditions.get("timeout");
            if (timeoutNode == null) {
                timeoutNode = (String) node.conditions.get("error");
            }
            if (timeoutNode == null) {
                timeoutNode = getNextNode(node, context);
            }
            
            updateContext(context, timeoutNode);
            return createResponse("api-request", "API request timeout", timeoutNode, context);
            
        } catch (Exception e) {
            LOG.errorf(e, "API request failed: %s", e.getMessage());
            context.put("api_error", e.getMessage());
            
            String errorNode = (String) node.conditions.get("error");
            if (errorNode == null) {
                errorNode = getNextNode(node, context);
            }
            
            updateContext(context, errorNode);
            
            return createResponse("api-request", "API call failed", errorNode, context);
        }
    }
    
    // 🔄 SUB-FLOW - Переход в подсценарий
    private Map<String, Object> executeSubFlow(ScenarioBlock node, Map<String, Object> context, Scenario scenario) {
        String subScenarioId = (String) node.parameters.get("scenario_id");
        boolean inheritContext = (Boolean) node.parameters.getOrDefault("inherit_context", true);
        
        LOG.infof("Executing sub-flow: %s", subScenarioId);
        
        // Здесь должна быть логика загрузки и выполнения подсценария
        // Пока заглушка
        context.put("sub_flow_executed", subScenarioId);
        
        String nextNode = (String) node.conditions.get("completed");
        if (nextNode == null) {
            nextNode = getNextNode(node, context);
        }
        
        updateContext(context, nextNode);
        
        return createResponse("sub-flow", "Sub-flow completed", nextNode, context);
    }
    
    // 📧 NOTIFICATION - Отправка уведомлений
    private Map<String, Object> executeNotification(ScenarioBlock node, Map<String, Object> context, Scenario scenario) {
        String type = (String) node.parameters.get("type"); // sms, email, push
        String template = (String) node.parameters.get("template");
        String recipient = (String) node.parameters.get("recipient");
        
        recipient = substituteVariables(recipient, context);
        
        LOG.infof("Sending %s notification to %s using template %s", type, recipient, template);
        
        // Здесь должна быть логика отправки уведомлений
        // Пока заглушка
        context.put("notification_sent", true);
        context.put("notification_type", type);
        
        String nextNode = getNextNode(node, context);
        updateContext(context, nextNode);
        
        return createResponse("notification", "Notification sent", nextNode, context);
    }
    
    // ⏱️ WAIT - Пауза
    private Map<String, Object> executeWait(ScenarioBlock node, Map<String, Object> context, Scenario scenario) {
        Integer duration = (Integer) node.parameters.get("duration");
        
        if (duration != null && duration > 0) {
            try {
                Thread.sleep(duration);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        String nextNode = getNextNode(node, context);
        updateContext(context, nextNode);
        
        return createResponse("wait", "Wait completed", nextNode, context);
    }
    
    // Вспомогательные методы
    
    private ScenarioBlock findNodeById(Scenario scenario, String nodeId) {
        if (scenario.nodes == null || nodeId == null) return null;
        return scenario.nodes.stream()
            .filter(node -> nodeId.equals(node.id))
            .findFirst()
            .orElse(null);
    }
    
    private String getNextNode(ScenarioBlock node, Map<String, Object> context) {
        if (node.nextNodes != null && !node.nextNodes.isEmpty()) {
            return node.nextNodes.get(0);
        }
        return null;
    }
    
    private void updateContext(Map<String, Object> context, String nextNode) {
        context.put("current_node", nextNode);
        context.put("last_execution_time", System.currentTimeMillis());
    }
    
    private String substituteVariables(String text, Map<String, Object> context) {
        if (text == null) return null;
        
        String result = text;
        for (Map.Entry<String, Object> entry : context.entrySet()) {
            String placeholder = "{context." + entry.getKey() + "}";
            if (result.contains(placeholder)) {
                result = result.replace(placeholder, String.valueOf(entry.getValue()));
            }
        }
        
        // Подстановка из API ответа
        if (context.containsKey("api_response")) {
            Map<String, Object> apiResponse = (Map<String, Object>) context.get("api_response");
            for (Map.Entry<String, Object> entry : apiResponse.entrySet()) {
                String placeholder = "{api_response." + entry.getKey() + "}";
                if (result.contains(placeholder)) {
                    result = result.replace(placeholder, String.valueOf(entry.getValue()));
                }
            }
        }
        
        return result;
    }
    
    private boolean executeParseScript(String script, String userInput, Map<String, Object> context) {
        if (script == null) return true;
        
        try {
            // Простая реализация парсинга
            if (script.contains("context.operation")) {
                String input = userInput.toLowerCase();
                if (input.contains("баланс") || input.equals("1")) {
                    context.put("operation", "balance");
                } else if (input.contains("закрыть") || input.equals("2")) {
                    context.put("operation", "close");
                } else if (input.contains("блок") || input.equals("3")) {
                    context.put("operation", "block");
                } else if (input.contains("история") || input.equals("4")) {
                    context.put("operation", "history");
                } else if (input.contains("поддержк") || input.equals("5")) {
                    context.put("operation", "support");
                } else {
                    context.put("operation", "unknown");
                }
                context.put("validChoice", !context.get("operation").equals("unknown"));
            }
            
            if (script.contains("context.wantsBalance")) {
                boolean wants = userInput.toLowerCase().contains("да") || userInput.toLowerCase().contains("yes");
                context.put("wantsBalance", wants);
            }
            
            if (script.contains("context.cardNumber")) {
                context.put("cardNumber", userInput);
                context.put("validCard", userInput.matches("\\d{4}"));
            }
            
            return true;
        } catch (Exception e) {
            LOG.errorf(e, "Parse script execution failed: %s", e.getMessage());
            return false;
        }
    }
    
    private boolean evaluateCondition(String condition, Map<String, Object> context) {
        if (condition == null) return true;
        
        // Простая реализация условий
        if (condition.contains("context.operation")) {
            return context.containsKey("operation");
        }
        
        if (condition.contains("context.wantsBalance == true")) {
            return Boolean.TRUE.equals(context.get("wantsBalance"));
        }
        
        if (condition.contains("context.validCard == true")) {
            return Boolean.TRUE.equals(context.get("validCard"));
        }
        
        return true;
    }
    
    private String getConditionValue(String condition, Map<String, Object> context) {
        if (condition.contains("context.operation")) {
            return (String) context.get("operation");
        }
        return null;
    }
    
    private String getServiceUrl(String service) {
        // Маппинг сервисов на URL
        switch (service) {
            case "bank-api":
                return "http://localhost:8094"; // Банковский API
            case "crm-service":
                return "http://localhost:8095"; // CRM система
            case "notification-service":
                return "http://localhost:8096"; // Сервис уведомлений
            default:
                return "http://localhost:8080"; // По умолчанию
        }
    }
    
    private String convertToJson(Map<String, Object> data, Map<String, Object> context) {
        if (data == null) return "{}";
        
        // Улучшенная JSON сериализация
        StringBuilder json = new StringBuilder("{");
        boolean first = true;
        
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            if (!first) json.append(",");
            first = false;
            
            json.append("\"").append(entry.getKey()).append("\":");
            
            Object value = entry.getValue();
            if (value instanceof String) {
                String strValue = substituteVariables((String) value, context);
                json.append("\"").append(escapeJson(strValue)).append("\"");
            } else if (value instanceof Map) {
                // Рекурсивно обрабатываем вложенные объекты
                json.append(convertToJson((Map<String, Object>) value, context));
            } else if (value == null) {
                json.append("null");
            } else {
                json.append("\"").append(escapeJson(String.valueOf(value))).append("\"");
            }
        }
        
        json.append("}");
        return json.toString();
    }
    
    private String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }
    
    private Map<String, Object> parseApiResponse(String responseBody) {
        // Простой парсер JSON ответа
        Map<String, Object> response = new HashMap<>();
        
        if (responseBody != null && responseBody.contains("balance")) {
            response.put("balance", "15,250.50");
            response.put("currency", "грн");
            response.put("available", "15,250.50");
        }
        
        if (responseBody != null && responseBody.contains("request_id")) {
            response.put("request_id", "REQ-" + System.currentTimeMillis());
        }
        
        return response;
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
    
    private String getNluServiceUrl() {
        return "http://localhost:8098"; // NLU Service URL
    }
    
    private Map<String, Object> parseNluResponse(String responseBody) {
        // Простой парсер JSON ответа от NLU
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Извлекаем основные поля из JSON
            if (responseBody.contains("\"intent\":")) {
                String intent = extractJsonValue(responseBody, "intent");
                response.put("intent", intent);
            }
            
            if (responseBody.contains("\"confidence\":")) {
                String confidenceStr = extractJsonValue(responseBody, "confidence");
                try {
                    double confidence = Double.parseDouble(confidenceStr);
                    response.put("confidence", confidence);
                } catch (NumberFormatException e) {
                    response.put("confidence", 0.0);
                }
            }
            
            if (responseBody.contains("\"suggested_scenario\":")) {
                String scenario = extractJsonValue(responseBody, "suggested_scenario");
                response.put("suggested_scenario", scenario);
            }
            
            // Для entities пока простая заглушка
            response.put("entities", new ArrayList<>());
            
        } catch (Exception e) {
            LOG.errorf(e, "Failed to parse NLU response: %s", responseBody);
        }
        
        return response;
    }
    
    private String extractJsonValue(String json, String key) {
        String pattern = "\"" + key + "\":\"([^\"]+)\"";
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
        java.util.regex.Matcher m = p.matcher(json);
        if (m.find()) {
            return m.group(1);
        }
        
        // Для числовых значений
        pattern = "\"" + key + "\":([^,}]+)";
        p = java.util.regex.Pattern.compile(pattern);
        m = p.matcher(json);
        if (m.find()) {
            return m.group(1).trim();
        }
        
        return null;
    }
    
    private Map<String, Object> substituteVariablesInData(Map<String, Object> data, Map<String, Object> context) {
        if (data == null) return null;
        
        Map<String, Object> result = new HashMap<>();
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof String) {
                result.put(entry.getKey(), substituteVariables((String) value, context));
            } else if (value instanceof Map) {
                result.put(entry.getKey(), substituteVariablesInData((Map<String, Object>) value, context));
            } else {
                result.put(entry.getKey(), value);
            }
        }
        return result;
    }
    
    private Map<String, Object> createErrorResponse(String error) {
        Map<String, Object> response = new HashMap<>();
        response.put("type", "error");
        response.put("message", error);
        response.put("timestamp", System.currentTimeMillis());
        return response;
    }
}
