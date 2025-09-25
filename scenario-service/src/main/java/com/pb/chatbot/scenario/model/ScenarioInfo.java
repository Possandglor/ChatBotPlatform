package com.pb.chatbot.scenario.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class ScenarioInfo {
    
    @JsonProperty("id")
    public String id;
    
    @JsonProperty("name")
    public String name;
    
    @JsonProperty("description")
    public String description;
    
    @JsonProperty("version")
    public String version = "1.0";
    
    @JsonProperty("language")
    public String language = "uk";
    
    @JsonProperty("category")
    public String category;
    
    @JsonProperty("tags")
    public List<String> tags;
    
    @JsonProperty("is_active")
    public Boolean isActive = true;
    
    @JsonProperty("is_entry_point")
    public Boolean isEntryPoint = false;
    
    @JsonProperty("created_at")
    public LocalDateTime createdAt;
    
    @JsonProperty("updated_at")
    public LocalDateTime updatedAt;
    
    @JsonProperty("created_by")
    public String createdBy;
    
    @JsonProperty("scenario_data")
    public Map<String, Object> scenarioData;
    
    public ScenarioInfo() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    public ScenarioInfo(String id, String name) {
        this();
        this.id = id;
        this.name = name;
    }
}
