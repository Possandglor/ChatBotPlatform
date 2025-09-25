package com.pb.chatbot.chat.client;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import java.util.Map;

@RegisterRestClient(configKey = "scenario-service")
@Path("/api/v1/scenarios")
public interface ScenarioServiceClient {
    
    @GET
    @Path("/entry-point")
    @Produces(MediaType.APPLICATION_JSON)
    Map<String, Object> getEntryPointScenario();
}
