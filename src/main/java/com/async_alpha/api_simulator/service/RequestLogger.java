package com.async_alpha.api_simulator.service;

import com.async_alpha.api_simulator.model.*;

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
}
