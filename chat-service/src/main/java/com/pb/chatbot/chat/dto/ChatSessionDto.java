package com.pb.chatbot.chat.dto;

import java.time.LocalDateTime;
import java.util.Map;

public class ChatSessionDto {
    public String sessionId;
    public String userId;
    public String scenarioId;
    public String status;
    public LocalDateTime startTime;
    public LocalDateTime endTime;
    public int messageCount;
    public String lastMessage;
    public Map<String, Object> context;
    
    public ChatSessionDto() {}
    
    public ChatSessionDto(String sessionId, String status) {
        this.sessionId = sessionId;
        this.status = status;
        this.startTime = LocalDateTime.now();
        this.messageCount = 0;
    }
}
