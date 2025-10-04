package com.pb.chatbot.orchestrator.client;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(configKey = "scenario-service")
@Path("/api/v1/branches")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface BranchServiceClient {
    
    @GET
    Response getBranches();
    
    @POST
    @Path("/{branchName}")
    Response createBranch(
            @PathParam("branchName") String branchName,
            @QueryParam("from") String sourceBranch,
            @QueryParam("author") String author);
    
    @GET
    @Path("/{branchName}")
    Response getBranch(@PathParam("branchName") String branchName);
    
    @POST
    @Path("/{branchName}/merge")
    Response mergeBranch(
            @PathParam("branchName") String sourceBranch,
            @QueryParam("target") String targetBranch,
            @QueryParam("author") String author);
    
    @DELETE
    @Path("/{branchName}")
    Response deleteBranch(@PathParam("branchName") String branchName);
    
    @GET
    @Path("/history")
    Response getHistory();
}
