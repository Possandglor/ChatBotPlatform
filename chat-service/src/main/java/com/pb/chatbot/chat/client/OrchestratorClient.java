package com.pb.chatbot.chat.client;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import java.util.Map;

@RegisterRestClient(configKey = "orchestrator-service")
@Path("/api/v1/orchestrator")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public interface OrchestratorClient {
    
    @POST
    @Path("/process")
    Map<String, Object> processMessage(Map<String, Object> request);
}
