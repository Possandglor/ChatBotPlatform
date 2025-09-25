package com.pb.chatbot.gateway.controller;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.jboss.logging.Logger;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

@Path("/api/v1")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ProxyController {

    
    private static final Logger LOG = Logger.getLogger(ProxyController.class);
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    // Chat Service proxy
    @GET
    @Path("/chat/status")
    public Response getChatStatus() {
        return proxyGet("http://localhost:8091/api/v1/chat/status");
    }
    
    @POST
    @Path("/chat/sessions")
    public Response createChatSession() {
        return proxyPost("http://localhost:8091/api/v1/chat/sessions", null);
    }
    
    @GET
    @Path("/chat/sessions")
    public Response getChatSessions() {
        return proxyGet("http://localhost:8091/api/v1/chat/sessions");
    }
    
    @POST
    @Path("/chat/messages")
    public Response addChatMessage(Map<String, Object> request) {
        return proxyPost("http://localhost:8091/api/v1/chat/messages", request);
    }
    
    // Orchestrator proxy
    @GET
    @Path("/orchestrator/status")
    public Response getOrchestratorStatus() {
        return proxyGet("http://localhost:8092/api/v1/orchestrator/status");
    }
    
    @POST
    @Path("/orchestrator/process")
    public Response processMessage(Map<String, Object> request) {
        return proxyPost("http://localhost:8092/api/v1/orchestrator/process", request);
    }
    
    // Dialogs proxy (к Orchestrator)
    @GET
    @Path("/dialogs")
    public Response getDialogs() {
        return proxyGet("http://localhost:8092/api/v1/dialogs");
    }
    
    @GET
    @Path("/dialogs/{sessionId}")
    public Response getDialog(@PathParam("sessionId") String sessionId) {
        return proxyGet("http://localhost:8092/api/v1/dialogs/" + sessionId);
    }
    
    @POST
    @Path("/dialogs/search")
    public Response searchDialogs(Map<String, Object> request) {
        return proxyPost("http://localhost:8092/api/v1/dialogs/search", request);
    }
    
    // Scenarios proxy
    @GET
    @Path("/scenarios")
    public Response getScenarios() {
        return proxyGet("http://localhost:8093/api/v1/scenarios");
    }
    
    @GET
    @Path("/scenarios/entry-point")
    public Response getEntryPointScenario() {
        return proxyGet("http://localhost:8093/api/v1/scenarios/entry-point");
    }
    
    @POST
    @Path("/scenarios")
    public Response createScenario(Map<String, Object> request) {
        return proxyPost("http://localhost:8093/api/v1/scenarios", request);
    }
    
    @PUT
    @Path("/scenarios/{id}")
    public Response updateScenario(@PathParam("id") String id, Map<String, Object> request) {
        return proxyPut("http://localhost:8093/api/v1/scenarios/" + id, request);
    }
    
    @DELETE
    @Path("/scenarios/{id}")
    public Response deleteScenario(@PathParam("id") String id) {
        return proxyDelete("http://localhost:8093/api/v1/scenarios/" + id);
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
        try {
            LOG.infof("Updating NLU intent: %s", id);
            
            String json = objectMapper.writeValueAsString(intentData);
            LOG.infof("PUT body: %s", json);
            
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8098/api/v1/nlu/intents/manage/" + id))
                    .header("Content-Type", "application/json")
                    .PUT(HttpRequest.BodyPublishers.ofString(json))
                    .build();
            
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            
            return Response.status(response.statusCode())
                    .entity(response.body())
                    .type(MediaType.APPLICATION_JSON)
                    .build();
                    
        } catch (Exception e) {
            LOG.errorf("Error updating NLU intent: %s", e.getMessage());
            return Response.status(500)
                    .entity(Map.of("error", "Failed to update intent: " + e.getMessage()))
                    .build();
        }
    }
    
    @POST
    @Path("/nlu/intents/{id}")
    public Response updateNluIntentViaPost(@PathParam("id") String id, Map<String, Object> intentData) {
        return proxyPost("http://localhost:8098/api/v1/nlu/intents/manage/" + id, intentData);
    }
    
    @DELETE
    @Path("/nlu/intents/{id}")
    public Response deleteNluIntent(@PathParam("id") String id) {
        return proxyDelete("http://localhost:8098/api/v1/nlu/intents/manage/" + id);
    }
    
    // Logs proxy (к Orchestrator)
    @GET
    @Path("/logs")
    public Response getLogs() {
        return proxyGet("http://localhost:8092/api/v1/logs");
    }
    
    @GET
    @Path("/logs/dialogs")
    public Response getDialogLogs() {
        return proxyGet("http://localhost:8092/api/v1/logs/dialogs");
    }
    
    // Простые proxy методы
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
                .timeout(Duration.ofSeconds(30))
                .build();
                
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            
            LOG.infof("PUT response status: %d", response.statusCode());
            
            if (response.body() != null && !response.body().isEmpty()) {
                return Response.status(response.statusCode())
                    .entity(response.body())
                    .build();
            } else {
                return Response.status(response.statusCode()).build();
            }
                
        } catch (Exception e) {
            LOG.errorf(e, "Error proxying PUT to %s", url);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(Map.of("error", "Proxy error: " + e.getMessage()))
                .build();
        }
    }
    
    private Response proxyDelete(String url) {
        // Простая реализация через GET для совместимости  
        return proxyGet(url);
    }
}
