package com.async_alpha.api_simulator.model;

import java.util.ArrayList;
import java.util.List;

public class RequestLog {

    private final String clientId;
    private final List<ServiceRequest> requests = new ArrayList<>();

    public RequestLog(String clientId) {
        this.clientId = clientId;
    }

    public void addRequest(ServiceRequest request) {
        requests.add(request);
    }

    public List<ServiceRequest> getRequests() {
        return requests;
    }

    public String getClientId() {
        return clientId;
    }
}