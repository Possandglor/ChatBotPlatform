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
    
    @GET
    @Path("/entry-point")
    Map<String, Object> getEntryPointScenario();
    
    @DELETE
    @Path("/{id}")
    Map<String, Object> deleteScenario(@PathParam("id") String id);
    
    @POST
    Map<String, Object> createScenario(Map<String, Object> scenario);
    
    @PUT
    @Path("/{id}")
    Map<String, Object> updateScenario(@PathParam("id") String id, Map<String, Object> scenario);
}
