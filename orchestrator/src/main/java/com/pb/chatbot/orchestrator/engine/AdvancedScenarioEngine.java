package com.pb.chatbot.orchestrator.engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pb.chatbot.orchestrator.client.GeminiClient;
import com.pb.chatbot.orchestrator.model.Scenario;
import com.pb.chatbot.orchestrator.model.ScenarioBlock;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@ApplicationScoped
public class AdvancedScenarioEngine {
    
    private static final Logger LOG = Logger.getLogger(AdvancedScenarioEngine.class);
    
    @Inject
    GeminiClient geminiClient;
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
                
            case "switch":
                return executeSwitch(node, context, scenario);
                
            case "api-request":
            case "api_call":
                return executeApiRequest(node, context, scenario);
                
            case "nlu-request":
                return executeNluRequest(node, userInput, context, scenario);
                
            case "scenario_jump":
                return executeScenarioJump(node, context, scenario);
                
            case "end":
                return executeEnd(node, context, scenario);
                
            case "end_dialog":
                return executeEndDialog(node, context, scenario);
                
            case "transfer":
                return executeTransfer(node, context, scenario);
                
            case "llm_call":
                return executeLlmCall(node, context, scenario);
                
            case "sub-flow":
                return executeSubFlow(node, context, scenario);
                
            case "notification":
                return executeNotification(node, context, scenario);
                
            case "wait":
                return executeWait(node, context, scenario);
                
            case "context-edit":
                return executeContextEdit(node, context, scenario);
                
            case "calculate":
                return executeCalculate(node, context, scenario);
                
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
        
        // НОВОЕ: Сохраняем ID узла для которого ожидается ответ
        context.put("waiting_for_answer_to_node", node.id);
        
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
            nextNode = null;
            if (node.conditions != null) {
                nextNode = (String) node.conditions.get("error");
            }
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
        // ИСПРАВЛЕНО: Проверяем наличие conditions и parameters
        if (node.conditions == null && node.parameters == null) {
            LOG.errorf("Condition node %s has no conditions or parameters", node.id);
            return createResponse("condition", "Ошибка конфигурации условия", null, context);
        }
        
        String nextNode = null;
        
        // Если есть conditions на верхнем уровне (старый формат)
        if (node.conditions != null) {
            String condition = (String) node.parameters.get("condition");
            boolean conditionResult = evaluateCondition(condition, context);
            
            if (conditionResult) {
                nextNode = node.conditions != null ? (String) node.conditions.get("true") : null;
            } else {
                nextNode = node.conditions != null ? (String) node.conditions.get("false") : null;
            }
            
            if (nextNode == null && node.conditions != null) {
                String conditionValue = getConditionValue(condition, context);
                nextNode = (String) node.conditions.get(conditionValue);
            }
            
            if (nextNode == null && node.conditions != null) {
                nextNode = (String) node.conditions.get("default");
            }
        } 
        // Новый формат: conditions в parameters + next_nodes
        else if (node.parameters != null && node.parameters.containsKey("conditions")) {
            Object conditionsObj = node.parameters.get("conditions");
            List<String> conditions = null;
            
            // Поддержка как List<String>, так и String (многострочный текст)
            if (conditionsObj instanceof List) {
                @SuppressWarnings("unchecked")
                List<String> conditionsList = (List<String>) conditionsObj;
                conditions = conditionsList;
            } else if (conditionsObj instanceof String) {
                // Разбиваем многострочный текст на отдельные условия
                String conditionsText = (String) conditionsObj;
                conditions = Arrays.asList(conditionsText.split("\\r?\\n"));
            }
            
            if (conditions != null && !conditions.isEmpty()) {
                // Проверяем каждое условие по порядку
                for (int i = 0; i < conditions.size(); i++) {
                    String condition = conditions.get(i).trim();
                    
                    // Пропускаем пустые строки и комментарии
                    if (condition.isEmpty() || condition.startsWith("//") || condition.startsWith("#")) {
                        continue;
                    }
                    
                    boolean conditionResult = evaluateCondition(condition, context);
                    LOG.infof("Condition %d: %s -> %s", i, condition, conditionResult);
                    
                    if (conditionResult) {
                        // ИСПРАВЛЕНО: Ищем целевой узел по sourceHandle в edges
                        nextNode = findTargetBySourceHandle(node.id, "output-" + i, scenario);
                        if (nextNode != null) {
                            LOG.infof("Taking branch %d to node: %s", i, nextNode);
                            break;
                        }
                        
                        // Fallback: используем next_nodes если edges нет
                        if (node.nextNodes != null && i < node.nextNodes.size()) {
                            nextNode = node.nextNodes.get(i);
                            LOG.infof("Fallback: Taking branch %d to node: %s", i, nextNode);
                            break;
                        }
                    }
                }
                
                // ELSE логика: Если ни одно условие не сработало
                if (nextNode == null) {
                    // Ищем ELSE выход (последний по счету)
                    int elseIndex = conditions.size(); // Пропускаем комментарии, поэтому берем размер условий
                    nextNode = findTargetBySourceHandle(node.id, "output-" + elseIndex, scenario);
                    
                    // Fallback: используем последний next_node
                    if (nextNode == null && node.nextNodes != null && !node.nextNodes.isEmpty()) {
                        int elseNodeIndex = node.nextNodes.size() - 1;
                        nextNode = node.nextNodes.get(elseNodeIndex);
                    }
                    
                    LOG.infof("No conditions matched, taking ELSE branch to node: %s", nextNode);
                }
            }
        }
        
        if (nextNode == null) {
            nextNode = getNextNode(node, context);
        }
        
        updateContext(context, nextNode);
        
        // ИСПРАВЛЕНО: Condition узел работает под капотом - сразу выполняем следующий узел
        if (nextNode != null) {
            ScenarioBlock nextNodeBlock = findNodeById(scenario, nextNode);
            if (nextNodeBlock != null) {
                // Используем пустую строку для userInput т.к. condition узел не обрабатывает пользовательский ввод
                return executeNodeByType(nextNodeBlock, "", context, scenario);
            }
        }
        
