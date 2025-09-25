package com.pb.chatbot.gateway.security.pws;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

@ApplicationScoped
public class ChameleonService {
    
    private static final Logger LOG = Logger.getLogger(ChameleonService.class);
    
    @Inject
    @RestClient
    ChameleonClient chameleonClient;
    
    public String getLoginBySession(String sessionId) {
        try {
            LOG.debugf("Getting login for session: %s", sessionId);
            // Заглушка для тестирования
            if ("test-session".equals(sessionId)) {
                return "test-user";
            }
            
            // TODO: Реальная интеграция с Chameleon
            // return chameleonClient.getLoginBySession(sessionId);
            return null;
        } catch (Exception e) {
            LOG.errorf(e, "Error getting login for session: %s", sessionId);
            return null;
        }
    }
    
    public boolean validateSession(String sessionId) {
        String login = getLoginBySession(sessionId);
        return login != null && !login.trim().isEmpty();
    }
}
