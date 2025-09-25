package com.pb.chatbot.chat.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ChatMessage {
    
    @JsonProperty("session_id")
    public String sessionId;
    
    @JsonProperty("message")
    public String message;
    
    @JsonProperty("type")
    public String type; // user, bot, system
    
    @JsonProperty("timestamp")
    public Long timestamp;
    
    @JsonProperty("metadata")
    public Object metadata;
    
    public ChatMessage() {}
    
    public ChatMessage(String sessionId, String message, String type) {
        this.sessionId = sessionId;
        this.message = message;
        this.type = type;
        this.timestamp = System.currentTimeMillis();
    }
    
    public static ChatMessage userMessage(String sessionId, String message) {
        return new ChatMessage(sessionId, message, "user");
    }
    
    public static ChatMessage botMessage(String sessionId, String message) {
        return new ChatMessage(sessionId, message, "bot");
    }
    
    public static ChatMessage systemMessage(String sessionId, String message) {
        return new ChatMessage(sessionId, message, "system");
    }
}
