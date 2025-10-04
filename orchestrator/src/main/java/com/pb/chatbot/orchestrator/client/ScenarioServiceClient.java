package com.pb.chatbot.orchestrator.client;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
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
    Map<String, Object> getAllScenarios(@QueryParam("search") String search, @HeaderParam("X-Branch") String branch);
    
    @GET
    @Path("/default")
    Map<String, Object> getDefaultScenario();
    
    @GET
    @Path("/entry-point")
    Map<String, Object> getEntryPointScenario(@HeaderParam("X-Branch") String branch);
    
    @DELETE
    @Path("/{id}")
    Map<String, Object> deleteScenario(@PathParam("id") String id);
    
    @POST
    Map<String, Object> createScenario(Map<String, Object> scenario);
    
    @PUT
    @Path("/{id}")
    Map<String, Object> updateScenario(@PathParam("id") String id, Map<String, Object> scenario, @HeaderParam("X-Branch") String branch);
    
    // Branch API methods
    @POST
    @Path("/{scenarioId}/branches/{branchName}")
    Response createBranch(
            @PathParam("scenarioId") String scenarioId,
            @PathParam("branchName") String branchName,
            @QueryParam("from") String sourceBranch,
            @QueryParam("author") String author);
    
    @GET
    @Path("/{scenarioId}/branches")
    Response getBranches(@PathParam("scenarioId") String scenarioId);
    
    @GET
    @Path("/{scenarioId}/branches/{branchName}")
    Response getBranch(
            @PathParam("scenarioId") String scenarioId,
            @PathParam("branchName") String branchName);
    
    @PUT
    @Path("/{scenarioId}/branches/{branchName}")
    Response updateBranch(
            @PathParam("scenarioId") String scenarioId,
            @PathParam("branchName") String branchName,
            @QueryParam("author") String author,
            Map<String, Object> scenarioData);
    
    @POST
    @Path("/{scenarioId}/branches/{branchName}/merge")
    Response mergeBranch(
            @PathParam("scenarioId") String scenarioId,
            @PathParam("branchName") String sourceBranch,
            @QueryParam("target") String targetBranch,
            @QueryParam("author") String author);
    
    @DELETE
    @Path("/{scenarioId}/branches/{branchName}")
    Response deleteBranch(
            @PathParam("scenarioId") String scenarioId,
            @PathParam("branchName") String branchName);
    
    @GET
    @Path("/{scenarioId}/branches/history")
    Response getHistory(@PathParam("scenarioId") String scenarioId);
    
    // Global Branch API methods
    @GET
    @Path("/../branches")
    Response getGlobalBranches();
    
    @POST
    @Path("/branches/{branchName}")
    Response createGlobalBranch(
            @PathParam("branchName") String branchName,
            @QueryParam("from") String sourceBranch,
            @QueryParam("author") String author);
    
    @GET
    @Path("/branches/{branchName}")
    Response getGlobalBranch(@PathParam("branchName") String branchName);
    
    @POST
    @Path("/branches/{branchName}/merge")
    Response mergeGlobalBranch(
            @PathParam("branchName") String sourceBranch,
            @QueryParam("target") String targetBranch,
            @QueryParam("author") String author);
    
    @DELETE
    @Path("/branches/{branchName}")
    Response deleteGlobalBranch(@PathParam("branchName") String branchName);
    
    @GET
    @Path("/branches/history")
    Response getGlobalBranchHistory();
}
