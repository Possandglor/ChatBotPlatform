package com.pb.chatbot.orchestrator.controller;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Path("/api/v1/dialogs")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class DialogController {
    
    private static final Logger LOG = Logger.getLogger(DialogController.class);
    
    // In-memory storage для диалогов
    private static final Map<String, Map<String, Object>> dialogs = new ConcurrentHashMap<>();
    private static final Map<String, List<Map<String, Object>>> dialogMessages = new ConcurrentHashMap<>();
    
    static {
        initTestData();
    }
    
    @GET
    public Response getDialogs(
            @QueryParam("search") String search,
            @QueryParam("status") String status,
            @QueryParam("limit") @DefaultValue("50") int limit) {
        
        LOG.infof("Getting dialogs with filters: search=%s, status=%s", search, status);
        
        List<Map<String, Object>> filteredDialogs = dialogs.values().stream()
            .filter(dialog -> search == null || 
                dialog.get("last_message").toString().toLowerCase().contains(search.toLowerCase()) ||
                dialog.get("scenario_name").toString().toLowerCase().contains(search.toLowerCase()))
            .filter(dialog -> status == null || dialog.get("status").equals(status))
            .sorted((d1, d2) -> ((String)d2.get("start_time")).compareTo((String)d1.get("start_time")))
            .limit(limit)
            .collect(Collectors.toList());
        
        return Response.ok(Map.of(
            "dialogs", filteredDialogs,
            "total", dialogs.size(),
            "filtered", filteredDialogs.size(),
            "timestamp", System.currentTimeMillis()
        )).build();
    }
    
    @GET
    @Path("/{sessionId}")
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
    @Path("/{sessionId}/messages")
    public Response getDialogMessages(@PathParam("sessionId") String sessionId) {
        if (!dialogs.containsKey(sessionId)) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity(Map.of("error", "Dialog not found"))
                .build();
        }
        
        List<Map<String, Object>> messages = dialogMessages.getOrDefault(sessionId, new ArrayList<>());
        
        return Response.ok(Map.of(
            "session_id", sessionId,
            "messages", messages,
            "count", messages.size(),
            "timestamp", System.currentTimeMillis()
        )).build();
    }
    
    @POST
    @Path("/search")
    public Response searchDialogs(Map<String, Object> searchRequest) {
        String query = (String) searchRequest.get("query");
        LOG.infof("Searching dialogs with query: %s", query);
        
        List<Map<String, Object>> results = dialogs.values().stream()
            .filter(dialog -> query == null || 
                dialog.get("last_message").toString().toLowerCase().contains(query.toLowerCase()) ||
                dialog.get("scenario_name").toString().toLowerCase().contains(query.toLowerCase()))
            .sorted((d1, d2) -> ((String)d2.get("start_time")).compareTo((String)d1.get("start_time")))
            .limit(100)
            .collect(Collectors.toList());
        
        return Response.ok(Map.of(
            "results", results,
            "count", results.size(),
            "query", query,
            "timestamp", System.currentTimeMillis()
        )).build();
    }
    
    // Публичные методы для добавления диалогов
    public static void addDialog(String sessionId, String scenarioName, String userId) {
        Map<String, Object> dialog = new HashMap<>();
        dialog.put("session_id", sessionId);
        dialog.put("user_id", userId);
        dialog.put("scenario_name", scenarioName != null ? scenarioName : "Неизвестный сценарий");
        dialog.put("status", "active");
        dialog.put("start_time", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "Z");
        dialog.put("message_count", 0);
        dialog.put("last_message", "");
        
        dialogs.put(sessionId, dialog);
        dialogMessages.put(sessionId, new ArrayList<>());
        
        LOG.infof("Added new dialog: %s", sessionId);
    }
    
    public static void updateDialog(String sessionId, String message, String botResponse, String intent) {
        Map<String, Object> dialog = dialogs.get(sessionId);
        if (dialog != null) {
            dialog.put("last_message", message);
            dialog.put("message_count", (Integer)dialog.get("message_count") + 1);
            
            // Добавляем сообщения
            List<Map<String, Object>> messages = dialogMessages.get(sessionId);
            if (messages != null) {
                String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "Z";
                
                // Сообщение пользователя
                messages.add(Map.of(
                    "id", UUID.randomUUID().toString(),
                    "type", "user",
                    "content", message,
                    "timestamp", timestamp,
                    "intent", intent != null ? intent : ""
                ));
                
                // Ответ бота
                if (botResponse != null) {
                    messages.add(Map.of(
                        "id", UUID.randomUUID().toString(),
                        "type", "bot",
                        "content", botResponse,
                        "timestamp", timestamp
                    ));
                }
            }
        }
    }
    
    public static void completeDialog(String sessionId) {
        Map<String, Object> dialog = dialogs.get(sessionId);
        if (dialog != null) {
            dialog.put("status", "completed");
            dialog.put("end_time", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "Z");
        }
    }
    
    private static void initTestData() {
        // Создаем тестовые диалоги
        addDialog("session-001", "Главное меню с NLU", "user-123");
        updateDialog("session-001", "Привет", "Привет! Я ваш банковский помощник. Що саме вас цікавить?", "greeting");
        updateDialog("session-001", "Хочу проверить баланс", "Проверяю ваш баланс...", "check_balance");
        completeDialog("session-001");
        
        addDialog("session-002", "Тест API интеграции", null);
        updateDialog("session-002", "Тестируем API", "API работает отлично!", null);
        
        addDialog("session-003", "Главное меню с NLU", "user-456");
        updateDialog("session-003", "Привет", "Привет! Я ваш банковский помощник.", "greeting");
        dialogs.get("session-003").put("status", "abandoned");
    }
}
