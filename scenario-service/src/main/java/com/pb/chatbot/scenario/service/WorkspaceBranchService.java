package com.pb.chatbot.scenario.service;

import com.pb.chatbot.scenario.model.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.time.LocalDateTime;
import java.util.*;

@ApplicationScoped
public class WorkspaceBranchService {
    
    private static final Logger LOG = Logger.getLogger(WorkspaceBranchService.class);
    
    private final WorkspaceWithBranches workspace = new WorkspaceWithBranches();
    
    @Inject
    ScenarioManagementService scenarioService;
    
    /**
     * Создает новую глобальную ветку
     */
    public WorkspaceBranch createBranch(String branchName, String sourceBranch, String author) {
        LOG.infof("Creating global branch %s from %s", branchName, sourceBranch);
        
        // Получаем исходную ветку
        WorkspaceBranch sourceWorkspaceBranch = workspace.getBranch(sourceBranch);
        
        // Если исходная ветка main и не существует, инициализируем
        if ("main".equals(sourceBranch) && sourceWorkspaceBranch == null) {
            initializeMainBranch();
            sourceWorkspaceBranch = workspace.getBranch("main");
        }
        
        if (sourceWorkspaceBranch == null) {
            throw new RuntimeException("Source branch not found: " + sourceBranch);
        }
        
        // Создаем новую ветку как копию всех сценариев
        Map<String, Map<String, Object>> copiedScenarios = deepCopyScenarios(sourceWorkspaceBranch.scenarios);
        
        WorkspaceBranch newBranch = new WorkspaceBranch(
                branchName,
                copiedScenarios,
                sourceBranch + "@" + LocalDateTime.now(),
                author
        );
        
        workspace.addBranch(branchName, newBranch);
        
        return newBranch;
    }
    
    /**
     * Получает список веток
     */
    public List<String> getBranches() {
        if (workspace.branches.isEmpty()) {
            initializeMainBranch();
        }
        return new ArrayList<>(workspace.branches.keySet());
    }
    
    /**
     * Получает ветку
     */
    public WorkspaceBranch getBranch(String branchName) {
        if (workspace.branches.isEmpty()) {
            initializeMainBranch();
        }
        return workspace.getBranch(branchName);
    }
    
    /**
     * Получает сценарий из ветки
     */
    public Map<String, Object> getScenarioFromBranch(String scenarioId, String branchName) {
        WorkspaceBranch branch = getBranch(branchName);
        if (branch == null) {
            return null;
        }
        
        return branch.scenarios.get(scenarioId);
    }
    
    /**
     * Обновляет сценарий в ветке
     */
    public void updateScenarioInBranch(String scenarioId, String branchName, Map<String, Object> fullScenarioData, String author) {
        LOG.infof("Updating scenario %s in branch %s", scenarioId, branchName);
        
        WorkspaceBranch branch = getBranch(branchName);
        if (branch == null) {
            throw new RuntimeException("Branch not found: " + branchName);
        }
        
        // Сохраняем полные данные сценария
        branch.scenarios.put(scenarioId, fullScenarioData);
        branch.lastModified = LocalDateTime.now();
        branch.author = author;
        
        workspace.updateBranch(branchName, branch);
    }
    
    /**
     * Сливает ветку
     */
    public MergeResult mergeBranch(String sourceBranch, String targetBranch, String author) {
        LOG.infof("Merging branch %s into %s", sourceBranch, targetBranch);
        
        WorkspaceBranch source = workspace.getBranch(sourceBranch);
        WorkspaceBranch target = workspace.getBranch(targetBranch);
        
        if (source == null || target == null) {
            throw new RuntimeException("Branch not found");
        }
        
        // Простое слияние - заменяем все сценарии в целевой ветке
        target.scenarios = deepCopyScenarios(source.scenarios);
        target.lastModified = LocalDateTime.now();
        target.author = author;
        
        workspace.mergeBranch(sourceBranch, targetBranch, author);
        
        return new MergeResult(true, "Merged successfully", null);
    }
    
    /**
     * Удаляет ветку
     */
    public void deleteBranch(String branchName) {
        LOG.infof("Deleting branch %s", branchName);
        
        if ("main".equals(branchName)) {
            throw new RuntimeException("Cannot delete main branch");
        }
        
        WorkspaceBranch branch = workspace.branches.get(branchName);
        if (branch != null) {
            branch.isDeleted = true;
            workspace.addHistoryEntry("DELETE_BRANCH", branchName, "system", "Deleted branch: " + branchName);
        }
    }
    
    /**
     * Получает историю изменений
     */
    public List<BranchHistoryEntry> getHistory() {
        return workspace.history;
    }
    
    /**
     * Инициализирует main ветку со всеми сценариями
     */
    private void initializeMainBranch() {
        if (workspace.branches.containsKey("main")) {
            return;
        }
        
        LOG.info("Initializing main branch with all scenarios");
        
        // Получаем все сценарии из основного сервиса
        Map<String, Map<String, Object>> allScenarios = new HashMap<>();
        
        try {
            List<ScenarioInfo> scenarios = scenarioService.getAllScenarios();
            for (ScenarioInfo scenario : scenarios) {
                Map<String, Object> scenarioMap = new HashMap<>();
                scenarioMap.put("id", scenario.id);
                scenarioMap.put("name", scenario.name);
                scenarioMap.put("description", scenario.description);
                scenarioMap.put("version", scenario.version);
                scenarioMap.put("language", scenario.language);
                scenarioMap.put("is_entry_point", scenario.isEntryPoint);
                
                if (scenario.scenarioData != null) {
                    scenarioMap.put("scenario_data", scenario.scenarioData);
                } else {
                    scenarioMap.put("scenario_data", new HashMap<>());
                }
                
                allScenarios.put(scenario.id, scenarioMap);
            }
        } catch (Exception e) {
            LOG.warnf("Could not load scenarios for main branch: %s", e.getMessage());
        }
        
        WorkspaceBranch mainBranch = new WorkspaceBranch("main", allScenarios, "initial", "system");
        workspace.addBranch("main", mainBranch);
    }
    
    @SuppressWarnings("unchecked")
    private Map<String, Map<String, Object>> deepCopyScenarios(Map<String, Map<String, Object>> original) {
        if (original == null) return new HashMap<>();
        
        Map<String, Map<String, Object>> copy = new HashMap<>();
        for (Map.Entry<String, Map<String, Object>> entry : original.entrySet()) {
            copy.put(entry.getKey(), deepCopyMap(entry.getValue()));
        }
        return copy;
    }
    
    @SuppressWarnings("unchecked")
    private Map<String, Object> deepCopyMap(Map<String, Object> original) {
        if (original == null) return new HashMap<>();
        
        Map<String, Object> copy = new HashMap<>();
        for (Map.Entry<String, Object> entry : original.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof Map) {
                copy.put(entry.getKey(), deepCopyMap((Map<String, Object>) value));
            } else if (value instanceof List) {
                copy.put(entry.getKey(), new ArrayList<>((List<?>) value));
            } else {
                copy.put(entry.getKey(), value);
            }
        }
        return copy;
    }
}