        // Fallback если узел не найден
        return createResponse("condition", "Ошибка: следующий узел не найден", null, context);
    }
    
    // 🧠 NLU-REQUEST - Анализ текста через NLU Service
    private Map<String, Object> executeNluRequest(ScenarioBlock node, String userInput,
                                                 Map<String, Object> context, Scenario scenario) {
        // Получаем service и endpoint из parameters или используем значения по умолчанию
        String service = node.parameters != null ? 
            (String) node.parameters.getOrDefault("service", "nlu-service") : "nlu-service";
        String endpoint = node.parameters != null ? 
            (String) node.parameters.getOrDefault("endpoint", "/api/v1/nlu/analyze") : "/api/v1/nlu/analyze";
        
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
                
                // ОТЛАДКА: Проверяем conditions
                LOG.infof("NLU node conditions: %s", node.conditions);
                
                String nextNode = node.conditions != null ? (String) node.conditions.get("success") : null;
                LOG.infof("NLU nextNode from conditions.success: %s", nextNode);
                
                if (nextNode == null) {
                    nextNode = getNextNode(node, context);
                    LOG.infof("NLU nextNode from getNextNode: %s", nextNode);
                }
                
                updateContext(context, nextNode);
                
                // ИСПРАВЛЕНО: Сразу выполняем следующий узел (как было раньше)
                if (nextNode != null) {
                    ScenarioBlock nextNodeBlock = findNodeById(scenario, nextNode);
                    if (nextNodeBlock != null) {
                        LOG.infof("NLU executing next node: %s (type: %s)", nextNodeBlock.id, nextNodeBlock.type);
                        return executeNodeByType(nextNodeBlock, userInput, context, scenario);
                    } else {
                        LOG.errorf("NLU next node not found: %s", nextNode);
                    }
                } else {
                    LOG.errorf("NLU nextNode is null!");
                }
                
                return createResponse("nlu-request", "NLU analysis completed", nextNode, context);
                
            } else {
                LOG.errorf("NLU request failed with status: %d", response.statusCode());
                context.put("nlu_error", "NLU service unavailable");
                
                String errorNode = node.conditions != null ? (String) node.conditions.get("error") : null;
                if (errorNode == null) {
                    errorNode = getNextNode(node, context);
                }
                
                updateContext(context, errorNode);
                return createResponse("nlu-request", "NLU analysis failed", errorNode, context);
            }
            
        } catch (Exception e) {
            LOG.errorf(e, "NLU request failed: %s", e.getMessage());
            context.put("nlu_error", e.getMessage());
            
            String errorNode = node.conditions != null ? (String) node.conditions.get("error") : null;
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
        
        // ИСПРАВЛЕНО: Поддержка headers как String или Map
        Map<String, String> headers = null;
        Object headersObj = node.parameters.get("headers");
        if (headersObj instanceof Map) {
            headers = (Map<String, String>) headersObj;
        } else if (headersObj instanceof String && !((String) headersObj).trim().isEmpty()) {
            try {
                headers = objectMapper.readValue((String) headersObj, Map.class);
            } catch (Exception e) {
                LOG.warnf("Failed to parse headers JSON: %s", headersObj);
            }
        }
        
        // ИСПРАВЛЕНО: Поддержка body параметра как String или Map
        Object body = node.parameters.get("body");
        if (body != null && data == null) {
            if (body instanceof Map) {
                data = (Map<String, Object>) body;
            } else if (body instanceof String && !((String) body).trim().isEmpty()) {
                try {
                    data = objectMapper.readValue((String) body, Map.class);
                } catch (Exception e) {
                    data = Map.of("_raw_body", body);
                }
            }
        }
        Integer timeout = (Integer) node.parameters.getOrDefault("timeout", 30000);
        
        LOG.infof("Making API request to %s: %s %s", service, method, endpoint);
        
        try {
            // Определяем URL
            String url = (String) node.parameters.get("url");
            if (url == null) {
                // Fallback на старый формат service+endpoint
                if (baseUrl != null) {
                    url = baseUrl + endpoint;
                } else {
                    url = getServiceUrl(service) + endpoint;
                }
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
                // ИСПРАВЛЕНО: Добавляем api_response_<node_id> как раньше
                context.put("api_response_" + node.id, apiResponse);
            } catch (Exception e) {
                context.put("api_response", response.body());
                context.put("api_response_" + node.id, response.body());
            }
            context.put("api_status_code", response.statusCode());
            
            String nextNode;
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                nextNode = node.conditions != null ? (String) node.conditions.get("success") : null;
            } else {
                nextNode = node.conditions != null ? (String) node.conditions.get("error") : null;
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
            
            String timeoutNode = null;
            if (node.conditions != null) {
                timeoutNode = (String) node.conditions.get("timeout");
                if (timeoutNode == null) {
                    timeoutNode = (String) node.conditions.get("error");
                }
            }
            if (timeoutNode == null) {
                timeoutNode = getNextNode(node, context);
            }
            
            updateContext(context, timeoutNode);
            return createResponse("api-request", "API request timeout", timeoutNode, context);
            
        } catch (Exception e) {
            LOG.errorf(e, "API request failed: %s", e.getMessage());
            context.put("api_error", e.getMessage());
            
            String errorNode = null;
            if (node.conditions != null) {
                errorNode = (String) node.conditions.get("error");
            }
            if (errorNode == null) {
                errorNode = getNextNode(node, context);
            }
            
            updateContext(context, errorNode);
            
            return createResponse("api-request", "API call failed", errorNode, context);
        }
    }
    
    // 🚀 SCENARIO_JUMP - Переход в другой сценарий
    private Map<String, Object> executeScenarioJump(ScenarioBlock node, Map<String, Object> context, Scenario scenario) {
        String targetScenarioId = null;
        
        // Получаем ID целевого сценария из parameters
        if (node.parameters != null) {
            targetScenarioId = (String) node.parameters.get("target_scenario");
        }
        
        if (targetScenarioId == null || targetScenarioId.isEmpty()) {
            LOG.errorf("Scenario jump node %s has no target_scenario", node.id);
            return createResponse("scenario_jump", "Ошибка: не указан целевой сценарий", null, context);
        }
        
        LOG.infof("Jumping to scenario: %s", targetScenarioId);
        
        try {
            // Загружаем новый сценарий
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(getScenarioServiceUrl() + "/api/v1/scenarios/" + targetScenarioId))
                .header("Content-Type", "application/json")
                .GET()
                .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                @SuppressWarnings("unchecked")
                Map<String, Object> scenarioResponse = objectMapper.readValue(response.body(), Map.class);
                
                @SuppressWarnings("unchecked")
                Map<String, Object> scenarioData = (Map<String, Object>) scenarioResponse.get("scenario_data");
                
                if (scenarioData != null) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> nodes = (List<Map<String, Object>>) scenarioData.get("nodes");
                    
                    // ИСПРАВЛЕНО: Используем ту же логику что и при запуске - findRealStartNode
                    String realStartNode = findRealStartNode(nodes, scenarioResponse);
                    
                    Scenario newScenario = convertMapToScenario(scenarioData);
                    
                    // Обновляем контекст для нового сценария
                    context.put("scenario_id", targetScenarioId);
                    context.put("current_node", realStartNode);
                    context.put("scenario_completed", false);
                    
                    // ИСПРАВЛЕНО: Сразу выполняем реальный стартовый узел нового сценария
                    ScenarioBlock startNode = findNodeById(newScenario, realStartNode);
                    if (startNode != null) {
                        LOG.infof("Executing real start node of new scenario: %s (type: %s)", realStartNode, startNode.type);
                        return executeNodeByType(startNode, "", context, newScenario);
                    }
                }
            }
            
            LOG.errorf("Failed to load target scenario: %s", targetScenarioId);
            return createResponse("scenario_jump", "Ошибка загрузки сценария", null, context);
            
        } catch (Exception e) {
            LOG.errorf(e, "Error during scenario jump: %s", e.getMessage());
            return createResponse("scenario_jump", "Ошибка перехода в сценарий", null, context);
        }
    }
    
    // 🏁 END - Завершение диалога или возврат из sub-flow
    private Map<String, Object> executeEnd(ScenarioBlock node, Map<String, Object> context, Scenario scenario) {
        Boolean inSubFlow = (Boolean) context.get("in_sub_flow");
        
        if (inSubFlow != null && inSubFlow) {
            // В sub-flow - возвращаемся в основной сценарий
            LOG.infof("Ending sub-flow, returning to main scenario");
            return returnFromSubFlow(context);
        } else {
            // В основном сценарии - завершаем диалог
            LOG.infof("Ending dialog");
            
            context.put("scenario_completed", true);
            context.put("dialog_ended", true);
            
            String message = "Диалог завершен.";
            if (node.parameters != null) {
                message = (String) node.parameters.getOrDefault("message", message);
            }
            
            return createResponse("end", message, null, context);
        }
    }
    
    // 🛑 END_DIALOG - Принудительное завершение диалога (игнорирует sub-flow)
    private Map<String, Object> executeEndDialog(ScenarioBlock node, Map<String, Object> context, Scenario scenario) {
        LOG.infof("Force ending dialog (ignoring sub-flow)");
        
        // ВСЕГДА завершаем диалог, даже в sub-flow
        context.put("scenario_completed", true);
        context.put("dialog_ended", true);
        context.put("waiting_for_input", false);  // Останавливаем ожидание ввода
        
        // Очищаем стек вызовов
        context.remove("call_stack");
        context.put("in_sub_flow", false);
        
        String message = "Диалог завершен.";
        if (node.parameters != null) {
            message = (String) node.parameters.getOrDefault("message", message);
        }
        
        return createResponse("end_dialog", message, null, context);
    }
    
    // 🔄 RETURN FROM SUB-FLOW - Возврат из подсценария в основной сценарий
    private Map<String, Object> returnFromSubFlow(Map<String, Object> context) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> callStack = (List<Map<String, Object>>) context.get("call_stack");
        
        if (callStack == null || callStack.isEmpty()) {
            LOG.errorf("Call stack is empty, cannot return from sub-flow");
            // Завершаем диалог если нет куда возвращаться
            context.put("scenario_completed", true);
            context.put("dialog_ended", true);
            return createResponse("end", "Диалог завершен.", null, context);
        }
        
        // Извлекаем последний элемент из стека (куда возвращаться)
        Map<String, Object> returnContext = callStack.remove(callStack.size() - 1);
        
        // Восстанавливаем контекст основного сценария
        String returnScenarioId = (String) returnContext.get("scenario_id");
        String nextNodeId = (String) returnContext.get("next_node");
        
        context.put("scenario_id", returnScenarioId);
        context.put("current_node", nextNodeId);
        context.put("call_stack", callStack);
        
        // Если стек пуст - выходим из sub-flow режима
        if (callStack.isEmpty()) {
            context.put("in_sub_flow", false);
        }
        
        LOG.infof("Returned from sub-flow to scenario %s, next node: %s", returnScenarioId, nextNodeId);
        
        // Продолжаем выполнение в основном сценарии
        if (nextNodeId != null && !nextNodeId.isEmpty()) {
            try {
                // Получаем основной сценарий через HTTP
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8093/api/v1/scenarios/" + returnScenarioId))
                    .header("Content-Type", "application/json")
                    .GET()
                    .build();
                
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                
                if (response.statusCode() == 200) {
                    ObjectMapper mapper = new ObjectMapper();
                    @SuppressWarnings("unchecked")
                    Map<String, Object> scenarioResponse = mapper.readValue(response.body(), Map.class);
                    @SuppressWarnings("unchecked")
                    Map<String, Object> scenarioData = (Map<String, Object>) scenarioResponse.get("scenario_data");
                    
                    if (scenarioData != null) {
                        Scenario mainScenario = convertMapToScenario(scenarioData);
                        ScenarioBlock nextNode = findNodeById(mainScenario, nextNodeId);
                        
                        if (nextNode != null) {
                            LOG.infof("Continuing main scenario with node: %s (type: %s)", nextNodeId, nextNode.type);
                            return executeNodeByType(nextNode, "", context, mainScenario);
                        }
                    }
                }
            } catch (Exception e) {
                LOG.errorf(e, "Error continuing main scenario after sub-flow return");
            }
        }
        
        // Если не удалось продолжить - завершаем
        context.put("scenario_completed", true);
        return createResponse("end", "Возврат из подсценария завершен.", null, context);
    }
    
    // 👤 TRANSFER - Перевод на оператора
    private Map<String, Object> executeTransfer(ScenarioBlock node, Map<String, Object> context, Scenario scenario) {
        LOG.infof("Transferring to operator");
        
        context.put("transferred_to_operator", true);
        context.put("scenario_completed", true);
        
        String message = "Переводим вас на оператора...";
        if (node.parameters != null) {
            message = (String) node.parameters.getOrDefault("message", message);
        }
        
        return createResponse("transfer", message, null, context);
    }
    
    // 🤖 LLM_CALL - Запрос к LLM модели
    private Map<String, Object> executeLlmCall(ScenarioBlock node, Map<String, Object> context, Scenario scenario) {
        LOG.infof("Executing LLM call for node: %s", node.id);
        
        String prompt = "Ответьте на вопрос пользователя";
        if (node.parameters != null) {
            prompt = (String) node.parameters.getOrDefault("prompt", prompt);
        }
        
        // Подставляем переменные из контекста в промпт
        prompt = substituteVariables(prompt, context);
        
        try {
            // Вызываем Gemini API через инжектированный клиент
            String llmResponse = geminiClient.generateContent(prompt);
            
            // Сохраняем ответ с ID узла для множественных LLM вызовов
            String responseKey = "llm_response_" + node.id;
            context.put(responseKey, llmResponse);
            context.put("llm_response", llmResponse); // Для обратной совместимости
            
            LOG.infof("LLM response saved to context key: %s", responseKey);
            
        } catch (Exception e) {
            LOG.errorf(e, "Error calling LLM API for node %s", node.id);
            String errorResponse = "Извините, произошла ошибка при обращении к AI модели.";
            String responseKey = "llm_response_" + node.id;
            context.put(responseKey, errorResponse);
            context.put("llm_response", errorResponse);
        }
        
        String nextNode = getNextNode(node, context);
        updateContext(context, nextNode);
        
        // Системный узел - сразу выполняем следующий БЕЗ сообщения пользователю
        if (nextNode != null) {
            ScenarioBlock nextNodeBlock = findNodeById(scenario, nextNode);
            if (nextNodeBlock != null) {
                return executeNodeByType(nextNodeBlock, "", context, scenario);
            }
        }
        
        // Если нет следующего узла - завершаем сценарий
        context.put("scenario_completed", true);
        return createResponse("llm_call", "", null, context);
    }
    
    // 🔀 SWITCH - Многоветвенное условное ветвление (улучшенная версия condition)
    private Map<String, Object> executeSwitch(ScenarioBlock node, Map<String, Object> context, Scenario scenario) {
        if (node.parameters == null || !node.parameters.containsKey("conditions")) {
            LOG.errorf("Switch node %s has no conditions", node.id);
            return createResponse("switch", "Ошибка конфигурации switch", null, context);
        }
        
        @SuppressWarnings("unchecked")
        List<String> conditions = (List<String>) node.parameters.get("conditions");
        
        if (conditions == null || conditions.isEmpty()) {
            LOG.errorf("Switch node %s has empty conditions", node.id);
            return createResponse("switch", "Пустые условия switch", null, context);
        }
        
        String nextNode = null;
        
        // Проверяем каждое условие по порядку
        for (int i = 0; i < conditions.size(); i++) {
            String condition = conditions.get(i).trim();
            
            // Пропускаем пустые строки и комментарии
            if (condition.isEmpty() || condition.startsWith("//") || condition.startsWith("#")) {
                continue;
            }
            
            boolean conditionResult = evaluateCondition(condition, context);
            LOG.infof("Switch condition %d: %s -> %s", i, condition, conditionResult);
            
            if (conditionResult && node.nextNodes != null && i < node.nextNodes.size()) {
                nextNode = node.nextNodes.get(i);
                LOG.infof("Switch taking branch %d to node: %s", i, nextNode);
                break;
            }
        }
        
        // ELSE логика: Если ни одно условие не сработало, берем последний next_node как default
        if (nextNode == null && node.nextNodes != null && !node.nextNodes.isEmpty()) {
            int elseIndex = node.nextNodes.size() - 1;
            nextNode = node.nextNodes.get(elseIndex);
            LOG.infof("Switch: No conditions matched, taking DEFAULT branch %d to node: %s", elseIndex, nextNode);
        }
        
        if (nextNode == null) {
            LOG.errorf("Switch node %s has no valid next nodes", node.id);
            return createResponse("switch", "Нет доступных переходов", null, context);
        }
        
        updateContext(context, nextNode);
        
        // Сразу выполняем следующий узел
        ScenarioBlock nextNodeBlock = findNodeById(scenario, nextNode);
        if (nextNodeBlock != null) {
            return executeNodeByType(nextNodeBlock, "", context, scenario);
        }
        
        return createResponse("switch", "Switch executed", nextNode, context);
    }
    
    // 🔄 SUB-FLOW - Переход в подсценарий с возвратом
    private Map<String, Object> executeSubFlow(ScenarioBlock node, Map<String, Object> context, Scenario scenario) {
        String subScenarioId = null;
        
        // Получаем ID подсценария из parameters
        if (node.parameters != null) {
            subScenarioId = (String) node.parameters.get("target_scenario");
        }
        
        if (subScenarioId == null || subScenarioId.isEmpty()) {
            LOG.errorf("Sub-flow node %s has no target_scenario", node.id);
            return createResponse("sub-flow", "Ошибка: не указан подсценарий", null, context);
        }
        
        LOG.infof("Starting sub-flow: %s", subScenarioId);
        
        try {
            // Загружаем подсценарий
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(getScenarioServiceUrl() + "/api/v1/scenarios/" + subScenarioId))
                .header("Content-Type", "application/json")
                .GET()
                .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                @SuppressWarnings("unchecked")
                Map<String, Object> scenarioResponse = objectMapper.readValue(response.body(), Map.class);
                
                @SuppressWarnings("unchecked")
                Map<String, Object> scenarioData = (Map<String, Object>) scenarioResponse.get("scenario_data");
                
                if (scenarioData != null) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> nodes = (List<Map<String, Object>>) scenarioData.get("nodes");
                    
                    String realStartNode = findRealStartNode(nodes, scenarioResponse);
                    Scenario subScenario = convertMapToScenario(scenarioData);
                    
                    // КЛЮЧЕВОЕ ОТЛИЧИЕ: Сохраняем стек вызовов для возврата
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> callStack = (List<Map<String, Object>>) context.getOrDefault("call_stack", new ArrayList<>());
                    
                    // Добавляем текущий контекст в стек
                    Map<String, Object> returnContext = new HashMap<>();
                    returnContext.put("scenario_id", context.get("scenario_id"));
                    returnContext.put("node_id", node.id);
                    returnContext.put("next_node", getNextNode(node, context));
                    
                    callStack.add(returnContext);
                    
                    // Обновляем контекст для подсценария
                    context.put("scenario_id", subScenarioId);
                    context.put("current_node", realStartNode);
                    context.put("call_stack", callStack);
                    context.put("in_sub_flow", true);
                    
                    // Выполняем стартовый узел подсценария
                    ScenarioBlock startNode = findNodeById(subScenario, realStartNode);
                    if (startNode != null) {
                        LOG.infof("Executing sub-flow start node: %s (type: %s)", realStartNode, startNode.type);
                        return executeNodeByType(startNode, "", context, subScenario);
                    }
                }
            }
            
            LOG.errorf("Failed to load sub-scenario: %s", subScenarioId);
            return createResponse("sub-flow", "Ошибка загрузки подсценария", null, context);
            
        } catch (Exception e) {
            LOG.errorf(e, "Error during sub-flow: %s", e.getMessage());
            return createResponse("sub-flow", "Ошибка выполнения подсценария", null, context);
        }
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
    
    // ✏️ CONTEXT-EDIT - Редактирование параметров контекста
    private Map<String, Object> executeContextEdit(ScenarioBlock node, Map<String, Object> context, Scenario scenario) {
        LOG.infof("Executing context edit for node: %s", node.id);
        
        if (node.parameters == null) {
            LOG.errorf("Context edit node %s has no parameters", node.id);
            return createResponse("context-edit", "Ошибка: нет параметров для редактирования", null, context);
        }
        
        // Операции редактирования контекста
        Object operationsObj = node.parameters.get("operations");
        if (operationsObj == null) {
            LOG.errorf("Context edit node %s has no operations", node.id);
            return createResponse("context-edit", "Ошибка: нет операций для выполнения", null, context);
        }
        
        List<Map<String, Object>> operations = null;
        if (operationsObj instanceof List) {
            operations = (List<Map<String, Object>>) operationsObj;
        } else if (operationsObj instanceof String) {
            // Парсим JSON строку с операциями
            try {
                operations = objectMapper.readValue((String) operationsObj, List.class);
            } catch (Exception e) {
                LOG.errorf("Failed to parse operations JSON: %s", e.getMessage());
                return createResponse("context-edit", "Ошибка парсинга операций", null, context);
            }
        }
        
        if (operations == null || operations.isEmpty()) {
            LOG.warnf("Context edit node %s has empty operations", node.id);
            return createResponse("context-edit", "Нет операций для выполнения", getNextNode(node, context), context);
        }
        
        int successCount = 0;
        int errorCount = 0;
        StringBuilder resultMessage = new StringBuilder();
        
        // Выполняем операции по порядку
        for (Map<String, Object> operation : operations) {
            try {
                String action = (String) operation.get("action");
                String path = (String) operation.get("path");
                Object value = operation.get("value");
                
                if (action == null || path == null) {
                    LOG.warnf("Invalid operation: action=%s, path=%s", action, path);
                    errorCount++;
                    continue;
                }
                
                // Подстановка переменных в значение
                if (value instanceof String) {
                    value = substituteVariables((String) value, context);
                }
                
                boolean success = executeContextOperation(context, action, path, value);
                if (success) {
                    successCount++;
                    LOG.debugf("Context operation successful: %s %s", action, path);
                } else {
                    errorCount++;
                    LOG.warnf("Context operation failed: %s %s", action, path);
                }
                
            } catch (Exception e) {
                LOG.errorf(e, "Error executing context operation: %s", e.getMessage());
                errorCount++;
            }
        }
        
        // Формируем сообщение о результате
        if (successCount > 0 || errorCount > 0) {
            resultMessage.append(String.format("Контекст обновлен: %d успешно, %d ошибок", successCount, errorCount));
        } else {
            resultMessage.append("Операции с контекстом выполнены");
        }
        
        String nextNode = getNextNode(node, context);
        updateContext(context, nextNode);
        
        // Системный узел - сразу выполняем следующий
        if (nextNode != null) {
            ScenarioBlock nextNodeBlock = findNodeById(scenario, nextNode);
            if (nextNodeBlock != null) {
                return executeNodeByType(nextNodeBlock, "", context, scenario);
            }
        }
        
        return createResponse("context-edit", resultMessage.toString(), nextNode, context);
    }
    
    /**
     * Выполняет операцию редактирования контекста
     * @param context контекст для редактирования
     * @param action тип операции: set, delete, add, merge
     * @param path путь к параметру (JSONPath)
     * @param value значение (для set, add, merge)
     * @return true если операция успешна
     */
    private boolean executeContextOperation(Map<String, Object> context, String action, String path, Object value) {
        try {
            switch (action.toLowerCase()) {
                case "set":
                    return setContextValue(context, path, value);
                    
                case "delete":
                case "remove":
                    return deleteContextValue(context, path);
                    
                case "add":
                    return addContextValue(context, path, value);
                    
                case "merge":
                    return mergeContextValue(context, path, value);
                    
                case "clear":
                    return clearContextPath(context, path);
                    
                default:
                    LOG.warnf("Unknown context operation: %s", action);
                    return false;
            }
        } catch (Exception e) {
            LOG.errorf(e, "Error in context operation %s for path %s", action, path);
            return false;
        }
    }
    
    /**
     * Устанавливает значение по пути (создает путь если не существует)
     */
    private boolean setContextValue(Map<String, Object> context, String path, Object value) {
        String[] parts = parseJsonPath(path);
        if (parts.length == 0) return false;
        
        Map<String, Object> current = context;
        
        // Проходим до предпоследнего элемента, создавая путь
        for (int i = 0; i < parts.length - 1; i++) {
            String part = parts[i];
            
            if (part.matches("\\[\\d+\\]")) {
                // Массив - пока не поддерживаем создание
                LOG.warnf("Array creation not supported in path: %s", path);
                return false;
            } else {
                // Объект
                if (!current.containsKey(part)) {
                    current.put(part, new HashMap<String, Object>());
                }
                Object next = current.get(part);
                if (!(next instanceof Map)) {
                    // Перезаписываем если не объект
                    next = new HashMap<String, Object>();
                    current.put(part, next);
                }
                current = (Map<String, Object>) next;
            }
        }
        
        // Устанавливаем финальное значение
        String finalKey = parts[parts.length - 1];
        if (finalKey.matches("\\[\\d+\\]")) {
            LOG.warnf("Cannot set array index directly: %s", path);
            return false;
        } else {
            current.put(finalKey, value);
            return true;
        }
    }
    
    /**
     * Удаляет значение по пути
     */
    private boolean deleteContextValue(Map<String, Object> context, String path) {
        String[] parts = parseJsonPath(path);
        if (parts.length == 0) return false;
        
        Object current = context;
        
        // Проходим до предпоследнего элемента
        for (int i = 0; i < parts.length - 1; i++) {
            String part = parts[i];
            
            if (part.matches("\\[\\d+\\]")) {
                int index = Integer.parseInt(part.substring(1, part.length() - 1));
                if (current instanceof List) {
                    List<?> list = (List<?>) current;
                    if (index >= 0 && index < list.size()) {
                        current = list.get(index);
                    } else {
                        return false;
                    }
                } else {
                    return false;
                }
            } else {
                if (current instanceof Map) {
                    Map<?, ?> map = (Map<?, ?>) current;
                    current = map.get(part);
                    if (current == null) return false;
                } else {
                    return false;
                }
            }
        }
        
        // Удаляем финальный элемент
        String finalKey = parts[parts.length - 1];
        if (finalKey.matches("\\[\\d+\\]")) {
            int index = Integer.parseInt(finalKey.substring(1, finalKey.length() - 1));
            if (current instanceof List) {
                List<Object> list = (List<Object>) current;
                if (index >= 0 && index < list.size()) {
                    list.remove(index);
                    return true;
                }
            }
            return false;
        } else {
            if (current instanceof Map) {
                Map<String, Object> map = (Map<String, Object>) current;
                return map.remove(finalKey) != null;
            }
            return false;
        }
    }
    
    /**
     * Добавляет значение (для массивов или создания нового ключа)
     */
    private boolean addContextValue(Map<String, Object> context, String path, Object value) {
        if (path.endsWith("[]")) {
            // Добавление в массив: "users[]"
            String arrayPath = path.substring(0, path.length() - 2);
            Object arrayObj = getValueByJsonPath(context, arrayPath);
            
            if (arrayObj instanceof List) {
                ((List<Object>) arrayObj).add(value);
                return true;
            } else {
                // Создаем новый массив
                List<Object> newArray = new ArrayList<>();
                newArray.add(value);
                return setContextValue(context, arrayPath, newArray);
            }
        } else {
            // Обычное добавление как set
            return setContextValue(context, path, value);
        }
    }
    
    /**
     * Объединяет значение с существующим (для объектов)
     */
    private boolean mergeContextValue(Map<String, Object> context, String path, Object value) {
        Object existing = getValueByJsonPath(context, path);
        
        if (existing instanceof Map && value instanceof Map) {
            Map<String, Object> existingMap = (Map<String, Object>) existing;
            Map<String, Object> valueMap = (Map<String, Object>) value;
            existingMap.putAll(valueMap);
            return true;
        } else if (existing instanceof List && value instanceof List) {
            List<Object> existingList = (List<Object>) existing;
            List<Object> valueList = (List<Object>) value;
            existingList.addAll(valueList);
            return true;
        } else {
            // Если типы не совпадают, просто заменяем
            return setContextValue(context, path, value);
        }
    }
    
    /**
     * Очищает путь (удаляет все содержимое массива или объекта)
     */
    private boolean clearContextPath(Map<String, Object> context, String path) {
        Object target = getValueByJsonPath(context, path);
        
        if (target instanceof Map) {
            ((Map<?, ?>) target).clear();
            return true;
        } else if (target instanceof List) {
            ((List<?>) target).clear();
            return true;
        } else {
            // Для примитивов - устанавливаем null
            return setContextValue(context, path, null);
        }
    }
    
    // 🧮 CALCULATE - Математические вычисления
    private Map<String, Object> executeCalculate(ScenarioBlock node, Map<String, Object> context, Scenario scenario) {
        LOG.infof("Executing calculate for node: %s", node.id);
        
        if (node.parameters == null) {
            LOG.errorf("Calculate node %s has no parameters", node.id);
            return createResponse("calculate", "Ошибка: нет параметров для вычислений", null, context);
        }
        
        // Операции вычислений
        Object operationsObj = node.parameters.get("operations");
        if (operationsObj == null) {
            LOG.errorf("Calculate node %s has no operations", node.id);
            return createResponse("calculate", "Ошибка: нет операций для выполнения", null, context);
        }
        
        List<Map<String, Object>> operations = null;
        if (operationsObj instanceof List) {
            operations = (List<Map<String, Object>>) operationsObj;
        } else if (operationsObj instanceof String) {
            try {
                operations = objectMapper.readValue((String) operationsObj, List.class);
            } catch (Exception e) {
                LOG.errorf("Failed to parse operations JSON: %s", e.getMessage());
                return createResponse("calculate", "Ошибка парсинга операций", null, context);
            }
        }
        
        if (operations == null || operations.isEmpty()) {
            LOG.warnf("Calculate node %s has empty operations", node.id);
            return createResponse("calculate", "Нет операций для выполнения", getNextNode(node, context), context);
        }
        
        int successCount = 0;
        int errorCount = 0;
        
        // Выполняем математические операции
        for (Map<String, Object> operation : operations) {
            try {
                String target = (String) operation.get("target");
                String operationType = (String) operation.get("operation");
                Object value = operation.get("value");
                
                if (target == null || operationType == null) {
                    LOG.warnf("Invalid operation: target=%s, operation=%s", target, operationType);
                    errorCount++;
                    continue;
                }
                
                boolean success = executeCalculateOperation(context, target, operationType, value);
                if (success) {
                    successCount++;
                    LOG.debugf("Calculate operation successful: %s %s %s", target, operationType, value);
                } else {
                    errorCount++;
                    LOG.warnf("Calculate operation failed: %s %s %s", target, operationType, value);
                }
                
            } catch (Exception e) {
                LOG.errorf(e, "Error executing calculate operation: %s", e.getMessage());
                errorCount++;
            }
        }
        
        String nextNode = getNextNode(node, context);
        updateContext(context, nextNode);
        
        // Системный узел - сразу выполняем следующий
        if (nextNode != null) {
            ScenarioBlock nextNodeBlock = findNodeById(scenario, nextNode);
            if (nextNodeBlock != null) {
                return executeNodeByType(nextNodeBlock, "", context, scenario);
            }
        }
        
        return createResponse("calculate", String.format("Вычисления выполнены: %d успешно, %d ошибок", successCount, errorCount), nextNode, context);
    }
    
    /**
     * Выполняет математическую операцию
     */
    private boolean executeCalculateOperation(Map<String, Object> context, String target, String operation, Object value) {
        try {
            // Получаем текущее значение
            Object currentValue = getValueByJsonPath(context, target);
            double current = parseNumber(currentValue);
            double operand = parseNumber(value);
            double result;
            
            switch (operation.toLowerCase()) {
                case "add":
                case "increment":
                case "+":
                    result = current + operand;
                    break;
                    
                case "subtract":
                case "decrement": 
                case "-":
                    result = current - operand;
                    break;
                    
                case "multiply":
                case "*":
                    result = current * operand;
                    break;
                    
                case "divide":
                case "/":
                    if (operand == 0) {
                        LOG.warnf("Division by zero for target: %s", target);
                        return false;
                    }
                    result = current / operand;
                    break;
                    
                case "modulo":
                case "%":
                    if (operand == 0) {
                        LOG.warnf("Modulo by zero for target: %s", target);
                        return false;
                    }
                    result = current % operand;
                    break;
                    
                case "power":
                case "^":
                    result = Math.pow(current, operand);
                    break;
                    
                case "set":
                case "=":
                    result = operand;
                    break;
                    
                case "min":
                    result = Math.min(current, operand);
                    break;
                    
                case "max":
                    result = Math.max(current, operand);
                    break;
                    
                case "abs":
                    result = Math.abs(current);
                    break;
                    
                case "random":
                    // Random от 0 до operand
                    result = Math.random() * operand;
                    break;
                    
                default:
                    LOG.warnf("Unknown calculate operation: %s", operation);
                    return false;
            }
            
            // Сохраняем результат (как целое число если возможно)
            Object finalResult = (result == Math.floor(result)) ? (int) result : result;
            return setContextValue(context, target, finalResult);
            
        } catch (Exception e) {
            LOG.errorf(e, "Error in calculate operation %s for target %s", operation, target);
            return false;
        }
    }
    
    /**
     * Парсит число из различных типов
     */
    private double parseNumber(Object value) {
        if (value == null) return 0.0;
        if (value instanceof Number) return ((Number) value).doubleValue();
        if (value instanceof String) {
            try {
                return Double.parseDouble((String) value);
            } catch (NumberFormatException e) {
                LOG.warnf("Cannot parse number from string: %s", value);
                return 0.0;
            }
        }
        return 0.0;
    }
    
    // Вспомогательные методы
    
    private ScenarioBlock findNodeById(Scenario scenario, String nodeId) {
        if (scenario.nodes == null || nodeId == null) return null;
        return scenario.nodes.stream()
            .filter(node -> nodeId.equals(node.id))
            .findFirst()
            .orElse(null);
    }
    
    private Scenario convertMapToScenario(Map<String, Object> scenarioData) {
        Scenario scenario = new Scenario();
        scenario.id = (String) scenarioData.get("id");
        scenario.name = (String) scenarioData.get("name");
        scenario.version = (String) scenarioData.get("version");
        scenario.language = (String) scenarioData.getOrDefault("language", "uk");
        scenario.startNode = (String) scenarioData.get("start_node");
        
        // Конвертируем узлы
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> nodesList = (List<Map<String, Object>>) scenarioData.get("nodes");
        if (nodesList != null) {
            scenario.nodes = new ArrayList<>();
            for (Map<String, Object> nodeMap : nodesList) {
                ScenarioBlock block = new ScenarioBlock();
                block.id = (String) nodeMap.get("id");
                block.type = (String) nodeMap.get("type");
                block.parameters = (Map<String, Object>) nodeMap.get("parameters");
                block.nextNodes = (List<String>) nodeMap.get("next_nodes");
                block.conditions = (Map<String, String>) nodeMap.get("conditions");
                scenario.nodes.add(block);
            }
        }
        
        return scenario;
    }
    
    private String getNextNode(ScenarioBlock node, Map<String, Object> context) {
        if (node.nextNodes != null && !node.nextNodes.isEmpty()) {
            return node.nextNodes.get(0);
        }
        return null;
    }
    
    private void updateContext(Map<String, Object> context, String nextNode) {
        if (nextNode == null) {
            // ИСПРАВЛЕНО: При завершении сценария устанавливаем флаг завершения
            context.put("current_node", "");
            context.put("scenario_completed", true);
        } else {
            context.put("current_node", nextNode);
            context.put("scenario_completed", false);
        }
        context.put("last_execution_time", System.currentTimeMillis());
    }
    
    private String substituteVariables(String text, Map<String, Object> context) {
        if (text == null) return null;
        
        String result = text;
        
        // Используем регулярное выражение для поиска всех плейсхолдеров
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\{([^}]+)\\}");
        java.util.regex.Matcher matcher = pattern.matcher(text);
        
        while (matcher.find()) {
            String fullPlaceholder = matcher.group(0); // {context.api_response.data[0].name}
            String path = matcher.group(1); // context.api_response.data[0].name
            
            Object value = getValueByJsonPath(context, path);
            if (value != null) {
                result = result.replace(fullPlaceholder, String.valueOf(value));
            }
        }
        
        return result;
    }
    
    /**
     * Извлекает значение из контекста по JSONPath-подобному пути
     * Поддерживает:
     * - context.key - простое поле
     * - context.api_response.data - вложенный объект
     * - context.users[0] - элемент массива
     * - context.api_response.users[0].name - комбинация объектов и массивов
     * - api_response.data.items[1].status - без префикса context
     */
    private Object getValueByJsonPath(Map<String, Object> context, String path) {
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
        
        LOG.infof("Evaluating condition: %s", condition);
        LOG.infof("Context intent: %s", context.get("intent"));
        
        try {
            // НОВОЕ: Поддержка OR условий (||)
            if (condition.contains("||")) {
                String[] orParts = condition.split("\\|\\|");
                for (String orPart : orParts) {
                    if (evaluateCondition(orPart.trim(), context)) {
                        LOG.infof("OR condition matched: %s", orPart.trim());
                        return true;
                    }
                }
                LOG.infof("No OR conditions matched");
                return false;
            }
            
            // ИСПРАВЛЕНО: Универсальная обработка условий
            
            // Обработка равенства строк: context.intent == "value" или intent == "value"
            if (condition.contains("==")) {
                String[] parts = condition.split("==");
                if (parts.length == 2) {
                    String leftPart = parts[0].trim();
                    String rightPart = parts[1].trim().replace("\"", "");
                    
                    // Убираем context. если есть
                    if (leftPart.startsWith("context.")) {
                        leftPart = leftPart.substring(8);
                    }
                    
                    Object contextValue = context.get(leftPart);
                    String contextValueStr = contextValue != null ? contextValue.toString() : "";
                    
                    boolean result = contextValueStr.equals(rightPart);
                    LOG.infof("Condition %s: %s == %s -> %s", condition, contextValueStr, rightPart, result);
                    return result;
                }
            }
            
            // Обработка неравенства: context.intent != "value" или intent != "value"
            if (condition.contains("!=")) {
                String[] parts = condition.split("!=");
                if (parts.length == 2) {
                    String leftPart = parts[0].trim();
                    String rightPart = parts[1].trim().replace("\"", "");
                    
                    // Убираем context. если есть
                    if (leftPart.startsWith("context.")) {
                        leftPart = leftPart.substring(8);
                    }
                    
                    Object contextValue = context.get(leftPart);
                    String contextValueStr = contextValue != null ? contextValue.toString() : "";
                    
                    boolean result = !contextValueStr.equals(rightPart);
                    LOG.infof("Condition %s: %s != %s -> %s", condition, contextValueStr, rightPart, result);
                    return result;
                }
            }
            
            // Старые хардкодные условия для совместимости
            if (condition.contains("context.operation")) {
                return context.containsKey("operation");
            }
            
            if (condition.contains("context.wantsBalance == true")) {
                return Boolean.TRUE.equals(context.get("wantsBalance"));
            }
            
            if (condition.contains("context.validCard == true")) {
                return Boolean.TRUE.equals(context.get("validCard"));
            }
            
            LOG.warnf("Unknown condition format: %s", condition);
            return false;
            
        } catch (Exception e) {
            LOG.errorf(e, "Error evaluating condition: %s", condition);
            return false;
        }
    }
    
    // Поиск целевого узла по sourceHandle в edges
    private String findTargetBySourceHandle(String sourceNodeId, String sourceHandle, Scenario scenario) {
        // Пытаемся найти в edges если они есть
        if (scenario.getEdges() != null) {
            for (Object edgeObj : scenario.getEdges()) {
                if (edgeObj instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> edge = (Map<String, Object>) edgeObj;
                    
                    String source = (String) edge.get("source");
                    String handle = (String) edge.get("sourceHandle");
                    String target = (String) edge.get("target");
                    
                    if (sourceNodeId.equals(source) && sourceHandle.equals(handle)) {
                        return target;
                    }
                }
            }
        }
        return null;
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
        // ИСПРАВЛЕНО: Устанавливаем node_type в контекст для правильного отображения
        context.put("node_type", type);
        
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
    
    private String getScenarioServiceUrl() {
        return "http://localhost:8093"; // Scenario Service URL
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
    
    public String processMessage(String sessionId, String userMessage, Map<String, Object> context) {
        try {
            LOG.infof("Processing message for session %s: %s", sessionId, userMessage);
            
            // Получаем текущий узел из контекста
            String currentNodeId = (String) context.get("current_node");
            String scenarioId = (String) context.get("scenario_id");
            
            LOG.infof("Current context - nodeId: %s, scenarioId: %s", currentNodeId, scenarioId);
            
            if (currentNodeId == null || scenarioId == null) {
                LOG.errorf("Session not initialized - nodeId: %s, scenarioId: %s", currentNodeId, scenarioId);
                return "Ошибка: сессия не инициализирована";
            }
            
            // Продолжаем выполнение сценария с текущего узла
            return continueScenarioExecution(sessionId, userMessage, context, scenarioId, currentNodeId);
            
        } catch (Exception e) {
            LOG.errorf(e, "Error processing message in scenario engine");
            return "Извините, произошла ошибка при обработке сообщения.";
        }
    }
    
    private String continueScenarioExecution(String sessionId, String userMessage, Map<String, Object> context, String scenarioId, String currentNodeId) {
        try {
            // Загружаем сценарий
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8093/api/v1/scenarios/" + scenarioId))
                .header("Content-Type", "application/json")
                .GET()
                .build();
            
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                ObjectMapper mapper = new ObjectMapper();
                @SuppressWarnings("unchecked")
                Map<String, Object> scenarioResponse = mapper.readValue(response.body(), Map.class);
                
                // Извлекаем scenario_data
                @SuppressWarnings("unchecked")
                Map<String, Object> scenarioData = (Map<String, Object>) scenarioResponse.get("scenario_data");
                if (scenarioData == null) {
                    return "Ошибка: некорректные данные сценария";
                }
                
                // Конвертируем в Scenario объект
                Scenario scenario = convertMapToScenario(scenarioData);
                
                context.put("user_message", userMessage);
                
                // ИСПРАВЛЕНО: Используем новую логику вместо старой executeNodesSequentially
                ScenarioBlock currentNode = findNodeById(scenario, currentNodeId);
                if (currentNode != null) {
                    LOG.infof("Executing node %s (%s) with user input: %s", currentNodeId, currentNode.type, userMessage);
                    Map<String, Object> result = executeNodeByType(currentNode, userMessage, context, scenario);
                    return (String) result.getOrDefault("message", "Узел выполнен");
                } else {
                    LOG.errorf("Node not found: %s", currentNodeId);
                    return "Узел не найден: " + currentNodeId;
                }
            }
            
        } catch (Exception e) {
            LOG.errorf(e, "Failed to continue scenario execution");
        }
        
        return "Ошибка выполнения сценария.";
    }
    
    private String executeNodesSequentially(Map<String, Object> scenarioData, String startNodeId, Map<String, Object> context) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) scenarioData.get("scenario_data");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> nodes = (List<Map<String, Object>>) data.get("nodes");
            
            // Находим текущий узел
            Map<String, Object> node = findNodeById(nodes, startNodeId);
            if (node == null) {
                return "Узел не найден.";
            }
            
            String type = (String) node.get("type");
            @SuppressWarnings("unchecked")
            Map<String, Object> parameters = (Map<String, Object>) node.get("parameters");
            @SuppressWarnings("unchecked")
            List<String> nextNodes = (List<String>) node.get("next_nodes");
            
            if ("ask".equals(type)) {
                // Узел требует ввода пользователя
                String question = (String) parameters.get("question");
                
                // Переходим к следующему узлу для следующего сообщения пользователя
                if (nextNodes != null && !nextNodes.isEmpty()) {
                    context.put("current_node", nextNodes.get(0));
                } else {
                    context.put("current_node", null);
                }
                
                // Помечаем что это ask узел
                context.put("node_type", "ask");
                
                return question != null ? question : "Вопрос пользователю";
                
            } else if ("announce".equals(type)) {
                // Узел announce - возвращаем сообщение
                String message = (String) parameters.get("message");
                
                // Переходим к следующему узлу
                if (nextNodes != null && !nextNodes.isEmpty()) {
                    context.put("current_node", nextNodes.get(0));
                } else {
                    // Конец сценария
                    context.put("current_node", null);
                    context.put("node_type", "exit");
                    return message != null ? message : "Диалог завершен.";
                }
                
                // Помечаем что это announce узел
                context.put("node_type", "announce");
                
                return message != null ? message : "Сообщение пользователю";
                
            } else if ("transfer".equals(type)) {
                // Узел transfer - передача оператору
                String message = (String) parameters.get("message");
                
                // Завершаем диалог
                context.put("current_node", null);
                context.put("node_type", "transfer");
                
                return message != null ? message : "Передаю вас оператору...";
                
            } else if ("end".equals(type)) {
                // Узел end - завершение диалога
                String message = (String) parameters.get("message");
                
                // Завершаем диалог
                context.put("current_node", null);
                context.put("node_type", "exit");
                
                return message != null ? message : "Диалог завершен.";
                
            } else if ("nlu-request".equals(type)) {
                // Узел NLU - анализ и переход дальше
                // Просто переходим к следующему узлу (NLU выполняется в другом месте)
                
                // Переходим к следующему узлу
                if (nextNodes != null && !nextNodes.isEmpty()) {
                    context.put("current_node", nextNodes.get(0));
                    // Рекурсивно выполняем следующий узел
                    return executeNodesSequentially(scenarioData, nextNodes.get(0), context);
                } else {
                    context.put("current_node", null);
                    context.put("node_type", "exit");
                    return "Диалог завершен.";
                }
                
            } else {
                // Другие типы узлов - пока просто переходим дальше
                if (nextNodes != null && !nextNodes.isEmpty()) {
                    context.put("current_node", nextNodes.get(0));
                    // Рекурсивно выполняем следующий узел
                    return executeNodesSequentially(scenarioData, nextNodes.get(0), context);
                } else {
                    context.put("current_node", null);
                    return "Диалог завершен.";
                }
            }
            
        } catch (Exception e) {
            LOG.errorf(e, "Failed to execute nodes sequentially");
            return "Ошибка выполнения узлов.";
        }
    }
    
    private Map<String, Object> findNodeById(List<Map<String, Object>> nodes, String nodeId) {
        for (Map<String, Object> node : nodes) {
            if (nodeId.equals(node.get("id"))) {
                return node;
            }
        }
        return null;
    }
    
    public String getInitialMessageFromEntryPoint(Map<String, Object> context) {
        try {
            // Загружаем entry point сценарий
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8093/api/v1/scenarios/entry-point"))
                .header("Content-Type", "application/json")
                .GET()
                .build();
            
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                ObjectMapper mapper = new ObjectMapper();
                @SuppressWarnings("unchecked")
                Map<String, Object> scenarioData = mapper.readValue(response.body(), Map.class);
                
                String scenarioId = (String) scenarioData.get("id");
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) scenarioData.get("scenario_data");
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> nodes = (List<Map<String, Object>>) data.get("nodes");
                
                // Находим реальный стартовый узел (узел без входящих связей)
                String realStartNode = findRealStartNode(nodes, scenarioData);
                
                // Сохраняем в контекст для будущих вызовов
                context.put("scenario_id", scenarioId);
                context.put("current_node", realStartNode);
                
                // Выполняем узлы подряд до первого ask
                return executeNodesSequentially(scenarioData, realStartNode, context);
            }
            
        } catch (Exception e) {
            LOG.errorf(e, "Failed to get initial message from entry point");
        }
        
        return "Привет! Добро пожаловать в банковский чат-бот.";
    }
    
    private String findRealStartNode(List<Map<String, Object>> nodes, Map<String, Object> scenarioData) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) scenarioData.get("scenario_data");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> edges = (List<Map<String, Object>>) data.get("edges");
            
            // Собираем все узлы, которые являются target (имеют входящие связи)
            Set<String> targetNodes = new HashSet<>();
            if (edges != null) {
                for (Map<String, Object> edge : edges) {
                    String target = (String) edge.get("target");
                    if (target != null) {
                        targetNodes.add(target);
                    }
                }
            }
            
            // Находим узел, который не является target ни одной связи
            for (Map<String, Object> node : nodes) {
                String nodeId = (String) node.get("id");
                if (!targetNodes.contains(nodeId)) {
                    LOG.infof("Found real start node: %s", nodeId);
                    return nodeId;
                }
            }
            
            // Если не нашли, используем start_node из сценария
            String startNode = (String) data.get("start_node");
            LOG.infof("Using scenario start_node: %s", startNode);
            return startNode;
            
        } catch (Exception e) {
            LOG.errorf(e, "Failed to find real start node");
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) scenarioData.get("scenario_data");
            return (String) data.get("start_node");
        }
    }
    
    public String continueExecution(String sessionId, Map<String, Object> context) {
        try {
            LOG.infof("Continuing execution for session %s", sessionId);
            
            // ИСПРАВЛЕНО: Проверяем флаг завершения сценария
            Boolean scenarioCompleted = (Boolean) context.get("scenario_completed");
            if (scenarioCompleted != null && scenarioCompleted) {
                return "Диалог завершен.";
            }
            
            // Получаем текущий узел из контекста
            String currentNodeId = (String) context.get("current_node");
            String scenarioId = (String) context.get("scenario_id");
            
            if (currentNodeId == null || currentNodeId.isEmpty() || scenarioId == null) {
                return "Диалог завершен.";
            }
            
            // ОТКАТ: Используем старую логику continueScenarioExecution для continue
            return continueScenarioExecution(sessionId, "", context, scenarioId, currentNodeId);
            
        } catch (Exception e) {
            LOG.errorf(e, "Error continuing execution in scenario engine");
            return "Извините, произошла ошибка при продолжении диалога.";
        }
    }
    
    // Старая логика для continue (без пользовательского ввода)
    private String continueScenarioExecutionOld(String sessionId, Map<String, Object> context, String scenarioId, String currentNodeId) {
        try {
            // Загружаем сценарий
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8093/api/v1/scenarios/" + scenarioId))
                .header("Content-Type", "application/json")
                .GET()
                .build();
            
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                ObjectMapper mapper = new ObjectMapper();
                @SuppressWarnings("unchecked")
                Map<String, Object> scenarioData = mapper.readValue(response.body(), Map.class);
                
                // ВАЖНО: Используем старую логику executeNodesSequentially для continue
                return executeNodesSequentially(scenarioData, currentNodeId, context);
            }
            
            return "Ошибка загрузки сценария.";
            
        } catch (Exception e) {
            LOG.errorf(e, "Error in continueScenarioExecutionOld");
            return "Ошибка выполнения сценария.";
        }
    }
}
