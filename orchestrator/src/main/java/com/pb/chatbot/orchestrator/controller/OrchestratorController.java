package com.pb.chatbot.orchestrator.controller;

import com.pb.chatbot.orchestrator.client.ChatServiceClient;
import com.pb.chatbot.orchestrator.client.ScenarioServiceClient;
import com.pb.chatbot.orchestrator.engine.AdvancedScenarioEngine;
import com.pb.chatbot.orchestrator.model.Scenario;
import com.pb.chatbot.orchestrator.model.ScenarioBlock;
import com.pb.chatbot.orchestrator.service.ScenarioTransitionService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Path("/api/v1/orchestrator")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class OrchestratorController {
    
    private static final Logger LOG = Logger.getLogger(OrchestratorController.class);
    
    // In-memory хранилище контекста сессий
    private static final Map<String, Map<String, Object>> sessionContexts = new ConcurrentHashMap<>();
    
    // In-memory хранилище диалогов
    private static final Map<String, Map<String, Object>> dialogs = new ConcurrentHashMap<>();
    
    // In-memory хранилище сообщений диалогов
    private static final Map<String, List<Map<String, Object>>> dialogMessages = new ConcurrentHashMap<>();
    
    // In-memory хранилище логов
    private static final List<Map<String, Object>> systemLogs = new ArrayList<>();
    
    @Inject
    AdvancedScenarioEngine scenarioEngine;
    
    @Inject
    ScenarioTransitionService transitionService;
    
    @RestClient
    ChatServiceClient chatClient;
    
    @RestClient
    ScenarioServiceClient scenarioClient;
    
    @GET
    @Path("/status")
    public Response getStatus() {
        return Response.ok(Map.of(
            "service", "orchestrator",
            "status", "running",
            "role", "main_coordinator",
            "active_sessions", sessionContexts.size(),
            "total_dialogs", dialogs.size(),
            "total_logs", systemLogs.size(),
            "timestamp", System.currentTimeMillis()
        )).build();
    }
    
    @GET
    @Path("/dialogs")
    public Response getDialogs() {
        List<Map<String, Object>> dialogList = new ArrayList<>();
        
        for (Map<String, Object> dialog : dialogs.values()) {
            Map<String, Object> dialogInfo = new HashMap<>(dialog);
            String sessionId = (String) dialog.get("session_id");
            
            // Добавляем количество сообщений
            List<Map<String, Object>> messages = dialogMessages.get(sessionId);
            dialogInfo.put("message_count", messages != null ? messages.size() : 0);
            
            dialogList.add(dialogInfo);
        }
        
        return Response.ok(Map.of(
            "dialogs", dialogList,
            "total", dialogList.size(),
            "timestamp", System.currentTimeMillis()
        )).build();
    }
    
    @GET
    @Path("/dialogs/{sessionId}")
    public Response getDialog(@PathParam("sessionId") String sessionId) {
        Map<String, Object> dialog = dialogs.get(sessionId);
        if (dialog == null) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity(Map.of("error", "Dialog not found"))
                .build();
        }
        
        List<Map<String, Object>> messages = dialogMessages.getOrDefault(sessionId, new ArrayList<>());
        
        Map<String, Object> result = new HashMap<>(dialog);
        result.put("messages", messages);
        
        return Response.ok(result).build();
    }
    
    @GET
    @Path("/logs")
    public Response getLogs(@QueryParam("limit") Integer limit) {
        int logLimit = limit != null ? Math.min(limit, 1000) : 100;
        
        List<Map<String, Object>> recentLogs = systemLogs.stream()
            .skip(Math.max(0, systemLogs.size() - logLimit))
            .toList();
        
        return Response.ok(Map.of(
            "logs", recentLogs,
            "total", systemLogs.size(),
            "showing", recentLogs.size(),
            "timestamp", System.currentTimeMillis()
        )).build();
    }
    
    @GET
    @Path("/logs/{sessionId}")
    public Response getDialogLogs(@PathParam("sessionId") String sessionId, @QueryParam("limit") Integer limit) {
        int logLimit = limit != null ? Math.min(limit, 1000) : 100;
        
        List<Map<String, Object>> dialogLogs = systemLogs.stream()
            .filter(log -> sessionId.equals(log.get("session_id")))
            .limit(logLimit)
            .toList();
        
        return Response.ok(Map.of(
            "logs", dialogLogs,
            "session_id", sessionId,
            "total", dialogLogs.size(),
            "timestamp", System.currentTimeMillis()
        )).build();
    }
    
    @POST
    @Path("/sessions")
    public Response createSession() {
        try {
            // Создать сессию через Chat Service
            Map<String, Object> session = chatClient.createSession();
            
            LOG.infof("Created session via Chat Service: %s", session.get("session_id"));
            
            return Response.ok(session).build();
            
        } catch (Exception e) {
            LOG.errorf(e, "Error creating session");
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(Map.of("error", "Failed to create session"))
                .build();
        }
    }
    
    @POST
    @Path("/process")
    public Response processMessage(Map<String, Object> request) {
        try {
            String sessionId = (String) request.get("session_id");
            String userInput = (String) request.get("message");
            String scenarioId = (String) request.get("scenario_id");
            
            if (sessionId == null || userInput == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "session_id and message are required"))
                    .build();
            }
            
            LOG.infof("Processing message for session %s: %s", sessionId, userInput);
            
            // 1. Получить контекст сессии (сначала для определения scenario_id)
            Map<String, Object> sessionContext = getSessionContext(sessionId, scenarioId);
            
            // 2. Если scenario_id не передан, берем из контекста
            if (scenarioId == null) {
                scenarioId = (String) sessionContext.getOrDefault("scenario_id", "greeting-001");
            }
            
            // 3. Получить или создать сценарий
            Scenario scenario = getScenario(scenarioId);
            
            // 4. Выполнить сценарий через полный engine
            Map<String, Object> result = scenarioEngine.executeScenario(scenario, userInput, sessionContext);
            
            String botResponse = (String) result.get("bot_response"); // Используем "bot_response"
            if (botResponse == null) {
                botResponse = (String) result.get("message"); // Fallback на "message"
            }
            String nextNode = (String) result.get("next_node");
            String responseType = (String) result.get("type");
            
            LOG.infof("DEBUG: result keys: %s", result.keySet());
            LOG.infof("DEBUG: bot_response from result: %s", botResponse);
            
            // 5. Сохранить сообщение в диалог
            saveDialogMessage(sessionId, userInput, botResponse, scenarioId);
            
            // 6. Сохранить обновленный контекст
            saveSessionContext(sessionId, (Map<String, Object>) result.get("context"));
            
            // 7. Добавить лог
            addSystemLog("MESSAGE_PROCESSED", sessionId, userInput, botResponse, responseType, nextNode);
            
            // 8. Подготовить ответ
            Map<String, Object> response = createResponse(sessionId, userInput, result);
            
            LOG.infof("Scenario execution completed for session %s", sessionId);
            
            return Response.ok(response).build();
            
        } catch (Exception e) {
            LOG.errorf(e, "Error processing message");
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(Map.of("error", "Processing failed: " + e.getMessage()))
                .build();
        }
    }
    
    private Map<String, Object> getSessionContext(String sessionId, String scenarioId) {
        // Сначала проверяем in-memory хранилище
        Map<String, Object> context = sessionContexts.get(sessionId);
        if (context != null) {
            LOG.infof("Found existing context for session %s", sessionId);
            return context;
        }
        
        try {
            // Попытка загрузить из Chat Service
            Map<String, Object> contextResponse = chatClient.getContext(sessionId);
            context = (Map<String, Object>) contextResponse.get("context");
            if (context != null) {
                sessionContexts.put(sessionId, context); // Кешируем
                return context;
            }
        } catch (Exception e) {
            LOG.warnf("Failed to load context from Chat Service for session %s: %s", sessionId, e.getMessage());
        }
        
        // Создаем новый контекст
        LOG.infof("Creating new context for session %s", sessionId);
        Map<String, Object> newContext = new HashMap<>();
        newContext.put("current_node", null);
        newContext.put("scenario_id", scenarioId);
        newContext.put("session_start", System.currentTimeMillis());
        
        sessionContexts.put(sessionId, newContext); // Сохраняем в память
        return newContext;
    }
    
    private Scenario getScenario(String scenarioId) {
        try {
            // Пытаемся загрузить из Scenario Service
            LOG.infof("Loading scenario from service: %s", scenarioId);
            Map<String, Object> scenarioData = scenarioClient.getScenario(scenarioId);
            Scenario scenario = mapScenarioFromService(scenarioData);
            LOG.infof("Successfully loaded scenario from service: %s", scenarioId);
            return scenario;
        } catch (Exception e) {
            LOG.warnf("Failed to load scenario %s from service: %s", scenarioId, e.getMessage());
            // Fallback на встроенные сценарии
            LOG.infof("Using built-in scenario: %s", scenarioId);
            return createBuiltInScenario(scenarioId);
        }
    }
    
    private void saveSessionContext(String sessionId, Map<String, Object> context) {
        // Сохраняем в in-memory хранилище
        sessionContexts.put(sessionId, context);
        LOG.infof("Context saved for session %s", sessionId);
        
        try {
            // Пытаемся также сохранить в Chat Service
            chatClient.updateContext(sessionId, context);
        } catch (Exception e) {
            LOG.warnf("Failed to save context to Chat Service for session %s: %s", sessionId, e.getMessage());
        }
    }
    
    private void saveDialogMessage(String sessionId, String userMessage, String botResponse, String scenarioId) {
        LOG.infof("DEBUG: saveDialogMessage called with botResponse: %s", botResponse);
        
        // Создаем или обновляем диалог
        dialogs.computeIfAbsent(sessionId, k -> {
            Map<String, Object> dialog = new HashMap<>();
            dialog.put("session_id", sessionId);
            dialog.put("scenario_id", scenarioId);
            dialog.put("start_time", System.currentTimeMillis());
            dialog.put("status", "active");
            dialog.put("message_count", 0);
            LOG.infof("Created new dialog for session: %s", sessionId);
            return dialog;
        });
        
        // Добавляем сообщение
        List<Map<String, Object>> messages = dialogMessages.computeIfAbsent(sessionId, k -> new ArrayList<>());
        
        // Сообщение пользователя
        Map<String, Object> userMsg = new HashMap<>();
        userMsg.put("timestamp", System.currentTimeMillis());
        userMsg.put("sender", "user");
        userMsg.put("message", userMessage);
        messages.add(userMsg);
        
        // Сохранить сообщение пользователя в Chat Service
        try {
            Map<String, Object> chatMessage = new HashMap<>();
            chatMessage.put("session_id", sessionId);
            chatMessage.put("content", userMessage);
            chatClient.addMessage(chatMessage);
            LOG.infof("User message saved to Chat Service: %s", userMessage);
        } catch (Exception e) {
            LOG.errorf(e, "Failed to save user message to Chat Service");
        }
        
        // Ответ бота
        Map<String, Object> botMsg = new HashMap<>();
        botMsg.put("timestamp", System.currentTimeMillis());
        botMsg.put("sender", "bot");
        botMsg.put("message", botResponse);
        messages.add(botMsg);
        
        LOG.infof("DEBUG: Bot message saved with: %s", botResponse);
        
        // Обновляем счетчик сообщений
        Map<String, Object> dialog = dialogs.get(sessionId);
        dialog.put("message_count", messages.size());
        dialog.put("last_message", userMessage);
        dialog.put("updated_at", System.currentTimeMillis());
    }
    
    private void addSystemLog(String action, String sessionId, String userMessage, String botResponse, String responseType, String nextNode) {
        Map<String, Object> log = new HashMap<>();
        log.put("timestamp", System.currentTimeMillis());
        log.put("action", action);
        log.put("session_id", sessionId);
        log.put("user_message", userMessage);
        log.put("bot_response", botResponse);
        log.put("response_type", responseType);
        log.put("next_node", nextNode);
        log.put("service", "orchestrator");
        log.put("level", "INFO");
        log.put("class", "OrchestratorController");
        
        systemLogs.add(log);
        
        // Ограничиваем размер логов (последние 1000)
        if (systemLogs.size() > 1000) {
            systemLogs.remove(0);
        }
    }
    
    private Map<String, Object> createResponse(String sessionId, String userInput, Map<String, Object> result) {
        Map<String, Object> response = new HashMap<>();
        response.put("session_id", sessionId);
        response.put("user_message", userInput);
        response.put("bot_response", result.get("message"));
        response.put("type", result.get("type"));
        response.put("next_node", result.get("next_node"));
        response.put("context_updated", true);
        response.put("timestamp", System.currentTimeMillis());
        return response;
    }
    
    private Scenario createBuiltInScenario(String scenarioId) {
        if ("main-menu-nlu-001".equals(scenarioId)) {
            return createMainMenuWithNlu();
        } else if ("api-test-001".equals(scenarioId)) {
            return createApiTestScenario();
        }
        
        // Старый сценарий для совместимости
        Scenario scenario = new Scenario();
        scenario.id = scenarioId;
        scenario.name = "Операции с картой";
        scenario.startNode = "greeting";
        scenario.nodes = new java.util.ArrayList<>();
        
        // Узел приветствия
        ScenarioBlock greeting = new ScenarioBlock();
        greeting.id = "greeting";
        greeting.type = "announce";
        greeting.parameters = new HashMap<>();
        greeting.parameters.put("message", "Привет! Что вас интересует?\n1. Проверить баланс\n2. Закрыть карту\n3. Заблокировать карту\n4. История операций\n5. Связаться с поддержкой");
        greeting.nextNodes = java.util.List.of("ask_operation");
        scenario.nodes.add(greeting);
        
        // Остальные узлы...
        addOldScenarioNodes(scenario);
        
        return scenario;
    }
    
    private Scenario createApiTestScenario() {
        Scenario scenario = new Scenario();
        scenario.id = "api-test-001";
        scenario.name = "Тест API интеграции";
        scenario.startNode = "api_greeting";
        scenario.nodes = new java.util.ArrayList<>();
        
        // Приветствие
        ScenarioBlock greeting = new ScenarioBlock();
        greeting.id = "api_greeting";
        greeting.type = "announce";
        greeting.parameters = new HashMap<>();
        greeting.parameters.put("message", "🚀 Тестируем API интеграцию с localhost:8181\nВыберите:\n1. GET /api/info\n2. POST /api/data");
        greeting.nextNodes = java.util.List.of("ask_api_action");
        scenario.nodes.add(greeting);
        
        // Запрос действия
        ScenarioBlock askAction = new ScenarioBlock();
        askAction.id = "ask_api_action";
        askAction.type = "ask";
        askAction.parameters = new HashMap<>();
        askAction.parameters.put("question", "Введите 1 или 2:");
        askAction.nextNodes = java.util.List.of("route_api_action");
        scenario.nodes.add(askAction);
        
        // Маршрутизация
        ScenarioBlock routeAction = new ScenarioBlock();
        routeAction.id = "route_api_action";
        routeAction.type = "condition";
        routeAction.parameters = new HashMap<>();
        routeAction.parameters.put("condition", "input");
        routeAction.conditions = new HashMap<>();
        routeAction.conditions.put("1", "get_api_info");
        routeAction.conditions.put("2", "post_api_data");
        routeAction.conditions.put("default", "unknown_api_action");
        scenario.nodes.add(routeAction);
        
        // GET запрос
        ScenarioBlock getInfo = new ScenarioBlock();
        getInfo.id = "get_api_info";
        getInfo.type = "api-request";
        getInfo.parameters = new HashMap<>();
        getInfo.parameters.put("service", "external-api");
        getInfo.parameters.put("endpoint", "/api/info");
        getInfo.parameters.put("method", "GET");
        getInfo.parameters.put("baseUrl", "http://localhost:8181");
        getInfo.parameters.put("timeout", 5000);
        getInfo.conditions = new HashMap<>();
        getInfo.conditions.put("success", "show_api_info");
        getInfo.conditions.put("error", "api_error");
        scenario.nodes.add(getInfo);
        
        // Показ результата GET
        ScenarioBlock showInfo = new ScenarioBlock();
        showInfo.id = "show_api_info";
        showInfo.type = "announce";
        showInfo.parameters = new HashMap<>();
        showInfo.parameters.put("message", "✅ GET успешен!\n📊 Сервис: {api_response.service}\n📈 Версия: {api_response.version}\n📋 Запросов: {api_response.stats.requests}");
        showInfo.nextNodes = java.util.List.of("ask_more_api");
        scenario.nodes.add(showInfo);
        
        // POST запрос
        ScenarioBlock postData = new ScenarioBlock();
        postData.id = "post_api_data";
        postData.type = "api-request";
        postData.parameters = new HashMap<>();
        postData.parameters.put("service", "external-api");
        postData.parameters.put("endpoint", "/api/data");
        postData.parameters.put("method", "POST");
        postData.parameters.put("baseUrl", "http://localhost:8181");
        postData.parameters.put("timeout", 5000);
        
        Map<String, Object> postDataMap = new HashMap<>();
        postDataMap.put("message", "Привет от чат-бота!");
        postDataMap.put("test", "integration");
        postDataMap.put("session_id", "{session_id}");
        postData.parameters.put("data", postDataMap);
        
        postData.conditions = new HashMap<>();
        postData.conditions.put("success", "show_post_result");
        postData.conditions.put("error", "api_error");
        scenario.nodes.add(postData);
        
        // Показ результата POST
        ScenarioBlock showPost = new ScenarioBlock();
        showPost.id = "show_post_result";
        showPost.type = "announce";
        showPost.parameters = new HashMap<>();
        showPost.parameters.put("message", "✅ POST успешен!\n🆔 ID: {api_response.processed.id}\n📥 Получено: {api_response.received.message}\n📊 Статус: {api_response.status}");
        showPost.nextNodes = java.util.List.of("ask_more_api");
        scenario.nodes.add(showPost);
        
        // Ошибка API
        ScenarioBlock apiError = new ScenarioBlock();
        apiError.id = "api_error";
        apiError.type = "announce";
        apiError.parameters = new HashMap<>();
        apiError.parameters.put("message", "❌ Ошибка API! Проверьте доступность localhost:8181");
        apiError.nextNodes = java.util.List.of("ask_more_api");
        scenario.nodes.add(apiError);
        
        // Неизвестное действие
        ScenarioBlock unknownAction = new ScenarioBlock();
        unknownAction.id = "unknown_api_action";
        unknownAction.type = "announce";
        unknownAction.parameters = new HashMap<>();
        unknownAction.parameters.put("message", "❓ Введите 1 или 2");
        unknownAction.nextNodes = java.util.List.of("ask_api_action");
        scenario.nodes.add(unknownAction);
        
        // Еще тесты?
        ScenarioBlock askMore = new ScenarioBlock();
        askMore.id = "ask_more_api";
        askMore.type = "ask";
        askMore.parameters = new HashMap<>();
        askMore.parameters.put("question", "Еще тесты? (да/нет)");
        askMore.nextNodes = java.util.List.of("check_more_api");
        scenario.nodes.add(askMore);
        
        // Проверка продолжения
        ScenarioBlock checkMore = new ScenarioBlock();
        checkMore.id = "check_more_api";
        checkMore.type = "condition";
        checkMore.parameters = new HashMap<>();
        checkMore.parameters.put("condition", "input.toLowerCase().contains('да')");
        checkMore.conditions = new HashMap<>();
        checkMore.conditions.put("true", "api_greeting");
        checkMore.conditions.put("false", "api_goodbye");
        checkMore.conditions.put("default", "api_goodbye");
        scenario.nodes.add(checkMore);
        
        // Прощание
        ScenarioBlock goodbye = new ScenarioBlock();
        goodbye.id = "api_goodbye";
        goodbye.type = "announce";
        goodbye.parameters = new HashMap<>();
        goodbye.parameters.put("message", "🎉 Тест API завершен!");
        goodbye.nextNodes = java.util.List.of();
        scenario.nodes.add(goodbye);
        
        return scenario;
    }
    
    private Scenario createMainMenuWithNlu() {
        Scenario scenario = new Scenario();
        scenario.id = "main-menu-nlu-001";
        scenario.name = "Главное меню с NLU";
        scenario.startNode = "greeting";
        scenario.nodes = new java.util.ArrayList<>();
        
        // 1. Приветствие
        ScenarioBlock greeting = new ScenarioBlock();
        greeting.id = "greeting";
        greeting.type = "announce";
        greeting.parameters = new HashMap<>();
        greeting.parameters.put("message", "Привет! Я ваш банковский помощник. Що саме вас цікавить?");
        greeting.nextNodes = java.util.List.of("wait_for_request");
        scenario.nodes.add(greeting);
        
        // 2. Ожидание запроса
        ScenarioBlock waitRequest = new ScenarioBlock();
        waitRequest.id = "wait_for_request";
        waitRequest.type = "ask";
        waitRequest.parameters = new HashMap<>();
        waitRequest.parameters.put("question", "Опишите, чем могу помочь:");
        waitRequest.parameters.put("inputType", "text");
        waitRequest.nextNodes = java.util.List.of("nlu_analysis");
        scenario.nodes.add(waitRequest);
        
        // 3. NLU анализ
        ScenarioBlock nluAnalysis = new ScenarioBlock();
        nluAnalysis.id = "nlu_analysis";
        nluAnalysis.type = "nlu-request";
        nluAnalysis.parameters = new HashMap<>();
        nluAnalysis.parameters.put("service", "nlu-service");
        nluAnalysis.parameters.put("endpoint", "/api/v1/nlu/analyze");
        nluAnalysis.nextNodes = java.util.List.of("route_by_intent");
        nluAnalysis.conditions = new HashMap<>();
        nluAnalysis.conditions.put("success", "route_by_intent");
        nluAnalysis.conditions.put("error", "nlu_error");
        scenario.nodes.add(nluAnalysis);
        
        // 4. Маршрутизация по интенту
        ScenarioBlock routeByIntent = new ScenarioBlock();
        routeByIntent.id = "route_by_intent";
        routeByIntent.type = "condition";
        routeByIntent.parameters = new HashMap<>();
        routeByIntent.parameters.put("condition", "context.intent");
        routeByIntent.conditions = new HashMap<>();
        routeByIntent.conditions.put("check_balance", "balance_flow");
        routeByIntent.conditions.put("block_card", "block_flow");
        routeByIntent.conditions.put("close_account", "close_flow");
        routeByIntent.conditions.put("transfer_money", "transfer_flow");
        routeByIntent.conditions.put("get_statement", "statement_flow");
        routeByIntent.conditions.put("complaint", "complaint_flow");
        routeByIntent.conditions.put("greeting", "greeting_response");
        routeByIntent.conditions.put("unknown", "unknown_intent");
        routeByIntent.conditions.put("default", "clarification");
        scenario.nodes.add(routeByIntent);
        
        // 5. Потоки для каждого интента
        addIntentFlows(scenario);
        
        return scenario;
    }
    
    private void addIntentFlows(Scenario scenario) {
        // Поток проверки баланса
        ScenarioBlock balanceFlow = new ScenarioBlock();
        balanceFlow.id = "balance_flow";
        balanceFlow.type = "announce";
        balanceFlow.parameters = new HashMap<>();
        balanceFlow.parameters.put("message", "Проверяю баланс карты...");
        balanceFlow.nextNodes = java.util.List.of("show_balance");
        scenario.nodes.add(balanceFlow);
        
        ScenarioBlock showBalance = new ScenarioBlock();
        showBalance.id = "show_balance";
        showBalance.type = "announce";
        showBalance.parameters = new HashMap<>();
        showBalance.parameters.put("message", "💳 Баланс карты ****1234: 15,250.50 грн\n💰 Доступно: 15,250.50 грн");
        showBalance.nextNodes = java.util.List.of("ask_more_help");
        scenario.nodes.add(showBalance);
        
        // Поток блокировки карты
        ScenarioBlock blockFlow = new ScenarioBlock();
        blockFlow.id = "block_flow";
        blockFlow.type = "announce";
        blockFlow.parameters = new HashMap<>();
        blockFlow.parameters.put("message", "Блокирую карту для вашей безопасности...");
        blockFlow.nextNodes = java.util.List.of("confirm_block");
        scenario.nodes.add(blockFlow);
        
        ScenarioBlock confirmBlock = new ScenarioBlock();
        confirmBlock.id = "confirm_block";
        confirmBlock.type = "announce";
        confirmBlock.parameters = new HashMap<>();
        confirmBlock.parameters.put("message", "🔒 Карта успешно заблокирована!\n📱 SMS с подтверждением отправлено\n📞 Для разблокировки: 0 800 123 456");
        confirmBlock.nextNodes = java.util.List.of("ask_more_help");
        scenario.nodes.add(confirmBlock);
        
        // Поток закрытия счета
        ScenarioBlock closeFlow = new ScenarioBlock();
        closeFlow.id = "close_flow";
        closeFlow.type = "announce";
        closeFlow.parameters = new HashMap<>();
        closeFlow.parameters.put("message", "Создаю заявку на закрытие счета...");
        closeFlow.nextNodes = java.util.List.of("close_request");
        scenario.nodes.add(closeFlow);
        
        ScenarioBlock closeRequest = new ScenarioBlock();
        closeRequest.id = "close_request";
        closeRequest.type = "announce";
        closeRequest.parameters = new HashMap<>();
        closeRequest.parameters.put("message", "📋 Заявка создана: REQ-" + System.currentTimeMillis() + "\n📧 Детали отправлены на email\n👤 Менеджер свяжется в течение дня");
        closeRequest.nextNodes = java.util.List.of("ask_more_help");
        scenario.nodes.add(closeRequest);
        
        // Общие узлы
        addCommonNodes(scenario);
    }
    
    private void addCommonNodes(Scenario scenario) {
        // Неизвестный интент
        ScenarioBlock unknownIntent = new ScenarioBlock();
        unknownIntent.id = "unknown_intent";
        unknownIntent.type = "announce";
        unknownIntent.parameters = new HashMap<>();
        unknownIntent.parameters.put("message", "Не смог понять ваш запрос. Соединяю с оператором...\n📞 Ожидайте, пожалуйста");
        unknownIntent.nextNodes = java.util.List.of("end_session");
        scenario.nodes.add(unknownIntent);
        
        // Уточнение
        ScenarioBlock clarification = new ScenarioBlock();
        clarification.id = "clarification";
        clarification.type = "announce";
        clarification.parameters = new HashMap<>();
        clarification.parameters.put("message", "Уточните, пожалуйста:\n💳 Баланс карты\n🔒 Блокировка карты\n📋 Закрытие счета\n💸 Перевод денег");
        clarification.nextNodes = java.util.List.of("wait_for_request");
        scenario.nodes.add(clarification);
        
        // Ошибка NLU
        ScenarioBlock nluError = new ScenarioBlock();
        nluError.id = "nlu_error";
        nluError.type = "announce";
        nluError.parameters = new HashMap<>();
        nluError.parameters.put("message", "Произошла техническая ошибка. Соединяю с оператором...");
        nluError.nextNodes = java.util.List.of("end_session");
        scenario.nodes.add(nluError);
        
        // Спросить еще помощь
        ScenarioBlock askMoreHelp = new ScenarioBlock();
        askMoreHelp.id = "ask_more_help";
        askMoreHelp.type = "ask";
        askMoreHelp.parameters = new HashMap<>();
        askMoreHelp.parameters.put("question", "Могу ли еще чем-то помочь?");
        askMoreHelp.nextNodes = java.util.List.of("parse_more_help");
        scenario.nodes.add(askMoreHelp);
        
        // Парсинг ответа на помощь
        ScenarioBlock parseMoreHelp = new ScenarioBlock();
        parseMoreHelp.id = "parse_more_help";
        parseMoreHelp.type = "nlu-request";
        parseMoreHelp.parameters = new HashMap<>();
        parseMoreHelp.nextNodes = java.util.List.of("check_more_help");
        parseMoreHelp.conditions = new HashMap<>();
        parseMoreHelp.conditions.put("success", "check_more_help");
        parseMoreHelp.conditions.put("error", "goodbye");
        scenario.nodes.add(parseMoreHelp);
        
        // Проверка нужна ли еще помощь
        ScenarioBlock checkMoreHelp = new ScenarioBlock();
        checkMoreHelp.id = "check_more_help";
        checkMoreHelp.type = "condition";
        checkMoreHelp.parameters = new HashMap<>();
        checkMoreHelp.parameters.put("condition", "context.intent");
        checkMoreHelp.conditions = new HashMap<>();
        checkMoreHelp.conditions.put("check_balance", "balance_flow");
        checkMoreHelp.conditions.put("block_card", "block_flow");
        checkMoreHelp.conditions.put("close_account", "close_flow");
        checkMoreHelp.conditions.put("default", "goodbye");
        scenario.nodes.add(checkMoreHelp);
        
        // Прощание
        ScenarioBlock goodbye = new ScenarioBlock();
        goodbye.id = "goodbye";
        goodbye.type = "announce";
        goodbye.parameters = new HashMap<>();
        goodbye.parameters.put("message", "Спасибо за обращение! До свидания! 👋");
        goodbye.nextNodes = java.util.List.of("end_session");
        scenario.nodes.add(goodbye);
        
        // Завершение сессии
        ScenarioBlock endSession = new ScenarioBlock();
        endSession.id = "end_session";
        endSession.type = "announce";
        endSession.parameters = new HashMap<>();
        endSession.parameters.put("message", "Сессия завершена.");
        endSession.nextNodes = java.util.List.of();
        scenario.nodes.add(endSession);
    }
    
    private void addOldScenarioNodes(Scenario scenario) {
        // Старые узлы для совместимости - пока пустая реализация
    }
    
    private void addResultNodes(Scenario scenario) {
        // Баланс
        ScenarioBlock balance = new ScenarioBlock();
        balance.id = "show_balance";
        balance.type = "announce";
        balance.parameters = new HashMap<>();
        balance.parameters.put("message", "Баланс карты ****1234: 15,250.50 грн. Доступно: 15,250.50 грн.");
        balance.nextNodes = java.util.List.of("ask_more");
        scenario.nodes.add(balance);
        
        // Закрытие карты
        ScenarioBlock close = new ScenarioBlock();
        close.id = "show_close";
        close.type = "announce";
        close.parameters = new HashMap<>();
        close.parameters.put("message", "Заявка на закрытие карты создана. Номер: REQ-" + System.currentTimeMillis() + ". SMS отправлено.");
        close.nextNodes = java.util.List.of("ask_more");
        scenario.nodes.add(close);
        
        // Блокировка
        ScenarioBlock block = new ScenarioBlock();
        block.id = "show_block";
        block.type = "announce";
        block.parameters = new HashMap<>();
        block.parameters.put("message", "Карта успешно заблокирована. Для разблокировки обратитесь в отделение банка.");
        block.nextNodes = java.util.List.of("ask_more");
        scenario.nodes.add(block);
        
        // История
        ScenarioBlock history = new ScenarioBlock();
        history.id = "show_history";
        history.type = "announce";
        history.parameters = new HashMap<>();
        history.parameters.put("message", "Последние операции:\n- 23.09 Покупка 150 грн\n- 22.09 Снятие 500 грн\n- 21.09 Пополнение 1000 грн");
        history.nextNodes = java.util.List.of("ask_more");
        scenario.nodes.add(history);
        
        // Поддержка
        ScenarioBlock support = new ScenarioBlock();
        support.id = "show_support";
        support.type = "announce";
        support.parameters = new HashMap<>();
        support.parameters.put("message", "Контакт-центр: 0 800 123 456 (круглосуточно). Чат поддержки в мобильном приложении.");
        support.nextNodes = java.util.List.of("ask_more");
        scenario.nodes.add(support);
        
        // Неизвестная операция
        ScenarioBlock unknown = new ScenarioBlock();
        unknown.id = "unknown_operation";
        unknown.type = "announce";
        unknown.parameters = new HashMap<>();
        unknown.parameters.put("message", "Не понял ваш выбор. Введите номер от 1 до 5.");
        unknown.nextNodes = java.util.List.of("ask_operation");
        scenario.nodes.add(unknown);
        
        // Спросить еще операции
        ScenarioBlock askMore = new ScenarioBlock();
        askMore.id = "ask_more";
        askMore.type = "ask";
        askMore.parameters = new HashMap<>();
        askMore.parameters.put("question", "Нужна ли еще помощь? (да/нет)");
        askMore.nextNodes = java.util.List.of("parse_more");
        scenario.nodes.add(askMore);
        
        // Парсинг ответа
        ScenarioBlock parseMore = new ScenarioBlock();
        parseMore.id = "parse_more";
        parseMore.type = "parse";
        parseMore.parameters = new HashMap<>();
        parseMore.parameters.put("script", "context.needMore = input.toLowerCase().includes('да')");
        parseMore.nextNodes = java.util.List.of("check_more");
        scenario.nodes.add(parseMore);
        
        // Проверка нужна ли еще помощь
        ScenarioBlock checkMore = new ScenarioBlock();
        checkMore.id = "check_more";
        checkMore.type = "condition";
        checkMore.parameters = new HashMap<>();
        checkMore.parameters.put("condition", "context.needMore == true");
        checkMore.conditions = new HashMap<>();
        checkMore.conditions.put("true", "ask_operation");
        checkMore.conditions.put("false", "goodbye");
        scenario.nodes.add(checkMore);
        
        // Прощание
        ScenarioBlock goodbye = new ScenarioBlock();
        goodbye.id = "goodbye";
        goodbye.type = "announce";
        goodbye.parameters = new HashMap<>();
        goodbye.parameters.put("message", "Спасибо за обращение! До свидания!");
        goodbye.nextNodes = java.util.List.of();
        scenario.nodes.add(goodbye);
    }
    
    private Scenario mapScenarioFromService(Map<String, Object> scenarioData) {
        // Маппинг сценария из Scenario Service
        Scenario scenario = new Scenario();
        scenario.id = (String) scenarioData.get("id");
        scenario.name = (String) scenarioData.get("name");
        
        // Извлекаем данные из scenario_data
        Map<String, Object> scenarioDataInner = (Map<String, Object>) scenarioData.get("scenario_data");
        if (scenarioDataInner != null) {
            scenario.startNode = (String) scenarioDataInner.get("start_node");
            
            List<Map<String, Object>> nodesList = (List<Map<String, Object>>) scenarioDataInner.get("nodes");
            if (nodesList != null) {
                scenario.nodes = nodesList.stream().map(this::mapNodeFromService).toList();
            }
            
            scenario.context = (Map<String, Object>) scenarioDataInner.get("context");
        }
        
        return scenario;
    }
    
    private ScenarioBlock mapNodeFromService(Map<String, Object> nodeData) {
        ScenarioBlock block = new ScenarioBlock();
        block.id = (String) nodeData.get("id");
        block.type = (String) nodeData.get("type");
        block.parameters = (Map<String, Object>) nodeData.get("parameters");
        block.nextNodes = (List<String>) nodeData.get("next_nodes");
        block.conditions = (Map<String, String>) nodeData.get("conditions");
        return block;
    }
    
    @POST
    @Path("/test")
    public Response testExecution(Map<String, Object> request) {
        try {
            String userInput = (String) request.get("message");
            
            Map<String, Object> response = new HashMap<>();
            response.put("type", "announce");
            response.put("message", "Orchestrator test response for: " + userInput);
            response.put("next_node", null);
            response.put("context", Map.of("test", true));
            response.put("timestamp", System.currentTimeMillis());
            
            return Response.ok(response).build();
            
        } catch (Exception e) {
            LOG.errorf(e, "Error in test execution");
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(Map.of("error", e.getMessage()))
                .build();
        }
    }
    
    private String generateResponse(String userInput) {
        String msg = userInput.toLowerCase();
        
        if (msg.contains("привет") || msg.contains("hello")) {
            return "Привет! Что вас интересует?\n1. Проверить баланс\n2. Закрыть карту\n3. Заблокировать карту\n4. История операций\n5. Связаться с поддержкой";
        } else if (msg.contains("1") || msg.contains("баланс")) {
            return "Баланс карты ****1234: 15,250.50 грн. Доступно: 15,250.50 грн.";
        } else if (msg.contains("2") || msg.contains("закрыть")) {
            return "Для закрытия карты создана заявка №12345. SMS с подтверждением отправлено.";
        } else if (msg.contains("3") || msg.contains("блок")) {
            return "Карта успешно заблокирована. Для разблокировки обратитесь в отделение.";
        } else if (msg.contains("4") || msg.contains("история")) {
            return "Последние операции:\n- 23.09 Покупка 150 грн\n- 22.09 Снятие 500 грн\n- 21.09 Пополнение 1000 грн";
        } else if (msg.contains("5") || msg.contains("поддержк")) {
            return "Контакт-центр: 0 800 123 456 (круглосуточно). Чат поддержки в мобильном приложении.";
        } else {
            return "Не понял. Выберите:\n1. Баланс\n2. Закрыть карту\n3. Блокировка\n4. История\n5. Поддержка";
        }
    }
}
