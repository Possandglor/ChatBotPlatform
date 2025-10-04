package com.pb.chatbot.scenario.service;

import com.pb.chatbot.scenario.model.ScenarioInfo;
import com.pb.chatbot.scenario.model.WorkspaceBranch;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class ScenarioManagementService {
    
    private static final Logger LOG = Logger.getLogger(ScenarioManagementService.class);
    private final Map<String, ScenarioInfo> scenarios = new ConcurrentHashMap<>();
    
    @Inject
    BranchService branchService;
    
    @Inject
    WorkspaceBranchService workspaceBranchService;
    
    public ScenarioManagementService() {
        // Создаем тестовые сценарии
        createTestScenarios();
        
        // Инициализируем main ветки для существующих сценариев
        initializeMainBranches();
    }
    
    private void initializeMainBranches() {
        // Отложенная инициализация через PostConstruct
        // Пока что оставим пустым, инициализация будет при первом обращении
    }
    
    public List<ScenarioInfo> getAllScenarios() {
        return new ArrayList<>(scenarios.values());
    }
    
    public ScenarioInfo getScenario(String id) {
        ScenarioInfo scenario = scenarios.get(id);
        
        // Ленивая инициализация main ветки
        if (scenario != null && branchService != null) {
            branchService.initializeMainBranch(id, scenario.scenarioData);
        }
        
        return scenario;
    }
    
    public ScenarioInfo createScenario(ScenarioInfo scenario) {
        if (scenario.id == null) {
            scenario.id = UUID.randomUUID().toString();
        }
        
        scenario.createdAt = LocalDateTime.now();
        scenario.updatedAt = LocalDateTime.now();
        
        scenarios.put(scenario.id, scenario);
        
        // Инициализируем main ветку для нового сценария
        if (branchService != null && scenario.scenarioData != null) {
            branchService.initializeMainBranch(scenario.id, scenario.scenarioData);
        }
        
        LOG.infof("Created scenario: %s", scenario.id);
        return scenario;
    }
    
    public ScenarioInfo updateScenario(String id, ScenarioInfo updatedScenario) {
        ScenarioInfo existing = scenarios.get(id);
        if (existing == null) {
            return null;
        }
        
        updatedScenario.id = id;
        updatedScenario.createdAt = existing.createdAt;
        updatedScenario.updatedAt = LocalDateTime.now();
        
        scenarios.put(id, updatedScenario);
        LOG.infof("Updated scenario: %s", updatedScenario.name);
        
        return updatedScenario;
    }
    
    public boolean deleteScenario(String id) {
        ScenarioInfo removed = scenarios.remove(id);
        if (removed != null) {
            LOG.infof("Deleted scenario: %s", removed.name);
            return true;
        }
        return false;
    }
    
    public List<ScenarioInfo> searchScenarios(String query) {
        if (query == null || query.trim().isEmpty()) {
            return getAllScenarios();
        }
        
        String lowerQuery = query.toLowerCase();
        return scenarios.values().stream()
            .filter(s -> s.name.toLowerCase().contains(lowerQuery) ||
                        (s.description != null && s.description.toLowerCase().contains(lowerQuery)) ||
                        (s.tags != null && s.tags.stream().anyMatch(tag -> tag.toLowerCase().contains(lowerQuery))))
            .toList();
    }
    
    public ScenarioInfo getEntryPointScenario() {
        return scenarios.values().stream()
            .filter(s -> Boolean.TRUE.equals(s.isEntryPoint) && Boolean.TRUE.equals(s.isActive))
            .findFirst()
            .orElse(null);
    }
    
    private void createTestScenarios() {
        // Тестовый сценарий приветствия (точка входа)
        ScenarioInfo greeting = new ScenarioInfo("greeting-001", "Приветствие пользователя");
        greeting.description = "Простой сценарий приветствия с запросом имени";
        greeting.category = "greeting";
        greeting.tags = List.of("приветствие", "знакомство");
        greeting.isEntryPoint = true; // Устанавливаем как точку входа
        greeting.scenarioData = Map.of(
            "start_node", "welcome",
            "nodes", List.of(
                Map.of(
                    "id", "welcome",
                    "type", "announce",
                    "parameters", Map.of("message", "Привет! Как дела?"),
                    "next_nodes", List.of("ask_name")
                ),
                Map.of(
                    "id", "ask_name", 
                    "type", "ask",
                    "parameters", Map.of("question", "Как вас зовут?"),
                    "next_nodes", List.of("greet_user")
                ),
                Map.of(
                    "id", "greet_user",
                    "type", "announce", 
                    "parameters", Map.of("message", "Приятно познакомиться, {last_answer}! Чем могу помочь?"),
                    "next_nodes", List.of()
                )
            )
        );
        scenarios.put(greeting.id, greeting);
        
        // Тестовый сценарий помощи
        ScenarioInfo help = new ScenarioInfo("help-001", "Помощь пользователю");
        help.description = "Сценарий предоставления помощи";
        help.category = "support";
        help.tags = List.of("помощь", "поддержка");
        help.scenarioData = Map.of(
            "start_node", "help_menu",
            "nodes", List.of(
                Map.of(
                    "id", "help_menu",
                    "type", "announce", 
                    "parameters", Map.of("message", "Чем могу помочь?"),
                    "next_nodes", List.of()
                )
            )
        );
        scenarios.put(help.id, help);
        
        // Создаем тестовые ветки
        createTestBranches();
        
        LOG.info("Created test scenarios");
    }
    
    private void createTestBranches() {
        try {
            // Создаем ветку test с измененным сообщением
            WorkspaceBranch testBranch = workspaceBranchService.createBranch("test", "main", "system");
            if (testBranch != null && testBranch.scenarios.containsKey("greeting-001")) {
                Map<String, Object> testScenario = new HashMap<>(testBranch.scenarios.get("greeting-001"));
                Map<String, Object> scenarioData = (Map<String, Object>) testScenario.get("scenario_data");
                List<Map<String, Object>> nodes = (List<Map<String, Object>>) scenarioData.get("nodes");
                
                // Изменяем сообщение в первом узле
                for (Map<String, Object> node : nodes) {
                    if ("welcome".equals(node.get("id"))) {
                        Map<String, Object> parameters = (Map<String, Object>) node.get("parameters");
                        parameters.put("message", "Привет! Как дела? фыа");
                        break;
                    }
                }
                
                testBranch.scenarios.put("greeting-001", testScenario);
                LOG.info("Updated test branch with custom message");
            }
            
            // Создаем ветку feature-new с эмодзи
            WorkspaceBranch featureBranch = workspaceBranchService.createBranch("feature-new", "main", "system");
            if (featureBranch != null && featureBranch.scenarios.containsKey("greeting-001")) {
                Map<String, Object> featureScenario = new HashMap<>(featureBranch.scenarios.get("greeting-001"));
                Map<String, Object> scenarioData = (Map<String, Object>) featureScenario.get("scenario_data");
                List<Map<String, Object>> nodes = (List<Map<String, Object>>) scenarioData.get("nodes");
                
                // Изменяем сообщение в первом узле
                for (Map<String, Object> node : nodes) {
                    if ("welcome".equals(node.get("id"))) {
                        Map<String, Object> parameters = (Map<String, Object>) node.get("parameters");
                        parameters.put("message", "🚀 Привет из ветки feature-new!");
                        break;
                    }
                }
                
                featureBranch.scenarios.put("greeting-001", featureScenario);
                LOG.info("Updated feature-new branch with custom message");
            }
            
        } catch (Exception e) {
            LOG.errorf(e, "Failed to create test branches");
        }
    }
}
