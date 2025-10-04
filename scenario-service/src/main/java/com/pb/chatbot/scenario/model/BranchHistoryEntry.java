package com.pb.chatbot.scenario.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

public class BranchHistoryEntry {
    
    @JsonProperty("action")
    public String action;
    
    @JsonProperty("branch")
    public String branch;
    
    @JsonProperty("author")
    public String author;
    
    @JsonProperty("message")
    public String message;
    
    @JsonProperty("timestamp")
    public LocalDateTime timestamp;
    
    public BranchHistoryEntry() {}
    
    public BranchHistoryEntry(String action, String branch, String author, String message) {
        this.action = action;
        this.branch = branch;
        this.author = author;
        this.message = message;
        this.timestamp = LocalDateTime.now();
    }
}
