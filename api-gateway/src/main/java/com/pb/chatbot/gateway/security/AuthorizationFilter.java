package com.pb.chatbot.gateway.security;

import com.pb.chatbot.gateway.security.pws.ChameleonService;
import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;

@Provider
public class AuthorizationFilter implements ContainerRequestFilter {
    
    private static final Logger LOG = Logger.getLogger(AuthorizationFilter.class);
    
    @Inject
    ChameleonService chameleonService;
    
    @Override
    public void filter(ContainerRequestContext requestContext) {
        // Временно отключено для тестирования
        return;
        
        /*
        String path = requestContext.getUriInfo().getPath();
        
        // Пропускаем health checks, swagger и создание сессий
        if (path.startsWith("health") || 
            path.startsWith("openapi") || 
            path.startsWith("swagger-ui") ||
            path.startsWith("api/v1/status") ||
            (path.startsWith("api/v1/chat/sessions") && "POST".equals(requestContext.getMethod()))) {
            return;
        }
        
        String sessionId = requestContext.getHeaderString("X-Session-ID");
        if (sessionId == null) {
            sessionId = requestContext.getUriInfo().getQueryParameters().getFirst("sessionId");
        }
        
        if (sessionId == null || sessionId.trim().isEmpty()) {
            LOG.warn("Missing session ID in request");
            requestContext.abortWith(
                Response.status(Response.Status.UNAUTHORIZED)
                    .entity("{\"error\":\"Missing session ID\"}")
                    .build()
            );
            return;
        }
        
        if (!chameleonService.validateSession(sessionId)) {
            LOG.warnf("Invalid session ID: %s", sessionId);
            requestContext.abortWith(
                Response.status(Response.Status.UNAUTHORIZED)
                    .entity("{\"error\":\"Invalid session\"}")
                    .build()
            );
            return;
        }
        
        // Добавляем пользователя в контекст
        String login = chameleonService.getLoginBySession(sessionId);
        requestContext.getHeaders().add("X-User-Login", login);
        */
    }
}
