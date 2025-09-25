package com.pb.chatbot.orchestrator.controller;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;

import java.util.*;

@Path("/api/v1/logs")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class LogController {
    
    private static final Logger LOG = Logger.getLogger(LogController.class);
    
    @GET
    public Response getSystemLogs() {
        LOG.info("Getting system logs");
        
        List<Map<String, Object>> logs = Arrays.asList(
            Map.of(
                "id", "1",
                "timestamp", "2025-09-24T17:35:00Z",
                "level", "INFO",
                "service", "orchestrator",
                "className", "OrchestratorController",
                "message", "Processing message for session session-001: Привет"
            ),
            Map.of(
                "id", "2",
                "timestamp", "2025-09-24T17:35:01Z",
                "level", "DEBUG",
                "service", "nlu-service",
                "className", "NluService",
                "message", "Analyzing text: Привет"
            )
        );
        
        return Response.ok(Map.of(
            "logs", logs,
            "total", logs.size(),
            "timestamp", System.currentTimeMillis()
        )).build();
    }
    
    @GET
    @Path("/dialogs")
    public Response getDialogLogs() {
        LOG.info("Getting dialog logs");
        
        List<Map<String, Object>> logs = Arrays.asList(
            Map.of(
                "session_id", "session-001",
                "timestamp", "2025-09-24T17:30:00Z",
                "event_type", "message",
                "details", "User message: Привет"
            ),
            Map.of(
                "session_id", "session-001",
                "timestamp", "2025-09-24T17:30:01Z",
                "event_type", "scenario_change",
                "details", "Started scenario: main-menu-nlu-001"
            )
        );
        
        return Response.ok(Map.of(
            "logs", logs,
            "total", logs.size(),
            "timestamp", System.currentTimeMillis()
        )).build();
    }
}
