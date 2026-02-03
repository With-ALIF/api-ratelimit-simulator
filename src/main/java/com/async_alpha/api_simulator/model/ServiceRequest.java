package com.async_alpha.api_simulator.model;

import java.time.LocalDateTime;

public class ServiceRequest {

    private final String clientId;
    private final RequestType requestType;
    private final LocalDateTime timestamp;

    public ServiceRequest(String clientId, RequestType requestType, LocalDateTime timestamp) {
        this.clientId = clientId;
        this.requestType = requestType;
        this.timestamp = timestamp;
    }

    public String getClientId() {
        return clientId;
    }

    public RequestType getRequestType() {
        return requestType;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }
}
