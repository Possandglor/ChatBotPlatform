package com.pb.chatbot.scenario.resource;

import com.pb.chatbot.scenario.model.*;
import com.pb.chatbot.scenario.service.BranchService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.Map;

@Path("/api/v1/scenarios/{scenarioId}/branches")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class BranchResource {
    
    private static final Logger LOG = Logger.getLogger(BranchResource.class);
    
    @Inject
    BranchService branchService;
    
    /**
     * Создать новую ветку
     */
    @POST
    @Path("/{branchName}")
    public Response createBranch(
            @PathParam("scenarioId") String scenarioId,
            @PathParam("branchName") String branchName,
            @QueryParam("from") @DefaultValue("main") String sourceBranch,
            @QueryParam("author") @DefaultValue("anonymous") String author) {
        
        try {
            ScenarioBranch branch = branchService.createBranch(scenarioId, branchName, sourceBranch, author);
            return Response.ok(branch).build();
        } catch (Exception e) {
            LOG.errorf("Error creating branch: %s", e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        }
    }
    
    /**
     * Получить список веток
     */
    @GET
    public Response getBranches(@PathParam("scenarioId") String scenarioId) {
        try {
            List<String> branches = branchService.getBranches(scenarioId);
            return Response.ok(Map.of("branches", branches)).build();
        } catch (Exception e) {
            LOG.errorf("Error getting branches: %s", e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        }
    }
    
    /**
     * Получить конкретную ветку
     */
    @GET
    @Path("/{branchName}")
    public Response getBranch(
            @PathParam("scenarioId") String scenarioId,
            @PathParam("branchName") String branchName) {
        
        try {
            ScenarioBranch branch = branchService.getBranch(scenarioId, branchName);
            if (branch == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(Map.of("error", "Branch not found"))
                        .build();
            }
            return Response.ok(branch).build();
        } catch (Exception e) {
            LOG.errorf("Error getting branch: %s", e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        }
    }
    
    /**
     * Обновить ветку
     */
    @PUT
    @Path("/{branchName}")
    public Response updateBranch(
            @PathParam("scenarioId") String scenarioId,
            @PathParam("branchName") String branchName,
            @QueryParam("author") @DefaultValue("anonymous") String author,
            Map<String, Object> scenarioData) {
        
        try {
            branchService.updateBranch(scenarioId, branchName, scenarioData, author);
            return Response.ok(Map.of("success", true, "message", "Branch updated")).build();
        } catch (Exception e) {
            LOG.errorf("Error updating branch: %s", e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        }
    }
    
    /**
     * Слить ветку
     */
    @POST
    @Path("/{branchName}/merge")
    public Response mergeBranch(
            @PathParam("scenarioId") String scenarioId,
            @PathParam("branchName") String sourceBranch,
            @QueryParam("target") @DefaultValue("main") String targetBranch,
            @QueryParam("author") @DefaultValue("anonymous") String author) {
        
        try {
            MergeResult result = branchService.mergeBranch(scenarioId, sourceBranch, targetBranch, author);
            return Response.ok(result).build();
        } catch (Exception e) {
            LOG.errorf("Error merging branch: %s", e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        }
    }
    
    /**
     * Удалить ветку
     */
    @DELETE
    @Path("/{branchName}")
    public Response deleteBranch(
            @PathParam("scenarioId") String scenarioId,
            @PathParam("branchName") String branchName) {
        
        try {
            branchService.deleteBranch(scenarioId, branchName);
            return Response.ok(Map.of("success", true, "message", "Branch deleted")).build();
        } catch (Exception e) {
            LOG.errorf("Error deleting branch: %s", e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        }
    }
    
    /**
     * Получить историю изменений
     */
    @GET
    @Path("/history")
    public Response getHistory(@PathParam("scenarioId") String scenarioId) {
        try {
            List<BranchHistoryEntry> history = branchService.getHistory(scenarioId);
            return Response.ok(Map.of("history", history)).build();
        } catch (Exception e) {
            LOG.errorf("Error getting history: %s", e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        }
    }
}
