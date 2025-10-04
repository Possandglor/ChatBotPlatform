package com.pb.chatbot.orchestrator.controller;

import com.pb.chatbot.orchestrator.client.ScenarioServiceClient;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import java.util.Map;

@Path("/api/v1/scenarios")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ScenarioProxyController {
    
    private static final Logger LOG = Logger.getLogger(ScenarioProxyController.class);
    
    @Inject
    @RestClient
    ScenarioServiceClient scenarioServiceClient;
    
    @GET
    public Response getAllScenarios(@QueryParam("search") String search, @Context HttpHeaders headers) {
        try {
            // Всегда передаем X-Branch header (может быть null для main)
            String branchHeader = headers.getHeaderString("X-Branch");
            Map<String, Object> result = scenarioServiceClient.getAllScenarios(search, branchHeader);
            
            return Response.ok(result).build();
        } catch (Exception e) {
            LOG.errorf("Error getting scenarios: %s", e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Failed to get scenarios"))
                    .build();
        }
    }
    
    @GET
    @Path("/{id}")
    public Response getScenario(@PathParam("id") String id) {
        try {
            Map<String, Object> result = scenarioServiceClient.getScenario(id);
            return Response.ok(result).build();
        } catch (Exception e) {
            LOG.errorf("Error getting scenario: %s", e.getMessage());
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", "Scenario not found"))
                    .build();
        }
    }
    
    @POST
    public Response createScenario(Map<String, Object> scenario) {
        try {
            Map<String, Object> result = scenarioServiceClient.createScenario(scenario);
            return Response.ok(result).build();
        } catch (Exception e) {
            LOG.errorf("Error creating scenario: %s", e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Failed to create scenario"))
                    .build();
        }
    }
    
    @PUT
    @Path("/{id}")
    public Response updateScenario(@PathParam("id") String id, Map<String, Object> scenario, @Context HttpHeaders headers) {
        try {
            // Всегда передаем X-Branch header (может быть null для main)
            String branchHeader = headers.getHeaderString("X-Branch");
            Map<String, Object> result = scenarioServiceClient.updateScenario(id, scenario, branchHeader);
            
            return Response.ok(result).build();
        } catch (Exception e) {
            LOG.errorf("Error updating scenario: %s", e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Failed to update scenario"))
                    .build();
        }
    }
    
    @DELETE
    @Path("/{id}")
    public Response deleteScenario(@PathParam("id") String id) {
        try {
            Map<String, Object> result = scenarioServiceClient.deleteScenario(id);
            return Response.ok(result).build();
        } catch (Exception e) {
            LOG.errorf("Error deleting scenario: %s", e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Failed to delete scenario"))
                    .build();
        }
    }
    
    @GET
    @Path("/entry-point")
    public Response getEntryPointScenario(@Context HttpHeaders headers) {
        try {
            // Всегда передаем X-Branch header (может быть null для main)
            String branchHeader = headers.getHeaderString("X-Branch");
            Map<String, Object> result = scenarioServiceClient.getEntryPointScenario(branchHeader);
            
            return Response.ok(result).build();
        } catch (Exception e) {
            LOG.errorf("Error getting entry point scenario: %s", e.getMessage());
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", "Entry point scenario not found"))
                    .build();
        }
    }
}
