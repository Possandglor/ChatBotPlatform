package com.pb.chatbot.chat.service;

import com.pb.chatbot.chat.client.ScenarioServiceClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class ScenarioBasedChatService {
    
    private static final Logger LOG = Logger.getLogger(ScenarioBasedChatService.class);
    private final Map<String, SessionState> sessions = new ConcurrentHashMap<>();
    
    @Inject
    @RestClient
    ScenarioServiceClient scenarioClient;
    
    public String createSession(String sessionId) {
        try {
            // Получаем стартовый сценарий
            Map<String, Object> entryPointResponse = scenarioClient.getEntryPointScenario();
            
            SessionState state = new SessionState();
            state.currentScenarioId = (String) entryPointResponse.get("id");
            state.currentScenario = entryPointResponse;
            
            sessions.put(sessionId, state);
            
            LOG.infof("Created session %s with entry point scenario: %s", 
                     sessionId, entryPointResponse.get("name"));
            
            return getInitialMessage(state);
            
        } catch (Exception e) {
            LOG.errorf("Failed to load entry point scenario: %s", e.getMessage());
            // Fallback к старому поведению
            SessionState state = new SessionState();
            sessions.put(sessionId, state);
            return "Привет! Я помогу вам проверить баланс карты.";
        }
    }
    
    private String getInitialMessage(SessionState state) {
        if (state.currentScenario == null) {
            return "Привет! Как дела?";
        }
        
        @SuppressWarnings("unchecked")
        Map<String, Object> scenarioData = (Map<String, Object>) state.currentScenario.get("scenario_data");
        if (scenarioData == null) {
            return "Привет! Как дела?";
        }
        
        String startNode = (String) scenarioData.get("start_node");
        if (startNode == null) {
            return "Привет! Как дела?";
        }
        
        // Найти стартовый узел и получить его сообщение
        @SuppressWarnings("unchecked")
        java.util.List<Map<String, Object>> nodes = (java.util.List<Map<String, Object>>) scenarioData.get("nodes");
        if (nodes != null) {
            for (Map<String, Object> node : nodes) {
                if (startNode.equals(node.get("id"))) {
                    // Проверяем поле content (новый формат)
                    if (node.containsKey("content")) {
                        state.currentStep = startNode;
                        return (String) node.get("content");
                    }
                    // Fallback к старому формату parameters.message
                    @SuppressWarnings("unchecked")
                    Map<String, Object> parameters = (Map<String, Object>) node.get("parameters");
                    if (parameters != null && parameters.containsKey("message")) {
                        state.currentStep = startNode;
                        return (String) parameters.get("message");
                    }
                }
            }
        }
        
        return "Привет! Как дела?";
    }
    
    public String processMessage(String sessionId, String message) {
        SessionState state = sessions.get(sessionId);
        if (state == null) {
            return "Сессия не найдена. Создайте новую сессию.";
        }
        
        String response = executeScenarioStep(state, message.toLowerCase().trim());
        
        LOG.infof("Session %s: %s -> %s (step: %s, scenario: %s)", 
                 sessionId, message, response, state.currentStep, state.currentScenarioId);
        
        return response;
    }
    
    private String executeScenarioStep(SessionState state, String message) {
        // Если есть загруженный сценарий, используем его
        if (state.currentScenario != null) {
            return executeFromScenario(state, message);
        }
        
        // Fallback к старой логике
        return executeOldLogic(state, message);
    }
    
    private String executeFromScenario(SessionState state, String message) {
        @SuppressWarnings("unchecked")
        Map<String, Object> scenarioData = (Map<String, Object>) state.currentScenario.get("scenario_data");
        if (scenarioData == null) {
            return executeOldLogic(state, message);
        }
        
        @SuppressWarnings("unchecked")
        java.util.List<Map<String, Object>> nodes = (java.util.List<Map<String, Object>>) scenarioData.get("nodes");
        if (nodes == null) {
            return executeOldLogic(state, message);
        }
        
        // Найти текущий узел
        Map<String, Object> currentNode = null;
        for (Map<String, Object> node : nodes) {
            if (state.currentStep.equals(node.get("id"))) {
                currentNode = node;
                break;
            }
        }
        
        if (currentNode == null) {
            return "Узел сценария не найден.";
        }
        
        String nodeType = (String) currentNode.get("type");
        @SuppressWarnings("unchecked")
        Map<String, Object> parameters = (Map<String, Object>) currentNode.get("parameters");
        
        switch (nodeType) {
            case "announce":
                // Переход к следующему узлу
                @SuppressWarnings("unchecked")
                java.util.List<String> nextNodes = (java.util.List<String>) currentNode.get("next_nodes");
                if (nextNodes != null && !nextNodes.isEmpty()) {
                    state.currentStep = nextNodes.get(0);
                    return processNextNode(state, nodes);
                }
                return "Диалог завершен.";
                
            case "ask":
                // Сохранить ответ пользователя и перейти к следующему узлу
                if (parameters != null) {
                    state.context.put("last_answer", message);
                }
                @SuppressWarnings("unchecked")
                java.util.List<String> askNextNodes = (java.util.List<String>) currentNode.get("next_nodes");
                if (askNextNodes != null && !askNextNodes.isEmpty()) {
                    state.currentStep = askNextNodes.get(0);
                    // Обработать следующий узел вместо возврата шаблонного ответа
                    return processNextNode(state, nodes);
                }
                return "Спасибо за ответ!";
                
            default:
                return executeOldLogic(state, message);
        }
    }
    
    private String processNextNode(SessionState state, java.util.List<Map<String, Object>> nodes) {
        for (Map<String, Object> node : nodes) {
            if (state.currentStep.equals(node.get("id"))) {
                String nodeType = (String) node.get("type");
                
                if ("ask".equals(nodeType)) {
                    // Проверяем поле content (новый формат)
                    if (node.containsKey("content")) {
                        return (String) node.get("content");
                    }
                    // Fallback к старому формату
                    @SuppressWarnings("unchecked")
                    Map<String, Object> parameters = (Map<String, Object>) node.get("parameters");
                    if (parameters != null) {
                        return (String) parameters.get("question");
                    }
                } else if ("announce".equals(nodeType)) {
                    // Проверяем поле content (новый формат)
                    if (node.containsKey("content")) {
                        String message = (String) node.get("content");
                        // Подстановка переменных
                        if (message != null && message.contains("{last_answer}")) {
                            String lastAnswer = (String) state.context.get("last_answer");
                            if (lastAnswer != null) {
                                message = message.replace("{last_answer}", lastAnswer);
                            }
                        }
                        return message;
                    }
                    // Fallback к старому формату
                    @SuppressWarnings("unchecked")
                    Map<String, Object> parameters = (Map<String, Object>) node.get("parameters");
                    if (parameters != null) {
                        String message = (String) parameters.get("message");
                        // Подстановка переменных
                        if (message != null && message.contains("{last_answer}")) {
                            String lastAnswer = (String) state.context.get("last_answer");
                            if (lastAnswer != null) {
                                message = message.replace("{last_answer}", lastAnswer);
                            }
                        }
                        return message;
                    }
                }
            }
        }
        return "Что-то еще?";
    }
    
    private String executeOldLogic(SessionState state, String message) {
        switch (state.currentStep) {
            case "start":
                state.currentStep = "greeting";
                return "Привет! Я помогу вам проверить баланс карты.";
                
            case "greeting":
                state.currentStep = "ask_balance";
                return "Хотите проверить баланс карты? (да/нет)";
                
            case "ask_balance":
                if (message.contains("да") || message.contains("yes")) {
                    state.currentStep = "ask_card";
                    state.wantsBalance = true;
                    return "Введите последние 4 цифры карты:";
                } else if (message.contains("нет") || message.contains("no")) {
                    state.currentStep = "end";
                    return "Хорошо, если понадобится помощь - обращайтесь!";
                } else {
                    return "Пожалуйста, ответьте 'да' или 'нет'";
                }
                
            case "ask_card":
                if (message.matches("\\d{4}")) {
                    state.cardNumber = message;
                    state.currentStep = "show_balance";
                    return String.format("Баланс карты ****%s: 15,250.50 грн. Спасибо за обращение!", message);
                } else {
                    return "Неверный формат. Введите 4 цифры:";
                }
                
            case "show_balance":
                state.currentStep = "end";
                return "До свидания! Обращайтесь еще.";
                
            case "end":
                return "Сессия завершена. Создайте новую сессию для нового диалога.";
                
            default:
                return "Что-то пошло не так. Начнем сначала.";
        }
    }
    
    public int getActiveSessionsCount() {
        return sessions.size();
    }
    
    public void clearSession(String sessionId) {
        sessions.remove(sessionId);
    }
    
    private static class SessionState {
        String currentStep = "start";
        String currentScenarioId = null;
        Map<String, Object> currentScenario = null;
        boolean wantsBalance = false;
        String cardNumber = null;
        Map<String, Object> context = new HashMap<>();
    }
}
