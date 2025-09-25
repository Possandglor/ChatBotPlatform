package com.pb.chatbot.nlu.model;

import java.util.List;
import java.util.Map;

public class NluResult {
    public String intent;
    public double confidence;
    public List<Entity> entities;
    public Map<String, Object> context;
    public String suggested_scenario;
    
    public String getSuggestedScenario() {
        return suggested_scenario;
    }
    
    public void setSuggestedScenario(String suggested_scenario) {
        this.suggested_scenario = suggested_scenario;
    }
    
    public static class Entity {
        public String type;
        public String value;
        public double confidence;
        public int start;
        public int end;
    }
}
