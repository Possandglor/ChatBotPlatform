package com.pb.chatbot.scenario.controller;

import com.pb.chatbot.scenario.model.ScenarioInfo;
import com.pb.chatbot.scenario.service.ScenarioManagementService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.Map;

@Path("/api/v1/scenarios")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ScenarioController {
    
    private static final Logger LOG = Logger.getLogger(ScenarioController.class);
    
    @Inject
    ScenarioManagementService scenarioService;
    
    @GET
    @Path("/status")
    public Response getStatus() {
        return Response.ok(Map.of(
            "service", "scenario-service",
            "status", "running",
            "scenarios_count", scenarioService.getAllScenarios().size(),
            "timestamp", System.currentTimeMillis()
        )).build();
    }
    
    @GET
    public Response getAllScenarios(@QueryParam("search") String search) {
        try {
            List<ScenarioInfo> scenarios = search != null ? 
                scenarioService.searchScenarios(search) : 
                scenarioService.getAllScenarios();
            
            return Response.ok(Map.of(
                "scenarios", scenarios,
                "count", scenarios.size(),
                "timestamp", System.currentTimeMillis()
            )).build();
            
        } catch (Exception e) {
            LOG.errorf(e, "Error getting scenarios");
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(Map.of("error", "Failed to get scenarios"))
                .build();
        }
    }
    
    @GET
    @Path("/{id}")
    public Response getScenario(@PathParam("id") String id) {
        ScenarioInfo scenario = scenarioService.getScenario(id);
        
        if (scenario == null) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity(Map.of("error", "Scenario not found"))
                .build();
        }
        
        return Response.ok(scenario).build();
    }
    
    @POST
    public Response createScenario(ScenarioInfo scenario) {
        try {
            ScenarioInfo created = scenarioService.createScenario(scenario);
            return Response.status(Response.Status.CREATED)
                .entity(created)
                .build();
                
        } catch (Exception e) {
            LOG.errorf(e, "Error creating scenario");
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(Map.of("error", "Failed to create scenario"))
                .build();
        }
    }
    
    @PUT
    @Path("/{id}")
    public Response updateScenario(@PathParam("id") String id, ScenarioInfo scenario) {
        try {
            ScenarioInfo updated = scenarioService.updateScenario(id, scenario);
            
            if (updated == null) {
                return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", "Scenario not found"))
                    .build();
            }
            
            return Response.ok(updated).build();
            
        } catch (Exception e) {
            LOG.errorf(e, "Error updating scenario");
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(Map.of("error", "Failed to update scenario"))
                .build();
        }
    }
    
    @DELETE
    @Path("/{id}")
    public Response deleteScenario(@PathParam("id") String id) {
        boolean deleted = scenarioService.deleteScenario(id);
        
        if (!deleted) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity(Map.of("error", "Scenario not found"))
                .build();
        }
        
        return Response.ok(Map.of(
            "message", "Scenario deleted successfully",
            "id", id
        )).build();
    }
    
    @GET
    @Path("/entry-point")
    public Response getEntryPointScenario() {
        try {
            ScenarioInfo entryPoint = scenarioService.getEntryPointScenario();
            if (entryPoint == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(Map.of("error", "No entry point scenario found"))
                        .build();
            }
            return Response.ok(entryPoint).build();
        } catch (Exception e) {
            LOG.errorf("Error getting entry point scenario: %s", e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Failed to get entry point scenario"))
                    .build();
        }
    }
}
