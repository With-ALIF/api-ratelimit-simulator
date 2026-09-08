package com.runtime_crew.api_simulator.service;

import com.runtime_crew.api_simulator.model.*;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class RequestLogger {

    private final Map<String, RequestLog> logs = new HashMap<>();

    public void logRequest(ServiceRequest request) {
        logs
          .computeIfAbsent(request.getClientId(), RequestLog::new)
          .addRequest(request);
    }

    public RequestLog getLog(String clientId) {
        return logs.get(clientId);
    }

    public Collection<String> getClientIds() {
        return Collections.unmodifiableSet(logs.keySet());
    }

    public Map<String, RequestLog> getAllLogs() {
        return Collections.unmodifiableMap(logs);
    }

    public int getTotalRequestCount() {
        return logs.values().stream()
            .mapToInt(RequestLog::getRequestCount)
            .sum();
    }

    public boolean hasLogs() {
        return !logs.isEmpty();
    }

    public void clearClient(String clientId) {
        logs.remove(clientId);
    }

    public void clearAll() {
        logs.clear();
    }
}
