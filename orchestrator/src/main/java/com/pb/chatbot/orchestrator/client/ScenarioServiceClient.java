package com.pb.chatbot.orchestrator.client;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import java.util.Map;

@RegisterRestClient(configKey = "scenario-service")
@Path("/api/v1/scenarios")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface ScenarioServiceClient {
    
    @GET
    @Path("/{id}")
    Map<String, Object> getScenario(@PathParam("id") String id);
    
    @GET
    Map<String, Object> getAllScenarios(@QueryParam("search") String search);
    
    @GET
    @Path("/default")
    Map<String, Object> getDefaultScenario();
}
