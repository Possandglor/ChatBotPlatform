package com.pb.chatbot.orchestrator.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

public class Scenario {
    
    @JsonProperty("id")
    public String id;
    
    @JsonProperty("name")
    public String name;
    
    @JsonProperty("version")
    public String version;
    
    @JsonProperty("language")
    public String language = "uk";
    
    @JsonProperty("start_node")
    public String startNode;
    
    @JsonProperty("nodes")
    public List<ScenarioBlock> nodes;
    
    @JsonProperty("edges")
    public List<Object> edges;
    
    @JsonProperty("context")
    public Map<String, Object> context;
    
    public List<Object> getEdges() {
        return edges;
    }
    
    public ScenarioBlock findNode(String nodeId) {
        return nodes.stream()
            .filter(node -> nodeId.equals(node.id))
            .findFirst()
            .orElse(null);
    }
}
