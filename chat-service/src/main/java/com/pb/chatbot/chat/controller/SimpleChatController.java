package com.pb.chatbot.chat.controller;

import com.pb.chatbot.chat.service.ScenarioBasedChatService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Path("/api/v1/chat")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SimpleChatController {
    
    private static final Logger LOG = Logger.getLogger(SimpleChatController.class);
    
    @Inject
    ScenarioBasedChatService chatService;
    
    // In-memory storage для сессий и сообщений
    private static final Map<String, Map<String, Object>> sessions = new ConcurrentHashMap<>();
    private static final Map<String, List<Map<String, Object>>> sessionMessages = new ConcurrentHashMap<>();
    
    @GET
    @Path("/status")
    public Response getStatus() {
        long activeSessions = sessions.values().stream()
            .filter(s -> "active".equals(s.get("status")))
            .count();
        
        return Response.ok(Map.of(
            "service", "chat-service",
            "status", "running",
            "role", "session_manager",
            "active_sessions", activeSessions,
            "total_sessions", sessions.size(),
            "timestamp", System.currentTimeMillis()
        )).build();
    }
    
    @POST
    @Path("/sessions")
    public Response createSession() {
        String sessionId = UUID.randomUUID().toString();
        
        // Создаем сессию в чат-сервисе и получаем начальное сообщение
        String initialMessage = chatService.createSession(sessionId);
        
        Map<String, Object> session = new HashMap<>();
        session.put("session_id", sessionId);
        session.put("status", "active");
        session.put("start_time", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "Z");
        session.put("message_count", 1);
        session.put("last_message", initialMessage);
        session.put("user_id", null);
        session.put("scenario_id", null);
        
        sessions.put(sessionId, session);
        
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
        
        LOG.infof("Created new session: %s with initial message: %s", sessionId, initialMessage);
        
        return Response.ok(Map.of(
            "session_id", sessionId,
            "status", "created",
            "initial_message", initialMessage,
            "timestamp", System.currentTimeMillis()
        )).build();
    }
    
    @GET
    @Path("/sessions/{sessionId}")
    public Response getSession(@PathParam("sessionId") String sessionId) {
        Map<String, Object> session = sessions.get(sessionId);
        if (session == null) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity(Map.of("error", "Session not found"))
                .build();
        }
        
        return Response.ok(session).build();
    }
    
    @GET
    @Path("/sessions")
    public Response getAllSessions(@QueryParam("status") String status) {
        List<Map<String, Object>> filteredSessions = sessions.values().stream()
            .filter(s -> status == null || status.equals(s.get("status")))
            .sorted((s1, s2) -> ((String)s2.get("start_time")).compareTo((String)s1.get("start_time")))
            .toList();
        
        return Response.ok(Map.of(
            "sessions", filteredSessions,
            "total", filteredSessions.size(),
            "timestamp", System.currentTimeMillis()
        )).build();
    }
    
    @POST
    @Path("/messages")
    public Response addMessage(Map<String, Object> request) {
        String sessionId = (String) request.get("session_id");
        String messageType = (String) request.getOrDefault("message_type", "user");
        String content = (String) request.get("content");
        String intent = (String) request.get("intent");
        
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
        
        // Генерируем ответ бота через сценарий
        String botResponse = chatService.processMessage(sessionId, content);
        LOG.infof("Generated bot response for session %s: %s", sessionId, botResponse);
        
        // Сохраняем сообщение пользователя
        List<Map<String, Object>> messages = sessionMessages.get(sessionId);
        if (messages != null) {
            Map<String, Object> userMessage = new HashMap<>();
            userMessage.put("id", UUID.randomUUID().toString());
            userMessage.put("session_id", sessionId);
            userMessage.put("type", messageType);
            userMessage.put("content", content);
            userMessage.put("intent", intent);
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
        
        return Response.ok(Map.of(
            "session_id", sessionId,
            "message_saved", true,
            "bot_response", botResponse,
            "timestamp", System.currentTimeMillis()
        )).build();
    }
    
    @GET
    @Path("/sessions/{sessionId}/messages")
    public Response getMessages(@PathParam("sessionId") String sessionId) {
        if (!sessions.containsKey(sessionId)) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity(Map.of("error", "Session not found"))
                .build();
        }
        
        List<Map<String, Object>> messages = sessionMessages.getOrDefault(sessionId, new ArrayList<>());
        
        return Response.ok(Map.of(
            "session_id", sessionId,
            "messages", messages,
            "count", messages.size(),
            "timestamp", System.currentTimeMillis()
        )).build();
    }
    
    @PUT
    @Path("/sessions/{sessionId}/context")
    public Response updateContext(@PathParam("sessionId") String sessionId, Map<String, Object> contextData) {
        Map<String, Object> session = sessions.get(sessionId);
        if (session == null) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity(Map.of("error", "Session not found"))
                .build();
        }
        
        session.put("context", contextData);
        
        LOG.debugf("Updated context for session %s", sessionId);
        
        return Response.ok(Map.of(
            "session_id", sessionId,
            "context_updated", true,
            "timestamp", System.currentTimeMillis()
        )).build();
    }
    
    @DELETE
    @Path("/sessions/{sessionId}")
    public Response endSession(@PathParam("sessionId") String sessionId) {
        Map<String, Object> session = sessions.get(sessionId);
        if (session == null) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity(Map.of("error", "Session not found"))
                .build();
        }
        
        session.put("status", "completed");
        session.put("end_time", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "Z");
        
        return Response.ok(Map.of(
            "session_id", sessionId,
            "status", "completed",
            "timestamp", System.currentTimeMillis()
        )).build();
    }
}
