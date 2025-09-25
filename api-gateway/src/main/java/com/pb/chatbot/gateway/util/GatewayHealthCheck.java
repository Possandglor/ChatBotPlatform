package com.pb.chatbot.gateway.util;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Readiness;

@Readiness
@ApplicationScoped
public class GatewayHealthCheck implements HealthCheck {
    
    @Override
    public HealthCheckResponse call() {
        return HealthCheckResponse.named("api-gateway")
            .status(true)
            .withData("service", "api-gateway")
            .withData("status", "UP")
            .withData("timestamp", System.currentTimeMillis())
            .build();
    }
}
