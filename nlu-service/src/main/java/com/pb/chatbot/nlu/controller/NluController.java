package com.pb.chatbot.nlu.controller;

import com.pb.chatbot.nlu.model.NluResult;
import com.pb.chatbot.nlu.service.NluService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.Map;

@Path("/api/v1/nlu")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class NluController {
    
    private static final Logger LOG = Logger.getLogger(NluController.class);
    
    @Inject
    NluService nluService;
    
    @GET
    @Path("/status")
    public Response getStatus() {
        return Response.ok(Map.of(
            "service", "nlu-service",
            "status", "running",
            "role", "natural_language_understanding",
            "version", "1.0.0",
            "timestamp", System.currentTimeMillis()
        )).build();
    }
    
    @POST
    @Path("/analyze")
    public Response analyzeText(Map<String, Object> request) {
        try {
            String text = (String) request.get("text");
            Map<String, Object> context = (Map<String, Object>) request.get("context");
            
            if (text == null || text.trim().isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "Text is required"))
                    .build();
            }
            
            LOG.infof("Analyzing text: %s", text);
            
            NluResult result = nluService.analyze(text, context);
            
            Map<String, Object> response = Map.of(
                "intent", result.intent,
                "confidence", result.confidence,
                "entities", result.entities,
                "context", result.context,
                "timestamp", System.currentTimeMillis()
            );
            
            return Response.ok(response).build();
            
        } catch (Exception e) {
            LOG.errorf(e, "Error analyzing text");
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(Map.of("error", "Analysis failed: " + e.getMessage()))
                .build();
        }
    }
    
    @GET
    @Path("/intents")
    public Response getAvailableIntents() {
        return Response.ok(Map.of(
            "intents", new String[]{
                "greeting", "check_balance", "block_card", "close_account",
                "transfer_money", "get_statement", "complaint", "unknown"
            },
            "entities", new String[]{
                "confirmation", "urgency", "card_reason", "card_number", "amount"
            }
        )).build();
    }
    
    @GET
    @Path("/intents/manage")
    public Response getIntentsForManagement() {
        List<Map<String, Object>> intents = nluService.getAllIntents();
        
        return Response.ok(Map.of(
            "intents", intents,
            "total", intents.size(),
            "timestamp", System.currentTimeMillis()
        )).build();
    }
    
    @POST
    @Path("/intents/manage")
    public Response createIntent(Map<String, Object> request) {
        try {
            String name = (String) request.get("name");
            String description = (String) request.get("description");
            List<String> examples = (List<String>) request.get("examples");
            
            if (name == null || description == null || examples == null || examples.isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "Name, description and examples are required"))
                    .build();
            }
            
            String intentId = nluService.createIntent(name, description, examples);
            
            return Response.ok(Map.of(
                "success", true,
                "message", "Intent created successfully",
                "id", intentId,
                "timestamp", System.currentTimeMillis()
            )).build();
        } catch (Exception e) {
            LOG.errorf(e, "Error creating intent");
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(Map.of("error", "Failed to create intent: " + e.getMessage()))
                .build();
        }
    }
    
    @PUT
    @Path("/intents/manage/{intentId}")
    public Response updateIntent(@PathParam("intentId") String intentId, Map<String, Object> request) {
        try {
            String name = (String) request.get("name");
            String description = (String) request.get("description");
            List<String> examples = (List<String>) request.get("examples");
            
            if (name == null || description == null || examples == null || examples.isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "Name, description and examples are required"))
                    .build();
            }
            
            boolean updated = nluService.updateIntent(intentId, name, description, examples);
            
            if (updated) {
                return Response.ok(Map.of(
                    "success", true,
                    "message", "Intent updated successfully",
                    "id", intentId,
                    "timestamp", System.currentTimeMillis()
                )).build();
            } else {
                return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", "Intent not found"))
                    .build();
            }
        } catch (Exception e) {
            LOG.errorf(e, "Error updating intent");
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(Map.of("error", "Failed to update intent: " + e.getMessage()))
                .build();
        }
    }
    
    @DELETE
    @Path("/intents/manage/{intentId}")
    public Response deleteIntent(@PathParam("intentId") String intentId) {
        try {
            boolean deleted = nluService.deleteIntent(intentId);
            
            if (deleted) {
                return Response.ok(Map.of(
                    "success", true,
                    "message", "Intent deleted successfully",
                    "id", intentId,
                    "timestamp", System.currentTimeMillis()
                )).build();
            } else {
                return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", "Intent not found"))
                    .build();
            }
        } catch (Exception e) {
            LOG.errorf(e, "Error deleting intent");
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(Map.of("error", "Failed to delete intent: " + e.getMessage()))
                .build();
        }
    }
    
    @POST
    @Path("/test")
    public Response testAnalysis(Map<String, Object> request) {
        String[] testPhrases = {
            "Привет, хочу проверить баланс карты",
            "Нужно срочно заблокировать карту из-за кражи", 
            "Хочу закрыть счет",
            "Да, согласен",
            "Нет, не хочу",
            "Перевести 1000 грн"
        };
        
        Map<String, Object> results = new java.util.HashMap<>();
        
        for (String phrase : testPhrases) {
            NluResult result = nluService.analyze(phrase, null);
            results.put(phrase, Map.of(
                "intent", result.intent,
                "confidence", result.confidence,
                "entities_count", result.entities.size()
            ));
        }
        
        return Response.ok(Map.of(
            "test_results", results,
            "timestamp", System.currentTimeMillis()
        )).build();
    }

}
