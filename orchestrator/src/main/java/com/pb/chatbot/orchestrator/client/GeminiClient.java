package com.pb.chatbot.orchestrator.client;

import com.google.cloud.vertexai.VertexAI;
import com.google.cloud.vertexai.api.GenerateContentResponse;
import com.google.cloud.vertexai.generativeai.GenerativeModel;
import com.google.cloud.vertexai.generativeai.ResponseHandler;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import io.quarkus.runtime.StartupEvent;

/**
 * Gemini client using Vertex AI SDK with ADC authentication
 */
@ApplicationScoped
public class GeminiClient {
    
    private static final Logger LOG = Logger.getLogger(GeminiClient.class);
    
    @ConfigProperty(name = "google.cloud.project-id", defaultValue = "itt-cc-miu")
    String projectId;
    
    @ConfigProperty(name = "gemini.region", defaultValue = "us-central1")
    String region;
    
    @ConfigProperty(name = "gemini.model", defaultValue = "gemini-2.0-flash-001")
    String modelName;
    
    private VertexAI vertexAI;
    private GenerativeModel model;
    
    @PostConstruct
    void init() {
        try {
            // Initialize Vertex AI with ADC (Application Default Credentials)
            this.vertexAI = new VertexAI(projectId, region);
            this.model = new GenerativeModel(modelName, vertexAI);
            
            LOG.infof("Initialized Vertex AI Gemini client - Project: %s, Region: %s, Model: %s", 
                     projectId, region, modelName);
        } catch (Exception e) {
            LOG.errorf(e, "Failed to initialize Vertex AI client");
            // Не бросаем исключение, чтобы не сломать запуск приложения
        }
    }
    
    void onStart(@Observes StartupEvent ev) {
        LOG.infof("GeminiClient startup check - initialized: %s", isInitialized());
    }
    
    @PreDestroy
    void cleanup() {
        if (vertexAI != null) {
            try {
                vertexAI.close();
                LOG.info("Vertex AI client closed");
            } catch (Exception e) {
                LOG.warnf(e, "Error closing Vertex AI client");
            }
        }
    }
    
    /**
     * Generate content using Gemini model
     * 
     * @param prompt The prompt to send to Gemini
     * @return Generated response text
     */
    public String generateContent(String prompt) {
        if (!isInitialized()) {
            LOG.errorf("GeminiClient not initialized - cannot generate content");
            throw new RuntimeException("GeminiClient not initialized");
        }
        
        try {
            LOG.debugf("Sending prompt to Gemini: %s", prompt.substring(0, Math.min(100, prompt.length())));
            
            GenerateContentResponse response = model.generateContent(prompt);
            String responseText = ResponseHandler.getText(response);
            
            LOG.debugf("Received response from Gemini: %s", responseText.substring(0, Math.min(200, responseText.length())));
            
            return responseText;
            
        } catch (Exception e) {
            LOG.errorf(e, "Error calling Gemini API");
            throw new RuntimeException("Gemini API call failed", e);
        }
    }
    
    /**
     * Check if the client is properly initialized
     */
    public boolean isInitialized() {
        return vertexAI != null && model != null;
    }
}
