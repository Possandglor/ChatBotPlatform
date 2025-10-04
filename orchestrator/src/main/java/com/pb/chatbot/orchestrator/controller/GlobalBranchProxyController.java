package com.pb.chatbot.orchestrator.controller;

import com.pb.chatbot.orchestrator.client.BranchServiceClient;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import java.util.Map;

@Path("/api/v1/branches")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class GlobalBranchProxyController {
    
    private static final Logger LOG = Logger.getLogger(GlobalBranchProxyController.class);
    
    @Inject
    @RestClient
    BranchServiceClient branchServiceClient;
    
    @GET
    public Response getBranches() {
        try {
            return branchServiceClient.getBranches();
        } catch (Exception e) {
            LOG.errorf("Error getting global branches: %s", e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Failed to get branches"))
                    .build();
        }
    }
    
    @POST
    @Path("/{branchName}")
    public Response createBranch(
            @PathParam("branchName") String branchName,
            @QueryParam("from") @DefaultValue("main") String sourceBranch,
            @QueryParam("author") @DefaultValue("anonymous") String author) {
        
        try {
            return branchServiceClient.createBranch(branchName, sourceBranch, author);
        } catch (Exception e) {
            LOG.errorf("Error creating global branch: %s", e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Failed to create branch"))
                    .build();
        }
    }
    
    @GET
    @Path("/{branchName}")
    public Response getBranch(@PathParam("branchName") String branchName) {
        try {
            return branchServiceClient.getBranch(branchName);
        } catch (Exception e) {
            LOG.errorf("Error getting global branch: %s", e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Failed to get branch"))
                    .build();
        }
    }
    
    @POST
    @Path("/{branchName}/merge")
    public Response mergeBranch(
            @PathParam("branchName") String sourceBranch,
            @QueryParam("target") @DefaultValue("main") String targetBranch,
            @QueryParam("author") @DefaultValue("anonymous") String author) {
        
        try {
            return branchServiceClient.mergeBranch(sourceBranch, targetBranch, author);
        } catch (Exception e) {
            LOG.errorf("Error merging global branch: %s", e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Failed to merge branch"))
                    .build();
        }
    }
    
    @DELETE
    @Path("/{branchName}")
    public Response deleteBranch(@PathParam("branchName") String branchName) {
        try {
            return branchServiceClient.deleteBranch(branchName);
        } catch (Exception e) {
            LOG.errorf("Error deleting global branch: %s", e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Failed to delete branch"))
                    .build();
        }
    }
    
    @GET
    @Path("/history")
    public Response getHistory() {
        try {
            return branchServiceClient.getHistory();
        } catch (Exception e) {
            LOG.errorf("Error getting global branch history: %s", e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Failed to get history"))
                    .build();
        }
    }
}
