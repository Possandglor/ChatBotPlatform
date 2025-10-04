package com.pb.chatbot.scenario.service;

import com.pb.chatbot.scenario.model.*;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class BranchService {
    
    private static final Logger LOG = Logger.getLogger(BranchService.class);
    
    // В памяти для MVP, потом в БД
    private final Map<String, ScenarioWithBranches> scenarioBranches = new ConcurrentHashMap<>();
    
    /**
     * Создает новую ветку из существующей
     */
    public ScenarioBranch createBranch(String scenarioId, String branchName, String sourceBranch, String author) {
        LOG.infof("Creating branch %s from %s for scenario %s", branchName, sourceBranch, scenarioId);
        
        ScenarioWithBranches scenarioWithBranches = scenarioBranches.computeIfAbsent(scenarioId, 
                k -> new ScenarioWithBranches(scenarioId));
        
        // Если исходная ветка main и она не существует, попробуем инициализировать
        if ("main".equals(sourceBranch) && !scenarioWithBranches.branches.containsKey("main")) {
            LOG.infof("Main branch not found, trying to initialize for scenario: %s", scenarioId);
            // Попробуем получить данные сценария из основного сервиса
            // Пока создадим пустую main ветку
            ScenarioBranch mainBranch = new ScenarioBranch("main", new HashMap<>(), "initial", "system");
            scenarioWithBranches.addBranch("main", mainBranch);
        }
        
        // Получаем исходную ветку (по умолчанию main)
        ScenarioBranch sourceScenarioBranch = scenarioWithBranches.getBranch(sourceBranch != null ? sourceBranch : "main");
        
        if (sourceScenarioBranch == null) {
            throw new RuntimeException("Source branch not found: " + sourceBranch);
        }
        
        // Создаем новую ветку как копию исходной
        ScenarioBranch newBranch = new ScenarioBranch(
                branchName,
                deepCopy(sourceScenarioBranch.scenarioData),
                sourceBranch + "@" + LocalDateTime.now(),
                author
        );
        
        scenarioWithBranches.addBranch(branchName, newBranch);
        
        return newBranch;
    }
    
    /**
     * Получает ветку сценария
     */
    public ScenarioBranch getBranch(String scenarioId, String branchName) {
        ScenarioWithBranches scenarioWithBranches = scenarioBranches.get(scenarioId);
        if (scenarioWithBranches == null) {
            return null;
        }
        
        return scenarioWithBranches.getBranch(branchName);
    }
    
    /**
     * Обновляет ветку
     */
    public void updateBranch(String scenarioId, String branchName, Map<String, Object> scenarioData, String author) {
        LOG.infof("Updating branch %s for scenario %s", branchName, scenarioId);
        
        ScenarioWithBranches scenarioWithBranches = scenarioBranches.get(scenarioId);
        if (scenarioWithBranches == null) {
            throw new RuntimeException("Scenario not found: " + scenarioId);
        }
        
        ScenarioBranch branch = scenarioWithBranches.getBranch(branchName);
        if (branch == null) {
            throw new RuntimeException("Branch not found: " + branchName);
        }
        
        branch.scenarioData = scenarioData;
        branch.lastModified = LocalDateTime.now();
        branch.author = author;
        
        scenarioWithBranches.updateBranch(branchName, branch);
    }
    
    /**
     * Получает список веток
     */
    public List<String> getBranches(String scenarioId) {
        ScenarioWithBranches scenarioWithBranches = scenarioBranches.get(scenarioId);
        if (scenarioWithBranches == null) {
            return Arrays.asList("main");
        }
        
        return new ArrayList<>(scenarioWithBranches.branches.keySet());
    }
    
    /**
     * Сливает ветку в целевую
     */
    public MergeResult mergeBranch(String scenarioId, String sourceBranch, String targetBranch, String author) {
        LOG.infof("Merging branch %s into %s for scenario %s", sourceBranch, targetBranch, scenarioId);
        
        ScenarioWithBranches scenarioWithBranches = scenarioBranches.get(scenarioId);
        if (scenarioWithBranches == null) {
            throw new RuntimeException("Scenario not found: " + scenarioId);
        }
        
        ScenarioBranch source = scenarioWithBranches.getBranch(sourceBranch);
        ScenarioBranch target = scenarioWithBranches.getBranch(targetBranch);
        
        if (source == null || target == null) {
            throw new RuntimeException("Branch not found");
        }
        
        // Простое слияние - заменяем целевую ветку исходной
        // TODO: Реализовать детекцию конфликтов
        target.scenarioData = deepCopy(source.scenarioData);
        target.lastModified = LocalDateTime.now();
        target.author = author;
        
        scenarioWithBranches.mergeBranch(sourceBranch, targetBranch, author);
        
        return new MergeResult(true, "Merged successfully", null);
    }
    
    /**
     * Удаляет ветку
     */
    public void deleteBranch(String scenarioId, String branchName) {
        LOG.infof("Deleting branch %s for scenario %s", branchName, scenarioId);
        
        if ("main".equals(branchName)) {
            throw new RuntimeException("Cannot delete main branch");
        }
        
        ScenarioWithBranches scenarioWithBranches = scenarioBranches.get(scenarioId);
        if (scenarioWithBranches != null) {
            ScenarioBranch branch = scenarioWithBranches.branches.get(branchName);
            if (branch != null) {
                branch.isDeleted = true;
                scenarioWithBranches.addHistoryEntry("DELETE_BRANCH", branchName, "system", "Deleted branch: " + branchName);
            }
        }
    }
    
    /**
     * Получает историю изменений
     */
    public List<BranchHistoryEntry> getHistory(String scenarioId) {
        ScenarioWithBranches scenarioWithBranches = scenarioBranches.get(scenarioId);
        if (scenarioWithBranches == null) {
            return new ArrayList<>();
        }
        
        return scenarioWithBranches.history;
    }
    
    /**
     * Инициализирует main ветку для сценария
     */
    public void initializeMainBranch(String scenarioId, Map<String, Object> scenarioData) {
        ScenarioWithBranches scenarioWithBranches = scenarioBranches.computeIfAbsent(scenarioId, 
                k -> new ScenarioWithBranches(scenarioId));
        
        if (!scenarioWithBranches.branches.containsKey("main")) {
            ScenarioBranch mainBranch = new ScenarioBranch("main", scenarioData, "initial", "system");
            scenarioWithBranches.addBranch("main", mainBranch);
            LOG.infof("Initialized main branch for scenario: %s", scenarioId);
        } else {
            // Обновляем данные main ветки если они изменились
            ScenarioBranch mainBranch = scenarioWithBranches.branches.get("main");
            if (mainBranch.scenarioData == null || mainBranch.scenarioData.isEmpty()) {
                mainBranch.scenarioData = scenarioData;
                mainBranch.lastModified = LocalDateTime.now();
                LOG.infof("Updated main branch data for scenario: %s", scenarioId);
            }
        }
    }
    
    @SuppressWarnings("unchecked")
    private Map<String, Object> deepCopy(Map<String, Object> original) {
        if (original == null) return null;
        
        Map<String, Object> copy = new HashMap<>();
        for (Map.Entry<String, Object> entry : original.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof Map) {
                copy.put(entry.getKey(), deepCopy((Map<String, Object>) value));
            } else if (value instanceof List) {
                copy.put(entry.getKey(), new ArrayList<>((List<?>) value));
            } else {
                copy.put(entry.getKey(), value);
            }
        }
        return copy;
    }
}
