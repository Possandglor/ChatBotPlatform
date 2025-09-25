package com.pb.chatbot.orchestrator.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

public class ScenarioBlock {
    
    @JsonProperty("id")
    public String id;
    
    @JsonProperty("type")
    public String type; // announce, ask, parse, api-request, llm-call, wait, condition, sub-flow
    
    @JsonProperty("parameters")
    public Map<String, Object> parameters;
    
    @JsonProperty("next_nodes")
    public List<String> nextNodes;
    
    @JsonProperty("conditions")
    public Map<String, String> conditions;
    
    public ScenarioBlock() {}
    
    public ScenarioBlock(String id, String type) {
        this.id = id;
        this.type = type;
    }
}
