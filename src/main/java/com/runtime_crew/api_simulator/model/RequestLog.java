package com.runtime_crew.api_simulator.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RequestLog {

    private final String clientId;
    private final List<ServiceRequest> requests = new ArrayList<>();

    public RequestLog(String clientId) {
        this.clientId = clientId;
    }

    public void addRequest(ServiceRequest request) {
        if (request != null) {
            requests.add(request);
        }
    }

    public List<ServiceRequest> getRequests() {
        return Collections.unmodifiableList(requests);
    }

    public String getClientId() {
        return clientId;
    }

    public int getRequestCount() {
        return requests.size();
    }

    public boolean isEmpty() {
        return requests.isEmpty();
    }

    public void clear() {
        requests.clear();
    }

    @Override
    public String toString() {
        return String.format("RequestLog{clientId='%s', requestCount=%d}", clientId, requests.size());
    }
}