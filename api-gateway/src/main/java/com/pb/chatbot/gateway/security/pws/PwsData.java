package com.pb.chatbot.gateway.security.pws;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PwsData {
    @JsonProperty("bucket_name")
    private String bucketName;
    
    @JsonProperty("project_id")
    private String projectId;
    
    @JsonProperty("chameleon_url")
    private String chameleonUrl;

    public PwsData() {}

    public String getBucketName() {
        return bucketName;
    }

    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public String getChameleonUrl() {
        return chameleonUrl;
    }

    public void setChameleonUrl(String chameleonUrl) {
        this.chameleonUrl = chameleonUrl;
    }
}
