package com.pb.chatbot.scenario.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class MergeResult {
    
    @JsonProperty("success")
    public boolean success;
    
    @JsonProperty("message")
    public String message;
    
    @JsonProperty("conflicts")
    public List<String> conflicts;
    
    public MergeResult() {}
    
    public MergeResult(boolean success, String message, List<String> conflicts) {
        this.success = success;
        this.message = message;
        this.conflicts = conflicts;
    }
}
