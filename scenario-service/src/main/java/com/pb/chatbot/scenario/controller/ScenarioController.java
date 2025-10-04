package com.pb.chatbot.scenario.controller;

import com.pb.chatbot.scenario.model.ScenarioInfo;
import com.pb.chatbot.scenario.model.WorkspaceBranch;
import com.pb.chatbot.scenario.service.ScenarioManagementService;
import com.pb.chatbot.scenario.service.BranchService;
import com.pb.chatbot.scenario.service.WorkspaceBranchService;
import com.pb.chatbot.scenario.model.ScenarioBranch;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

@Path("/api/v1/scenarios")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ScenarioController {
    
    private static final Logger LOG = Logger.getLogger(ScenarioController.class);
    
    @Inject
    ScenarioManagementService scenarioService;
    
    @Inject
    BranchService branchService;
    
    @Inject
    WorkspaceBranchService workspaceBranchService;
    
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
    public Response getAllScenarios(@QueryParam("search") String search, @Context HttpHeaders headers) {
        // Проверяем header для ветки
        String branchName = headers.getHeaderString("X-Branch");
        
        if (branchName != null && !branchName.equals("main")) {
            // Получаем сценарии из глобальной ветки
            try {
                WorkspaceBranch branch = workspaceBranchService.getBranch(branchName);
                if (branch != null) {
                    List<Map<String, Object>> branchScenarios = new ArrayList<>();
                    
                    for (Map.Entry<String, Map<String, Object>> entry : branch.scenarios.entrySet()) {
                        Map<String, Object> scenarioData = entry.getValue();
                        Map<String, Object> scenarioInfo = new HashMap<>();
                        
                        scenarioInfo.put("id", entry.getKey());
                        scenarioInfo.put("name", scenarioData.get("name"));
                        scenarioInfo.put("description", scenarioData.get("description"));
                        scenarioInfo.put("version", scenarioData.get("version"));
                        scenarioInfo.put("language", scenarioData.get("language"));
                        scenarioInfo.put("is_entry_point", scenarioData.get("is_entry_point"));
                        scenarioInfo.put("scenario_data", scenarioData.get("scenario_data"));
                        scenarioInfo.put("_branch_info", Map.of(
                            "branch_name", branchName,
                            "source", "workspace_branch"
                        ));
                        branchScenarios.add(scenarioInfo);
                    }
                    
                    return Response.ok(Map.of(
                        "scenarios", branchScenarios,
                        "count", branchScenarios.size(),
                        "branch", branchName,
                        "timestamp", System.currentTimeMillis()
                    )).build();
                }
            } catch (Exception e) {
                LOG.errorf(e, "Error getting scenarios from branch %s", branchName);
            }
        }
        
        // Получаем сценарии из main ветки (обычная загрузка)
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
    public Response getScenario(@PathParam("id") String id, @Context HttpHeaders headers) {
        // Проверяем header для ветки
        String branchName = headers.getHeaderString("X-Branch");
        
        if (branchName != null && !branchName.equals("main")) {
            // Получаем сценарий из глобальной ветки
            Map<String, Object> scenarioFromBranch = workspaceBranchService.getScenarioFromBranch(id, branchName);
            if (scenarioFromBranch != null) {
                return Response.ok(Map.of(
                    "id", id,
                    "scenario_data", scenarioFromBranch,
                    "_branch_info", Map.of(
                        "branch_name", branchName,
                        "source", "workspace_branch"
                    )
                )).build();
            }
        }
        
        // Получаем сценарий из основного хранилища (main ветка)
        try {
            ScenarioInfo scenario = scenarioService.getScenario(id);
            if (scenario == null) {
                return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", "Scenario not found"))
                    .build();
            }
            
            return Response.ok(scenario).build();
        } catch (Exception e) {
            LOG.errorf(e, "Error getting scenario %s", id);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(Map.of("error", "Failed to get scenario"))
                .build();
        }
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
    public Response updateScenario(@PathParam("id") String id, ScenarioInfo scenario, @Context HttpHeaders headers) {
        // Проверяем header для ветки
        String branchName = headers.getHeaderString("X-Branch");
        
        if (branchName != null && !branchName.equals("main")) {
            // Сохраняем в глобальную ветку
            try {
                // Создаем полный объект сценария для ветки
                Map<String, Object> fullScenario = new HashMap<>();
                fullScenario.put("id", id);
                fullScenario.put("name", scenario.name);
                fullScenario.put("description", scenario.description);
                fullScenario.put("version", scenario.version);
                fullScenario.put("language", scenario.language);
                fullScenario.put("is_entry_point", scenario.isEntryPoint);
                fullScenario.put("scenario_data", scenario.scenarioData);
                
                workspaceBranchService.updateScenarioInBranch(id, branchName, fullScenario, "developer");
                
                return Response.ok(Map.of(
                    "id", id,
                    "message", "Scenario updated in branch " + branchName,
                    "branch", branchName
                )).build();
            } catch (Exception e) {
                LOG.errorf(e, "Error updating scenario in branch %s", branchName);
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Failed to update scenario in branch"))
                    .build();
            }
        }
        
        // Сохраняем в main ветку (обычное обновление)
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
    public Response getEntryPointScenario(@Context HttpHeaders headers) {
        // Проверяем header для ветки
        String branchName = headers.getHeaderString("X-Branch");
        
        if (branchName != null && !branchName.equals("main")) {
            // Получаем entry-point из глобальной ветки
            try {
                WorkspaceBranch branch = workspaceBranchService.getBranch(branchName);
                if (branch != null) {
                    // Ищем entry-point сценарий в ветке
                    for (Map.Entry<String, Map<String, Object>> entry : branch.scenarios.entrySet()) {
                        Map<String, Object> scenarioData = entry.getValue();
                        Boolean isEntryPoint = (Boolean) scenarioData.get("is_entry_point");
                        if (Boolean.TRUE.equals(isEntryPoint)) {
                            Map<String, Object> scenarioInfo = new HashMap<>();
                            scenarioInfo.put("id", entry.getKey());
                            scenarioInfo.put("name", scenarioData.get("name"));
                            scenarioInfo.put("description", scenarioData.get("description"));
                            scenarioInfo.put("version", scenarioData.get("version"));
                            scenarioInfo.put("language", scenarioData.get("language"));
                            scenarioInfo.put("is_entry_point", scenarioData.get("is_entry_point"));
                            scenarioInfo.put("scenario_data", scenarioData.get("scenario_data"));
                            scenarioInfo.put("_branch_info", Map.of(
                                "branch_name", branchName,
                                "source", "workspace_branch"
                            ));
                            return Response.ok(scenarioInfo).build();
                        }
                    }
                }
            } catch (Exception e) {
                LOG.errorf(e, "Error getting entry point from branch %s", branchName);
            }
        }
        
        // Получаем entry-point из main ветки (обычная загрузка)
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
