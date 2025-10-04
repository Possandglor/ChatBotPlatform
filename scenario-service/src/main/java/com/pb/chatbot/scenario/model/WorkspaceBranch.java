package com.pb.chatbot.scenario.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.HashMap;

public class WorkspaceBranch {
    
    @JsonProperty("branch_name")
    public String branchName;
    
    @JsonProperty("scenarios")
    public Map<String, Map<String, Object>> scenarios = new HashMap<>();
    
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
    
    public WorkspaceBranch() {}
    
    public WorkspaceBranch(String branchName, Map<String, Map<String, Object>> scenarios, String baseCommit, String author) {
        this.branchName = branchName;
        this.scenarios = scenarios != null ? scenarios : new HashMap<>();
        this.baseCommit = baseCommit;
        this.author = author;
        this.lastModified = LocalDateTime.now();
    }
}
