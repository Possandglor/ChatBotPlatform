package com.pb.chatbot.orchestrator.client;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import java.util.Map;

@RegisterRestClient(configKey = "chat-service")
@Path("/api/v1/chat")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface ChatServiceClient {
    
    @POST
    @Path("/sessions")
    Map<String, Object> createSession();
    
    @GET
    @Path("/sessions/{sessionId}")
    Map<String, Object> getSession(@PathParam("sessionId") String sessionId);
    
    @PUT
    @Path("/sessions/{sessionId}/context")
    Map<String, Object> updateContext(@PathParam("sessionId") String sessionId, Map<String, Object> context);
    
    @GET
    @Path("/sessions/{sessionId}/context")
    Map<String, Object> getContext(@PathParam("sessionId") String sessionId);
    
    @POST
    @Path("/messages")
    Map<String, Object> addMessage(Map<String, Object> message);
}
