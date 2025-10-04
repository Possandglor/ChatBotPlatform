package com.pb.chatbot.scenario.resource;

import com.pb.chatbot.scenario.model.*;
import com.pb.chatbot.scenario.service.WorkspaceBranchService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.Map;

@Path("/api/v1/branches")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class WorkspaceBranchResource {
    
    private static final Logger LOG = Logger.getLogger(WorkspaceBranchResource.class);
    
    @Inject
    WorkspaceBranchService branchService;
    
    /**
     * Получить список веток
     */
    @GET
    public Response getBranches() {
        try {
            List<String> branches = branchService.getBranches();
            return Response.ok(Map.of("branches", branches)).build();
        } catch (Exception e) {
            LOG.errorf("Error getting branches: %s", e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        }
    }
    
    /**
     * Создать новую ветку
     */
    @POST
    @Path("/{branchName}")
    public Response createBranch(
            @PathParam("branchName") String branchName,
            @QueryParam("from") @DefaultValue("main") String sourceBranch,
            @QueryParam("author") @DefaultValue("anonymous") String author) {
        
        try {
            WorkspaceBranch branch = branchService.createBranch(branchName, sourceBranch, author);
            return Response.ok(branch).build();
        } catch (Exception e) {
            LOG.errorf("Error creating branch: %s", e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        }
    }
    
    /**
     * Получить конкретную ветку
     */
    @GET
    @Path("/{branchName}")
    public Response getBranch(@PathParam("branchName") String branchName) {
        try {
            WorkspaceBranch branch = branchService.getBranch(branchName);
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
     * Слить ветку
     */
    @POST
    @Path("/{branchName}/merge")
    public Response mergeBranch(
            @PathParam("branchName") String sourceBranch,
            @QueryParam("target") @DefaultValue("main") String targetBranch,
            @QueryParam("author") @DefaultValue("anonymous") String author) {
        
        try {
            MergeResult result = branchService.mergeBranch(sourceBranch, targetBranch, author);
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
    public Response deleteBranch(@PathParam("branchName") String branchName) {
        try {
            branchService.deleteBranch(branchName);
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
    public Response getHistory() {
        try {
            List<BranchHistoryEntry> history = branchService.getHistory();
            return Response.ok(Map.of("history", history)).build();
        } catch (Exception e) {
            LOG.errorf("Error getting history: %s", e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        }
    }
}
