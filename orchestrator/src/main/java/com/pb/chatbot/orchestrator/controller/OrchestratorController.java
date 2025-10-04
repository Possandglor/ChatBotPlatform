package com.pb.chatbot.orchestrator.controller;

import com.pb.chatbot.orchestrator.client.ScenarioServiceClient;
import com.pb.chatbot.orchestrator.engine.AdvancedScenarioEngine;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import com.fasterxml.jackson.databind.ObjectMapper;

@ApplicationScoped
@Path("/api/v1")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class OrchestratorController {

    private static final Logger LOG = Logger.getLogger(OrchestratorController.class);

    @RestClient
    ScenarioServiceClient scenarioClient;
    
    @Inject
    AdvancedScenarioEngine scenarioEngine;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    // In-memory storage для сессий и сообщений (как было в Chat Service)
    private static final Map<String, Map<String, Object>> sessions = new ConcurrentHashMap<>();
    private static final Map<String, List<Map<String, Object>>> sessionMessages = new ConcurrentHashMap<>();
    private static final Map<String, Map<String, Object>> sessionContexts = new ConcurrentHashMap<>();
    
    // Отдельный список активных сессий (для быстрого доступа)
    private static final Set<String> activeSessions = ConcurrentHashMap.newKeySet();

    @GET
    @Path("/health")
    public Response health() {
        return Response.ok(Map.of("status", "UP", "service", "orchestrator")).build();
    }

    // === CHAT API (как было в Chat Service) ===
    @POST
    @Path("/chat/sessions")
    public Response createChatSession() {
        String sessionId = UUID.randomUUID().toString();
        
        // Создаем контекст для сценария
        Map<String, Object> context = new HashMap<>();
        
        // Получаем начальное сообщение от entry point сценария
        String initialMessage = getInitialMessage();
        
        // Сохраняем контекст сессии
        sessionContexts.put(sessionId, context);
        
        // Получаем node_type из контекста
        String nodeType = (String) context.getOrDefault("node_type", "announce");
        
        Map<String, Object> session = new HashMap<>();
        session.put("session_id", sessionId);
        session.put("status", "active");
        session.put("start_time", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "Z");
        session.put("message_count", 1);
        session.put("last_message", initialMessage);
        session.put("user_id", null);
        session.put("scenario_id", null);
        
        sessions.put(sessionId, session);
        activeSessions.add(sessionId); // Добавляем в активные сессии
        
        // Добавляем начальное сообщение бота в историю
        List<Map<String, Object>> messages = new ArrayList<>();
        Map<String, Object> botMessage = new HashMap<>();
        botMessage.put("id", UUID.randomUUID().toString());
        botMessage.put("session_id", sessionId);
        botMessage.put("sender", "bot");
        botMessage.put("message", initialMessage);
        botMessage.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "Z");
        botMessage.put("message_type", "text");
        messages.add(botMessage);
        
        sessionMessages.put(sessionId, messages);
        
        LOG.infof("Created new session: %s with initial message: %s (type: %s)", sessionId, initialMessage, nodeType);
        
        return Response.ok(Map.of(
            "session_id", sessionId,
            "status", "created",
            "initial_message", initialMessage,
            "node_type", nodeType,
            "timestamp", System.currentTimeMillis()
        )).build();
    }

    @POST
    @Path("/chat/messages")
    public Response sendChatMessage(Map<String, Object> request) {
        String sessionId = (String) request.get("session_id");
        String content = (String) request.get("content");
        
        if (sessionId == null || content == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(Map.of("error", "Missing session_id or content"))
                .build();
        }
        
        // Обновляем сессию
        Map<String, Object> session = sessions.get(sessionId);
        if (session != null) {
            session.put("message_count", (Integer)session.get("message_count") + 1);
            session.put("last_message", content);
        }
        
        // Обрабатываем сообщение через Orchestrator (внутренний вызов)
        String botResponse = processUserMessage(sessionId, content);
        
        // Проверяем что botResponse не null
        if (botResponse == null) {
            LOG.errorf("processUserMessage returned null for session %s, content: %s", sessionId, content);
            botResponse = "Извините, произошла ошибка при обработке сообщения.";
        }
        
        // Проверяем завершение диалога и убираем из активных сессий
        Map<String, Object> currentSessionContext = sessionContexts.get(sessionId);
        if (currentSessionContext != null) {
            Boolean dialogEnded = (Boolean) currentSessionContext.get("dialog_ended");
            Boolean scenarioCompleted = (Boolean) currentSessionContext.get("scenario_completed");
            
            if ((dialogEnded != null && dialogEnded) || (scenarioCompleted != null && scenarioCompleted)) {
                // Диалог завершен - убираем только из активных сессий
                activeSessions.remove(sessionId);
                // Обновляем статус сессии на "completed"
                Map<String, Object> currentSession = sessions.get(sessionId);
                if (currentSession != null) {
                    currentSession.put("status", "completed");
                }
                LOG.infof("Session %s removed from active sessions - dialog ended", sessionId);
                
                // Возвращаем финальный ответ
                return Response.ok(Map.of(
                    "session_id", sessionId,
                    "message_saved", true,
                    "bot_response", botResponse,
                    "node_type", "end",
                    "context", currentSessionContext,
                    "session_ended", true,
                    "timestamp", System.currentTimeMillis()
                )).build();
            }
        }
        
        // Сохраняем сообщение пользователя
        List<Map<String, Object>> messages = sessionMessages.get(sessionId);
        if (messages != null) {
            Map<String, Object> userMessage = new HashMap<>();
            userMessage.put("id", UUID.randomUUID().toString());
            userMessage.put("session_id", sessionId);
            userMessage.put("type", "user");
            userMessage.put("content", content);
            userMessage.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "Z");
            
            messages.add(userMessage);
            
            // Сохраняем ответ бота
            Map<String, Object> botMessage = new HashMap<>();
            botMessage.put("id", UUID.randomUUID().toString());
            botMessage.put("session_id", sessionId);
            botMessage.put("sender", "bot");
            botMessage.put("message", botResponse);
            botMessage.put("message_type", "text");
            botMessage.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "Z");
            
            messages.add(botMessage);
        }
        
        // Получаем контекст для node_type
        Map<String, Object> sessionContext = sessionContexts.getOrDefault(sessionId, new HashMap<>());
        
        // Проверяем что все значения не null
        String nodeType = (String) sessionContext.getOrDefault("node_type", "unknown");
        String intent = (String) sessionContext.getOrDefault("intent", "");
        Object entities = sessionContext.getOrDefault("entities", new ArrayList<>());
        String lastAnswer = (String) sessionContext.getOrDefault("last_answer", "");
        String currentNode = (String) sessionContext.getOrDefault("current_node", "");
        String scenarioId = (String) sessionContext.getOrDefault("scenario_id", "");
        
        // Дополнительная проверка на null
        if (nodeType == null) nodeType = "unknown";
        if (intent == null) intent = "";
        if (entities == null) entities = new ArrayList<>();
        if (lastAnswer == null) lastAnswer = "";
        if (currentNode == null) currentNode = "";
        if (scenarioId == null) scenarioId = "";
        if (content == null) content = "";
        
        Map<String, Object> contextMap = new HashMap<>();
        contextMap.put("intent", intent);
        contextMap.put("entities", entities);
        contextMap.put("last_message", content);
        contextMap.put("last_answer", lastAnswer);
        contextMap.put("current_node", currentNode);
        contextMap.put("scenario_id", scenarioId);
        
        Map<String, Object> responseMap = new HashMap<>();
        responseMap.put("session_id", sessionId);
        responseMap.put("message_saved", true);
        responseMap.put("bot_response", botResponse != null ? botResponse : "Ошибка обработки");
        responseMap.put("node_type", nodeType);
        // ИСПРАВЛЕНО: Возвращаем полный контекст вместо фильтрованного contextMap
        responseMap.put("context", sessionContext);
        responseMap.put("timestamp", System.currentTimeMillis());
        
        return Response.ok(responseMap).build();
    }

    @GET
    @Path("/chat/sessions")
    public Response getChatSessions() {
        List<Map<String, Object>> sessionList = new ArrayList<>(sessions.values());
        sessionList.sort((s1, s2) -> ((String)s2.get("start_time")).compareTo((String)s1.get("start_time")));
        
        return Response.ok(Map.of(
            "sessions", sessionList,
            "count", sessionList.size()
        )).build();
    }
    
    @GET
    @Path("/chat/sessions/active")
    public Response getActiveChatSessions() {
        List<Map<String, Object>> activeSessionList = activeSessions.stream()
            .map(sessions::get)
            .filter(Objects::nonNull)
            .sorted((s1, s2) -> ((String)s2.get("start_time")).compareTo((String)s1.get("start_time")))
            .collect(Collectors.toList());
        
        return Response.ok(Map.of(
            "sessions", activeSessionList,
            "count", activeSessionList.size()
        )).build();
    }

    @GET
    @Path("/chat/sessions/{sessionId}/messages")
    public Response getSessionMessages(@PathParam("sessionId") String sessionId) {
        if (!sessions.containsKey(sessionId)) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity(Map.of("error", "Session not found"))
                .build();
        }
        
        List<Map<String, Object>> messages = sessionMessages.getOrDefault(sessionId, new ArrayList<>());
        return Response.ok(Map.of(
            "messages", messages,
            "count", messages.size()
        )).build();
    }

    // NLU proxy
    @GET
    @Path("/nlu/status")
    public Response getNluStatus() {
        return proxyGet("http://localhost:8098/api/v1/nlu/status");
    }
    
    @POST
    @Path("/nlu/analyze")
    public Response analyzeText(Map<String, Object> request) {
        return proxyPost("http://localhost:8098/api/v1/nlu/analyze", request);
    }
    
    @GET
    @Path("/nlu/intents")
    public Response getNluIntents() {
        return proxyGet("http://localhost:8098/api/v1/nlu/intents/manage");
    }
    
    @POST
    @Path("/nlu/intents")
    public Response createNluIntent(Map<String, Object> intentData) {
        return proxyPost("http://localhost:8098/api/v1/nlu/intents/manage", intentData);
    }
    
    @PUT
    @Path("/nlu/intents/{id}")
    public Response updateNluIntent(@PathParam("id") String id, Map<String, Object> intentData) {
        return proxyPut("http://localhost:8098/api/v1/nlu/intents/manage/" + id, intentData);
    }
    
    @DELETE
    @Path("/nlu/intents/{id}")
    public Response deleteNluIntent(@PathParam("id") String id) {
        return proxyDelete("http://localhost:8098/api/v1/nlu/intents/manage/" + id);
    }

    // Dialogs proxy
    @GET
    @Path("/dialogs")
    public Response getDialogs() {
        // Возвращаем наши сессии как диалоги
        return getChatSessions();
    }
    
    @GET
    @Path("/dialogs/{sessionId}")
    public Response getDialog(@PathParam("sessionId") String sessionId) {
        return getSessionMessages(sessionId);
    }
    
    @POST
    @Path("/dialogs/search")
    public Response searchDialogs(Map<String, Object> request) {
        // Простой поиск по сессиям
        return getChatSessions();
    }

    // Logs proxy
    @GET
    @Path("/logs")
    public Response getLogs() {
        return Response.ok(Map.of(
            "logs", new ArrayList<>(),
            "count", 0
        )).build();
    }
    
    @GET
    @Path("/logs/dialogs")
    public Response getDialogLogs() {
        return Response.ok(Map.of(
            "logs", new ArrayList<>(),
            "count", 0
        )).build();
    }

    // === PROXY МЕТОДЫ ===
    private Response proxyGet(String url) {
        try {
            LOG.infof("Proxying GET to: %s", url);
            
            HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .GET()
                .build();
            
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            
            return Response.status(response.statusCode())
                .entity(response.body())
                .type(MediaType.APPLICATION_JSON)
                .build();
                
        } catch (Exception e) {
            LOG.errorf(e, "Error proxying GET to %s", url);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(Map.of("error", "Proxy error: " + e.getMessage()))
                .build();
        }
    }
    
    private Response proxyPost(String url, Object request) {
        try {
            LOG.infof("Proxying POST to: %s", url);
            
            String requestBody = request != null ? objectMapper.writeValueAsString(request) : "{}";
            
            HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();
            
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            
            return Response.status(response.statusCode())
                .entity(response.body())
                .type(MediaType.APPLICATION_JSON)
                .build();
                
        } catch (Exception e) {
            LOG.errorf(e, "Error proxying POST to %s", url);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(Map.of("error", "Proxy error: " + e.getMessage()))
                .build();
        }
    }
    
    private Response proxyPut(String url, Object request) {
        try {
            LOG.infof("Proxying PUT to: %s", url);
            
            HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(request)))
                .build();
                
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            
            return Response.status(response.statusCode())
                .entity(response.body())
                .build();
                
        } catch (Exception e) {
            LOG.errorf(e, "Error proxying PUT to %s", url);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(Map.of("error", "Proxy error: " + e.getMessage()))
                .build();
        }
    }
    
    private Response proxyDelete(String url) {
        try {
            LOG.infof("Proxying DELETE to: %s", url);
            
            HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .DELETE()
                .build();
            
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            
            return Response.status(response.statusCode())
                .entity(response.body())
                .build();
                
        } catch (Exception e) {
            LOG.errorf(e, "Error proxying DELETE to %s", url);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(Map.of("error", "Proxy error: " + e.getMessage()))
                .build();
        }
    }

    // === ВНУТРЕННИЕ МЕТОДЫ ===
    private String getInitialMessage() {
        try {
            // Получаем начальное сообщение от entry point сценария
            // Используем временный контекст для получения сообщения
            Map<String, Object> tempContext = new HashMap<>();
            return scenarioEngine.getInitialMessageFromEntryPoint(tempContext);
        } catch (Exception e) {
            LOG.errorf(e, "Failed to get initial message from entry point scenario");
            return "Привет! Я готов к тестированию. Напишите что-нибудь для начала диалога.";
        }
    }

    private Map<String, Object> processUserMessageAdvanced(String sessionId, String userMessage) {
        try {
            // Получаем контекст сессии
            Map<String, Object> context = getOrCreateSessionContext(sessionId);
            
            // Используем AdvancedScenarioEngine для обработки сообщения
            String response = scenarioEngine.processMessage(sessionId, userMessage, context);
            
            // Определяем node_type из контекста после обработки
            String nodeType = (String) context.getOrDefault("node_type", "announce");
            
            Map<String, Object> result = new HashMap<>();
            result.put("message", response);
            result.put("node_type", nodeType);
            return result;
            
        } catch (Exception e) {
            LOG.errorf(e, "Error in processUserMessageAdvanced");
            Map<String, Object> result = new HashMap<>();
            result.put("message", "Ошибка обработки сообщения");
            result.put("node_type", "error");
            return result;
        }
    }
    
    private String processUserMessage(String sessionId, String userMessage) {
        try {
            // Получаем или создаем контекст сессии
            Map<String, Object> context = getOrCreateSessionContext(sessionId);
            
            // Сохраняем последний ответ пользователя
            context.put("last_answer", userMessage);
            context.put("last_message", userMessage);
            
            // НОВОЕ: Сохраняем ответ по ID узла (если это ответ на ask узел)
            String waitingForNodeId = (String) context.get("waiting_for_answer_to_node");
            if (waitingForNodeId != null && !waitingForNodeId.isEmpty()) {
                context.put("answer_" + waitingForNodeId, userMessage);
                LOG.infof("Saved answer for node %s: %s", waitingForNodeId, userMessage);
                // Очищаем флаг ожидания
                context.remove("waiting_for_answer_to_node");
            }
            
            // Простой NLU анализ для сохранения интента
            if (userMessage != null && !userMessage.trim().isEmpty()) {
                String intent = "unknown";
                if (userMessage.toLowerCase().contains("баланс")) {
                    intent = "check_balance";
                } else if (userMessage.toLowerCase().contains("карт")) {
                    intent = "card_info";
                } else if (userMessage.toLowerCase().contains("перевод")) {
                    intent = "transfer";
                }
                context.put("intent", intent);
            }
            
            // Если контекст пустой - инициализируем от entry point
            if (!context.containsKey("scenario_id")) {
                initializeContextFromEntryPoint(context);
            }
            
            // Выполняем сценарий через AdvancedScenarioEngine
            String response = scenarioEngine.processMessage(sessionId, userMessage, context);
            
            // Сохраняем обновленный контекст
            saveSessionContext(sessionId, context);
            
            return response != null ? response : "Извините, произошла ошибка при обработке сообщения.";
            
        } catch (Exception e) {
            LOG.errorf(e, "Failed to process message via scenario engine");
            return "Извините, произошла ошибка. Попробуйте еще раз.";
        }
    }

    private void initializeContextFromEntryPoint(Map<String, Object> context) {
        try {
            // Инициализируем контекст от entry point, но не возвращаем сообщение
            scenarioEngine.getInitialMessageFromEntryPoint(context);
        } catch (Exception e) {
            LOG.errorf(e, "Failed to initialize context from entry point");
        }
    }

    @POST
    @Path("/chat/continue")
    public Response continueChatSession(Map<String, Object> request) {
        String sessionId = (String) request.get("session_id");
        
        if (sessionId == null || sessionId.trim().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(Map.of("error", "session_id is required"))
                .build();
        }
        
        // Проверяем существование сессии
        if (!sessions.containsKey(sessionId)) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity(Map.of("error", "Session not found"))
                .build();
        }
        
        List<Map<String, Object>> messages = sessionMessages.get(sessionId);
        if (messages == null) {
            messages = new ArrayList<>();
            sessionMessages.put(sessionId, messages);
        }
        
        // Продолжаем выполнение сценария без ввода пользователя
        String botResponse = continueScenarioExecution(sessionId);
        
        // ИСПРАВЛЕНО: Получаем контекст ПОСЛЕ выполнения сценария
        Map<String, Object> sessionContext = sessionContexts.getOrDefault(sessionId, new HashMap<>());
        
        if (botResponse != null && !botResponse.trim().isEmpty()) {
            // Сохраняем ответ бота
            Map<String, Object> botMessage = new HashMap<>();
            botMessage.put("id", UUID.randomUUID().toString());
            botMessage.put("session_id", sessionId);
            botMessage.put("sender", "bot");
            botMessage.put("message", botResponse);
            botMessage.put("message_type", "text");
            botMessage.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "Z");
            
            messages.add(botMessage);
        }
        
        LOG.infof("Continued session %s: %s (type: %s)", sessionId, botResponse, sessionContext.get("node_type"));
        
        // Проверяем что sessionContext не null
        if (sessionContext == null) {
            sessionContext = new HashMap<>();
        }
        
        Map<String, Object> responseMap = new HashMap<>();
        responseMap.put("session_id", sessionId);
        responseMap.put("bot_response", botResponse != null ? botResponse : "Нет ответа");
        responseMap.put("node_type", sessionContext.getOrDefault("node_type", "unknown"));
        // ИСПРАВЛЕНО: Возвращаем полный контекст вместо фильтрованного contextMap
        responseMap.put("context", sessionContext);
        responseMap.put("timestamp", System.currentTimeMillis());
        
        return Response.ok(responseMap).build();
    }
    
    private String continueScenarioExecution(String sessionId) {
        try {
            // Получаем или создаем контекст сессии
            Map<String, Object> context = getOrCreateSessionContext(sessionId);
            
            // Если контекст пустой - инициализируем от entry point
            if (!context.containsKey("scenario_id")) {
                initializeContextFromEntryPoint(context);
            }
            
            // Выполняем следующий узел сценария
            String response = scenarioEngine.continueExecution(sessionId, context);
            
            // Сохраняем обновленный контекст
            saveSessionContext(sessionId, context);
            
            return response != null ? response : "Диалог завершен.";
            
        } catch (Exception e) {
            LOG.errorf(e, "Failed to continue scenario execution");
            return "Извините, произошла ошибка.";
        }
    }
    
    private Map<String, Object> getOrCreateSessionContext(String sessionId) {
        Map<String, Object> context = sessionContexts.get(sessionId);
        if (context == null) {
            context = new HashMap<>();
            context.put("session_id", sessionId);
            context.put("created_at", System.currentTimeMillis());
            context.put("messages", new ArrayList<>());
            sessionContexts.put(sessionId, context);
        }
        return context;
    }

    private void saveSessionContext(String sessionId, Map<String, Object> context) {
        sessionContexts.put(sessionId, context);
        LOG.debugf("Saved context for session %s: %s", sessionId, context.keySet());
    }

    // === ORCHESTRATOR PROCESS ENDPOINT ===
    @POST
    @Path("/process")
    public Response processMessage(Map<String, Object> request) {
        String sessionId = (String) request.get("session_id");
        String userMessage = (String) request.get("content");
        
        if (sessionId == null || userMessage == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(Map.of("error", "Missing session_id or content"))
                .build();
        }

        try {
            // Обрабатываем через внутренний метод
            String botResponse = processUserMessage(sessionId, userMessage);
            
            return Response.ok(Map.of(
                "bot_response", botResponse,
                "session_id", sessionId,
                "timestamp", System.currentTimeMillis()
            )).build();

        } catch (Exception e) {
            LOG.errorf(e, "Error processing message");
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(Map.of("error", "Processing failed: " + e.getMessage()))
                .build();
        }
    }
}
