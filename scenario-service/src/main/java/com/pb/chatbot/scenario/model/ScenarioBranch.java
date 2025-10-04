package com.pb.chatbot.scenario.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.Map;

public class ScenarioBranch {
    
    @JsonProperty("branch_name")
    public String branchName;
    
    @JsonProperty("scenario_data")
    public Map<String, Object> scenarioData;
    
    @JsonProperty("base_commit")
    public String baseCommit;
    
    @JsonProperty("last_modified")
    public LocalDateTime lastModified;
    
    @JsonProperty("author")
    public String author;
    
    @JsonProperty("is_deleted")
    public boolean isDeleted = false;
    
    @JsonProperty("commit_message")
    public String commitMessage;
    
    public ScenarioBranch() {}
    
    public ScenarioBranch(String branchName, Map<String, Object> scenarioData, String baseCommit, String author) {
        this.branchName = branchName;
        this.scenarioData = scenarioData;
        this.baseCommit = baseCommit;
        this.author = author;
        this.lastModified = LocalDateTime.now();
    }
}
