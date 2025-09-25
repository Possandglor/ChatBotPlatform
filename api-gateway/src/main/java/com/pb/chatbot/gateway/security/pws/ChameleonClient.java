package com.pb.chatbot.gateway.security.pws;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(configKey = "chameleon")
public interface ChameleonClient {
    
    @GET
    @Path("/api/session/login")
    String getLoginBySession(@QueryParam("sid") String sessionId);
}
